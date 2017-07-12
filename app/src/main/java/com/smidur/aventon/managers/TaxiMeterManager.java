package com.smidur.aventon.managers;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.model.SnapToRoadService;
import com.smidur.aventon.model.SnappedPoints;
import com.smidur.aventon.utilities.FareUtil;
import com.smidur.aventon.utilities.GpsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by marqueg on 7/9/17.
 */

public class TaxiMeterManager {

    private final Context context;
    private static TaxiMeterManager instance;
    private TaxiMeterManager(Context context) {this.context = context;}
    public static TaxiMeterManager i(Context context) {
        if(instance == null) {
            instance = new TaxiMeterManager(context);
        }
        return instance;
    }

    private static final int INCREASE_PRICE_TIME = 45 * 1000; //milliseconds
    private static final int DISTANCE_SEGMENT_THRESHOLD = 250;

//    long elapsedTime;

    float currentPrice;
    Timer checkPriceIncreaseTimer;

    List<LatLng> segmentLocations;

    Location lastLocation = null;
    int likelyDistanceSegment = 0;

    public void clear() {
        currentPrice = 0.0f;
        segmentLocations = new ArrayList<>();
    }
    public void clearLocations() {
        segmentLocations = new ArrayList<>();
    }

    public void resetSegment() {
        likelyDistanceSegment = 0;
//        elapsedTime = new Date().getTime();
        if(checkPriceIncreaseTimer==null)checkPriceIncreaseTimer = new Timer();
        checkPriceIncreaseTimer.cancel();
        checkPriceIncreaseTimer.schedule(checkPriceIncreaseSegment,INCREASE_PRICE_TIME);
    }

    private int percentageSubtractAccuracy(int distance, int accuracy) {
        //todo distance percentage of threshold and use it to determine above or below 50%
        //todo then subtract a third or a fourth or even a fifth depending
        //todo put that in a configuration file?

    }

    public void newLocationAvailable(Location location) {
        //accuracy might be real bad
        if(location.getAccuracy() < 80) {
            if(lastLocation!=null) {

                int minimumDistanceSoFar = (int)(location.distanceTo(lastLocation));
                minimumDistanceSoFar -= (lastLocation.getAccuracy()/3);
                minimumDistanceSoFar -= (location.getAccuracy()/3);

                //keep segment locations anyhow
                segmentLocations.add(GpsUtil.getLatLng(location));
                //minimum distance can be negative if distance is too close, don't process it
                if(minimumDistanceSoFar > 0) {
                    minimumDistanceSoFar += likelyDistanceSegment;

                    likelyDistanceSegment += minimumDistanceSoFar;


                    if(minimumDistanceSoFar > 250) {
                        likelyMetThresholdDistance(segmentLocations);
                    }
                }


            }
            lastLocation = location;
        }

//        long difference = new Date().getTime() - elapsedTime;
//        if(difference < INCREASE_PRICE_TIME) {
//        }

    }
    public void likelyMetThresholdDistance(List<LatLng> snapshotLocations) {

        float distanceSinceLastSegment = snapLocationsBatchAndCalcDistance(snapshotLocations);

        if(distanceSinceLastSegment > DISTANCE_SEGMENT_THRESHOLD) {

            resetSegment();
            clearLocations();

            float calculateFareDistance = FareUtil.calculateFareMexNoFee(distanceSinceLastSegment,0);
            currentPrice += calculateFareDistance;
        }


    }

    public float getTotalPrice() {
        return currentPrice;
    }


    private float snapLocationsBatchAndCalcDistance(List<LatLng> snapshotLocations) {
        HttpController controller = new HttpController(context);
        SnapToRoadService snapToRoadService = controller.requestSnapToRoad(segmentLocations);
        List<SnappedPoints> snappedPoints = Arrays.asList(snapToRoadService.getSnappedPoints());

        float distanceInMeters = 0;
        Location lastPoint = null;

        for(SnappedPoints point: snappedPoints) {

            Location tempLocation = new Location("");
            tempLocation.setLatitude(point.getLocation().getLatitude());
            tempLocation.setLongitude(point.getLocation().getLongitude());

            if(lastPoint != null) {
                distanceInMeters += lastPoint.distanceTo(tempLocation);
            }
            lastPoint = tempLocation;
        }
        return distanceInMeters;
    }

    TimerTask checkPriceIncreaseSegment = new TimerTask() {
        @Override
        public void run() {
            // still keep 45 second rate steady independently of calculation of new segment
            resetSegment();

            float calculateFareDuration = FareUtil.calculateFareMexNoFee(0,45);
            currentPrice += calculateFareDuration;

        }
    };
}
