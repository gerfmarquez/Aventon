package com.smidur.aventon.utilities;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by marqueg on 7/22/17.
 */

public class DetectedActivityIntentService extends IntentService {
    public DetectedActivityIntentService() {
        super("Detected Activity Intent Service");
    }
    @Override
    protected void onHandleIntent(Intent intent) {

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();


        GoogleApiWrapper.getInstance(this).postDetectedActivityCallback(detectedActivities);
    }
}
