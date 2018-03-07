package io.yuma.vegpi.guts;

import java.util.Arrays;
import java.util.List;

/**
 * Created by bkennedy on 2/3/18.
 */

public class Photon {
    private final String uartDeviceName;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;

    private Photon(PhotonBuilder builder) {
        this.uartDeviceName = builder.uartDeviceName;
        this.baudRate = builder.baudRate;
        this.dataBits = builder.dataBits;
        this.stopBits = builder.stopBits;
    }

    String getUartDeviceName() {
        return uartDeviceName;
    }

    int getBaudRate() {
        return baudRate;
    }

    int getDataBits() {
        return dataBits;
    }

    int getStopBits() {
        return stopBits;
    }

    public static class PhotonBuilder {
        private String uartDeviceName = "UART0";
        private int baudRate = 115200;
        private int dataBits = 8;
        private int stopBits = 1;


        public PhotonBuilder uartDeviceName(String uartDeviceName) {
            if (uartDeviceName == null)
                throw new IllegalArgumentException("fileFormat == null");

            this.uartDeviceName = uartDeviceName;
            return this;
        }

        public PhotonBuilder baudRate(int baudRate) {
            List<Integer> baudRates = Arrays.asList( 9600, 19200, 38400, 57600, 115200);
            boolean test = baudRates.contains(baudRate);
            if(!test)
                throw new IllegalArgumentException("baudRates are:" + baudRates.toString());

            this.baudRate = baudRate;
            return this;
        }

        public PhotonBuilder dataBits(int dataBits) {
            if (dataBits < 1)
                throw new IllegalArgumentException("dataBits must > 1");

            this.dataBits = dataBits;
            return this;
        }

        public PhotonBuilder stopBits(int stopBits) {
            if (stopBits < 1)
                throw new IllegalArgumentException("stopBits must > 1");

            this.stopBits = stopBits;
            return this;
        }

        public Photon build() {
            return new Photon(this);
        }

    }
}
