package org.c99.dexcomsharedashclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

public class SyncBroadcastReceiver extends BroadcastReceiver {
    public SyncBroadcastReceiver() {
    }

    public static void schedule(Context context) {
        ComponentName receiver = new ComponentName(context, SyncBroadcastReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        Log.d("DexcomShareDashclock", "Removing pending alarms");
        alarmMgr.cancel(alarmIntent);

        Log.d("DexcomShareDashclock", "Scheduling alarm to run in 30 minutes");
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000 * 60 * 30, alarmIntent);
        } else {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000 * 60 * 30, alarmIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DexcomShareDashclock", "Sync broadcast received");
        new SyncTask(context).execute((Void)null);
        schedule(context);
    }

    private class SyncTask extends AsyncTask<Void, Void, Void> {
        private PowerManager.WakeLock wl;
        private WifiManager.WifiLock wifiLock;
        private Context context;

        public SyncTask(Context c) {
            context = c;
        }

        @Override
        protected void onPreExecute() {
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DexcomShareDashclock");
            wl.acquire();

            WifiManager wfm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            wifiLock = wfm.createWifiLock("DexcomShareDashclock");
            wifiLock.acquire();
        }

        @Override
        protected Void doInBackground(Void... params) {
            BackgroundTaskService.runGlucoseSync(context);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            wl.release();
            wifiLock.release();
            Log.d("DexcomShareDashclock", "Sync broadcast complete");
        }
    }
}
