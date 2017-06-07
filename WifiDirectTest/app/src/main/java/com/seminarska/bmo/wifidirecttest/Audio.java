package com.seminarska.bmo.wifidirecttest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

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

class Audio {
    // hardcodirane spremenljivke za snemanje
    final int SAMPLE_RATE = 8000;
    final int MIN_BYTES;                // minimalno B za delujoc buffer

    // buffer za snemanje (ki se bo kasneje tudi poslal)
    private short buffer[];

    // audio source
    private AudioRecord record;
    // audio sink
    private AudioTrack audioTrack;

    /**
     * Konstruktor razreda.
     * Inicializira audio source, sink.
     */
    Audio() {
        MIN_BYTES = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, // IN_MONO vrne napako

                // zaenkrat direktno, izkaze se, da ce zelimo kompresijo, moramo poslati frame
                // skozi MediaCodec
                AudioFormat.ENCODING_PCM_16BIT
        );
        if(MIN_BYTES == AudioTrack.ERROR || MIN_BYTES == AudioTrack.ERROR_BAD_VALUE) {
            Log.e("bmo_audio", "Failed to init Audio class");
            return;
        }
        // vzpostavimo audio source
        record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                MIN_BYTES);
        if(record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("bmo_audio", "Failed to init recording");
            return;
        }
        // vzpostavimo audio sink
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                MIN_BYTES, // enota, ki jo prenese v predvajanje
                AudioTrack.MODE_STREAM
        );
        if(audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e("bmo_audio", "Failed to init playback");
            return;
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    /**
     * Zahteva zacetek zajemanja vzorcev.
     * Potrebno klicati pred recordAudio.
     */
    void startRecording() {
        record.startRecording();
    }

    /**
     * Zahteva konec zajemanja vzorcev.
     * Priporoceno, da se klice po prenehanju intenzivne uporabe recordAudio.
     */
    void stopRecording() {
        record.stop();
    }

    /**
     * Zajame vzorce iz mikrofona.
     * @param buffer v katerega pisemo vzorce
     * @param bufferOffset kazalec znotraj bufferja, v katerega zacnemo pisati
     * @return stevilo prebranih byte-ov (iz vhoda)
     */
    int recordAudio(short buffer[], int bufferOffset) {
        int bytesRead = 0;
        if(bufferOffset+MIN_BYTES < buffer.length)
            bytesRead = record.read(buffer, bufferOffset, MIN_BYTES);
        return bytesRead;
    }
    /*--------------------------------------------------------------------------------------------*/
    /**
     * Zahteva zacetek predvajanja vzorcev.
     * Potrebno klicati pred playAudio.
     */
    void startPlayback() {
        audioTrack.play();
    }

    /**
     * Zahteva konec predvajanaj vzorcev.
     * Priporoceno, da se klice po prenehanju intenzivne uporabe playAudio.
     */
    void stopPlayback() {
        audioTrack.flush();
    }

    /**
     * Zapise vzorce v izhodno napravo (predvidoma zvocnik)
     * @param buffer iz katerega beremo vzorce
     * @param bufferOffset kazalec znotraj bufferja, od katerega naprej zacnemo brati
     * @return stevilo zapisanih byte-ov (v izhod)
     */
    int playAudio(short buffer[], int bufferOffset) {
        int bytesWritten = 0;
        if (bufferOffset+MIN_BYTES < buffer.length)
            bytesWritten = audioTrack.write(buffer, bufferOffset, MIN_BYTES);
        return bytesWritten;
    }
    /*--------------------------------------------------------------------------------------------*/
    // ko dokoncno prenehamo uporabljati source/sink
    void release() {
        record.release();
        audioTrack.release();
    }
}
