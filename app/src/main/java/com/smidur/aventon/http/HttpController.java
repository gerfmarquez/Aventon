package com.smidur.aventon.http;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.smidur.aventon.R;
import com.smidur.aventon.exceptions.TokenInvalidException;
import com.smidur.aventon.model.GoogleApiDirections;
import com.smidur.aventon.model.SnapToRoadService;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncDriver;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncPassenger;
import com.smidur.aventon.model.SyncRideSummary;
import com.smidur.aventon.utilities.GoogleMapRouteRequestBuilder;
import com.smidur.aventon.utilities.GoogleMapsModelRequestBuilder;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by marqueg on 4/16/17.
 */

public class HttpController {

    Context context;

    public static final int HTTP_OK = 200;
    HttpWrapper wrapper;

    public HttpController(Context context) {
        this.context = context;
    }

    //todo generic callback? or specific? delivery generic message instead?
    public interface RidesAvailableCallback {
        void onNewRideAvailable(String message);
    }
    public interface SchedulePickupCallback {
        void onConfirmedPickupScheduled(String message);
    }

    public void availableRidesCall(SyncDriver syncDriver, @NonNull  final RidesAvailableCallback callback) throws IOException, TokenInvalidException {

        wrapper = new HttpWrapper();

        String driverJson = new Gson().toJson(syncDriver);

        HttpResponse response = wrapper.httpPOST("available_rides", new HttpWrapper.UpdateCallback() {
            @Override
            public void onUpdate(String message) {

                callback.onNewRideAvailable(message);

            }
        },driverJson,context);
        if(response.code==401) {
            throw new TokenInvalidException();
        }


    }

    public void updateDriverLocation(SyncLocation driverLocation) throws IOException, TokenInvalidException {

        wrapper = new HttpWrapper();

        String driverLocationJson = new Gson().toJson(driverLocation);

        HttpResponse response = wrapper.httpPUT("update_location",driverLocationJson,context);
        if(response.code==401) {
            throw new TokenInvalidException();
        }


    }
    public void confirmRide(SyncPassenger passenger) throws IOException, TokenInvalidException {

        wrapper = new HttpWrapper();
        //todo add headers for passenger which driver is confirming ride?
        Hashtable<String,String> parameters = new Hashtable<>();
        parameters.put("passengerId",passenger.getPassengerId());
        HttpResponse response = wrapper.httpPOST("complete_ride",parameters,context);

        if(response.code==401) {
            throw new TokenInvalidException();
        }
        if(response.code != 200) {
            throw new IOException("Http Code not expected:"+response.code);
        }
    }
    public void completeRide(SyncRideSummary rideSummary) throws IOException, TokenInvalidException {

        wrapper = new HttpWrapper();

        String jsonBody = new Gson().toJson(rideSummary);

        HttpResponse response = wrapper.httpPOST("accept_ride",null,jsonBody,context);

        if(response.code==401) {
            throw new TokenInvalidException();
        }
        if(response.code != 200) {
            throw new IOException("Http Code not expected:"+response.code);
        }
    }
    public void schedulePickupCall(SyncPassenger syncPassenger, @NonNull final SchedulePickupCallback callback) throws IOException, TokenInvalidException {

        wrapper = new HttpWrapper();

        String syncDestinationJson = new Gson().toJson(syncPassenger);
        //todo add a 30 seconds timeout so that driver doesn't keep there waiting forever.

        HttpResponse response = wrapper.httpPOST("shcedule_pickup",new HttpWrapper.UpdateCallback() {
            @Override
            public void onUpdate(String message) {

                callback.onConfirmedPickupScheduled(message);

            }
        }, syncDestinationJson ,context);
        if(response.code==401) {
            throw new TokenInvalidException();
        }

    }

    public GoogleApiDirections requestDirections(@NonNull double[][] startAndEnd, Context context) throws IOException {
        HttpWrapper wrapper = new HttpWrapper("https://maps.googleapis.com/");
        HttpResponse response = wrapper.httpGET(
                String.format("maps/api/directions/json?%s%s",
                        GoogleMapRouteRequestBuilder.buildRequest(startAndEnd),
                        "&units=metric&key="+context.getString(R.string.directions_key)), context);

        if (response.code == 200) {
            return new Gson().fromJson(response.message, GoogleApiDirections.class);
        } else {
            throw new IOException("Http Response Code not expected");
        }

    }

    public SnapToRoadService requestSnapToRoad(List<Location> snapPoints)  {
        try {


            HttpWrapper wrapper = new HttpWrapper("https://roads.googleapis.com/");
            HttpResponse response = wrapper.httpGET(
                    "v1/snapToRoads?path="+ GoogleMapsModelRequestBuilder.buildRequestSnap(snapPoints)
                            +"&interpolate=true&key="+context.getString(R.string.directions_key),context);

            if (response.code == 200) {
                SnapToRoadService snappedPoints = new Gson().fromJson(response.message, SnapToRoadService.class);
                return snappedPoints;

            } else {
                throw new IOException("Http Response Code not expected");
            }
        }catch(IOException ioe) {
            Log.e("","",ioe);
        }
        return null;
    }

    public boolean closeStream() {

        if(wrapper!=null && wrapper.getConnection()!=null) {

                wrapper.getConnection().disconnect();

        }
        return false;
    }

}
