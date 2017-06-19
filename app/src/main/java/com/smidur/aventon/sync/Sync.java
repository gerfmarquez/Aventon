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

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Created by marqueg on 4/16/17.
 */

public class Sync {

    String TAG = getClass().getSimpleName();



    private static final int RETRY_SYNC_AVAILABLE_RIDES = 3000;//3 sec
    private static final int SYNC_DRIVER_LOCATION_RATE = 15 * 1000;//40 sec


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

    public void startDriverShift() {
        handler.post(syncDriverLocation);
    }

    public void startSyncAvailableRides() {

        handler.post(syncAvailableRides);


    }
    public void stopSyncAvailableRides() {

        closeConnectionIfOpen();

        if(syncAvailableRides!=null)
            handler.removeCallbacks(syncAvailableRides);

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
        //todo interrupt might not be necessary
//        syncAvailableRidesThread.interrupt();
        if(syncAvailableDriversController!=null)
            syncAvailableDriversController.closeStream();
        //todo interrupt might not be necessary
//        syncSchedulePickupThread.interrupt();
    }

    /**
     * This assumes that the user is logged in.
     */
    private Runnable syncAvailableRides = new Runnable() {
        @Override
        public void run() {
            AsyncTask.execute(new Runnable() {
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

                            ioe.printStackTrace();

                            closeConnectionIfOpen();

                        }
                    } while(false);
                    handler.removeCallbacks(syncAvailableRides);
                    handler.postDelayed(syncAvailableRides,RETRY_SYNC_AVAILABLE_RIDES);
                }
            });

        }
    };

    private Runnable syncDriverLocation = new Runnable() {
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
                            if(driverLocations.size()>0) {
                                SyncLocation driverLocation = driverLocations.pop();
                                syncAvailableDriversController = new HttpController(context);
                                syncAvailableDriversController.updateDriverLocation(driverLocation);
                                driverLocations.empty();
                            }

                        } catch(TokenInvalidException tie) {
                            IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                                    .getIdentityManager();

                            identityManager.refresh();

                        } catch(IOException ioe) {

                            ioe.printStackTrace();

                            closeConnectionIfOpen();

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
            AsyncTask.execute(new Runnable() {
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

                            ioe.printStackTrace();

                            closeConnectionIfOpen();

                        }
                    } while(false);
                    handler.removeCallbacks(syncSchedulePickup);
                    handler.postDelayed(syncSchedulePickup,RETRY_SYNC_AVAILABLE_RIDES);
                }
            });

        }
    };

}
