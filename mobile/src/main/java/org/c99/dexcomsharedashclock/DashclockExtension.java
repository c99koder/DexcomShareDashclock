package org.c99.dexcomsharedashclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sam on 11/15/15.
 */
public class DashclockExtension extends DashClockExtension {
    public final static String REFRESH_INTENT = "org.c99.dexcomsharedashclock.REFRESH";
    RefreshReceiver receiver;

    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdateData(0);
        }
    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        if (receiver != null)
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        IntentFilter intentFilter = new IntentFilter(REFRESH_INTENT);
        receiver = new RefreshReceiver();
        registerReceiver(receiver, intentFilter);

        setUpdateWhenScreenOn(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null)
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @Override
    protected void onUpdateData(int reason) {
        if(PreferenceManager.getDefaultSharedPreferences(this).contains("token")) {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.dexcom.follow");
            if(PreferenceManager.getDefaultSharedPreferences(this).contains("Trend") && PreferenceManager.getDefaultSharedPreferences(this).contains("Value")) {
                int trend = PreferenceManager.getDefaultSharedPreferences(this).getInt("Trend", 8);
                int value = PreferenceManager.getDefaultSharedPreferences(this).getInt("Value", 0);
                long time = PreferenceManager.getDefaultSharedPreferences(this).getLong("Time", 0);
                int icon = R.drawable.ic_fail;
                switch(trend) {
                    case 1:
                        icon = R.drawable.ic_trend_rise_rapid;
                        break;
                    case 2:
                        icon = R.drawable.ic_trend_rise;
                        break;
                    case 3:
                        icon = R.drawable.ic_trend_rise_slow;
                        break;
                    case 4:
                        icon = R.drawable.ic_trend_steady;
                        break;
                    case 5:
                        icon = R.drawable.ic_trend_fall_slow;
                        break;
                    case 6:
                        icon = R.drawable.ic_trend_fall;
                        break;
                    case 7:
                        icon = R.drawable.ic_trend_fall_rapid;
                        break;
                }

                publishUpdate(new ExtensionData()
                                .visible(true)
                                .icon(icon)
                                .status(NumberFormat.getInstance().format(value))
                                .expandedTitle(NumberFormat.getInstance().format(value) + " mg/dL")
                                .expandedBody("Updated " + SimpleDateFormat.getTimeInstance().format(new Date(time)))
                                .clickIntent(i)
                );
            } else {
                publishUpdate(new ExtensionData()
                                .visible(true)
                                .icon(R.drawable.ic_fail)
                                .status("?")
                                .expandedTitle("No Glucose Data")
                                .clickIntent(i)
                );
            }
        } else {
            Intent i = new Intent(this, LoginActivity.class);
            publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_fail)
                            .status("?")
                            .expandedTitle("Not logged in")
                            .expandedBody("Tap to login to Dexcom Share")
                            .clickIntent(i)
            );
        }
    }
}