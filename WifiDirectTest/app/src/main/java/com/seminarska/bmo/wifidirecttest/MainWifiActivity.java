package com.seminarska.bmo.wifidirecttest;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
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

public class MainWifiActivity extends AppCompatActivity {
    public TextView alertText;
    public ListView deviceListView;

    protected FloatingActionButton fabSearch;
    protected FloatingActionButton fabRecord;

    protected ArrayAdapter<String> wifiP2pArrayAdapter;
    // pozicija znotraj seznama
    private int position = 0;

    // WifiDirect funkcije
    WifiDirect wifiDirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // inicializiramo WifiDirect
        wifiDirect = new WifiDirect(this);

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
                wifiDirect.reciever.connect(pos);

            }
        });
        // vezemo evente na gumbe
        // - gumb za iskanje sosednjih vozlisc
        fabSearch = (FloatingActionButton) findViewById(R.id.fabSearch);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiDirect.search(v);
            }
        });
        // - gumb za zacetek posiljanja
        fabRecord = (FloatingActionButton) findViewById(R.id.fabSpeak);
        fabRecord.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    //pressed
                    Log.d("bmo", "started client thread");
                    //clientRunningThread.start();

                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    //released
                    Log.d("bmo", "stopped client thread");
                    //clientRunningThread.interrupt();
                    return false;
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
}
