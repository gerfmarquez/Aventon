package com.smidur.aventon.utilities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
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

public class GoogleApiWrapper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private String TAG = GoogleApiWrapper.class.getCanonicalName();
    private static GoogleApiWrapper instance = null;

    private GoogleApiWrapper(Context context) {mContext = context;}

    public static GoogleApiWrapper getInstance(Context context) {
        if(instance ==null) instance = new GoogleApiWrapper(context);
        instance.mContext = context;
        return instance;
    }
    private DetectedActivityCallback detectedActivityCallback;

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
            //gps locations actual rate seem to average 12 seconds, does this depend on manufacturer? or road/gps satellite conditions?
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,7 * 1000,0,locationListener);

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

    public void requestDriverActivityUpdates(int activityUpdatesRate, DetectedActivityCallback callback) {
        this.detectedActivityCallback = callback;

        PendingResult<Status> result = ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                googleApiClient,
                activityUpdatesRate,
                getActivityDetectionPendingIntent());

        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.i(TAG, "Successfully requested Detected Activity updates for: " + status.describeContents());
                } else {
                    Log.w(TAG, "Failed to request GPS updates for: " + status.getStatusCode());
                }
            }
        });

    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(mContext, DetectedActivityIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

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
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(ActivityRecognition.API)
                    .build();
            this.googleApiEstablishedCallback = googleApiEstablishedCallback;
            googleApiClient.connect();
        }


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(googleApiEstablishedCallback != null) {
            googleApiEstablishedCallback.onGoogleServicesConnectionSucceeded();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(googleApiEstablishedCallback != null) {
            googleApiEstablishedCallback.onGoogleServicesConnectionFailed();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //NOP//todo handle
    }

    public void postDetectedActivityCallback(List<DetectedActivity> detectedActivities) {
        if(detectedActivityCallback!=null)
            detectedActivityCallback.onDetectedActivity(detectedActivities);
    }

    public interface GoogleApiEstablishedCallback {
        void onGoogleServicesConnectionSucceeded();
        void onGoogleServicesConnectionFailed();
    }
    public interface DetectedActivityCallback {
        void onDetectedActivity(List<DetectedActivity> detectedActivityList);
    }
}
