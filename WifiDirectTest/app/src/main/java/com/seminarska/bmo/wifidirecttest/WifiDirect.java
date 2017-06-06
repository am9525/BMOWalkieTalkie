package com.seminarska.bmo.wifidirecttest;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static android.os.Looper.getMainLooper;

/**
 * Created by tpecar on 6/6/17.
 */

public class WifiDirect {
    // referenca na activiti - vsi kontrolniki
    private MainWifiActivity mainWifiActivity;

    // WifiDirect specific
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WifiDirectBroadcastReciever reciever;

    IntentFilter intentFilter;

    private ClientThread clientThread;
    private ServerThread serverThread;
    private InetAddress hostAddress;
    private InetAddress clientAddress = null;
    private String stringHostAddress;
    private boolean connected = false;

    private boolean isHost;
    private int hostPort = 8888;
    private int clientPort = 8889;

    WifiDirect(MainWifiActivity mainWifiActivity) {
        this.mainWifiActivity = mainWifiActivity;

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // init za WifiDirect
        manager = (WifiP2pManager) mainWifiActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(mainWifiActivity, getMainLooper(), null);
        reciever = new WifiDirectBroadcastReciever(manager, channel, mainWifiActivity);
    }

    void search(View view){
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener(){
            private final String[] stringReason = {
                    "ERROR",
                    "P2P_UNSUPPORTED",
                    "BUSY"
            };

            @Override
            public void onSuccess() {
                mainWifiActivity.alertText.setText("Searching for peers...");
            }

            @Override
            public void onFailure(int reason) {
                mainWifiActivity.alertText.setText("Error: "+ stringReason[reason]);
            }
        });
    }
    void connected(final InetAddress hostAddress, final boolean isHost){
        this.isHost = isHost;
        this.hostAddress = hostAddress;
        this.stringHostAddress = hostAddress.toString();
        connected = true;

        //handshake
        Log.d("bmo", "isHost: "+isHost+" hostAddress: "+ hostAddress);
        if(isHost){
            new AsyncTask<InetAddress, Void, InetAddress>(){
                @Override
                protected InetAddress doInBackground(InetAddress... params) {
                    DatagramSocket socket = null;

                    byte[] sendData;
                    byte[] recievedData = new byte[64];

                    InetAddress clientAddress = null;

                    while(true){
                        try{
                            if(socket == null){
                                socket = new DatagramSocket(hostPort);
                                socket.setSoTimeout(1000);
                            }
                        }catch (IOException e){
                            Log.e("Set Socket", e.getMessage());
                        }
                        DatagramPacket recievePacket = new DatagramPacket(recievedData, recievedData.length);

                        Log.e("bmo", "Waiting for handshake");

                        try{
                            socket.receive(recievePacket);

                            String stringData = new String(recievePacket.getData(), 0, recievePacket.getLength());
                            Log.e("bmo", "recieved Packet, contained "+ stringData);

                            if(clientAddress == null){
                                clientAddress = recievePacket.getAddress();
                                Log.e("bmo", "Packet was recieved from"+ clientAddress+ "Sending back response");
                                break;
                            }
                        }catch (IOException e){

                        }
                    }
                    try{
                        if(clientAddress != null){
                            sendData = ("Hello my name is Jeff").getBytes();
                            DatagramPacket packet = new DatagramPacket(sendData, sendData.length,clientAddress,hostPort);
                            socket.send(packet);
                        }
                    }catch (IOException e){
                    }
                    socket.close();
                    return clientAddress;
                }
                @Override
                protected void onPostExecute(InetAddress inetAddress) {
                    clientThread = new ClientThread(inetAddress, clientPort, mainWifiActivity.fabRecord, isHost);
                    new Thread(clientThread).start();
                    super.onPostExecute(inetAddress);
                    //start the server thread
                    serverThread = new ServerThread(hostPort);
                    new Thread(serverThread).start();
                }
            }.execute();
        }
        else{
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    //send the host a hello packet
                    byte[] recievedData = new byte[64];
                    byte[] sendData = new byte[64];
                    DatagramSocket socket = null;
                    DatagramPacket recievePacket = new DatagramPacket(recievedData, recievedData.length);
                    try {
                        if (socket == null) {
                            socket = new DatagramSocket(hostPort);
                            socket.setSoTimeout(1000);
                        }
                    } catch (IOException e) {
                        if (e.getMessage() == null) {
                            Log.e("Set Socket", "Unknown message");
                        } else {
                            Log.e("Set Socket", e.getMessage());
                        }
                    }
                    //send until we get response
                    while (true) {
                        try {
                            sendData = ("Hello my name is Jeff").getBytes();
                            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, hostAddress, hostPort);
                            //packet sent
                            socket.send(packet);
                            Log.e("bmo", "Sending handshake");
                        } catch (IOException e) {
                            if (e.getMessage() == null)
                                Log.e("bmo", "Unknown message: Timeout");
                            else
                                Log.e("Set Socket", e.getMessage());
                        }
                        try {
                            recievePacket = new DatagramPacket(recievedData, recievedData.length);
                            socket.receive(recievePacket);
                            recievePacket.getData();
                            String response = new String(recievePacket.getData(), 0, recievePacket.getLength());
                            //if we get response from server stop
                            if (response.equals("Hello my name is Jeff")) {
                                Log.d("bmo", "Got handshake response from server");
                                break;
                            }
                        } catch (IOException e) {

                        }

                    }
                    socket.close();
                    return null;
                }
                @Override
                protected void onPostExecute(Void  result) {
                    super.onPostExecute(result);
                    clientThread = new ClientThread(hostAddress, hostPort, mainWifiActivity.fabRecord, isHost);
                    new Thread(clientThread).start();
                    //start the server thread
                    serverThread = new ServerThread(clientPort);
                    new Thread(serverThread).start();
                }
            }.execute();

        }

    }
    void displayPeers(WifiP2pDeviceList peerList){
        // pobrise seznam trenutno najdenih
        mainWifiActivity.wifiP2pArrayAdapter.clear();
        for(WifiP2pDevice device : peerList.getDeviceList()){
            mainWifiActivity.alertText.setText("Found Devices");
            mainWifiActivity.wifiP2pArrayAdapter.add(device.deviceName + "\n" + device.deviceAddress);
        }

    }
}
