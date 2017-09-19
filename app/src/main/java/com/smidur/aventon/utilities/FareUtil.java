package com.smidur.aventon.utilities;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by marqueg on 7/3/17.
 */

public class FareUtil {

    private static final float MINUTE = 1.80F;
    private static final float KM = 1.80F;

    public static final float MINIMUM_FARE = 35.0F;

    public static float calculateInitialFareMex(Context context, float distance, float duration) {

        float durationPrice = (duration/60) * MINUTE;
        float distancePrice = (distance/1000.0f) * KM;

        float banderazo = 7.0f;
        float totalPrice = 0.0f;
        if(distancePrice > durationPrice) {
            totalPrice = distancePrice+banderazo;
        } else {
            totalPrice =  durationPrice+banderazo;
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
