package org.c99.dexcomsharedashclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class DataListenerService extends WearableListenerService {
    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if(messageEvent.getPath().compareTo("/latest_glucose") == 0) {
            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
            int trend = dataMap.getInt("trend");
            int value = dataMap.getInt("value");
            long time = dataMap.getLong("time");

            Intent displayIntent = new Intent(this, GlucoseDisplayActivity.class);
            displayIntent.putExtra("trend", trend);
            displayIntent.putExtra("value", value);
            displayIntent.putExtra("time", time);

            Notification notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Blood Glucose")
                    .setContentText(value + " mg/dL")
                    .extend(new Notification.WearableExtender()
                            .setDisplayIntent(PendingIntent.getActivity(this, 0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                    .build();
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification);
        }
    }
}
