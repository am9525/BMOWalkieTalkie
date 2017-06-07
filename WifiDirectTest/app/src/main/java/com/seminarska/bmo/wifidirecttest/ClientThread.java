package com.seminarska.bmo.wifidirecttest;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by aljaz on 2.6.2017.
 */

public class ClientThread implements Runnable {
    InetAddress hostAddress;
    int port = 0;
    DatagramSocket socket;

    byte[] sendData = new byte[64];

    int sendCount = 1;
    boolean recording = false;
    private MainWifiActivity mainWifiActivity;

    public ClientThread(InetAddress hostAddress, int port, MainWifiActivity mainWifiActivity) {
        this.hostAddress = hostAddress;
        this.port = port;
        this.mainWifiActivity = mainWifiActivity;
    }

    @Override
    public void run() {

        mainWifiActivity.fabRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //pressed
                    Log.d("bmo", "started client thread");
                    recording = true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //released
                    Log.d("bmo", "stopped client thread");
                    recording = false;
                }
                return true;
            }
        });
        if (hostAddress != null && port != 0) {
            while (true) {
                if (recording) {
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
                    try {
                        sendData = ("Hello" + sendCount).getBytes();
                        sendCount++;

                        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, hostAddress, port);
                        //packet sent
                        socket.send(packet);
                        Log.e("bmo", "Client packet was sent");
                    } catch (IOException e) {
                        if (e.getMessage() == null)
                            Log.e("bmo", "Unknown message: Timeout");
                        else
                            Log.e("Set Socket", e.getMessage());
                    }
                }

            }
        }
    }
}
