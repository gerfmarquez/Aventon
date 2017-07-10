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
import java.util.Date;
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

//    long elapsedTime;

    float currentPrice;
    Timer checkPriceIncreaseTimer;

    List<LatLng> segmentLocations;

    public void clear() {
        currentPrice = 0.0f;
        segmentLocations = new ArrayList<>();
    }
    public void clearLocations() {
        segmentLocations = new ArrayList<>();
    }

    public void resetSegment() {
//        elapsedTime = new Date().getTime();
        if(checkPriceIncreaseTimer==null)checkPriceIncreaseTimer = new Timer();
        checkPriceIncreaseTimer.cancel();
        checkPriceIncreaseTimer.schedule(checkPriceIncreaseSegment,INCREASE_PRICE_TIME);
    }

    public void newLocationAvailable(Location location) {
        //accuracy might be real bad
        if(location.getAccuracy() < 80) {
            segmentLocations.add(GpsUtil.getLatLng(location));
        }

//        long difference = new Date().getTime() - elapsedTime;
//        if(difference < INCREASE_PRICE_TIME) {
//        }

    }
    public void newTimeSegmentAvailable(List<LatLng> snapshotLocations) {

        //todo before snapping which is expensive check if its worth it from number and distance of locations?
        float distanceSinceLastSegment = snapLocationsBatchAndCalcDistance(snapshotLocations);

        float calculateFareDistance = FareUtil.calculateFareMexNoFee(distanceSinceLastSegment,0);
        float calculateFareDuration = FareUtil.calculateFareMexNoFee(0,45);

        if(calculateFareDistance > calculateFareDuration) {
            currentPrice += calculateFareDistance;
        } else if (calculateFareDuration > calculateFareDistance) {
            currentPrice += calculateFareDuration;
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
            //make a copy of current locations and clear memory locations
            List<LatLng> snapshotLocations = new ArrayList<>(segmentLocations);
            clearLocations();
            newTimeSegmentAvailable(snapshotLocations);
        }
    };
}
