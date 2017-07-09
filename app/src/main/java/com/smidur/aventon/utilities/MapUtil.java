package com.smidur.aventon.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.os.Vibrator;
import android.support.annotation.MainThread;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.smidur.aventon.R;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.model.GoogleApiDirections;
import com.smidur.aventon.model.GoogleApiLeg;
import com.smidur.aventon.model.GoogleApiPolyline;
import com.smidur.aventon.model.GoogleApiRoute;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marqueg on 7/9/17.
 */

public class MapUtil {
    @MainThread
    public static void selectDestinationPlaceOnMap(final DestinationSelectedCallback callback,
                                                   final String address, final Location originLocation,
                                                   final LatLng destLatLng, final Activity activity) {

        new Thread() {
            public void run() {

                Location placeLocation  = new Location("");

                placeLocation.setLatitude(destLatLng.latitude);
                placeLocation.setLongitude(destLatLng.longitude);


                float distanceToAddress = placeLocation.distanceTo(originLocation);

                if(distanceToAddress <  Constants.MAXIMUM_RIDE_DISTANCE) {

                    SyncLocation syncDestLocation = new SyncLocation(destLatLng.latitude,destLatLng.longitude);
                    final SyncDestination syncDestination = new SyncDestination(address.toString(),syncDestLocation);

                            //zoom in selected place
                            final LatLng userLatLng = GpsUtil.getLatLng(originLocation);

                            GoogleApiRoute route = getRoute(userLatLng,destLatLng, activity);
                            final GoogleApiPolyline googlePolyLine = route.getOverview_polyline();
                            final GoogleApiLeg leg = route.getLegs()[0];

                            final PolylineOptions options = getPolylineOptions(googlePolyLine,activity);

                            //todo make price estimate calculation
                            Float price = FareUtil.calculateFareMex(
                                    leg.getDistance().getValue(),leg.getDuration().getValue());

                            final String formattedPrice = String.format("$%.2f",price);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                callback.onDestinationSelected(userLatLng, destLatLng,options, leg, syncDestination, formattedPrice);
                                }
                            });


                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onDestinationTooFar();
                        }
                    });

                }
            }
        }.start();
    }

    private static GoogleApiRoute getRoute(LatLng origin, LatLng target, Activity activity) {

        double[][] startEndArray = new double[][]{
                new double[]{origin.latitude,origin.longitude},
                new double[]{target.latitude,target.longitude}};

        try {
            HttpController controller = new HttpController(activity);
            GoogleApiDirections directions = controller.requestDirections(startEndArray,activity);
            return directions.getRoutes()[0];

        } catch(IOException ioe) {
            ioe.printStackTrace();
            //todo analytics
        }
        return null;

    }
    private static PolylineOptions getPolylineOptions(GoogleApiPolyline googleApiPolyline,Activity activity) {
        ArrayList<LatLng> polyline = PolyLineUtil.decodePoly(googleApiPolyline.getPoints());

        final PolylineOptions options = new PolylineOptions().width(20)
                .color(activity.getResources().getColor(R.color.aventon_secondary_color,null)).geodesic(true);
        for (int i = 0; i < polyline.size(); i++) {
            LatLng point = polyline.get(i);
            options.add(point);
        }
        return options;
    }
    public interface DestinationSelectedCallback {
        public void onDestinationSelected(LatLng userLatLng,LatLng destLatLng,PolylineOptions options,GoogleApiLeg leg, SyncDestination syncDestination,String formattedPrice);
        public void onDestinationTooFar();
    }
}
