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
 */

public class WifiDirectBroadcastReciever extends BroadcastReceiver{

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainWifiActivity activity;
    private List<WifiP2pDevice> peerList;
    private List<WifiP2pConfig> configs;
    private WifiP2pDevice selectedPeer;

    public WifiDirectBroadcastReciever(WifiP2pManager manager, WifiP2pManager.Channel channel, MainWifiActivity activity){
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }
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
            if(manager != null){
                WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener(){

                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {

                        peerList.clear();
                        peerList.addAll(peers.getDeviceList());
                        activity.wifiDirect.displayPeers(peers);

                        for(WifiP2pDevice device : peerList){
                            WifiP2pConfig deviceConfig = new WifiP2pConfig();
                            deviceConfig.deviceAddress = device.deviceAddress;
                            configs.add(deviceConfig);
                        }
                    }
                };
                manager.requestPeers(channel, peerListListener);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        // Respond to new connection or disconnections
            if(manager == null)
                return;
            NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(netInfo.isConnected()){

                manager.requestConnectionInfo(channel, infoListener);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        // Respond to this device's wifi state changing
        }
    }
    //Function called from main activity when the user selects an available peer
    public void connect(int position){
        //uses position to obtain the same name and address of the device to connect to
        WifiP2pConfig deviceConfig = configs.get(position);
        selectedPeer = peerList.get(position);

        //connects the two devices
        manager.connect(channel, deviceConfig, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                //activity.alertText.setText("Connecting...");
                activity.makeToast("Connecting...");
                Log.d("bmo", "Connecting...");

            }

            @Override
            public void onFailure(int reason) {
                //activity.alertText.setText("Connection failed: "+reason);
                activity.makeToast("Connection failed: "+reason);
                Log.d("bmo", "Connection failed: "+reason);
            }
        });
    }
    WifiP2pManager.ConnectionInfoListener infoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if(info.groupFormed){
                if(info.isGroupOwner){
                    activity.alertText.setText("Connected as Host");
                    Log.d("bmo", "HOST");
                    activity.wifiDirect.connected(groupOwnerAddress, true);
                }
                else{
                    activity.alertText.setText("Connected as Client");
                    Log.d("bmo", "CLIENT");
                    activity.wifiDirect.connected(groupOwnerAddress, false);
                }
            }
        }
    };
}
