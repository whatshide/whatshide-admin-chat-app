package com.whatshide.android.Notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.whatshide.android.ChatActivity;
import com.whatshide.android.utilities.Constants;

public class MyFirebaseIdService extends FirebaseMessagingService {
    private static String CHANNEL_ID = "com.whatshide.android";
    private static String CHANNEL_NAME = "whatshide";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("tokens");
            Token token = new Token(s);
            databaseReference.child(user.getPhoneNumber()).setValue(token);
        }

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String sented = remoteMessage.getData().get("sented");
        Log.d("user check", "onMessageReceived: "+FirebaseAuth.getInstance().getCurrentUser().getUid()+sented);
        Log.d("this", "onMessageReceived: ");

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            sendOeroNotification(remoteMessage);
        }else {
            sendNotification(remoteMessage);

        }

    }
    @TargetApi(Build.VERSION_CODES.O)
    private void sendOeroNotification(RemoteMessage remoteMessage){
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userid",user);
        intent.putExtras(bundle);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        OreoNotification oreoNotification = new OreoNotification(this);
        Notification.Builder builder = oreoNotification.getOreoNotification(title,body,pendingIntent,defaultSound,icon);
        Log.d("this", "sendOeroNotification:23 ");
        int i = 0;
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
        notificationManager.notify(i,builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]",""));
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.KEY_UID,user);
        intent.putExtras(bundle);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,j,intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        int i = 0;
        if(j>0){
            i = j;
        }
        notificationManager.notify(i,builder.build());

    }
}
