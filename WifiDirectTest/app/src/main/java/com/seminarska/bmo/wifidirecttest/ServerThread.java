package com.seminarska.bmo.wifidirecttest;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by aljaz on 2.6.2017.
 */

public class ServerThread implements Runnable {

    DatagramSocket socket;
    int port;
    InetAddress clientAddress;
    int recieveCount = 0;

    byte[] recievedData;

    MainWifiActivity mainWifiActivity;

    public ServerThread(int port, MainWifiActivity mainWifiActivity) {
        recievedData = new byte[mainWifiActivity.audio.MIN_BYTES];

        this.port = port;
        this.mainWifiActivity = mainWifiActivity;
    }

    @Override
    public void run() {
        while (true) {
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
                    Log.e("Set Socket", e.getMessage());
                }
            }
            mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.activeColor);
            DatagramPacket recievePacket = new DatagramPacket(recievedData, recievedData.length);

            //Log.e("bmo", "Waiting for packet");

            try {
                socket.receive(recievePacket);
                mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.readyColor);

                // podatki so tu ze v dataBuffer-ju
                // potrebno jih je preoblikovati v array shortov, ki so jih audio metode zmozne
                // predvajati


                recieveCount++;
                if (clientAddress == null) {
                    clientAddress = recievePacket.getAddress();
                    Log.e("bmo", "Packet was recieved from" + clientAddress);
                }
            } catch (IOException e) {
                mainWifiActivity.networkIndicators.setHostIndication(mainWifiActivity.networkIndicators.dropoutColor);

                if (e.getMessage() == null) {
                    Log.e("Recieve", "Unknown message");
                    continue;
                } else {
                    Log.e("Recieved ", "Nonempty message");
                    continue;
                }
            }

        }
    }
}
