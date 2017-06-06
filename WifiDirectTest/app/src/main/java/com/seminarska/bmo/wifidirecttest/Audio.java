package com.seminarska.bmo.wifidirecttest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Created by tpecar on 6/6/17.
 *
 * ===============================================================================================
 *
 * Razred, zadolzen za raw snemanje, predvajanje.
 *
 * V osnovi metode za predvajanje ter snemanje zahtevajo stream byte-ov, iz/v katerega berejo/pisejo
 * sample.
 * V primeru lokalnega testiranja bo to byte buffer omejene velikosti, v katerega se bo prvo
 * shranilo, nato pa iz njega prebralo podatke.
 *
 * V primeru omreznega prenosa bo ByteArrayInput/OutputStream, v katerega se bo sproti bralo, pisalo
 * v svojem threadu.
 *
 * -- Kar je se potrebno ugotoviti, je, kako v primeru datagramov zagotoviti, da se paketi
 *    bodisi droppajo oz. kako na drugem koncu ta drop zanemariti.
 *
 *  Trenutna ideja je, da preprosto snemamo, pretvarjamo ter posiljamo odseke v zaporednih
 *  threadih in sicer.
 *
 *  Snemalni thread je v loopu, on zajame 0.1s posnetek, ter ga poslje v MediaCodec da ga ta
 *  kompresira - ko ta konca, ga ta da v posiljanje.
 *
 *  Na zacetku bo najbrz treba nek buffering frameov (recimo za 1s) zato da lahko kompresija
 *  ujame posiljanje (?)
 *
 * ===============================================================================================
 *  Koda za vzpostavitev snemanja/predvajanja temelji na
 *      https://gist.github.com/yavor87/b44c5096d211ce63c595
 *      https://developer.android.com/reference/android/media/AudioTrack.html
 *      https://developer.android.com/reference/android/media/AudioRecord.html
 */

public class Audio {
    // hardcodirane spremenljivke za snemanje
    private final int SAMPLE_RATE = 8000;
    private final int MIN_BUF_MULT = 10; // veckratnik minimalne kapacitete bufferja

    // buffer za snemanje (ki se bo kasneje tudi poslal)
    private byte buffer[];
    private final int bufferSize;

    // thread pool za taske
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public Audio() {
        bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, // IN_MONO vrne napako

                // zaenkrat direktno, izkaze se, da ce zelimo kompresijo, moramo poslati frame
                // skozi MediaCodec
                AudioFormat.ENCODING_PCM_8BIT
        ) * MIN_BUF_MULT;
        if(bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            Log.e("bmo_audio", "Failed to init Audio class");
            return;
        }
        Log.i("bmo_audio", String.format("%d B allocated for audio buffer", bufferSize));
        buffer = new byte[bufferSize];
    }

    public int recordAudio() throws InterruptedException, ExecutionException {
        Log.i("bmo_audio", "audio record start");
        FutureTask<Integer> recordTask = new FutureTask<Integer>(new Callable<Integer>(){
            @Override
            public Integer call() throws Exception {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_8BIT,
                        bufferSize);
                if(record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("bmo_audio", "Failed to init recording");
                    return 0;
                }
                record.startRecording();
                int bytesRead = record.read(buffer, 0, bufferSize);
                record.stop();
                record.release();

                return bytesRead;
            }
        });
        threadPool.submit(recordTask);

        Log.i("bmo_audio", "audio record end");
        // pocakamo ter poberemo rezultat (v tem primeru se nafila buffer, nazaj pa dobimo
        // stevilo dobljenih byte-ov, ki so v tem primeru enakovredni samplom)
        return recordTask.get();
    }

    public int playAudio() throws InterruptedException, ExecutionException {
        Log.i("bmo_audio", "audio play start");
        FutureTask<Integer> playTask = new FutureTask<Integer>(new Callable<Integer>(){
            @Override
            public Integer call() throws Exception {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_8BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                );
                if(audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                    Log.e("bmo_audio", "Failed to init playback");
                    return 0;
                }

                audioTrack.play();
                int bytesWritten = audioTrack.write(buffer, 0, bufferSize);
                audioTrack.stop();
                audioTrack.release();

                return bytesWritten;
            }
        });
        threadPool.submit(playTask);

        Log.i("bmo_audio", "audio play end");
        // pocakamo ter poberemo rezultat (stevilo predvajanih byte-ov)
        return playTask.get();
    }
}
