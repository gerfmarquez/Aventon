package com.smidur.aventon.utilities;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class GpsUtil {

    private static Location requestedLocation;

    public static Location lastKnownLocation;

    @WorkerThread
    public static Location getUserLocation(Context context) throws SecurityException {

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);


        final CountDownLatch latch = new CountDownLatch(1);


        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                requestedLocation = location;
                lastKnownLocation = location;
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

        return requestedLocation;
    }


    public static Location getLastKnownLocation(final Context context) throws SecurityException {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        new Thread() {
            public void run() {
                //run a location request in the background for next time that is needed.
                getUserLocation(context);
            }
        }.start();

        return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }
    public static LatLng getLatLng(Location location) {
        return new LatLng(location.getLatitude(),location.getLongitude());
    }

    public static String geoCodeLocation(double latitude, double longitude, Context context) {
        List<Address> geocoderResult;
        try {
            Geocoder geocoder = new Geocoder(context);
            geocoderResult = geocoder.getFromLocation(latitude,longitude,1);
        } catch(IOException ioe) {
            //todo analytics
            Log.e("Geocoder","Geocoder Failed to translate passenger location");

            return null;
        }
        Address address = geocoderResult.get(0);
        String originAddress = new String();
        for(int i = 0; i < address.getMaxAddressLineIndex();i++) {
            originAddress = originAddress.concat(address.getAddressLine(i)+",");
        }
        return originAddress;
    }
}
