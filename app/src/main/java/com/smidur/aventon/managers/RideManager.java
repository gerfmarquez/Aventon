package com.smidur.aventon.managers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;

import com.google.gson.Gson;
import com.smidur.aventon.exceptions.TokenInvalidException;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncDriver;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncOrigin;
import com.smidur.aventon.model.SyncPassenger;
import com.smidur.aventon.sync.Sync;
import com.smidur.aventon.utilities.GoogleApiWrapper;
import com.smidur.aventon.utilities.GpsUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by marqueg on 4/17/17.
 */

public class RideManager {

    String TAG = getClass().getSimpleName();

    private Set<DriverEventsListener> driverEventsListeners;
    private Set<PassengerEventsListener> passengerEventsListeners;
    private boolean isDriverAvailable = false;
    private boolean isDriverOnRide = false;
    private boolean isPassengerScheduling = false;

    SyncDriver syncDriver;

    TaxiMeterManager taxiMeterManager;

    SyncPassenger syncPassenger;


    /**
     *  Start - Singleton
     */
    private static RideManager instance;
    private final Context context;
    private Handler handler;

    public RideManager(Context context) {
        this.context = context;
    }
    public static RideManager i(Context context){
        if(instance==null) {
            instance = new RideManager(context);
            instance.handler = new Handler(context.getMainLooper());
            instance.driverEventsListeners = new HashSet<>();
            instance.passengerEventsListeners = new HashSet<>();
        }
        return instance;
    }

    public boolean isDriverOnRide() {
        return isDriverOnRide;
    }

    /**
     * Register for  events
     */
    @MainThread
    public void startDriverShift() {

        isDriverAvailable = true;


        new Thread() {
            public void run() {

                Sync.i(context).startSyncDriverLocation();
                Sync.i(context).startSyncRideInfo(syncDriver);
            }
        }.start();


        GoogleApiWrapper.getInstance(context).requestAndroidLocationUpdates(driverLocationListener);


    }

    /**
     * mainly start taxi meter.
     */
    public void pauseDriverShiftAndStartRide() {
        isDriverOnRide = true;
        isDriverAvailable = false;
        //DEPRECATED--By closing connection we let the service know that driver is on a ride or not available.
        //Server will keep track of what drivers have Passengers assigned (Are on a ride) and avoid forwarding ride requests.
        //this way we can still communicate with driver through the long-polling commands for future enhancements
//        Sync.i(context).stopSyncRideInfo();

        startTaxiMeter();

    }
    public void resumeDriverShiftAndEndRide() {

        stopTaxiMeter();//create callback to let activity know about the total price.
        //also move the above line and separate it from resuming shift until after driver clicks a dialog ok.

        isDriverOnRide = false;
        isDriverAvailable = true;
        Sync.i(context).startSyncRideInfo(syncDriver);
    }
    @MainThread
    public void endDriverShift() {

        if(isDriverAvailable) {
            Sync.i(context).stopSyncDriverLocation();
            Sync.i(context).stopSyncRideInfo();
        }
        isDriverAvailable = false;

        GoogleApiWrapper.getInstance(context).stopAndroidLocationUpdates(driverLocationListener);

    }

