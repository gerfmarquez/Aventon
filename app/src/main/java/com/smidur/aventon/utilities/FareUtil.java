package com.smidur.aventon.utilities;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class FareUtil {

    private static final float MINUTE = 1.80F;
    private static final float KM = 1.80F;

    public static final float MINIMUM_FARE = 35.0F;
    public static final float BANDERAZO = 7.0f;

    public static float calculateInitialFareMex(Context context, float distance, float duration) {

        float durationPrice = (duration/60) * MINUTE;
        float distancePrice = (distance/1000.0f) * KM;


        float totalPrice = 0.0f;
        if(distancePrice > durationPrice) {
            totalPrice = distancePrice+BANDERAZO;
        } else {
            totalPrice =  durationPrice+BANDERAZO;
        }

        Calendar time = Calendar.getInstance();
        int hourOfDay = time.get(Calendar.HOUR_OF_DAY);
        if(isTimeAutomatic(context) && hourOfDay > 22 && hourOfDay < 6) {
            totalPrice *= 1.20f;
        }

        return totalPrice;
    }
    public static float calculateFareMexNoFee(Context context,float distance, float duration) {
        Calendar time = Calendar.getInstance();



        float durationPrice = (duration/60) * MINUTE;
        float distancePrice = (distance/1000.0f) * KM;

        float totalPrice = 0.0f;
        if(distancePrice > durationPrice) {
            totalPrice = distancePrice;
        } else {
            totalPrice =  durationPrice;
        }

        int hourOfDay = time.get(Calendar.HOUR_OF_DAY);

        if(isTimeAutomatic(context) && hourOfDay > 22 && hourOfDay < 6) {
            totalPrice *= 1.20f;
        }

        return totalPrice;
    }

    private static boolean isTimeAutomatic(Context context) {
        int automatic =  android.provider.Settings.Global.getInt(
                context.getContentResolver(), android.provider.Settings.Global.AUTO_TIME, 0);

        return automatic == 1;
    }
}
