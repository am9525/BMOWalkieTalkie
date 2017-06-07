package com.seminarska.bmo.wifidirecttest;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainWifiActivity extends AppCompatActivity {
    /*--------------------------------------------------------------------------------------------*/
    // logika
    // low level manager, setup
    WifiDirect wifiDirect;
    // UDP povezavni del (handshake, tvorba povezav)
    WifiDirectNetwork wifiDirectNetwork;
    // audio
    Audio audio;
    /*--------------------------------------------------------------------------------------------*/
    // komponente
    TextView alertText;
    ListView deviceListView;
    ArrayAdapter<String> wifiP2pArrayAdapter;

    FloatingActionButton fabSearch;
    FloatingActionButton fabRecord;

    // indikatorji za aktivnost
    NetworkIndicators networkIndicators;

    // audio self test gumb + event listener
    FloatingActionButton fabSelfTest;
    AudioSelfTest fabSelfTestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*----------------------------------------------------------------------------------------*/
        // inicializiaramo WifiDirect - omrezni del
        wifiDirect = new WifiDirect(this);
        // inicializiramo WifiDirectNetwork - povezavni del
        wifiDirectNetwork = new WifiDirectNetwork(this);

        // inicializiramo Audio del
        audio = new Audio();
        /*----------------------------------------------------------------------------------------*/
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

        // indikatorji
        networkIndicators = new NetworkIndicators(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirect.disconnect();
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
