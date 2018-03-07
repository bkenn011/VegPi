package io.yuma.vegpi.components;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;

import com.jaygoo.widget.RangeSeekBar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import io.yuma.vegpi.BR;
import io.yuma.vegpi.Helpers.BrightnessRangeCallback;
import io.yuma.vegpi.Helpers.Utility;
import io.yuma.vegpi.R;
import io.yuma.vegpi.guts.Reading;

/**
 * Created by bkennedy on 1/28/18.
 */

public class RemoteViewModel extends BaseObservable implements RangeSeekBar.OnRangeChangedListener {

    private Context mContext;
    public String particlePath;

    public String title;
    private String shortName; //all caps identifier

    private String averageTemperature;
    private String humidity;
    private String fanSpeed;

    private String highLowSettings;

    public final String tempFormat = "%1$s° | %2$s%%";
    public final String exhaustTempFormat = "Exhaust Temp: %s°";

    public boolean isLeft;

    private int[] lightWattageSetting;

    private BrightnessRangeCallback mBrightnessCallback;

    private int[] exhaustRangeSetting; //for remote (right vm) only

    private double graphLastXValue = 0d;
    private LineGraphSeries<DataPoint> temperatureHistory;


    public RemoteViewModel(Context context, BrightnessRangeCallback bc, String title, String shortName, String particlePath, boolean isLeft, GraphView graph, RangeSeekBar slider){
        this.mContext = context;
        this.mBrightnessCallback=bc;
        this.title = title;
        this.shortName = shortName;
        this.isLeft = isLeft;
        this.particlePath = particlePath;

        lightWattageSetting = new int[]{context.getSharedPreferences(shortName,Context.MODE_PRIVATE).getInt("min_light",7)
            , context.getSharedPreferences(shortName,Context.MODE_PRIVATE).getInt("max_light",11)};

        //set up temperature low and high values if isRight (home)
        if(isLeft) {
            exhaustRangeSetting =
            new int[]{context.getSharedPreferences(shortName,Context.MODE_PRIVATE).getInt("min_exhausttemp",68)
                    , context.getSharedPreferences(shortName,Context.MODE_PRIVATE).getInt("max_exhausttemp",80)};
            setExhaustRangeSetting(68,80);
        }

        //init temperature history series
        bindGraphAndWattageSlider(graph,slider);
    }

    private void bindGraphAndWattageSlider(GraphView view, RangeSeekBar seekBar) {

        temperatureHistory = new LineGraphSeries<>(new DataPoint[]{});
        temperatureHistory.setThickness(3);
        temperatureHistory.setDrawBackground(true);

        view.getViewport().setXAxisBoundsManual(true);
        view.getViewport().setMinX(0);
        view.getViewport().setMaxX(60);

        view.getViewport().setYAxisBoundsManual(true);
        view.getViewport().setMinY(66);
        view.getViewport().setMaxY(80);

        view.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        view.getGridLabelRenderer().setTextSize(10f);
        view.getGridLabelRenderer().setNumHorizontalLabels(0);
        view.getGridLabelRenderer().setNumVerticalLabels(5);
        view.getViewport().setXAxisBoundsManual(true);

        view.addSeries(temperatureHistory);

        //now slider
        seekBar.setValue(lightWattageSetting[0],lightWattageSetting[1]);
        seekBar.setOnRangeChangedListener(this);

        //now force update of wattage slider
        setLightWattageSetting(lightWattageSetting[0],lightWattageSetting[1]);
    }

    @Override
    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
        if(isFromUser) {
            setLightWattageSetting(min,max);
        }
    }

    @Override
    public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {}

    @Override
    public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
        if(mBrightnessCallback!=null){
            mBrightnessCallback.brightnessRangeChanged(isLeft,lightWattageSetting[0],lightWattageSetting[1]);
        }
    }

    public void mergeInReading(Reading reading, boolean IGNORE_NEXT) {
        Double successfulAverage = null;
        if(reading == null) {
            return;
        }

        String avg = reading.getAverageTemperature();
        String rh = reading.getHumidity();
        String fan = reading.getFanSpeed();
        String hilo = reading.getHighLow();

        if(avg!=null) {
            setAverageTemperature(avg);
            successfulAverage = Double.parseDouble(avg);
        }
        if(rh != null) {
            setHumidity(rh);
        }
        if(fan!=null) {
            setFanSpeed(fan);
        }
        if(hilo!=null && !IGNORE_NEXT) {
            setHighLowSettings(hilo);
            String[] splits = hilo.split("\\|");

            int lowy = Math.round(Integer.parseInt(splits[0]));
            int highy = Math.round(Integer.parseInt(splits[1]));

            mContext.getSharedPreferences(shortName,Context.MODE_PRIVATE).edit().putInt("min_exhausttemp",lowy)
                    .putInt("max_exhausttemp",highy).apply();

            setExhaustRangeSetting(lowy,highy);
        }

        if(successfulAverage!=null) {
            graphLastXValue+=1d;
            temperatureHistory.appendData(new DataPoint(graphLastXValue,Utility.roundToHalf(successfulAverage)), true, 60);
        }
    }

    @BindingAdapter("setViewRange")
    public static void setViewRange(RangeSeekBar bar, int[] range) {
        if(range==null) {return;}
        bar.setValue(range[0],range[1]);
    }

    @Bindable
    public int[] getExhaustRangeSetting() {
        return exhaustRangeSetting;
    }

    public void setExhaustRangeSetting(int low, int high) {
        exhaustRangeSetting[0] = low;
        exhaustRangeSetting[1] = high;
        notifyPropertyChanged(BR.exhaustRangeSetting);
    }

    @Bindable
    public String getLightWattageSetting() {
        String output = "";
        if(lightWattageSetting[0] == lightWattageSetting[1]) {
            //fixed voltage
            output = mContext.getResources().getStringArray(isLeft ? R.array.left_display_values : R.array.right_display_values)[lightWattageSetting[0]].split(" - ")[1];
        } else {
            output = String.format("%1$s - %2$s", mContext.getResources().getStringArray(isLeft ? R.array.left_display_values : R.array.right_display_values)[lightWattageSetting[0]].split(" - ")[1],
                    mContext.getResources().getStringArray(isLeft ? R.array.left_display_values : R.array.right_display_values)[lightWattageSetting[1]].split(" - ")[1]);
        }
        return output;
    }

    private void setLightWattageSetting(float min, float max) {
        lightWattageSetting[0] = Math.round(min);
        lightWattageSetting[1] = Math.round(max);
        notifyPropertyChanged(BR.lightWattageSetting);
    }

    @Bindable
    public String getAverageTemperature() {
        return averageTemperature;
    }

    private void setAverageTemperature(String averageTemperature) {
        if(averageTemperature!=null && averageTemperature.length() > 4) {
            averageTemperature = averageTemperature.substring(0,4);
        }
        this.averageTemperature = averageTemperature;
        notifyPropertyChanged(BR.averageTemperature);
    }

    @Bindable
    public String getHumidity() {
        return humidity;
    }

    private void setHumidity(String humidity) {
        this.humidity = humidity;
        notifyPropertyChanged(BR.humidity);
    }

    @Bindable
    public String getFanSpeed() {
        return fanSpeed;
    }

    private void setFanSpeed(String fanSpeed) {
        this.fanSpeed = fanSpeed;
        notifyPropertyChanged(BR.fanSpeed);
    }

    @Bindable
    public String getHighLowSettings() {
        return highLowSettings;
    }

    private void setHighLowSettings(String highLowSettings) {
        this.highLowSettings = highLowSettings;
        notifyPropertyChanged(BR.highLowSettings);
    }

}
