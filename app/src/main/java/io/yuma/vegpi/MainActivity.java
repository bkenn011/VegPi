package io.yuma.vegpi;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.AnimationUtils;

import com.jaygoo.widget.RangeSeekBar;

import java.util.Timer;
import java.util.TimerTask;

import io.yuma.vegpi.Helpers.BrightnessRangeCallback;
import io.yuma.vegpi.Helpers.Utility;
import io.yuma.vegpi.components.RemoteViewModel;
import io.yuma.vegpi.databinding.ActivityMainBinding;
import io.yuma.vegpi.guts.Reading;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements RangeSeekBar.OnRangeChangedListener, BrightnessRangeCallback {


    //WEB TIMEOUT CALLS
    int DELAY_TIME = 15000;

    private ActivityMainBinding mBinding;
    private RemoteViewModel vmLeft, vmRight;

    public boolean IGNORE_NEXT_LOHI = false; //if true user is setting temp setting

    public int[] tempSides = new int[]{68,80};

    private void sendTempsToPhoton(String builtOut) {
        Call<ResponseBody> DocsCall = apiInterface.setLowHigh(builtOut, Utility.PARTICLE_TOKEN);
        DocsCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
               IGNORE_NEXT_LOHI = false;
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                IGNORE_NEXT_LOHI = false;
            }
        });
    }

    @Override
    public void brightnessRangeChanged(boolean isLeft, int minVal, int maxVal) {
        if(driver==null || IS_EMULATOR) {
            return;
        }
        driver.setLightDimmingRange(minVal,maxVal);
    }

    @Override
    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
        if(isFromUser && view.getId() == R.id.seekbar1) {
            tempSides[0] = Math.round(min);
            tempSides[1] = Math.round(max);
            vmLeft.setExhaustRangeSetting(tempSides[0],tempSides[1]);
        }
    }

    @Override
    public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
        IGNORE_NEXT_LOHI = true; //no matter what it gets set upon release
    }

    @Override
    public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
        if(view.getId() == R.id.seekbar1) {
            sendTempsToPhoton(String.format("LOW=%1$s,HIGH=%2$s", tempSides[0], tempSides[1]));
        }
    }

    void initializeSliders() {
        mBinding.setLowhi(tempSides);
        mBinding.seekbar1.setOnRangeChangedListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this,R.layout.activity_main);

        mBinding.fan.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadeinout));
        mBinding.setLowhi(tempSides);

        vmLeft = new RemoteViewModel(this,this,"Left Tent", "vmLeft","remote_info",true, mBinding.graph1, mBinding.seekbarLowHighLeft);
        vmRight = new RemoteViewModel(this,this,"Right Tent","vmRight","home_info",false, mBinding.graph2, mBinding.seekbarLowHighRight);
        mBinding.setLeftViewModel(vmLeft);
        mBinding.setRightViewModel(vmRight);

        initializeSliders();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(photonBuddy==null && !TextUtils.isEmpty(Utility.PARTICLE_TOKEN)) {
            Log.d("TEST","Ok here we go pull reading");
            //USB connection to photon is bad, so poll from the photon's web output instead of via serial
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    pullReading(vmLeft);
                }
            }, 5, DELAY_TIME);
        }
    }

    @Override
    public void bounceBack(Reading reading, boolean isHome) {
        if(reading==null){
            return;
        } else if(isHome) {
            vmRight.mergeInReading(reading, IGNORE_NEXT_LOHI);
        } else {
            vmLeft.mergeInReading(reading, IGNORE_NEXT_LOHI);
        }

        if (!TextUtils.isEmpty(reading.EXHAUST_TEMP)) {
            mBinding.setExhaustTemp(reading.EXHAUST_TEMP);
        }
    }

    public void pullReading(final RemoteViewModel vm) {
        try {
            Call<Reading> DocsCall = apiInterface.getReading(vm.particlePath, Utility.PARTICLE_TOKEN);
            DocsCall.enqueue(new Callback<Reading>() {
                @Override
                public void onResponse(Call<Reading> call, Response<Reading> response) {
                    Log.d("TEST","Uhh response is something:"+response.toString());
                    bounceBack(response.body(), !vm.isLeft);
                    //now call the opposite
                    if(vm.isLeft) {
                        pullReading(vmRight);
                    }
                }

                @Override
                public void onFailure(Call<Reading> call, Throwable t) {
                    Log.d("TEST","Epic fail:"+t.getMessage());
                }
            });
        } catch (Exception se) {
            Log.d("TEST","Bihg bad:"+se.getMessage());
        }
    }

}
