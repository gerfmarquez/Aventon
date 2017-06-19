package com.smidur.aventon.utilities;

import android.app.PendingIntent;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by marqueg on 5/18/17.
 */

public class GoogleApiWrapper {

    private String TAG = GoogleApiWrapper.class.getCanonicalName();
    private static GoogleApiWrapper instance = null;

    private GoogleApiWrapper(Context context) {mContext = context;}

    public static GoogleApiWrapper getInstance(Context context) {
        if(instance ==null) instance = new GoogleApiWrapper(context);
        instance.mContext = context;
        return instance;
    }

    GoogleApiEstablishedCallback googleApiEstablishedCallback;
    GoogleApiClient googleApiClient;

    Context mContext;

    public void requestUpdates(LocationRequest request, LocationListener locationListener) {
        try {
            PendingResult<Status> result = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, locationListener);
            result.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if(status.isSuccess()) {
                        Log.i(TAG,"Successfully requested gps updates for: "+status.describeContents());
                    } else {
                        Log.w(TAG, "Failed to requested gps updates for: "+status.getStatusCode());
                    }
                }
            });
        } catch(SecurityException se) {
            Log.e(TAG,"Permission for location is not enabled");
        }

    }
    public void stopUpdates(LocationListener locationListener) {
        if(!isConnected()) return;
        PendingResult<Status> result = LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener);
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.i(TAG, "Successfully stopped GPS updates for: "+status.describeContents());
                } else {
                    Log.w(TAG, "Failed to stop GPS updates for: "+status.getStatusCode());
                }
            }
        });
    }

    public void requestAndroidLocationUpdates(android.location.LocationListener locationListener) {
        try {
            LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,locationListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000,0,locationListener);
        } catch(SecurityException se) {
            throw new IllegalStateException("This shouldn't happen");
        }


    }
    public void stopAndroidLocationUpdates(android.location.LocationListener locationListener) {
        try {
            LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationListener);
        } catch(SecurityException se) {
            throw new IllegalStateException("This shouldn't happen");
        }

    }

//    public void requestGeofence(final Geofence geofence, PendingIntent radius, int initialTrigger) {
//
//        List<Geofence> fences = new ArrayList<Geofence>();
//        fences.add(geofence);
//        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
//
//        builder.setInitialTrigger(initialTrigger);
//        builder.addGeofences(fences);
//
//        PendingResult<Status> result = LocationServices.
//                GeofencingApi.addGeofences(googleApiClient, builder.build(), radius);
//
//        result.setResultCallback(new ResultCallback<Status>() {
//            @Override
//            public void onResult(Status status) {
//                if (status.isSuccess()) {
//                    Log.d(TAG, "GeoFence setup succeeded : " + geofence.getRequestId());
//                } else {
//                    Log.w(TAG, "GeoFence setup failed with status code: " + status.getStatusCode());
//                }
//            }
//        });
//    }

//    public void removeGeoFence(String... geoFenceRequestId) {
//        if(!isConnected()) return;
//        PendingResult<Status> result =
//                LocationServices.GeofencingApi.removeGeofences(
//                        googleApiClient, Arrays.asList(geoFenceRequestId));
//
//        result.setResultCallback(new ResultCallback<Status>() {
//            @Override
//            public void onResult(Status status) {
//                if (status.isSuccess()) {
//                    Log.d(TAG, "GeoFence successfully removed.");
//                } else {
//                    Log.w(TAG, "GeoFence failed to be removed with status code: " + status.getStatusCode());
//                }
//            }
//        });
//    }

//    @Override
//    public void onConnected(Bundle bundle) {
//        googleApiEstablishedCallback.onGoogleServicesConnectionSucceeded();
//        Log.i(TAG, "on connected");
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Log.e(TAG, "Google API Connection Failed!!");
//        googleApiEstablishedCallback.onGoogleServicesConnectionFailed();
//    }
//
//    @Override
//    public void onConnectionSuspended(int cause) {
//        Log.w(TAG,"on connect suspended, cause: "+(
//                (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST)?"Network Lost"
//                        :(cause == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED)?"Service Disconnected":" Something")
//        );
//        //silently reconnect
//        connect(new GoogleApiEstablishedCallback() {
//            @Override
//            public void onGoogleServicesConnectionSucceeded() {
//                Log.d(TAG,"Successfully re-connected to Google Location Services.");
//            }
//
//            @Override
//            public void onGoogleServicesConnectionFailed() {
//                Log.e(TAG,"Error reconnecting to Google Api Location Services after connection got suspended.");
//            }
//        });
//    }



    public boolean isConnected() {
        return googleApiClient!=null && googleApiClient.isConnected();
    }

    @MainThread
    public void  connect(GoogleApiEstablishedCallback googleApiEstablishedCallback) {

        if(isConnected()) {
            googleApiEstablishedCallback.onGoogleServicesConnectionSucceeded();
        } else {
            Log.i(TAG, "Google Api - Starting initialization");
            googleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(null)
                    .addOnConnectionFailedListener(null)
                    .addApi(LocationServices.API)
                    .build();
            this.googleApiEstablishedCallback = googleApiEstablishedCallback;
            googleApiClient.connect();
        }


    }


    public interface GoogleApiEstablishedCallback {
        void onGoogleServicesConnectionSucceeded();
        void onGoogleServicesConnectionFailed();
    }
}
