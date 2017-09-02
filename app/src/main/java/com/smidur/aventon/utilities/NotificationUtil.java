package com.smidur.aventon.utilities;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.RemoteViews;

import com.smidur.aventon.MainActivity;
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

    private PowerManager.WakeLock wakeLock;

    public void updateOngoingRideNotification(float cost, int distance) {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String format = context.getString(R.string.ride_summary).replace("ddd","%d").replace("fff","%.2f");
        String rideSummaryText = String.format(format,distance,cost);

        Notification notif = new Notification.Builder(context).
                setContentTitle(context.getString(R.string.ride_summary_title)).
                setAutoCancel(true).
                setOngoing(true).
                setSmallIcon(R.drawable.ic_launcher).

                setContentText(rideSummaryText).build();

        notifMan.notify(ongoingRideNotifId, notif);
    }
    public void endOngoingRideNotification() {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMan.cancel(ongoingRideNotifId);

    }
    public void cancelIncomingRideRequestNotification() {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMan.cancel(newAvailableRide);

    }

    public void createNewRideAvailableNotification() {

        //acquire wakelock for few seconds to let user confirm or discard ride that just popped up!
        turnDisplayOn();
        chime();
        newAvailableRide++;

        Intent confirmRideIntent = new Intent(context, MainActivity.class);
        confirmRideIntent.putExtra("confirm_ride",true);

        Intent rejectRideIntent = new Intent(context, MainActivity.class);
        rejectRideIntent.putExtra("reject_ride",true);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 16151413, confirmRideIntent,   PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteWidget = new RemoteViews(context.getPackageName(), R.layout.new_ride_available);
        remoteWidget.setOnClickPendingIntent(R.id.confirm,pendingIntent);

        pendingIntent = PendingIntent.getActivity(context, 16151415, rejectRideIntent,   PendingIntent.FLAG_UPDATE_CURRENT);
        remoteWidget.setOnClickPendingIntent(R.id.reject,pendingIntent);

        final NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notif = new Notification.Builder(context).

                setAutoCancel(true).
                setContent(remoteWidget).

                setSmallIcon(R.drawable.ic_launcher).
                setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher)).
                build();

        notifMan.notify(newAvailableRide, notif);



        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notifMan.cancel(newAvailableRide);
                releaseWakeLock();
            }
        },30 * 1000);
        //todo move to constants class with time outs


    }
    public void createNewArrivedDestinationNotification(String title, String text) {

        chime();
        newAvailableRide++;

        final NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notif = new Notification.Builder(context).

                setAutoCancel(true).
                setContentTitle(title).
                setContentText(text).

                setSmallIcon(R.drawable.ic_launcher).
                setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher)).
                build();

        notifMan.notify(newAvailableRide, notif);


    }

    private void turnDisplayOn() {
        PowerManager pw = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pw.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock.acquire();
    }
    private void releaseWakeLock() {
        if(wakeLock!=null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    public void chime() {
        Ringtone ringtone = RingtoneManager.getRingtone(context,
                RingtoneManager.getActualDefaultRingtoneUri(context,RingtoneManager.TYPE_NOTIFICATION));
        ringtone.play();

    }

//    class RemoteControlWidget extends RemoteViews
//    {
//        private final Context mContext;
//
//        public static final String ACTION_CONFIRM = "com.smidur.aventon.ACTION_CONFIRM";
//        public static final String ACTION_REJECT = "com.smidur.aventon.ACTION_REJECT";
//
//
//        public RemoteControlWidget(Context context , String packageName, int layoutId)
//        {
//            super(packageName, layoutId);
//            mContext = context;
//            Intent intent = new Intent(ACTION_CONFIRM);
//            PendingIntent pendingIntent = PendingIntent.getService(mContext.getApplicationContext(),100,
//                    intent,PendingIntent.FLAG_UPDATE_CURRENT);
//            setOnClickPendingIntent(R.id.reject,pendingIntent);
//            intent = new Intent(ACTION_REJECT);
//            pendingIntent = PendingIntent.getService(mContext.getApplicationContext(),101,
//                    intent,PendingIntent.FLAG_UPDATE_CURRENT);
//            setOnClickPendingIntent(R.id.confirm,pendingIntent);
//
//        }
//    }

}
