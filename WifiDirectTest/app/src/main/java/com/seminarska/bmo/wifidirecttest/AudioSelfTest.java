package com.seminarska.bmo.wifidirecttest;

import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by tpecar on 6/7/17.
 */

public class AudioSelfTest implements View.OnTouchListener {
    // indikator ali snemamo ali ne
    boolean recording;

    // audio handler
    Audio audio;
    // (zaenkrat) buffer za audio
    short audioBuffer[];

    // gumb, na katerega vezemo event (ga spreminjamo)
    FloatingActionButton fabSelfTest;
    // handler za ui
    Handler uiHandler = new Handler(Looper.getMainLooper());

    public AudioSelfTest(FloatingActionButton fabSelfTest, Audio audio) {
        this.fabSelfTest = fabSelfTest;

        this.audio = audio;
        audioBuffer = new short[audio.MIN_BYTES * 50];
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //pressed
            Log.d("bmo", "started client thread");
            recording = true;
            new Thread(new Runnable() {
                int recordingPointer = 0;
                int playbackPointer = 0;

                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                    audio.startRecording();
                    int bytesRead;
                    do
                        recordingPointer += (bytesRead = audio.recordAudio(audioBuffer, recordingPointer));
                    while (recording && bytesRead > 0);
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
                    do
                        playbackPointer += (bytesWritten = audio.playAudio(audioBuffer, playbackPointer));
                    while (playbackPointer < recordingPointer && bytesWritten > 0);
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
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //released
            Log.d("bmo", "stopped client thread");
            recording = false;
        }
        return true;
    }
}
