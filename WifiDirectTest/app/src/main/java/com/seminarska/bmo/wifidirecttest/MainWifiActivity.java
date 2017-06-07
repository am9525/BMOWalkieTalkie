package com.seminarska.bmo.wifidirecttest;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;

public class MainWifiActivity extends AppCompatActivity {
    public TextView alertText;
    public ListView deviceListView;

    protected FloatingActionButton fabSearch;
    protected FloatingActionButton fabRecord;
    protected FloatingActionButton fabSelfTest;

    protected ArrayAdapter<String> wifiP2pArrayAdapter;
    // pozicija znotraj seznama
    private int position = 0;
    ClientThread clientThread;
    ServerThread serverThread;
    InetAddress hostAddress;
    InetAddress clientAddress = null;
    String stringHostAddress;
    boolean connected = false;

    // low level manager, setup
    WifiDirect wifiDirect;
    // UDP povezavni del (handshake, tvorba povezav)
    WifiDirectNetwork wifiDirectNetwork;

    // audio
    Audio audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // inicializiaramo WifiDirect - omrezni del
        wifiDirect = new WifiDirect(this);
        // inicializiramo WifiDirectNetwork - povezavni del
        wifiDirectNetwork = new WifiDirectNetwork(this);

        // inicializiramo Audio del
        audio = new Audio();

        // inicializiramo kontrolnike
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wifi);

        alertText = (TextView) findViewById(R.id.alertText);
        deviceListView = (ListView) findViewById(R.id.deviceList);

        // vezemo evente na seznam naprav
        wifiP2pArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(wifiP2pArrayAdapter);

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                position = pos;
                wifiDirect.connect(pos);

            }
        });
        // vezemo evente na gumbe
        // - gumb za iskanje sosednjih vozlisc
        fabSearch = (FloatingActionButton) findViewById(R.id.fabSearch);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiDirect.search();
            }
        });
        // - gumb za zacetek posiljanja
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
        // gumb za selftest zvoka
        fabSelfTest = (FloatingActionButton) findViewById(R.id.fabSelfTest);
        fabSelfTest.setOnTouchListener(new View.OnTouchListener() {
            // indikator ali snemamo ali ne
            boolean recording;

            // handler za ui
            Handler uiHandler = new Handler(Looper.getMainLooper());

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    //pressed
                    Log.d("bmo", "started client thread");
                    recording = true;
                    new Thread(new Runnable() {
                        int recordingPointer=0;
                        int playbackPointer=0;

                        @Override
                        public void run() {
                            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                            audio.startRecording();
                            int bytesRead;
                            do {
                                bytesRead = 0;
                                recordingPointer += (bytesRead = audio.recordAudio(recordingPointer));
                            }
                            while(recording && bytesRead > 0);
                            audio.stopRecording();

                            // izklopimo gumb za snemanje
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    fabSelfTest.setEnabled(false);
                                }
                            });

                            audio.startPlayback();
                            int bytesWritten;
                            do {
                                bytesWritten = 0;
                                playbackPointer += (bytesWritten = audio.playAudio(playbackPointer));
                            }
                            while(playbackPointer < recordingPointer && bytesWritten > 0);

                            // dummy pisanje, samo zato da AudioTrack prebere cel prejsnji buffer, to enoto pa
                            // ignorira
                            // https://stackoverflow.com/questions/22058290/android-audiotrack-stream-cuts-out-early
                            audio.playAudio(0);
                            audio.stopPlayback();

                            // vklopimo gumb za snemanje
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    fabSelfTest.setEnabled(true);
                                }
                            });
                        }
                    }).start();
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    //released
                    Log.d("bmo", "stopped client thread");
                    recording = false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirect.manager.cancelConnect(wifiDirect.channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reason) {
            }
        });
        registerReceiver(wifiDirect.reciever, wifiDirect.intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiDirect.reciever);
    }

    // ostale metode, ki so vezane na UI
    void makeToast(String text){
        Toast.makeText(this.getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
    void displayPeers(WifiP2pDeviceList peerList){
        // pobrise seznam trenutno najdenih
        wifiP2pArrayAdapter.clear();
        for(WifiP2pDevice device : peerList.getDeviceList()){
            alertText.setText("Found Devices");
            wifiP2pArrayAdapter.add(device.deviceName + "\n" + device.deviceAddress);
        }

    }
}
