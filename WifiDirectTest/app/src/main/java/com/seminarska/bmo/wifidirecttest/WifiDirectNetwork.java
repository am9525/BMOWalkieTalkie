package com.seminarska.bmo.wifidirecttest;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Razred, ki vsebuje metode za tvorbo client/server povezav v ze povezanem WifiDirect P2P omrezju.
 *
 * Created by aljaz on 6/6/17.
 */

public class WifiDirectNetwork {
    // referenca na activity - vsi kontrolniki
    private MainWifiActivity mainWifiActivity;

    private ClientThread clientThread;
    private ServerThread serverThread;

    private int hostPort = 8888;
    private int clientPort = 8889;

    WifiDirectNetwork(MainWifiActivity mainWifiActivity) {
        this.mainWifiActivity = mainWifiActivity;
    }

    /**
     * Niz, ki se uporablja za handshake obeh strani.
     */
    private final String HANDSHAKE = "Hello my name is Jeff";

    /**
     * Izvede rokovanje med vozliscema ter tvori client/server niti za medsebojni prenos podatkov.
     * @param hostAddress IP vozlisca, s katerim smo povezani v P2P omrezje
     * @param isHost ali je to vozlisce HOST ali CLIENT - to se doloci znotraj {@link WifiDirectBroadcastReciever}
     */
    void connected(final InetAddress hostAddress, final boolean isHost) {
        //handshake
        Log.d("bmo", "isHost: " + isHost + " hostAddress: " + hostAddress);
        if (isHost) {
            new AsyncTask<InetAddress, Void, InetAddress>() {
                // thread, ki opravi handshake kot HOST
                // 1. caka na prvi paket z HANDSHAKE vsebino (tu ne vemo naslov posiljatelja)
                // 2. ko dobimo paket, posljemo nazaj 1 paket s HANDSHAKE vsebino
                // nato gremo v tvorbo client/server threadov
                @Override
                protected InetAddress doInBackground(InetAddress... params) {
                    DatagramSocket socket = null;

                    byte[] sendData;
                    byte[] recievedData = new byte[64];

                    InetAddress clientAddress = null;

                    while (true) {
                        // kot host ustvarimo socket, s katerim poslusamo na paketke klienta
                        try {
                            if (socket == null) {
                                socket = new DatagramSocket(hostPort);
                                socket.setSoTimeout(1000);
                            }
                        } catch (IOException e) {
                            Log.e("Set Socket", e.getMessage());
                        }
                        DatagramPacket recievePacket = new DatagramPacket(recievedData, recievedData.length);

                        Log.e("bmo", "Waiting for handshake");

                        try {
                            socket.receive(recievePacket);

                            String stringData = new String(recievePacket.getData(), 0, recievePacket.getLength());
                            Log.e("bmo", "recieved Packet, contained " + stringData);

                            if (clientAddress == null) {
                                clientAddress = recievePacket.getAddress();
                                Log.e("bmo", "Packet was recieved from" + clientAddress + "Sending back response");
                                break;
                            }
                        } catch (IOException e) {
                            mainWifiActivity.makeToast("Host handshake failed. Retrying...");
                        }
                    }
                    try {
                        if (clientAddress != null) {
                            sendData = (HANDSHAKE).getBytes();
                            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, clientAddress, hostPort);
                            socket.send(packet);
                        }
                    } catch (IOException e) {
                    }
                    socket.close();
                    return clientAddress;
                }

                // po koncu izvedbe host threada - dobi client IP
                @Override
                protected void onPostExecute(InetAddress inetAddress) {
                    clientThread = new ClientThread(inetAddress, clientPort, mainWifiActivity);
                    new Thread(clientThread).start();
                    super.onPostExecute(inetAddress);
                    //start the server thread
                    serverThread = new ServerThread(hostPort, mainWifiActivity);
                    new Thread(serverThread).start();

                    mainWifiActivity.makeToast("Connected!");
                }
            }.execute();
        } else {
            new AsyncTask<Void, Void, Void>() {
                // thread, ki opravi handshake kot CLIENT
                // 1. poslje 1 HANDSHAKE paket
                // 2. caka na odgovor v obliki HANDSHAKE paketka, ko ga dobimo
                // gremo v tvorbo client/server threadov
                @Override
                protected Void doInBackground(Void... params) {
                    //send the host a hello packet
                    byte[] recievedData = new byte[64];
                    byte[] sendData;
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
                            sendData = (HANDSHAKE).getBytes();
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
                            if (response.equals(HANDSHAKE)) {
                                Log.d("bmo", "Got handshake response from server");
                                break;
                            }
                        } catch (IOException e) {
                            mainWifiActivity.makeToast("Client handshake failed. Retrying...");
                        }
                    }
                    socket.close();
                    return null;
                }
                // po koncu izvedbe host threada - dobi host IP
                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    clientThread = new ClientThread(hostAddress, hostPort, mainWifiActivity);
                    new Thread(clientThread).start();
                    //start the server thread
                    serverThread = new ServerThread(clientPort, mainWifiActivity);
                    new Thread(serverThread).start();

                    mainWifiActivity.makeToast("Connected!");
                }
            }.execute();
        }
        /*
            Ker ena stran izvede HOST->CLIENT, druga pa CLIENT->HOST rokovanje, si obe strani
            pridobita medsebojne podatke (s tem tudi potrdita delovanje povezave), nato pa s temi
            ustvarita client, server niti, ki sta za dejansko posiljanje podatkov.
         */
    }
}
