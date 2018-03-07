package io.yuma.vegpi.guts;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;


/**
 * Created by bkennedy on 2/3/18.
 */

public class PhotonBuddy implements BaseSensor, AutoCloseable {
    private static final String TAG = "PhoDriver";
    private static final int CHUNK_SIZE = 10;
    private ByteBuffer mMessageBuffer = ByteBuffer.allocate(CHUNK_SIZE);
    private final Photon photon;

    private UartDevice mDevice;
    private boolean receiving;
    private PhotonBounceBack mPhotonCallback;

    private LinkedHashSet<String> fullResponse = new LinkedHashSet<>();

    public PhotonBuddy(Photon photon, PhotonBounceBack bb) {
        this.mPhotonCallback = bb;
        this.photon = photon;
    }

    public interface PhotonBounceBack {
        void bounceBack(Reading reading, boolean isHome);
    }

    private synchronized void readJsonFromCharacteristic(byte[] data) throws UnsupportedEncodingException, JSONException {
        String value = null;
        if (data != null && data.length > 0) {
            value = new String(data, Charset.forName("UTF-8"));
        }

        if (!TextUtils.isEmpty(value)) {

            // Add to a set so duplicate respones get filtered out.
            if (value.contains("[")) {
                fullResponse.clear();
            }

            fullResponse.add(value);

            if (value.contains("]")) {
                JSONArray jsonObject = null;
                try {
                    jsonObject = new JSONArray(fullResponse.toString().toLowerCase());
                } catch (Exception e) {

                    try {
                        String joinedResponses = TextUtils.join("", fullResponse);

                        int firstIndexOf = joinedResponses.indexOf("]");
                        int lastIndexOf = joinedResponses.lastIndexOf("]");

                        if (firstIndexOf != lastIndexOf) {
                            joinedResponses = joinedResponses.substring(firstIndexOf + 1);
                        }

                        jsonObject = new JSONArray(joinedResponses.toLowerCase());


                        fullResponse.clear();
                    } catch (Exception ee) {
                        Log.e(TAG, ee.getLocalizedMessage());
                    }
                }

                if (jsonObject != null && jsonObject.length() == 2 && mPhotonCallback != null) {
                    mPhotonCallback.bounceBack(new Gson().fromJson(jsonObject.get(0).toString(), Reading.class), false);
                    mPhotonCallback.bounceBack(new Gson().fromJson(jsonObject.get(1).toString(), Reading.class), true);
                }
            }
        }
    }

    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            transferUartData();
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    private void transferUartData() {
        if (mDevice != null) {
            // Loop until there is no more data in the RX buffer.
            try {
                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                while ((read = mDevice.read(buffer, buffer.length)) > 0) {
                    readJsonFromCharacteristic(buffer);
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to transfer data over UART", e);
            }
        }
    }

    @Override
    public void startup() {
        PeripheralManagerService mPeripheralManagerService = new PeripheralManagerService();

        try {
            mDevice = mPeripheralManagerService.openUartDevice(photon.getUartDeviceName());
            mDevice.setDataSize(photon.getDataBits());
            mDevice.setParity(UartDevice.PARITY_NONE);
            mDevice.setStopBits(photon.getStopBits());
            mDevice.setBaudrate(photon.getBaudRate());
            mDevice.registerUartDeviceCallback(mCallback);
        } catch (IOException e) {
            try {
                close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw new IllegalStateException("Sensor can't start", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    @Override
    public void shutdown() {
        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }
    }
}
