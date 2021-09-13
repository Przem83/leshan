package org.eclipse.leshan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.util.ClockUtil;

public class FileConnector implements Connector {

    private final String myFilename;
    private final String theirFilename;

    private RawDataChannel messageHandler;

    public FileConnector(boolean host) {
        if (host) {
            myFilename = "/tmp/server-fifo";
            theirFilename = "/tmp/client-fifo";
        } else {
            myFilename = "/tmp/client-fifo";
            theirFilename = "/tmp/server-fifo";
        }

        Runtime runtime = Runtime.getRuntime();
        try {
            File f = new File(myFilename);
            if (!f.exists()) {
                runtime.exec("mkfifo " + myFilename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataReceiver dataReceiver = new DataReceiver();
        dataReceiver.start();
    }

    @Override
    public void start() throws IOException {

    }

    @Override
    public void stop() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void send(RawData msg) {
        DataSender dataSender = new DataSender(msg);
        dataSender.start();
    }

    @Override
    public void setRawDataReceiver(RawDataChannel messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void setEndpointContextMatcher(EndpointContextMatcher matcher) {
    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(0xffff);
    }

    @Override
    public String getProtocol() {
        return "UDP";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void processDatagram(DatagramPacket datagram) {

    }

    private class DataSender extends Thread {

        private RawData rawData;

        public DataSender(RawData msg) {
            this.rawData = msg;
        }

        public void run() {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(theirFilename);
                rawData.onContextEstablished(new AddressEndpointContext(getAddress()));
                fileOutputStream.write(rawData.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class DataReceiver extends Thread {

        private byte[] buffer = new byte[1024];

        public void run() {
            while (true) {
                try {
                    FileInputStream fileOutputStream = new FileInputStream(myFilename);
                    int size = fileOutputStream.read(buffer);
                    if (size > 0) {
                        byte[] msg = new byte[size];
                        System.arraycopy(buffer, 0, msg, 0, size);

                        RawData inbound = RawData.inbound(msg, new AddressEndpointContext(getAddress()), false, ClockUtil.nanoRealtime(),
                                getAddress());
                        messageHandler.receiveData(inbound);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
