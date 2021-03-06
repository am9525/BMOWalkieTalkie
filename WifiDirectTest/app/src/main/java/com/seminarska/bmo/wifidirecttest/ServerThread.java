package com.seminarska.bmo.wifidirecttest;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by aljaz on 2.6.2017.
 */

public class ServerThread implements Runnable {

    DatagramSocket socket;
    int port;
    InetAddress clientAddress;
    int recieveCount = 0;

    byte[] recievedData;
    short[] audioData;

    MainWifiActivity mainWifiActivity;

    public ServerThread(int port, MainWifiActivity mainWifiActivity) {
        recievedData = new byte[mainWifiActivity.audio.MIN_BYTES * WifiDirectNetwork.NETWORK_UNIT__MINIMAL_BUFFER_UNITS];
        // potrebno zaradi pretvorbe
        // https://stackoverflow.com/questions/11930385/how-can-i-get-short-from-a-bytebuffer
        //dataBuffer = ByteBuffer.allocateDirect(mainWifiActivity.audio.MIN_BYTES);
        audioData = new short[recievedData.length / 2];

        this.port = port;
        this.mainWifiActivity = mainWifiActivity;
    }

    @Override
    public void run() {
        while (true) {
            mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.activeColor);

            try {
                if (socket == null) {
                    socket = new DatagramSocket(port);
                    socket.setSoTimeout(1000);
                }
            } catch (IOException e) {
                mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.dropoutColor);

                if (e.getMessage() == null) {
                    Log.e("Set Socket", "Unknown message");
                } else {
                    Log.e("Set Socket server", e.getMessage());
                }
            }
            DatagramPacket recievePacket = new DatagramPacket(recievedData, recievedData.length);

            //Log.e("bmo", "Waiting for packet");

            try {
                socket.receive(recievePacket);
                mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.readyColor);

                // podatki so tu ze v dataBuffer-ju
                // potrebno jih je preoblikovati v array shortov, ki so jih audio metode zmozne
                // predvajati
                ByteBuffer.wrap(recievedData).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(audioData);

                // vzpostavimo predvajanje
                mainWifiActivity.audio.startPlayback();
                mainWifiActivity.audio.playAudioBuffer(audioData, 0);
                mainWifiActivity.audio.stopPlayback();

                recieveCount++;
                if (clientAddress == null) {
                    clientAddress = recievePacket.getAddress();
                    Log.e("bmo", "Packet was recieved from" + clientAddress);
                }
            } catch (IOException e) {
                mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.dropoutColor);

                if (e.getMessage() == null) {
                    // tu tipicno pridemo zaradi timeouta
                } else {
                    Log.e("bmo_server_recieve", e.toString());
                    continue;
                }
            }
            if(Thread.currentThread().isInterrupted()) {
                Log.i("bmo_server", "was interrupted!");
                if(socket!=null)
                    socket.close();
                return;
            }
        }
    }
}
