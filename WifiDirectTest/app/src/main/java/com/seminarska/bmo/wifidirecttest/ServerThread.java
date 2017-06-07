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

    int recieveCount = 0;
    byte[] recievedData = new byte[64];

    InetAddress clientAddress;

    public ServerThread(int port) {
        this.port = port;
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
                if (e.getMessage() == null) {
                    Log.e("Set Socket", "Unknown message");
                } else {
                    Log.e("Set Socket", e.getMessage());
                }
            }
            DatagramPacket recievePacket = new DatagramPacket(recievedData, recievedData.length);

            Log.e("bmo", "Waiting for packet");

            try {
                socket.receive(recievePacket);

                String stringData = new String(recievePacket.getData(), 0, recievePacket.getLength());
                Log.e("bmo", "recieved Packet, contained " + stringData);

                recieveCount++;
                if (clientAddress == null) {
                    clientAddress = recievePacket.getAddress();
                    Log.e("bmo", "Packet was recieved from" + clientAddress);
                }
            } catch (IOException e) {
                if (e.getMessage() == null) {
                    Log.e("Recieve", "Unknown message");
                    continue;
                } else {
                    Log.e("Recieve", e.getMessage());
                    continue;
                }
            }

        }
    }
}
