package io.yuma.vegpi.guts;

import com.google.gson.annotations.SerializedName;

/**
 * Created by bkennedy on 1/27/18.
 */

public class Reading {

    //DS18B20 temperature
    @SerializedName("ds")
    double ds;

    //DHT11 Temperature
    @SerializedName("ht1")
    double ht1;

    //DHT11 HUMIDITY
    @SerializedName("h")
    int h;


    //OPTIONAL - FAN SPEED FROM ONE SENSOR
    @SerializedName("f")
    int f;

    //OPTIONAL - ##|## HIGH|LOW
    @SerializedName("t")
    String t;

    public String EXHAUST_TEMP = null; //temp at scrubber

    public String getHumidity() {
        return h < 0 ? null : String.valueOf(h);
    }

    public String getAverageTemperature() {
        if(ds < -1 || ht1 < 0) {
            return null;
        }
        //home info has no t.  if it has t the average should exclude the ht1 reading because that's by the carbon scrubber exhaust
        if(t==null){
            EXHAUST_TEMP = String.format("%s",ht1);
            return String.format("%s",ds);
        }

        return String.format("%s",(ds + ht1)/2);
    }

    public String getFanSpeed() {
        return f > -1 ? String.valueOf(f) : null;
    }

    public String getHighLow() {
        return t;
    }





}
