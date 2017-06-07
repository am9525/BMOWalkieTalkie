package com.seminarska.bmo.wifidirecttest;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by aljaz on 2.6.2017.
 */

public class ClientThread implements Runnable {

    DatagramSocket socket;
    int port = 0;
    InetAddress hostAddress;
    int sendCount = 1;

    byte[] sendData;
    short[] audioData;

    boolean recording = false;

    private MainWifiActivity mainWifiActivity;

    public ClientThread(InetAddress hostAddress, int port, MainWifiActivity mainWifiActivity) {
        sendData = new byte[mainWifiActivity.audio.MIN_BYTES];
        audioData = new short[mainWifiActivity.audio.MIN_BYTES / 2];

        this.hostAddress = hostAddress;
        this.port = port;
        this.mainWifiActivity = mainWifiActivity;
    }

    @Override
    public void run() {
        // vzpostavimo snemanje
        mainWifiActivity.audio.startRecording();

        mainWifiActivity.fabRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //pressed
                    recording = true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //released
                    recording = false;
                }
                return true;
            }
        });
        if (hostAddress != null && port != 0) {
            while (true) {
                if (recording) {
                    mainWifiActivity.networkIndicators.setClientIndication(mainWifiActivity.networkIndicators.activeColor);
                    try {
                        if (socket == null) {
                            socket = new DatagramSocket(port);
                            socket.setSoTimeout(1000);
                        }
                    } catch (IOException e) {
                        mainWifiActivity.networkIndicators.setClientIndication(mainWifiActivity.networkIndicators.dropoutColor);

                        if (e.getMessage() == null) {
                            Log.e("Set Socket", "Unknown message");
                        } else {
                            Log.e("Set Socket", e.getMessage());
                        }
                    }
                    try {
                        // podatke (array short-ov) preoblikujemo v array byte-ov
                        // ter jih posljemo
                        mainWifiActivity.audio.recordAudio(audioData, 0);

                        // shranimo v byte array - tu se kaze nepotrebna striktnost Jave, kajti namrec
                        // kljub temu da bi moral le drugace gledati na podatke, to ni mogoce narediti
                        // drugace kot pa ce jih ponovno v celoti skopiras
                        //
                        // vec o tem na https://stackoverflow.com/questions/10804852/how-to-convert-short-array-to-byte-array
                        //
                        ByteBuffer dataBuf = ByteBuffer.wrap(sendData);
                        for(short s : audioData)
                            dataBuf.putShort(s);

                        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, hostAddress, port);
                        //packet sent
                        sendCount++;
                        socket.send(packet);

                        mainWifiActivity.networkIndicators.setClientIndication(mainWifiActivity.networkIndicators.readyColor);
                        //Log.e("bmo", "Client packet was sent");
                    } catch (IOException e) {
                        mainWifiActivity.networkIndicators.setClientIndication(mainWifiActivity.networkIndicators.dropoutColor);

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
