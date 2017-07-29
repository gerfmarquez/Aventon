package com.smidur.aventon.utilities;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Created by marqueg on 1/25/17.
 */

public class NotificationUtil {

    static int notificationCount = 0;

    public static void createNotification(Context context,String title) {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification.Builder(context).
                setContentTitle(title).
                setAutoCancel(true).
                setSmallIcon(android.R.drawable.ic_notification_overlay).

                setContentText(
                        title).build();
        notificationCount += 1;

        notifMan.notify(notificationCount, notif);
    }
}
