package com.seminarska.bmo.wifidirecttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MainWifiActivity extends AppCompatActivity {
    public TextView aletText;
    public ListView deviceListView;

    private FloatingActionButton fabSearch;
    private FloatingActionButton fabRecord;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiDirectBroadcastReciever reciever;
    private IntentFilter intentFilter;
    private ArrayAdapter<String> wifiP2pArrayAdapter;

    private int position = 0;
    ClientThread clientThread;
    ServerThread serverThread;
    InetAddress hostAddress;
    InetAddress clientAddress = null;
    String stringHostAddress;
    boolean connected = false;

    boolean isHost;
    int hostPort = 8888;
    int clientPort = 8889;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wifi);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        aletText = (TextView) findViewById(R.id.alertText);
        deviceListView = (ListView) findViewById(R.id.deviceList);

        wifiP2pArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(wifiP2pArrayAdapter);

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                position = pos;
                reciever.connect(pos);

            }
        });

        fabSearch = (FloatingActionButton) findViewById(R.id.fabSearch);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(v);
            }
        });
        fabRecord = (FloatingActionButton) findViewById(R.id.fabSpeak);
        /*
        fabRecord.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    //pressed
                    Log.d("bmo", "started client thread");
                    clientRunningThread.start();

                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    //released
                    Log.d("bmo", "stopped client thread");
                    clientRunningThread.interrupt();
                    return false;
                }
                return true;
            }
        });*/
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        reciever = new WifiDirectBroadcastReciever(manager,channel,this);
    }
    public void search(View view){
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener(){
            private final String[] stringReason = {
                    "ERROR",
                    "P2P_UNSUPPORTED",
                    "BUSY"
            };

            @Override
            public void onSuccess() {
                aletText.setText("Searching for peers...");
            }

            @Override
            public void onFailure(int reason) {
                aletText.setText("Error: "+ stringReason[reason]);
            }
        });
    }
    public void connected(final InetAddress hostAddress, final boolean isHost){
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
                    clientThread = new ClientThread(inetAddress, clientPort, fabRecord, isHost);
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
                    clientThread = new ClientThread(hostAddress, hostPort, fabRecord, isHost);
                    new Thread(clientThread).start();
                    //start the server thread
                    serverThread = new ServerThread(clientPort);
                    new Thread(serverThread).start();
                }
            }.execute();

        }

    }
    public void displayPeers(WifiP2pDeviceList peerList){
        wifiP2pArrayAdapter.clear();
        for(WifiP2pDevice device : peerList.getDeviceList()){
            aletText.setText("Found Devices");
            wifiP2pArrayAdapter.add(device.deviceName + "\n" + device.deviceAddress);
        }

    }
    public void makeToast(String text){
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
        registerReceiver(reciever, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(reciever);
    }
}
