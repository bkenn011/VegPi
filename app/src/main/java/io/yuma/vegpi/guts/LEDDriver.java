package io.yuma.vegpi.guts;

import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Created by bkennedy on 2/3/18.
 */

public class LEDDriver implements BaseSensor, AutoCloseable {
    private static final String TAG = "LEDDriver";
    private static final int CHUNK_SIZE = 10;
    private ByteBuffer mMessageBuffer = ByteBuffer.allocate(CHUNK_SIZE);
    private final Arduino arduino;

    private UartDevice mDevice;
    private boolean receiving;

    public LEDDriver(Arduino arduino) {
        this.arduino = arduino;
    }

    @Override
    public void startup() {
        PeripheralManagerService mPeripheralManagerService = new PeripheralManagerService();
        try {
            mDevice = mPeripheralManagerService.openUartDevice(arduino.getUartDeviceName());
            mDevice.setDataSize(arduino.getDataBits());
            mDevice.setParity(UartDevice.PARITY_NONE);
            mDevice.setStopBits(arduino.getStopBits());
            mDevice.setBaudrate(arduino.getBaudRate());
        } catch (IOException e) {
            try {
                close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw new IllegalStateException("Sensor can't start", e);
        }
    }

    private void writeTempToDevice(int val, String suffix) {
        //D = LEFT(LOW), S = RIGHT(HIGH).
        if (val > 255) {
            val = 255;
        } else if (val < 20) {
            val = 20;
        }

        String mode = (String.valueOf(val) + suffix);
        String response = "";
        byte[] buffer = new byte[CHUNK_SIZE];

        try {
            fillBuffer(buffer, mode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLightDimmingRange(int... values) {
        writeTempToDevice(values[0], "D");
        if (values.length > 1) {
            writeTempToDevice(values[1], "S");
        }
    }

    private String fillBuffer(byte[] buffer, String mode) throws IOException {
        mDevice.write(mode.getBytes(), mode.length());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mDevice.read(buffer, buffer.length);
        processBuffer(buffer, buffer.length);
        return new String(mMessageBuffer.array(), "UTF-8").replaceAll("\0", "");
    }

    private void processBuffer(byte[] buffer, int length) {
        for (int i = 0; i < length; i++) {
            if (0x24 == buffer[i]) {
                receiving = true;
            } else if (0x23 == buffer[i]) {
                receiving = false;
                mMessageBuffer.clear();
            } else {
                //Insert all other characters into the buffer
                if (receiving && buffer[i] != 0x00)
                    mMessageBuffer.put(buffer[i]);
            }
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
