package com.smidur.aventon.utilities;

import android.location.Location;


import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by marqueg on 11/16/15.
 */
public class GoogleMapsModelRequestBuilder {
    /**
     * Takes list of locations to build the route calculation request
     *
     * @param deliveriesLocations double array of arrays,
     *                            expects a minimum of 2 locations (origin and destination)
     *                            need to be sorted as well already.
     *                            first array has every delivery location for route calculation.
     *                            second nested array has latitude and longitude
     *
     * @return request to send to Google Api with properly formatted "way points".
     */
    public static String buildRequest(@NonNull double[][] deliveriesLocations) {
        StringBuilder deliveriesBuilder = new StringBuilder();
        if(deliveriesLocations.length <2)return null;//dont even bother
        else {//size is at least 2 is good
            deliveriesBuilder.append(
                    String.format("origin=%.05f,%.05f&destination=%.05f,%.05f",
                            deliveriesLocations[0][0],deliveriesLocations[0][1],//always use first location
                            //always use last location for destination
                            deliveriesLocations[deliveriesLocations.length][0],deliveriesLocations[deliveriesLocations.length][1])
            );

        }
        //more than two locations means we need to add  way points
        if(deliveriesLocations.length>2) {
            deliveriesBuilder.append("&waypoints=");
            String multipleWaypointsSeparator = "";
            //origin and destination are not waypoints
            for(int i = 1; i < deliveriesLocations.length-1;i++) {
                //we still have another waypoint to process
                if(i != deliveriesLocations.length-1) {
                    multipleWaypointsSeparator = "|";
                } else {
                    multipleWaypointsSeparator = "";
                }
                deliveriesBuilder.append(String.format("%.05f,%.05f"
                        +multipleWaypointsSeparator
                        ,deliveriesLocations[i][0],deliveriesLocations[i][1]));
            }
        }
        return deliveriesBuilder.toString();
    }

    /**
     * Takes list of locations to build the route calculation request
     *
     * @param snapPoints double array of arrays,
     *                            expects a minimum of 2 locations (origin and destination)
     *                            need to be sorted as well already.
     *                            first array has every delivery location for route calculation.
     *                            second nested array has latitude and longitude
     *
     * @return request to send to Google Api with properly formatted "way points".
     */
    public static String buildRequestSnap(@NonNull List<Location> snapPoints) {
        StringBuilder deliveriesBuilder = new StringBuilder();
//        if(snapPoints.length <2)return null;//dont even bother
//        else {//size is at least 2 is good
//            deliveriesBuilder.append(
//                    String.format("origin=%.05f,%.05f&destination=%.05f,%.05f",
//                            snapPoints[0][0],snapPoints[0][1],//always use first location
//                            //always use last location for destination
//                            snapPoints[snapPoints.length][0],snapPoints[snapPoints.length][1])
//            );
//
//        }
        //more than two locations means we need to add  way points
        deliveriesBuilder.append("");
        String multipleWaypointsSeparator = "";
        //origin and destination are not waypoints
        for(int i = 0; i < snapPoints.size();i++) {
            //we still have another waypoint to process
            if(i != snapPoints.size()-1) {
                multipleWaypointsSeparator = "|";
            } else {
                multipleWaypointsSeparator = "";
            }
            deliveriesBuilder.append(String.format("%.05f,%.05f"
                    +multipleWaypointsSeparator
                    ,snapPoints.get(i).getLatitude(),snapPoints.get(i).getLongitude()));
        }

        return deliveriesBuilder.toString();
    }
}
