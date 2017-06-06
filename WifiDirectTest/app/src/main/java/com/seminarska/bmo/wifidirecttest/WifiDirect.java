package com.seminarska.bmo.wifidirecttest;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import static android.os.Looper.getMainLooper;

/**
 * Created by aljaz on 6/6/17.
 *
 * Low level inicializacija za WifiDirect + podporni objekti za iskanje ter povezavo na omrezje.
 */

public class WifiDirect {
    MainWifiActivity mainWifiActivity;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WifiDirectBroadcastReciever reciever;
    IntentFilter intentFilter;
    private WifiP2pDevice selectedPeer;

    WifiDirect(MainWifiActivity mainWifiActivity) {
        this.mainWifiActivity = mainWifiActivity;

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // init za WifiDirectNetwork
        manager = (WifiP2pManager) mainWifiActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(mainWifiActivity, getMainLooper(), null);
        reciever = new WifiDirectBroadcastReciever(this, mainWifiActivity);
    }

    /**
     * Zahteva iskanje novih sosednjih P2P vozlisc.
     * Napolni WifiDirectBroadcastReciever.peerList ter .configs seznama.
     */
    void search(){
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
    /**
     * Se poveze na vozlisce, specificirano pod pozicijo v seznamu vseh najdenih P2P vozlisc.
     * @param position pozicija znotraj WifiDirectBroadcastReciever seznama
     */
    public void connect(int position){
        //uses position to obtain the same name and address of the device to connect to
        WifiP2pConfig deviceConfig = reciever.configs.get(position);
        selectedPeer = reciever.peerList.get(position);

        //connects the two devices
        manager.connect(channel, deviceConfig, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                //activity.alertText.setText("Connecting...");
                reciever.activity.makeToast("Connecting...");
                Log.d("bmo", "Connecting...");
            }

            @Override
            public void onFailure(int reason) {
                //activity.alertText.setText("Connection failed: "+reason);
                reciever.activity.makeToast("Connection failed: "+reason);
                Log.d("bmo", "Connection failed: "+reason);
            }
        });
    }
}
