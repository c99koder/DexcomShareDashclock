package org.c99.dexcomsharedashclock;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GlucoseDisplayActivity extends Activity {

    private TextView mValue;
    private ImageView mTrend;
    private TextView mTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glucose);
        mTrend = (ImageView) findViewById(R.id.trend);
        mValue = (TextView) findViewById(R.id.value);
        mTime = (TextView) findViewById(R.id.time);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(getIntent() != null && getIntent().hasExtra("trend")) {
            int icon = 0;
            switch(getIntent().getIntExtra("trend", 0)) {
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
            if(icon != 0) {
                mTrend.setImageResource(icon);
            }
        }

        if(getIntent() != null && getIntent().hasExtra("value"))
            mValue.setText(getIntent().getIntExtra("value", 0) + " mg/dL");

        if(getIntent() != null && getIntent().hasExtra("time"))
            mTime.setText("Updated "+ DateUtils.getRelativeTimeSpanString(getIntent().getLongExtra("time", 0), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

    }
}
