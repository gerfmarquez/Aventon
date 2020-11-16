package com.smidur.aventon;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class AventonApplication extends Application {
    private static final String LOG_TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Application.onCreate - Initializing application...");
        super.onCreate();
        initializeApplication();
        Log.d(LOG_TAG, "Application.onCreate - Application initialized OK");

        initCrashlytics();

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    private void initializeApplication() {
        AWSMobileClient.initializeMobileClientIfNecessary(getApplicationContext());

        // ...Put any application-specific initialization logic here...
    }
    private void initCrashlytics() {
        Crashlytics crashlyticsKit = new com.crashlytics.android.Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(false).build())
                .build();
        Fabric.with(this, crashlyticsKit, new Crashlytics());
    }
}
