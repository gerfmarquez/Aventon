package com.smidur.aventon.managers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.smidur.aventon.exceptions.TokenInvalidException;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;
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
    @MainThread
    public void startDriverShift() {

        isDriverAvailable = true;


        GoogleApiWrapper.getInstance(context).requestAndroidLocationUpdates(driverLocationListener);

        Sync.i(context).startSyncAvailableRides();
        Sync.i(context).startDriverShift();


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

        Location passengerLocation = GpsUtil.getUserLocation(context);
        if(passengerLocation == null) {
            //todo anaylytics error
            return;
        }

        SyncLocation syncPassengerLocation = new SyncLocation(
                passengerLocation.getLatitude(),passengerLocation.getLongitude());

        LatLng latLng = placeDestination.getLatLng();
        SyncLocation syncDestLocation = new SyncLocation(latLng.latitude,latLng.longitude);

        SyncDestination syncDestination = new SyncDestination(placeDestination.getAddress().toString(),syncDestLocation);

        SyncPassenger syncPassenger = new SyncPassenger();
        syncPassenger.setSyncDestination(syncDestination);
        syncPassenger.setSyncPassengerLocation(syncPassengerLocation);


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
        if(passengerEventsListeners ==null)return;
        synchronized (passengerEventsListeners) {
            if(passengerEventsListeners.contains(listener)) {
                passengerEventsListeners.remove(listener);
            }
        }
    }



    public void confirmPassengerPickup(final SyncPassenger passenger) {
        new Thread(){public void run() {
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
        }}.start();

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
            String driver = value;

            postPickupScheduledCallback(driver);

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
    private void postDriverLocationUpdateCallback(final SyncLocation syncLocation) {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDriverApproaching(syncLocation);
                        }
                    });

                }
            }

        }
    }
    private void postNoDriverFoundCallback() {
        synchronized (passengerEventsListeners) {
            for(final PassengerEventsListener listener: passengerEventsListeners) {
                if(listener!=null) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onNoDriverFoundNearby();
                        }
                    });

                }
            }

        }
    }


    private void postRideStartedCallback(final SyncPassenger passenger) {
        synchronized (driverEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRideStarted(passenger);
                        }
                    });

                }
            }

        }
    }
    private void postRideAcceptFailedCallback() {
        synchronized (driverEventsListeners) {
            for(final DriverEventsListener listener: driverEventsListeners) {
                if(listener!=null) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRideAcceptFailed();
                        }
                    });

                }
            }

        }
    }
    //todo move this to another class
    LocationListener driverLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Sync.i(context).pushDriverLocationToSync(location);
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
        void onRideAcceptFailed();
    }
    public interface PassengerEventsListener {
        void onPickupScheduled(String driver);
        void onDriverApproaching(SyncLocation driverNewLocation);
        void onDriverArrived();
        void onNoDriverFoundNearby();//todo
    }

}
