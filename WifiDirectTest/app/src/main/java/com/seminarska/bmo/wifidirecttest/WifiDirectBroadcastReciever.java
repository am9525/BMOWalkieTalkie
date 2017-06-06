package com.seminarska.bmo.wifidirecttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aljaz on 2.6.2017.
 *
 * Broadcast reciever, ki se odziva na evente P2P Wifi evente, ki jih tvori Android.
 * Vec o tem na: https://developer.android.com/guide/topics/connectivity/wifip2p.html
 */

public class WifiDirectBroadcastReciever extends BroadcastReceiver {

    MainWifiActivity activity;
    List<WifiP2pDevice> peerList;
    List<WifiP2pConfig> configs;

    WifiDirect wifiDirect;

    public WifiDirectBroadcastReciever(WifiDirect wifiDirect, MainWifiActivity activity){
        super();
        this.wifiDirect = wifiDirect;
        this.activity = activity;
    }

    WifiP2pManager.ConnectionInfoListener infoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if(info.groupFormed){
                if(info.isGroupOwner){
                    activity.alertText.setText("Connected as Host");
                    Log.d("bmo", "HOST");
                    activity.wifiDirectNetwork.connected(groupOwnerAddress, true);
                }
                else{
                    activity.alertText.setText("Connected as Client");
                    Log.d("bmo", "CLIENT");
                    activity.wifiDirectNetwork.connected(groupOwnerAddress, false);
                }
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                activity.alertText.setText("WiFi-Direct Enabled");
            }
            else{
                activity.alertText.setText("WiFi-Direct Disabled");
            }
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            peerList = new ArrayList<WifiP2pDevice>();
            configs = new ArrayList<WifiP2pConfig>();
            Log.d("bmo", "Peers changed action");
            if(wifiDirect.manager != null){
                WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener(){

                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {

                        peerList.clear();
                        peerList.addAll(peers.getDeviceList());
                        activity.displayPeers(peers);

                        for(WifiP2pDevice device : peerList){
                            WifiP2pConfig deviceConfig = new WifiP2pConfig();
                            deviceConfig.deviceAddress = device.deviceAddress;
                            configs.add(deviceConfig);
                        }
                    }
                };
                wifiDirect.manager.requestPeers(wifiDirect.channel, peerListListener);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if(wifiDirect.manager == null)
                return;

            NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(netInfo.isConnected()){
                // tu smo se povezali na P2P omrezje

                // znotraj infoListener (ki se prav tako prozi na event, ki ga tvori WifiP2pManager)
                // ugotovimo kateri del povezave smo (ali smo jo mi sprejeli ali zahtevali)
                // ter se na podlagi tega doloci kdo je host, client
                wifiDirect.manager.requestConnectionInfo(wifiDirect.channel, infoListener);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        // Respond to this device's wifi state changing
        }
    }
}
