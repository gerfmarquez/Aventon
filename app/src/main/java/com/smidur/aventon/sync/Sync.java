package com.smidur.aventon.sync;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.google.android.gms.maps.model.LatLng;
import com.smidur.aventon.exceptions.TokenInvalidException;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.http.HttpWrapper;
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncPassenger;
import com.smidur.aventon.utilities.GpsUtil;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Created by marqueg on 4/16/17.
 */

public class Sync {

    String TAG = getClass().getSimpleName();



    private static final int RETRY_SYNC_AVAILABLE_RIDES = 3000;//3 sec
    //todo update faster when driver is picking up somebody
    private static final int SYNC_DRIVER_LOCATION_RATE = 5 * 1000;//40 sec


    Thread syncAvailableRidesThread;
    Thread syncDriverLocationThread;
    Thread syncSchedulePickupThread;
    HttpController syncAvailableDriversController;
    HttpController syncSchedulePickupController;


    SyncPassenger syncPassenger;

    Stack<SyncLocation> driverLocations;


    private static Sync instance;
    private final Context context;
    private Handler handler;

    public Sync(Context context) {
        this.context = context;
    }
    public static Sync i(Context context){
        if(instance==null) {
            instance = new Sync(context);
            instance.driverLocations = new Stack<>();
            instance.handler = new Handler(context.getMainLooper());
        }
        return instance;
    }

    public void startSyncRideInfo() {
        //sync rides info
        handler.post(syncAvailableRides);

    }
    public void stopSyncRideInfo() {
        //stop sync ride infos
        closeConnectionIfOpen();

        if(syncAvailableRides!=null)
            handler.removeCallbacks(syncAvailableRides);
    }
    public void startSyncDriverLocation() {
        //sync location
        Location location = GpsUtil.getUserLocation(context);
        pushDriverLocationToSync(location);
        handler.post(syncDriverLocation);
    }
    public void stopSyncDriverLocation() {
        //stop sync location
        handler.removeCallbacks(syncDriverLocation);
        if(syncDriverLocationThread!=null)
            syncDriverLocationThread.interrupt();

    }

    public void startSyncSchedulePickup(SyncPassenger syncPassenger) {
        this.syncPassenger = syncPassenger;

        handler.post(syncSchedulePickup);

    }
    public void stopSyncSchedulePickup() {
        this.syncPassenger = null;

        closeConnectionIfOpen();

        if(syncSchedulePickup!=null)
            handler.removeCallbacks(syncSchedulePickup);

    }

    public void pushDriverLocationToSync(Location driverLocation) {
        SyncLocation newSyncLocation = new SyncLocation(driverLocation.getLatitude(),driverLocation.getLongitude());
        driverLocations.push(newSyncLocation);
    }

    private void closeConnectionIfOpen() {
        if(syncSchedulePickupController!=null)
            syncSchedulePickupController.closeStream();

        if(syncAvailableDriversController!=null)
            syncAvailableDriversController.closeStream();

    }

    /**
     * This assumes that the user is logged in.
     */
    private Runnable syncAvailableRides = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    syncAvailableRidesThread = Thread.currentThread();
                    Thread.currentThread().setName("SyncAvailableRides");

                    //this label is to make sure we always schedule this task in a cycle
                    scheduleNextIteration:
                    do {
                        try {
                            syncAvailableDriversController = new HttpController(context);
                            syncAvailableDriversController.availableRidesCall(new HttpController.RidesAvailableCallback() {
                                @Override
                                public void onNewRideAvailable(String message) {

                                    RideManager.i(context).processMessage(message);

                                }
                            });
                        } catch(TokenInvalidException tie) {
                            IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                                    .getIdentityManager();

                            identityManager.refresh();

                        } catch(IOException ioe) {

                            handler.removeCallbacks(syncAvailableRides);
                            RideManager.i(context).postLookForRideConnectionErrorCallback();

                            ioe.printStackTrace();

                            closeConnectionIfOpen();

                            return;

                        }
                    } while(false);
                    handler.removeCallbacks(syncAvailableRides);
                    handler.postDelayed(syncAvailableRides,RETRY_SYNC_AVAILABLE_RIDES);
                }
            }).start();

        }
    };

    private Runnable syncDriverLocation = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("SyncAvailableRides");

                    //this label is to make sure we always schedule this task in a cycle
                    scheduleNextIteration:
                    do {
                        try {
                            if(driverLocations.size()>0) {
                                SyncLocation driverLocation = driverLocations.pop();
                                //this controller doesn't keep a connection open so keep it as local variable
                                HttpController syncDriverLocationController = new HttpController(context);
                                syncDriverLocationController.updateDriverLocation(driverLocation);
                                driverLocations.empty();
                            }

                        } catch(TokenInvalidException tie) {
                            IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                                    .getIdentityManager();

                            identityManager.refresh();

                        } catch(IOException ioe) {

                            handler.removeCallbacks(syncDriverLocation);

                            ioe.printStackTrace();

                            closeConnectionIfOpen();

                            return;

                        }
                    } while(false);
                    handler.removeCallbacks(syncDriverLocation);
                    handler.postDelayed(syncDriverLocation,SYNC_DRIVER_LOCATION_RATE);
                }
            }).start();

        }
    };
    /**
     * This assumes that the user is logged in.
     */
    private Runnable syncSchedulePickup = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    syncSchedulePickupThread = Thread.currentThread();
                    Thread.currentThread().setName("SyncSchedulePickup");

                    if(syncPassenger==null)return;

                    //this label is to make sure we always schedule this task in a cycle
                    scheduleNextIteration:
                    do {
                        try {
                            syncSchedulePickupController = new HttpController(context);
                            syncSchedulePickupController.schedulePickupCall(syncPassenger,new HttpController.SchedulePickupCallback() {
                                @Override
                                public void onConfirmedPickupScheduled(String message) {

                                    RideManager.i(context).processMessage(message);

                                }
                            });
                        } catch(TokenInvalidException tie) {
                            IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                                    .getIdentityManager();

                            identityManager.refresh();

                        } catch(IOException ioe) {

                            RideManager.i(context).postScheduleConnectionErrorCallback();

                            handler.removeCallbacks(syncSchedulePickup);

                            ioe.printStackTrace();

                            closeConnectionIfOpen();

                            return;

                        }
                    } while(false);
                    handler.removeCallbacks(syncSchedulePickup);
                    handler.postDelayed(syncSchedulePickup,RETRY_SYNC_AVAILABLE_RIDES);
                }
            }).start();

        }
    };

}
