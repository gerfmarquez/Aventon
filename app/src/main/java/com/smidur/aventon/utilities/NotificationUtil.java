package com.smidur.aventon.utilities;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;

import com.smidur.aventon.R;

/**
 * Created by marqueg on 1/25/17.
 */

public class NotificationUtil {

    private final Context context;
    private static NotificationUtil instance;
    private NotificationUtil(Context context) {this.context = context;}
    public static NotificationUtil i(Context context) {
        if(instance == null) {
            instance = new NotificationUtil(context);
        }
        return instance;
    }

    static final int ongoingRideNotifId = 131415;
    static  int newAvailableRide = 141516;

    public void updateOngoingRideNotification(float cost, int distance) {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String format = context.getString(R.string.ride_summary).replace("ddd","%d").replace("fff","%.2f");
        String rideSummaryText = String.format(format,distance,cost);

        Notification notif = new Notification.Builder(context).
                setContentTitle(context.getString(R.string.ride_summary_title)).
                setAutoCancel(true).
                setOngoing(true).
                setSmallIcon(android.R.drawable.ic_notification_overlay).

                setContentText(rideSummaryText).build();

        notifMan.notify(ongoingRideNotifId, notif);
    }
    public void endOngoingRideNotification() {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMan.cancel(ongoingRideNotifId);

    }

    public void createNewRideAvailableNotification() {

        newAvailableRide++;

        final NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notif = new Notification.Builder(context).
                setContentTitle("New Ride Available").
                setAutoCancel(true).

                setSmallIcon(android.R.drawable.ic_notification_overlay).

                setContentText("New Ride Available Nearby At: ").build();

        notifMan.notify(newAvailableRide, notif);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notifMan.cancel(newAvailableRide);
            }
        },30 * 1000);
        //todo move to constants class with time outs


    }

}
