package com.smidur.aventon.managers;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.WorkerThread;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.smidur.aventon.http.HttpWrapper;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncPassenger;
import com.smidur.aventon.sync.Sync;
import com.smidur.aventon.utilities.Constants;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

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

    Location requestedPassengerLocation;

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
    /**
     * Finish - Singleton
     */


    /**
     * Register for  events
     */
    public void startDriverShift() {

        isDriverAvailable = true;
        Sync.i(context).startSyncAvailableRides();
    }
    public void pauseDriverShiftAndStartRide(String passenger) {
        isDriverOnRide = true;
        isDriverAvailable = false;
        //we might wanna keep connection open
        if(isDriverAvailable)   Sync.i(context).stopSyncAvailableRides();
    }
    public void resumeDriverShiftAndEndRide() {
        isDriverOnRide = false;
        isDriverAvailable = true;
        Sync.i(context).startSyncAvailableRides();
    }
    public void endDriverShift() {

        if(isDriverAvailable) {
            Sync.i(context).stopSyncAvailableRides();
        }
        isDriverAvailable = false;

    }
    /**
     * Register for  events
     */
    @WorkerThread
    public void startSchedulePassengerPickup(Place placeDestination) throws SecurityException {

        isPassengerScheduling = true;


        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Criteria minimumCriteria = new Criteria();
        minimumCriteria.setAccuracy(Constants.MINIMUM_ACCURACY_FOR_PASSENGER_PICKUP_LOCATION);

        final CountDownLatch latch = new CountDownLatch(1);

        locationManager.requestSingleUpdate(minimumCriteria, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                requestedPassengerLocation = location;
                latch.countDown();
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
        },context.getMainLooper());
        try {
            latch.await();

        } catch(InterruptedException ie){}

        if(requestedPassengerLocation == null) {
            //todo anaylytics error
            return;
        }

        SyncLocation syncPassengerLocation = new SyncLocation(
                requestedPassengerLocation.getLatitude(),requestedPassengerLocation.getLongitude());

        LatLng latLng = placeDestination.getLatLng();
        SyncLocation syncDestLocation = new SyncLocation(latLng.latitude,latLng.longitude);

        SyncDestination syncDestination = new SyncDestination(placeDestination.getAddress().toString(),syncDestLocation);

        SyncPassenger syncPassenger = new SyncPassenger();
        syncPassenger.setSyncDestination(syncDestination);
        syncPassenger.setSyncPassengerLocation(syncPassengerLocation);


        Sync.i(context).startSyncSchedulePickup(syncPassenger);
    }
    public void pauseSchedulePassengerPickup(String passenger) {

        //we might wanna keep connection open
        if(isPassengerScheduling)   Sync.i(context).stopSyncSchedulePickup();
    }
    public void resumeSchedulePassengerPickup() {

//        Sync.i(context).startSyncSchedulePickup();
    }
    //todo
    public void cancelSchedulePassengerPickup() {

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
        if(passengerEventsListeners ==null)return;
        synchronized (passengerEventsListeners) {
            if(passengerEventsListeners.contains(listener)) {
                passengerEventsListeners.remove(listener);
            }
        }
    }



    public void confirmPassengerPickup() {
        new Thread() {
            public void run() {
                try {
                    HttpWrapper wrapper = new HttpWrapper();
                    //todo add headers for passenger which driver is confirming ride?
                    wrapper.httpGET("accept_ride",context);
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }

            }
        }.start();
    }

    @WorkerThread
    public void processMessage(String message) {
        String value = (message.split(":"))[1].trim();

        if(message.contains("Passenger")) {
            String passenger = value;

            postRideAvailableCallback(passenger);

        }
        if(message.contains("Driver")) {
            String driver = value;

            postPickupScheduledCallback(driver);

        }
    }
    private void postRideAvailableCallback(final String passenger) {
        synchronized (driverEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRideAvailable(passenger);
                        }
                    });

                }
            }

        }
    }
    private void postPickupScheduledCallback(final String driver) {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPickupScheduled(driver);
                        }
                    });

                }
            }

        }
    }



    public interface DriverEventsListener {
        public void onRideAvailable(String passenger);
        public void ongoingRide();
    }
    public interface PassengerEventsListener {
        public void onPickupScheduled(String driver);
    }
    public interface RideEventsListener {
        public void onRideStart();
        public void onRideEnd();
        public void onRideCanceled();

    }
}
