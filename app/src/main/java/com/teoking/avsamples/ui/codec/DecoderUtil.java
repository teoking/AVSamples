package com.teoking.avsamples.ui.codec;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecoderUtil {

    private static final String TAG = "DecoderUtil";

    private static final int RESET_MODE_NONE = 0;
    private static final int RESET_MODE_RECONFIGURE = 1;
    private static final int RESET_MODE_FLUSH = 2;
    private static final int RESET_MODE_EOS_FLUSH = 3;

    private static final String[] CSD_KEYS = new String[]{"csd-0", "csd-1"};

    private static final int CONFIG_MODE_NONE = 0;
    private static final int CONFIG_MODE_QUEUE = 1;

    private static short[] mMasterBuffer;

    // Class handling all audio parameters relevant for testing
    protected static class AudioParameter {

        public AudioParameter() {
            this.reset();
        }

        public void reset() {
            this.numChannels = 0;
            this.samplingRate = 0;
        }

        public int getNumChannels() {
            return this.numChannels;
        }

        public int getSamplingRate() {
            return this.samplingRate;
        }

        public void setNumChannels(int numChannels) {
            this.numChannels = numChannels;
        }

        public void setSamplingRate(int samplingRate) {
            this.samplingRate = samplingRate;
        }

        private int numChannels;
        private int samplingRate;
    }

    private static short[] decodeToMemory(String codecName, AudioParameter audioParams,
                                          final String testinput, int resetMode, int configMode, int eossample,
                                          List<Long> timestamps) throws IOException {
        String localTag = TAG + "#decodeToMemory";
        Log.v(localTag, String.format("reset = %d; config: %s", resetMode, configMode));
        short[] decoded = new short[0];
        int decodedIdx = 0;

        MediaExtractor extractor;
        MediaCodec codec;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        extractor = new MediaExtractor();
        extractor.setDataSource(testinput);

        assertEquals("wrong number of tracks", 1, extractor.getTrackCount());
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        assertTrue("not an audio file", mime.startsWith("audio/"));

        MediaFormat configFormat = format;
        codec = MediaCodec.createByCodecName(codecName);
        if (configMode == CONFIG_MODE_QUEUE && format.containsKey(CSD_KEYS[0])) {
            configFormat = MediaFormat.createAudioFormat(mime,
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));

            configFormat.setLong(MediaFormat.KEY_DURATION,
                    format.getLong(MediaFormat.KEY_DURATION));
            String[] keys = new String[]{"max-input-size", "encoder-delay", "encoder-padding"};
            for (String k : keys) {
                if (format.containsKey(k)) {
                    configFormat.setInteger(k, format.getInteger(k));
                }
            }
        }
        Log.v(localTag, "configuring with " + configFormat);
        codec.configure(configFormat, null /* surface */, null /* crypto */, 0 /* flags */);

        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        if (resetMode == RESET_MODE_RECONFIGURE) {
            codec.stop();
            codec.configure(configFormat, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();
            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();
        } else if (resetMode == RESET_MODE_FLUSH) {
            codec.flush();
        }

        extractor.selectTrack(0);

        if (configMode == CONFIG_MODE_QUEUE) {
            queueConfig(codec, format);
        }

        // start decoding
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int samplecounter = 0;
        while (!sawOutputEOS && noOutputCounter < 50) {
            noOutputCounter++;
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0 && eossample > 0) {
                        fail("test is broken: never reached eos sample");
                    }
                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        if (samplecounter == eossample) {
                            sawInputEOS = true;
                        }
                        samplecounter++;
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }

            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);

                if (info.size > 0) {
                    noOutputCounter = 0;
                    if (timestamps != null) {
                        timestamps.add(info.presentationTimeUs);
                    }
                }
                if (info.size > 0 &&
                        resetMode != RESET_MODE_NONE && resetMode != RESET_MODE_EOS_FLUSH) {
                    // once we've gotten some data out of the decoder, reset and start again
                    if (resetMode == RESET_MODE_RECONFIGURE) {
                        codec.stop();
                        codec.configure(configFormat, null /* surface */, null /* crypto */,
                                0 /* flags */);
                        codec.start();
                        codecInputBuffers = codec.getInputBuffers();
                        codecOutputBuffers = codec.getOutputBuffers();
                        if (configMode == CONFIG_MODE_QUEUE) {
                            queueConfig(codec, format);
                        }
                    } else /* resetMode == RESET_MODE_FLUSH */ {
                        codec.flush();
                    }
                    resetMode = RESET_MODE_NONE;
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    sawInputEOS = false;
                    samplecounter = 0;
                    if (timestamps != null) {
                        timestamps.clear();
                    }
                    continue;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                if (decodedIdx + (info.size / 2) >= decoded.length) {
                    decoded = Arrays.copyOf(decoded, decodedIdx + (info.size / 2));
                }

                buf.position(info.offset);
                for (int i = 0; i < info.size; i += 2) {
                    decoded[decodedIdx++] = buf.getShort();
                }

                codec.releaseOutputBuffer(outputBufIndex, false /* render */);

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    if (resetMode == RESET_MODE_EOS_FLUSH) {
                        resetMode = RESET_MODE_NONE;
                        codec.flush();
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                        sawInputEOS = false;
                        samplecounter = 0;
                        decoded = new short[0];
                        decodedIdx = 0;
                        if (timestamps != null) {
                            timestamps.clear();
                        }
                    } else {
                        sawOutputEOS = true;
                    }
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.d(TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
                audioParams.setNumChannels(oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                audioParams.setSamplingRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                Log.d(TAG, "output format has changed to " + oformat);
            } else {
                Log.d(TAG, "dequeueOutputBuffer returned " + res);
            }
        }
        if (noOutputCounter >= 50) {
            fail("decoder stopped outputing data");
        }

        codec.stop();
        codec.release();
        return decoded;
    }

    private static void queueConfig(MediaCodec codec, MediaFormat format) {
        for (String csdKey : CSD_KEYS) {
            if (!format.containsKey(csdKey)) {
                continue;
            }
            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
            int inputBufIndex = codec.dequeueInputBuffer(-1);
            if (inputBufIndex < 0) {
                fail("failed to queue configuration buffer " + csdKey);
            } else {
                ByteBuffer csd = (ByteBuffer) format.getByteBuffer(csdKey).rewind();
                Log.v(TAG + "#queueConfig", String.format("queueing %s:%s", csdKey, csd));
                codecInputBuffers[inputBufIndex].put(csd);
                codec.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        csd.limit(),
                        0 /* presentation time (us) */,
                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
            }
        }
    }

    private static List<String> codecsFor(String resource) throws IOException {
        MediaExtractor ex = new MediaExtractor();
        AssetFileDescriptor fd = getAssetFileDescriptorFor(resource);
        try {
            ex.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        } finally {
            fd.close();
        }
        MediaCodecInfo[] codecInfos = new MediaCodecList(
                MediaCodecList.REGULAR_CODECS).getCodecInfos();
        ArrayList<String> matchingCodecs = new ArrayList<String>();
        MediaFormat format = ex.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        for (MediaCodecInfo info : codecInfos) {
            if (info.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
                if (caps != null) {
                    matchingCodecs.add(info.getName());
                }
            } catch (IllegalArgumentException e) {
                // type is not supported
            }
        }
        assertTrue("no matching codecs found", matchingCodecs.size() != 0);
        return matchingCodecs;
    }

    private static AssetFileDescriptor getAssetFileDescriptorFor(final String resPath)
            throws FileNotFoundException {
        File inpFile = new File(resPath);
        ParcelFileDescriptor parcelFD =
                ParcelFileDescriptor.open(inpFile, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(parcelFD, 0, parcelFD.getStatSize());
    }

    /**
     * Calculate the RMS of the difference signal between a given signal and the reference samples
     * located in mMasterBuffer.
     *
     * @param signal the decoded samples to test
     * @return RMS of error signal
     * @throws RuntimeException
     */
    private static double getRmsError(short[] signal) throws RuntimeException {
        long totalErrorSquared = 0;
        int stride = mMasterBuffer.length / signal.length;
        assertEquals("wrong data size", mMasterBuffer.length, signal.length * stride);

        for (int i = 0; i < signal.length; i++) {
            short sample = signal[i];
            short mastersample = mMasterBuffer[i * stride];
            int d = sample - mastersample;
            totalErrorSquared += d * d;
        }
        long avgErrorSquared = (totalErrorSquared / signal.length);
        return Math.sqrt(avgErrorSquared);
    }

    private static void assertEquals(String msg, int expected, int actual) {
        if (expected != actual) {
            Log.e(TAG, "AssertEquals fail: " + msg);
        }
    }

    private static void assertTrue(String msg, boolean value) {
        if (!value) {
            Log.e(TAG, "AssertTrue fail: " + msg);
        }
    }

    private static void fail(String s) {
        Log.e(TAG, "Fail: " + s);
    }

    // ----- Public Methods ------ //

    /**
     * Decode aac to an short array in memory.
     *
     * @param inputAacFile
     * @return decoded short array or short[0]
     * @throws IOException
     */
    public static short[] decodeToShortArray(final String inputAacFile) throws IOException {
        List<String> codecList = codecsFor(inputAacFile);

        AudioParameter decParams = new AudioParameter();
        short[] decoded = decodeToMemory(codecList.get(0), decParams, inputAacFile,
                RESET_MODE_NONE, CONFIG_MODE_NONE, -1, null);
//        double rmse = getRmsError(decoded);
        return decoded;
    }
}
