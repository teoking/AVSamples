package com.teoking.avsamples.ui.codec;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by ZhangHao on 2017/5/10.
 * Play pcm data
 */
public class MyAudioTrack {
    private static final String TAG = "MyAudioTrack";
    private final int mFrequency;// sampling rate
    private final int mChannel;// Vocal tract
    private final int mSampleBit;// Sampling accuracy
    private AudioTrack mAudioTrack;

    public MyAudioTrack(int frequency, int channel, int sampleBit) {
        this.mFrequency = frequency;
        this.mChannel = channel;
        this.mSampleBit = sampleBit;
    }

    /**
     * Initialization
     */
    public void init() {
        if (mAudioTrack != null) {
            release();
        }
        // Get the minimum buffer size of the constructed object
        int minBufSize = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrequency, mChannel, mSampleBit, minBufSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    /**
     * Releasing resources
     */
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    /**
     * Write the decoded pcm data to audioTrack for playback
     *
     * @param data   data
     * @param offset deviation
     * @param length Length to play
     */
    public void playAudioTrack(short[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            Log.e(TAG, "AudioTrack failed : " + e.toString());
        }
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(mFrequency,
                mChannel, mSampleBit);
    }
}
