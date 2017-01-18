package com.smidur.aventon;


import android.app.Application;
import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;

/**
 * Created by marqueg on 1/16/17.
 */

public class AventonApplication extends Application {

    private String TAG = "AventonApplicaton";

    @Override
    public void onCreate() {
        Log.d(TAG, "Application.onCreate - Initializing application...");
        super.onCreate();
        initializeApplication();
        super.onCreate();
        initializeApplication();
        Log.d(TAG, "Application.onCreate - Application initialized OK");
    }

    private void initializeApplication() {

        // Initialize the AWS Mobile Client
        AWSMobileClient.initializeMobileClientIfNecessary(getApplicationContext());

        // ... Put any application-specific initialization logic here ...
    }
}
