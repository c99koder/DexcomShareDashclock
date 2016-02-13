package org.c99.dexcomsharedashclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class SyncBroadcastReceiver extends BroadcastReceiver {
    public static final long SYNC_INTERVAL = 1000 * 60 * 15;

    public SyncBroadcastReceiver() {
    }

    public static void schedule(Context context, long interval) {
        ComponentName receiver = new ComponentName(context, SyncBroadcastReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, alarmIntent);
        } else {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, alarmIntent);
        }

        Log.d("DexcomShareDashclock", "Sync scheduled in " + (int)(interval/60000) + " minutes");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DexcomShareDashclock", "Sync broadcast received");
        new SyncTask(context).execute((Void)null);
        schedule(context, SYNC_INTERVAL);
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
            Dexcom dexcom = new Dexcom();

            try {
                String token = dexcom.login(PreferenceManager.getDefaultSharedPreferences(context).getString("username", ""),
                        PreferenceManager.getDefaultSharedPreferences(context).getString("password", ""));
                if (token != null && token.length() > 0 && token.startsWith("\"")) {
                    token = token.substring(1, token.length() - 2); //Strip the "s
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    editor.putString("token", token);
                    editor.apply();
                    JSONArray a = dexcom.latestGlucoseValues(token, 1440, 1);
                    if(a != null && a.length() > 0) {
                        JSONObject o = a.getJSONObject(0);
                        final int trend = o.getInt("Trend");
                        final int value = o.getInt("Value");
                        String WT = o.getString("WT");
                        final long time = Long.valueOf(WT.substring(6, WT.length() - 2));
                        Log.i("DexcomShareDashclock", "Latest glucose reading: " + value + " mg/dL");

                        editor.putInt("Trend", trend);
                        editor.putInt("Value", value);
                        editor.putLong("Time", time);
                        editor.apply();

                        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                                .addApi(Wearable.API)
                                .build();

                        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

                        if (!connectionResult.isSuccess()) {
                            Log.e("DexcomShareDashclock", "Failed to connect to GoogleApiClient.");
                            return null;
                        }

                        DataMap map = new DataMap();
                        map.putInt("trend", trend);
                        map.putInt("value", value);
                        map.putLong("time", time);

                        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( googleApiClient ).await();
                        for(Node node : nodes.getNodes()) {
                            Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), "/latest_glucose", map.toByteArray() ).await();
                        }

                        googleApiClient.disconnect();
                    }
                } else {
                    Log.e("Dexcom", "Response: " + token);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
