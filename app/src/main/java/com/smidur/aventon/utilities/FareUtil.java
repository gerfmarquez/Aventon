package com.smidur.aventon.utilities;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by marqueg on 7/3/17.
 */

public class FareUtil {
    public static float calculateFareMex(Context context, float distance, float duration) {
        Calendar time = Calendar.getInstance();
        int hourOfDay = time.get(Calendar.HOUR_OF_DAY);

        float banderazo = 9.0f;
        banderazo *= 1.20;//add 20 percent more
        float perEach = 1.07f;
        perEach *= 1.20;
        float durationPrice = (duration/45) * perEach;
        float distancePrice = (distance/250.0f) * perEach;

        float totalPrice = 0.0f;
        if(distancePrice > durationPrice) {
            totalPrice = distancePrice+banderazo;
        } else {
            totalPrice =  durationPrice+banderazo;
        }

        if(isTimeAutomatic(context) && hourOfDay > 22 && hourOfDay < 6) {
            totalPrice *= 1.20f;
        }

        return totalPrice;
    }
    public static float calculateFareMexNoFee(Context context,float distance, float duration) {
        Calendar time = Calendar.getInstance();

        int hourOfDay = time.get(Calendar.HOUR_OF_DAY);


        float perEach = 1.07f;
        perEach *= 1.20;
        float durationPrice = (duration/45) * perEach;
        float distancePrice = (distance/250.0f) * perEach;

        float totalPrice = 0.0f;
        if(distancePrice > durationPrice) {
            totalPrice = distancePrice;
        } else {
            totalPrice =  durationPrice;
        }

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
