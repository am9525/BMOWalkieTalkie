package com.seminarska.bmo.wifidirecttest;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.widget.FrameLayout;

/**
 * Created by tpecar on 6/7/17.
 *
 * Drzi skupaj statusne indikatorje.
 */

class NetworkIndicators {
    // indikatorji
    protected FrameLayout hostActive;
    protected FrameLayout clientActive;

    public final ColorDrawable activeColor;
    public final ColorDrawable readyColor;
    public final ColorDrawable dropoutColor;
    public final ColorDrawable inactiveColor;

    // handler za ui
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    NetworkIndicators(MainWifiActivity mainWifiActivity) {

        activeColor = new ColorDrawable(ContextCompat.getColor(mainWifiActivity.getApplicationContext(), R.color.colorAccent));
        readyColor = new ColorDrawable(ContextCompat.getColor(mainWifiActivity.getApplicationContext(), R.color.colorReady));
        dropoutColor = new ColorDrawable(ContextCompat.getColor(mainWifiActivity.getApplicationContext(), R.color.colorDropout));
        inactiveColor = new ColorDrawable(Color.TRANSPARENT);

        // indikatorji za aktivnost (posiljanje)
        hostActive = (FrameLayout) mainWifiActivity.findViewById(R.id.hostActive);
        clientActive = (FrameLayout) mainWifiActivity.findViewById(R.id.clientActive);
    }
    // metode za nastavljanje barv indikatorjev - za primer dostopa iz drugih niti
    public void setHostIndication(final ColorDrawable c) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                hostActive.setForeground(c);
            }
        });
    }
    public void setClientIndication(final ColorDrawable c) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                clientActive.setForeground(c);
            }
        });
    }
}
