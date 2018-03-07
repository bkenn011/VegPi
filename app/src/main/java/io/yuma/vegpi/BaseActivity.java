package io.yuma.vegpi;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;

import com.google.android.things.device.ScreenManager;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import io.yuma.vegpi.Helpers.ReadingDeserializer;
import io.yuma.vegpi.Helpers.Utility;
import io.yuma.vegpi.guts.Arduino;
import io.yuma.vegpi.guts.LEDDriver;
import io.yuma.vegpi.guts.Photon;
import io.yuma.vegpi.guts.PhotonBuddy;
import io.yuma.vegpi.guts.Reading;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by bkennedy on 2/13/18.
 */

public class BaseActivity extends Activity implements PhotonBuddy.PhotonBounceBack {
    ScreenManager screenManager;

    public static final String TAG = "BaseActivity";

    public boolean IS_EMULATOR;
    public int[] dimmerValues;
    public String[] leftTextValues, rightTextValues;

    public LEDDriver driver;
    public PhotonBuddy photonBuddy;
    public Retrofit retrofit;
    public MainActivity.Getter apiInterface;

    @Override
    public void bounceBack(Reading reading, boolean isHome) {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String particleId = getString(R.string.particle_id);
        String particleToken = getString(R.string.particle_token);
        Utility.PARTICLE_TOKEN = particleToken;
        Utility.PARTICLE_ID = particleId;

        IS_EMULATOR = !Build.MODEL.startsWith("iot");
        dimmerValues = getResources().getIntArray(R.array.dim_values);
        leftTextValues = getResources().getStringArray(R.array.left_display_values);
        rightTextValues = getResources().getStringArray(R.array.right_display_values);

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().registerTypeAdapter(Reading.class, new ReadingDeserializer()).create()))
                .baseUrl("https://api.particle.io/v1/devices/"+particleId+"/")
                .build();
        apiInterface = retrofit.create(Getter.class);

        if (!IS_EMULATOR) {
            Photon photon = new Photon.PhotonBuilder()
                    .uartDeviceName("USB1-1.3:1.0")
                    .baudRate(9600)
                    .build();
            Arduino mArduino = new Arduino.ArduinoBuilder()
                    .uartDeviceName("USB1-1.5:1.0")
                    .baudRate(9600)
                    .build();
            try {
                photonBuddy = new PhotonBuddy(photon, this);
                photonBuddy.startup();
                driver = new LEDDriver(mArduino);
                driver.startup();
            } catch (IllegalStateException e) {
                Log.e("TEST", "Can't start driver:" + e.getMessage());
            }

            screenManager = new ScreenManager(Display.DEFAULT_DISPLAY);
            screenManager.lockRotation(ScreenManager.ROTATION_180); //180 = UPSIDE DOWN PI WITH POWER CORD UPWARDS
            screenManager.setBrightnessMode(ScreenManager.BRIGHTNESS_MODE_MANUAL);
            screenManager.setScreenOffTimeout(1, TimeUnit.HOURS);
        }
    }

    public interface Getter {
        //?access_token=897d6ce387e404ac5b3578d02e3dc1c2452979a5
        @GET("{type}")
        Call<Reading> getReading(@Path("type") String typeName, @Query("access_token") String token);

        @POST("setBothTemps")
        @FormUrlEncoded
        Call<ResponseBody> setLowHigh(@Field("arg") String text, @Query("access_token") String token);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (driver != null) {
            driver.shutdown();
        }
        if (photonBuddy != null) {
            photonBuddy.shutdown();
        }
    }
}
