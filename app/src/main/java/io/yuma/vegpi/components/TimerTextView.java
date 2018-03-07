package io.yuma.vegpi.components;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * Created by bkennedy on 2/27/18.
 */

public class TimerTextView extends AppCompatTextView {

    private Handler mHandler = new Handler();
    private int mDay, mHour, mMinute, mSecond;// variables holding the hour and minute
    private Runnable mUpdate = new Runnable() {

        @Override
        public void run() {
            mSecond += 1;
            // just some checks to keep everything in order
            if (mSecond >= 60) {
                mSecond = 0;
                mMinute+=1;

            }
            if (mMinute >= 60) {
                mMinute = 0;
                mHour += 1;
            }
            if (mHour >= 24) {
                mHour = 0;
                mDay += 1;
            }
            // or call your method
            updateText();
            mHandler.postDelayed(this, 1000);
        }
    };



    public TimerTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    void updateText() {
        if(mDay == 1) {
            setText(String.format("%02d day %02d:%02d:%02d", mDay, mHour,mMinute,mSecond));
        } else if (mDay > 0) {
            setText(String.format("%02d days %02d:%02d:%02d", mDay, mHour,mMinute,mSecond));
        } else {
            setText(String.format("%02d:%02d:%02d", mHour,mMinute,mSecond));
        }
    }

    private void init(Context context) {
        mHandler.removeCallbacks(mUpdate);
        mDay = 0;
        mHour = 0;
        mSecond = 0;
        mMinute = 0;

        updateText();
        mHandler.postDelayed(mUpdate, 1000); // 60000 a minute


    }

    public TimerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TimerTextView(Context context) {
        super(context);
        init(context);
    }

}
