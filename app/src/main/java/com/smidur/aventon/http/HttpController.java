package com.smidur.aventon.http;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Created by marqueg on 4/16/17.
 */

public class HttpController {

    Context context;

    public static final int HTTP_OK = 200;
    HttpWrapper wrapper;

    public HttpController(Context context) {
        this.context = context;
    }

    //todo generic callback? or specific? delivery generic message instead?
    public interface RidesAvailableCallback {
        void onNewRideAvailable(String message);
    }
    public interface SchedulePickupCallback {
        void onConfirmedPickupScheduled(String message);
    }

    public void availableRidesCall(@NonNull  final RidesAvailableCallback callback) throws IOException {

        wrapper = new HttpWrapper();

        wrapper.httpGET("available_rides", new HttpWrapper.UpdateCallback() {
            @Override
            public void onUpdate(String message) {

                callback.onNewRideAvailable(message);

            }
        },context);


    }

    public void schedulePickupCall(@NonNull  final SchedulePickupCallback callback) throws IOException {

        wrapper = new HttpWrapper();

        wrapper.httpGET("shcedule_pickup", new HttpWrapper.UpdateCallback() {
            @Override
            public void onUpdate(String message) {

                callback.onConfirmedPickupScheduled(message);

            }
        },context);


    }

    public boolean closeStream() {

        wrapper = new HttpWrapper();

        if(wrapper!=null && wrapper.getStreamReader()!=null) {
            try {
                wrapper.getStreamReader().close();
                return true;
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return false;
    }

}
