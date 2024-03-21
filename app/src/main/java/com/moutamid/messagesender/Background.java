package com.moutamid.messagesender;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fxn.stash.Stash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Background extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MessageSender";
    private static final String CHANNEL_NAME = "Message Sender";
    private Context context;
    private static final String TAG = "Background";

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
        if (!Stash.getBoolean("key", false)) {
            Constants.databaseReference().addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (!Stash.getBoolean("key", false)) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                                MessageModel stashMessage = (MessageModel) Stash.getObject(String.valueOf(messageModel.number), MessageModel.class);
                                if (stashMessage != null){
                                    if (!stashMessage.message.equals(messageModel.message)) {
                                        sendAutoMessage(messageModel);
                                    }
                                } else {
                                    sendAutoMessage(messageModel);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        return START_STICKY;
    }

    private void sendAutoMessage(MessageModel message) {
        Log.d(TAG, "inside sendAutoMessage");
        try {
            String SENT = "SMS_SENT";
            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), PendingIntent.FLAG_IMMUTABLE);
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message.message);
            ArrayList<PendingIntent> sendList = new ArrayList<>();
            sendList.add(sentPI);
            ArrayList<PendingIntent> deliverList = new ArrayList<>();
            deliverList.add(sentPI);
            sms.sendMultipartTextMessage(String.valueOf(message.number), null, parts, sendList, deliverList);
            Log.d(TAG, "SMS sent successfully");
            Stash.put(String.valueOf(message.number), message);
        } catch (ActivityNotFoundException ae) {
            ae.printStackTrace();
            Log.d(TAG, "ActivityNotFoundException \t " + ae.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Missed Calll E \t " + e.getMessage());
        }
    }

    private Notification createNotification() {
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Message Sending Service")
                    .setContentText("Listening for any changes...")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .build();
        } else {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Message Sending Service")
                    .setContentText("Listening for any changes...")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .build();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
