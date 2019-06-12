package com.gae.scaffolder.plugin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.content.Intent;
import android.media.RingtoneManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.Notification;
import java.util.Map;
import java.util.HashMap;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


import org.json.JSONException;
import org.json.JSONObject;
import org.sunbird.app.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Felipe Echanique on 08/06/2016.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMPlugin";

    private String getStringResource(String name) {
        return this.getString(
                this.getResources().getIdentifier(name, "string", this.getPackageName())
        );
    }

    public class NotificationUtils extends ContextWrapper {
        private NotificationManager mManager;
        public static final String ANDROID_CHANNEL_ID = "announcement";
        public static final String ANDROID_CHANNEL_NAME = "Announcement";
        public NotificationUtils(Context base) {
            super(base);
            createChannels();
        }

        public void createChannels() {
            // create android channel
            NotificationChannel androidChannel = new NotificationChannel(ANDROID_CHANNEL_ID,
                    ANDROID_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            // Sets whether notifications posted to this channel should display notification lights
            androidChannel.enableLights(true);
            // Sets whether notification posted to this channel should vibrate.
            androidChannel.enableVibration(true);
            // Sets the notification light color for notifications posted to this channel
            androidChannel.setLightColor(Color.GREEN);
            // Sets whether notifications posted to this channel appear on the lockscreen or not
            androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getManager().createNotificationChannel(androidChannel);
        }

        private NotificationManager getManager() {
            if (mManager == null) {
                mManager = (NotificationManager)
                        getSystemService(Context.NOTIFICATION_SERVICE);
            }
            return mManager;
        }
    }
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d(TAG, "==> MyFirebaseMessagingService onMessageReceived");
        Log.d(TAG, "==>" + remoteMessage.toString());
        if( remoteMessage.getNotification() != null){
            Log.d(TAG, "\tNotification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "\tNotification Message: " + remoteMessage.getNotification().getBody());
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("wasTapped", false);
        for (String key : remoteMessage.getData().keySet()) {
            Object value = remoteMessage.getData().get(key);
            Log.d(TAG, "\tKey: " + key + " Value: " + value);
            data.put(key, value);
        }

        Log.d(TAG, "\tNotification Data: " + data.toString());
        FCMPlugin.sendPushPayload( data );
        //To get a Bitmap image from the URL received
        Map<String,String> notificationData = remoteMessage.getData();
        try {
            JSONObject actionData = new JSONObject(notificationData.get("actionData"));
            sendNotification(actionData.get("title").toString(), actionData.get("description").toString(), data, actionData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String title, String messageBody, Map<String, Object> data, JSONObject actionData) {

        Log.d(TAG, "\tEnter Send notification method: ");
        Intent intent = new Intent(this, FCMPluginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        for (String key : data.keySet()) {
            intent.putExtra(key, data.get(key).toString());
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Log.d(TAG, "\tBitmap is running: ");
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("announcement", "Announcement",NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Announcement");
            channel.enableLights(true);
            // Sets whether notification posted to this channel should vibrate.
            channel.enableVibration(true);
            // Sets the notification light color for notifications posted to this channel
            channel.setLightColor(Color.GREEN);
            notificationManager.createNotificationChannel(channel);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.n_icon)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setChannelId("announcement")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            try {
                if(actionData.get("thumbnail") != null) {
                    Bitmap bitmap = getBitmapfromUrl(actionData.get("thumbnail").toString());
                    notificationBuilder.setLargeIcon(bitmap);
                }
                if(actionData.get("banner") != null) {
                    Bitmap bitmap = getBitmapfromUrl(actionData.get("banner").toString());
                    notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        } else {

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.n_icon)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            try {
                if(actionData.get("thumbnail") != null) {
                    Bitmap bitmap = getBitmapfromUrl(actionData.get("thumbnail").toString());
                    notificationBuilder.setLargeIcon(bitmap);
                }
                if(actionData.get("banner") != null) {
                    Bitmap bitmap = getBitmapfromUrl(actionData.get("banner").toString());
                    notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            notificationManager.notify(100 /* ID of notification */, notificationBuilder.build());
        }
        //https://stackoverflow.com/questions/46990995/on-android-8-1-api-27-notification-does-not-display

    }

    /*
     *To get a Bitmap image from the URL received
     * */
    public Bitmap getBitmapfromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;

        }
    }
}
