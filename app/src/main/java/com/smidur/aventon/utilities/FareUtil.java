package com.smidur.aventon.utilities;

import android.os.SystemClock;

import java.util.Calendar;

/**
 * Created by marqueg on 7/3/17.
 */

public class FareUtil {
    public static float calculateFareMex(float distance, float duration) {
        Calendar time = Calendar.getInstance();
        int hourOfDay = time.get(Calendar.HOUR_OF_DAY);


        float banderazo = 9.0f;
        float perEach = 1.07f;
        float durationPrice = (duration/45) * perEach;
        float distancePrice = (distance/250.0f) * perEach;

        float totalPrice = 0.0f;
        if(distancePrice > durationPrice) {
            totalPrice = distancePrice+banderazo;
        } else {
            totalPrice =  durationPrice+banderazo;
        }

        if(hourOfDay > 22 && hourOfDay < 6) {
            totalPrice *= 1.20f;
        }

        return totalPrice;
    }
}
