package com.smidur.aventon.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.google.android.gms.location.places.Place;
import com.smidur.aventon.http.HttpWrapper;
import com.smidur.aventon.sync.Sync;

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
    public void startSchedulePassengerPickup(Place place) {

        isPassengerScheduling = true;
        Sync.i(context).startSyncSchedulePickup();
    }
    public void pauseSchedulePassengerPickup(String passenger) {

        //we might wanna keep connection open
        if(isPassengerScheduling)   Sync.i(context).stopSyncSchedulePickup();
    }
    public void resumeSchedulePassengerPickup() {

        Sync.i(context).startSyncSchedulePickup();
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