    public void reAcquireGpsSignal() {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                GoogleApiWrapper.getInstance(context).stopAndroidLocationUpdates(driverLocationListener);
                GoogleApiWrapper.getInstance(context).requestAndroidLocationUpdates(driverLocationListener);
            }
        });
    }

    public void startTaxiMeter() {

        GoogleApiWrapper.getInstance(context).connect(new GoogleApiWrapper.GoogleApiEstablishedCallback() {
            @Override
            public void onGoogleServicesConnectionSucceeded() {
                TaxiMeterManager.i(context).startActivityDetectionForTaxiMeter();

            }

            @Override
            public void onGoogleServicesConnectionFailed() {
                throw new IllegalStateException("Couldn't connect to google play services");
            }
        });
        TaxiMeterManager.i(context).init();
        TaxiMeterManager.i(context).resetSegment(0);

    }
    public void stopTaxiMeter() {
        TaxiMeterManager.i(context).stopTaximeter();
    }

    public void setDriverInfo(String makeModel, String plates) {
        syncDriver = new SyncDriver();
        syncDriver.setMakeModel(makeModel);
        syncDriver.setPlates(plates);
    }
    public SyncDriver getDriverInfo() {
        return syncDriver;
    }
    public SyncPassenger getSyncPassenger() {
        return syncPassenger;
    }
    /**
     * Register for  events
     */
    @WorkerThread
    public void startSchedulePassengerPickup(SyncDestination syncDestination) {

        Location passengerLocation = GpsUtil.getUserLocation(context);
        if(passengerLocation == null) {
            //todo anaylytics error
            return;
        }

        SyncLocation syncPassengerLocation = new SyncLocation(
                passengerLocation.getLatitude(),passengerLocation.getLongitude());



        String originAddress = GpsUtil.geoCodeLocation(
                passengerLocation.getLatitude(),passengerLocation.getLongitude(), context);

        SyncOrigin syncOrigin = new SyncOrigin(syncPassengerLocation,originAddress);

        SyncPassenger syncPassenger = new SyncPassenger();
        syncPassenger.setSyncDestination(syncDestination);
        syncPassenger.setSyncOrigin(syncOrigin);


        Sync.i(context).startSyncSchedulePickup(syncPassenger);

        isPassengerScheduling = true;
    }
    public void pauseSchedulePassengerPickup(String passenger) {

        //we might wanna keep connection open
        if(isPassengerScheduling)   Sync.i(context).stopSyncSchedulePickup();
    }
    public void resumeSchedulePassengerPickup() {

//        Sync.i(context).startSyncSchedulePickup();
    }

    public void cancelSchedulePassengerPickup() {
        //todo notify server that user stopped scheduling a pickup
    }

    public void endSchedulePassengerPickup() {

        if(isPassengerScheduling) {
            Sync.i(context).stopSyncSchedulePickup();
        }
        isPassengerScheduling = false;

    }


    /**
     * Register for driver events.
     * @param listener
     */
    public void register(DriverEventsListener listener) {
        if(listener!=null) {
            synchronized (driverEventsListeners) {
                driverEventsListeners.add(listener);
            }
        }
    }
    /**
     * Unregister for driver events.
     * @param listener
     */
    public void unregister(DriverEventsListener listener) {
        if(driverEventsListeners ==null)return;
        synchronized (driverEventsListeners) {
            if(driverEventsListeners.contains(listener)) {
                driverEventsListeners.remove(listener);
            }
        }
    }
    /**
     * Register for passenger events.
     * @param listener
     */
    public void register(PassengerEventsListener listener) {
        if(listener!=null) {
            synchronized (passengerEventsListeners) {
                passengerEventsListeners.add(listener);
            }
        }
    }
    /**
     * Unregister for passenger events.
     * @param listener
     */
    public void unregister(PassengerEventsListener listener) {
        if(passengerEventsListeners == null)return;
        synchronized (passengerEventsListeners) {
            if(passengerEventsListeners.contains(listener)) {
                passengerEventsListeners.remove(listener);
            }
        }
    }



    public void confirmPassengerPickup(final SyncPassenger passenger) {
        this.syncPassenger = passenger;
        new Thread(){
            public void run() {
                try {
                    HttpController controller = new HttpController(context);
                    controller.confirmRide(passenger);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            postRideStartedCallback(passenger);
                        }
                    });

                } catch(TokenInvalidException tokenInvalid) {

                    tokenInvalid.printStackTrace();
                }
                catch(IOException ioe) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            postRideAcceptFailedCallback();
                        }
                    });

                }
            }
        }.start();

    }

    @WorkerThread
    public void processMessage(String message) {
        String[] pair = message.split(":");
        String command = (pair)[0].trim();
        String value = (pair)[1].trim();

        if(command.startsWith("Passenger")) {
            String passenger = message.replace("Passenger:","").trim();

            SyncPassenger passengerObj = new Gson().fromJson(passenger,SyncPassenger.class);

            postRideAvailableCallback(passengerObj);
            //todo parse passenger object along with pickup location

        }
        if(command.startsWith("Driver")) {
            String driverJson = message.replace("Driver:","");

            SyncDriver syncDriver = new Gson().fromJson(driverJson,SyncDriver.class);

            postPickupScheduledCallback(syncDriver);

        }
        if(command.startsWith("NewDriverLocation")) {

            String latitude = value.split(",")[0];
            String longitude = value.split(",")[1];

            postDriverLocationUpdateCallback(
                    new SyncLocation(
                            Double.valueOf(latitude),
                            Double.valueOf(longitude)
                    )
            );

        }
        if(command.startsWith("NoDriverFound")) {
            postNoDriverFoundCallback();
        }
    }
    private void postRideAvailableCallback(final SyncPassenger passenger) {
        synchronized (driverEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRideAvailable(passenger);
                        }
                    }).start();

                }
            }

        }
    }
    private void postPickupScheduledCallback(final SyncDriver driver) {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPickupScheduled(driver);
                        }
                    }).start();

                }
            }

        }
    }
    private void postDriverLocationUpdateCallback(final SyncLocation syncLocation) {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDriverApproaching(syncLocation);
                        }
                    }).start();

                }
            }

        }
    }
    public void postNoDriverFoundCallback() {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onNoDriverFoundNearby();
                        }
                    }).start();

                }
            }

        }
    }
    public void postScheduleConnectionErrorCallback() {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSchedulePickupConnectionError();
                        }
                    }).start();

                }
            }

        }
    }


    private void postRideStartedCallback(final SyncPassenger passenger) {
        synchronized (driverEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRideStarted(passenger);
                        }
                    }).start();

                }
            }

        }
    }
    private void postRideAcceptFailedCallback() {
        synchronized (driverEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRideAcceptFailed();
                        }
                    }).start();

                }
            }

        }
    }
    public void postLookForRideConnectionErrorCallback() {
        synchronized (passengerEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onLookForRideConnectionError();
                        }
                    }).start();

                }
            }

        }
    }




    //todo move this to another class
    LocationListener driverLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Sync.i(context).pushDriverLocationToSync(location);

            TaxiMeterManager.i(context).newLocationAvailable(location);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };



    public interface DriverEventsListener {
        void onRideAvailable(SyncPassenger passenger);
        void onRideStarted(SyncPassenger passenger);
        void onRideEnded();
        void onRideAcceptFailed();
        void onLookForRideConnectionError();
        //TODO onRideEnded
    }
    public interface PassengerEventsListener {
        void onPickupScheduled(SyncDriver driver);
        void onDriverApproaching(SyncLocation driverNewLocation);
        void onDriverArrived();
        void onSchedulePickupConnectionError();
        void onNoDriverFoundNearby();//todo
    }

}
