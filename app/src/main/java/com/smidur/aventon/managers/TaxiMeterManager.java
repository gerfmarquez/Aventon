package com.smidur.aventon.managers;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.DetectedActivity;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.model.SnapToRoadService;
import com.smidur.aventon.model.SnappedPoints;
import com.smidur.aventon.model.SyncRideSummary;
import com.smidur.aventon.utilities.FareUtil;
import com.smidur.aventon.utilities.GoogleApiWrapper;
import com.smidur.aventon.utilities.NotificationUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.smidur.aventon.utilities.FareUtil.MINIMUM_FARE;

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
            instance.segmentLocations = new ArrayList<>();
        }
        return instance;
    }

    private static final int INCREASE_PRICE_TIME = 60 * 1000; //milliseconds
    private static final int DISTANCE_SEGMENT_THRESHOLD = 1000;//meters
    private static final int MAX_CACHE_GPS_LOCATION_TIME = 7000;//7 seconds

    private static final int MINIMUM_ACCURACY_ALLOWED = 65;//meters

    private boolean inVehicle = false;

    private boolean taxiMeterOn = false;

    private long timeTaxiMeterStarted;
    private long totalRideDistance;

    private float currentPrice;
    Timer checkPriceIncreaseTimer;

    List<Location> segmentLocations;

    Location lastLocation = null;
    int likelyDistanceSegment = 0;

    public void init() {
        currentPrice = 0.0f;
        segmentLocations = new ArrayList<>();
        checkPriceIncreaseTimer = new Timer();
        likelyDistanceSegment = 0;
        timeTaxiMeterStarted = System.currentTimeMillis();
        totalRideDistance = 0;

    }
    public void showInitialRideNotification() {
        taxiMeterOn = true;
        NotificationUtil.i(context).updateOngoingRideNotification(currentPrice, (int)totalRideDistance);
    }
    public synchronized void resetSegment(int rolloverMeters) {
            checkPriceIncreaseTimer.cancel();
            checkPriceIncreaseTimer = new Timer();
            checkPriceIncreaseTimer.schedule(new checkPriceIncreaseSegmentTask(),INCREASE_PRICE_TIME);
            likelyDistanceSegment = rolloverMeters;
    }
    public void clearLocations() {
        List<Location> newLocationsSegment = new ArrayList<>();
        if(segmentLocations.size() > 0) {
            //keep last recent location
            Location lastLocation = segmentLocations.get(segmentLocations.size()-1);
            long lastLocationTime  = lastLocation.getTime();
            long currentTime = System.currentTimeMillis();
            //if the last location's time was less than 10 seconds ago keep it
            if((currentTime - lastLocationTime) < MAX_CACHE_GPS_LOCATION_TIME){
                newLocationsSegment.add(lastLocation);
            }
        }

        segmentLocations = newLocationsSegment;
    }
    public void stopTaximeter() {
        init();
        clearLocations();
        taxiMeterOn = false;

    }



    public void startActivityDetectionForTaxiMeter() {
        GoogleApiWrapper.getInstance(context).requestDriverActivityUpdates(MAX_CACHE_GPS_LOCATION_TIME, new GoogleApiWrapper.DetectedActivityCallback() {
            @Override
            public void onDetectedActivity(List<DetectedActivity> detectedActivityList) {
                for(DetectedActivity detectedActivity: detectedActivityList) {

                    short confidence = (short)(detectedActivity.getConfidence());

                    switch (detectedActivity.getType()) {
                        case DetectedActivity.WALKING:
                            break;
                        case DetectedActivity.RUNNING:
                            break;
                        case DetectedActivity.IN_VEHICLE:
                            //todo don't disable driving until we're completely sure it stopped?
                            //maybe some offset of 10 seconds until disabling? probably NOT
                            if(confidence > 29) {
                                inVehicle = true;
                            } else {
                                inVehicle = false;
                            }
                            break;
                        case DetectedActivity.ON_BICYCLE:
                            break;
                        case DetectedActivity.TILTING:
                            break;
                        case DetectedActivity.ON_FOOT:
                            break;
                        case DetectedActivity.STILL:
                            break;
                    }
                }
            }
        });
    }


    public void newLocationAvailable(Location location) {
        if(!RideManager.i(context).isDriverOnRide())return;

        //accuracy might be real bad
        if(location.getAccuracy() < MINIMUM_ACCURACY_ALLOWED && inVehicle) {
            if(lastLocation!=null) {

                //method to subtract accuracies from distance
                float minimumDistanceSoFar = figureOutDistanceFromUnreliablePairOfLocations(location,lastLocation);
                //minimum distance can be negative if distance is too close, don't process it
                if(minimumDistanceSoFar > 0) {
                    //keep segment locations anyhow
                    segmentLocations.add(location);
                    likelyDistanceSegment += minimumDistanceSoFar;
                    //add a 10 meter margin of error before submitting to snap to road service
                    if(likelyDistanceSegment > DISTANCE_SEGMENT_THRESHOLD-(DISTANCE_SEGMENT_THRESHOLD*.75f)) {
                        likelyMetThresholdDistance(segmentLocations);
                    }
                }



            }
            lastLocation = location;
        }

    }

    private float figureOutDistanceFromUnreliablePairOfLocations(Location locationA, Location locationB) {

        float accuracyA = locationA.getAccuracy();
        float accuracyB = locationB.getAccuracy();

        int minimumDistanceSoFar = (int)(locationA.distanceTo(locationB));

//        if(minimumDistanceSoFar > 249) return 250;//triggers snap road check
//
//        float distanceRatioSubtract = minimumDistanceSoFar/(DISTANCE_SEGMENT_THRESHOLD);//less than a 100%
//        distanceRatioSubtract /= 2;//at most subtract half of the accuracy of each point
//
//        //LOWEST_ALLOWED_ACCURACY == 1-80 MAX
//
//        float subtractRatioA = ((accuracyA * distanceRatioSubtract)/(float)LOWEST_ALLOWED_ACCURACY);
//
//        float subtractRatioB = ((accuracyB * distanceRatioSubtract)/(float)LOWEST_ALLOWED_ACCURACY);
//
//
//        accuracyA -= subtractRatioA * accuracyA;
//        accuracyB -= subtractRatioB * accuracyB;
//
//
//        minimumDistanceSoFar -= accuracyA;
//        minimumDistanceSoFar -= accuracyB;
//
//        if(minimumDistanceSoFar < 0) return 0;

        return minimumDistanceSoFar;

    }

    private void likelyMetThresholdDistance(final List<Location> snapRoadLocations) {

        new Thread() {
            public void run() {
                List<Location> copySnapRoadLocations = new ArrayList<>(snapRoadLocations);


                float distanceSinceLastSegment = snapLocationsBatchAndCalcDistance(copySnapRoadLocations);

                if(distanceSinceLastSegment >= DISTANCE_SEGMENT_THRESHOLD) {

                    totalRideDistance += DISTANCE_SEGMENT_THRESHOLD;

                    int rollOverDistance = (int)(distanceSinceLastSegment - DISTANCE_SEGMENT_THRESHOLD);
                    resetSegment(rollOverDistance);
                    clearLocations();

                    //interrupt current ongoing gps rate to start over as soon as possible.
                    RideManager.i(context).reAcquireGpsSignal();

                    float calculateFareDistance = FareUtil.calculateFareMexNoFee(context,distanceSinceLastSegment,0);
                    currentPrice += calculateFareDistance;

                    NotificationUtil.i(context).updateOngoingRideNotification(currentPrice, (int)totalRideDistance);
                }
            }
        }.start();



    }

    public SyncRideSummary getRideSummary() {
        SyncRideSummary rideSummary = new SyncRideSummary();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

        long rideDuration = System.currentTimeMillis() - timeTaxiMeterStarted;
        rideSummary.setDuration(rideDuration);
        rideSummary.setDistance(totalRideDistance);
        rideSummary.setTimeCompleted(System.currentTimeMillis());
        if(currentPrice < MINIMUM_FARE) currentPrice = MINIMUM_FARE;
        rideSummary.setTotalCost(currentPrice);
        rideSummary.setDateTimeCompleted(simpleDateFormat.format(Calendar.getInstance().getTime()));

        return rideSummary;
    }

    /**
     * This method returns a distance of zero if there is any connection issue.
     * @param snapshotLocations
     * @return
     */
    private float snapLocationsBatchAndCalcDistance(List<Location> snapshotLocations) {
        HttpController controller = new HttpController(context);
        SnapToRoadService snapToRoadService = controller.requestSnapToRoad(snapshotLocations);
        if(snapToRoadService == null) return 0;
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

    class checkPriceIncreaseSegmentTask extends TimerTask {
        @Override
        public void run() {
            if(!taxiMeterOn)return;
            // still keep 45 second rate steady independently of calculation of new segment
            resetSegment(0);
            clearLocations();


            float calculateFareDuration = FareUtil.calculateFareMexNoFee(context,0,60);
            currentPrice += calculateFareDuration;

            //interrupt current ongoing gps rate to start over as soon as possible.
            RideManager.i(context).reAcquireGpsSignal();

            NotificationUtil.i(context).updateOngoingRideNotification(currentPrice,(int)totalRideDistance);

        }
    };
}
