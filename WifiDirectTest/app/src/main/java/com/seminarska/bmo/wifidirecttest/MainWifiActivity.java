package com.seminarska.bmo.wifidirecttest;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainWifiActivity extends AppCompatActivity {
    public TextView alertText;
    public ListView deviceListView;

    protected FloatingActionButton fabSearch;
    protected FloatingActionButton fabRecord;

    // audio self test gumb + event listener
    protected FloatingActionButton fabSelfTest;
    private AudioSelfTest fabSelfTestListener;

    protected ArrayAdapter<String> wifiP2pArrayAdapter;

    // indikatorji
    protected FrameLayout hostActive;
    protected FrameLayout clientActive;

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

        // gumb za selftest zvoka
        fabSelfTest = (FloatingActionButton) findViewById(R.id.fabSelfTest);
        fabSelfTestListener = new AudioSelfTest(fabSelfTest, audio);
        fabSelfTest.setOnTouchListener(fabSelfTestListener);

        // indikatorji za aktivnost (posiljanje)
        hostActive = (FrameLayout) findViewById(R.id.hostActive);
        clientActive = (FrameLayout) findViewById(R.id.clientActive);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            ColorDrawable activeColor = new ColorDrawable(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            ColorDrawable inactiveColor = new ColorDrawable(Color.TRANSPARENT);

            boolean active = true;

            @Override
            public void run() {
                hostActive.setForeground(active ? activeColor : inactiveColor);
                active = !active;
                handler.postDelayed(this, 500);
            }
        }, 500);
        //hostActive.setForeground();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // zapremo audio resurse
        audio.release();
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
