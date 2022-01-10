package com.whatshide.android.Notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

public class OreoNotification extends ContextWrapper {
    public static final String CHANNEL_ID = "com.whatshide.android";
    public static final String CHANNEL_NAME = "whatshide";

    private NotificationManager notificationManager;
    public OreoNotification(Context base) {
        super(base);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            createChannel();
        }
    }
    @TargetApi(Build.VERSION_CODES.O)
    public void createChannel(){
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME,NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(false);
        notificationChannel.enableVibration(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

    }

    public NotificationManager getManager(){
        if(notificationManager == null){
            notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getOreoNotification(String title, String body, PendingIntent pendingIntent, Uri uri, String icon){
        return new Notification.Builder(getApplicationContext(),CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentText(body)
                .setContentTitle(title)
                .setAutoCancel(true);
    }

}
