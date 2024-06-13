package com.example.fcmpushnotification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingService";
    private static final String CHANNEL_ID = "your_channel_id";
    private static final CharSequence CHANNEL_NAME = "Your Channel Name";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        try {
            // Handle data payload
            if (remoteMessage.getData().size() > 0) {
                handleDataPayload(remoteMessage.getData());
            }

            // Handle notification payload
            if (remoteMessage.getNotification() != null) {
                handleNotificationPayload(remoteMessage.getNotification());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling the message: " + e.getMessage());
        }
    }

    private void handleDataPayload(Map<String, String> data) {
        try {
            String title = data.getOrDefault("title", "Default Title");
            String body = data.getOrDefault("body", "Default Body");
            double latitude = Double.parseDouble(data.getOrDefault("latitude", "0.0"));
            double longitude = Double.parseDouble(data.getOrDefault("longitude", "0.0"));
            String action1 = data.getOrDefault("action1", "");
            String action2 = data.getOrDefault("action2", "");

            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Body: " + body);
            Log.d(TAG, "Action1: " + action1);
            Log.d(TAG, "Action2: " + action2);
            Log.d(TAG, "Latitude: " + latitude);
            Log.d(TAG, "Longitude: " + longitude);

            // Show Toast on the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(MyFirebaseMessagingService.this, "Latitude: " + latitude, Toast.LENGTH_SHORT).show();
                Toast.makeText(MyFirebaseMessagingService.this, "Longitude: " + longitude, Toast.LENGTH_SHORT).show();
            });

            generateNotification(this, title, body, action1, action2);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing data payload: " + e.getMessage());
        }
    }

    private void handleNotificationPayload(RemoteMessage.Notification notification) {
        try {
            String title = notification.getTitle() != null ? notification.getTitle() : "Default Title";
            String body = notification.getBody() != null ? notification.getBody() : "Default Body";

            generateNotification(this, title, body, "", "");
        } catch (Exception e) {
            Log.e(TAG, "Error handling notification payload: " + e.getMessage());
        }
    }

    private void generateNotification(Context context, String title, String body, String action1, String action2) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent acceptIntent = new Intent(context, AcceptBroadcastReceiver.class);
            PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent rejectIntent = new Intent(context, RejectBroadcastReceiver.class);
            PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(context, 0, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

            if (!action1.isEmpty()) {
                builder.addAction(new NotificationCompat.Action(0, action1, acceptPendingIntent));
            }
            if (!action2.isEmpty()) {
                builder.addAction(new NotificationCompat.Action(0, action2, rejectPendingIntent));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error generating notification: " + e.getMessage());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveTokenToDatabase(token);
        retrieveAndSaveFCMToken();
    }

    private void saveTokenToDatabase(String token) {
        String deviceId = getDeviceIdentifier();
        DatabaseReference tokenRef = FirebaseDatabase.getInstance().getReference("FCM_Tokens");
        tokenRef.child(deviceId).setValue(token);
    }

    private String getDeviceIdentifier() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void retrieveAndSaveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        saveTokenToDatabase(token);
                    }
                });
    }

    public static class AcceptBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Request Accepted", Toast.LENGTH_LONG).show();
        }
    }

    public static class RejectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Request Rejected", Toast.LENGTH_LONG).show();
        }
    }
}
