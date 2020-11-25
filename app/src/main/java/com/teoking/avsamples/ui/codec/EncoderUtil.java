package com.teoking.avsamples.ui.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

public class EncoderUtil {

    private static final String TAG = "EncoderUtil";
    private static final boolean VERBOSE = false;

    private static final int kNumInputBytes = 512 * 1024;
    private static final long kTimeoutUs = 100;

    // not all combinations are valid
    private static final int MODE_SILENT = 0;
    private static final int MODE_RANDOM = 1;
    private static final int MODE_RESOURCE = 2;
    private static final int MODE_QUIET = 4;
    private static final int MODE_SILENTLEAD = 8;

    /*
     * Set this to true to save the encoding results to /data/local/tmp
     * You will need to make /data/local/tmp writeable, run "setenforce 0",
     * and remove files left from a previous run.
     */
    private static boolean sSaveResults = true;

    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    private String mInputFilePath;
    private String mOutputFilePath;

    // returns the list of codecs that support any one of the formats
    private static String[] getCodecNames(
            boolean isEncoder, MediaFormat... formats) {
        ArrayList<String> result = new ArrayList<>();
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (info.isEncoder() != isEncoder) {
                continue;
            }

            for (MediaFormat format : formats) {
                String mime = format.getString(MediaFormat.KEY_MIME);

                MediaCodecInfo.CodecCapabilities caps = null;
                try {
                    caps = info.getCapabilitiesForType(mime);
                } catch (IllegalArgumentException e) {  // mime is not supported
                    continue;
                }
                if (caps.isFormatSupported(format)) {
                    result.add(info.getName());
                    break;
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static String getCodecName(boolean isEncoder, MediaFormat format) {
        String[] names = getCodecNames(isEncoder, format);
        if (names.length > 0) {
            return names[0];
        }
        return null;
    }

    public void encode(byte[] data, String componentName, MediaFormat format, String outputFileDir) {
        Log.i(TAG, "testEncoder " + format);
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int inBitrate = sampleRate * channelCount * 16;  // bit/sec
        int outBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

        MediaMuxer muxer = null;
        int muxidx = -1;
        if (sSaveResults) {
            try {
                String outFile = outputFileDir + "/encoded-" + componentName + "-"
                        + sampleRate + "Hz-" + channelCount + "ch-" + outBitrate + "-" +
                        (android.os.Process.is64Bit() ? "64bit" : "32bit") + ".mp4";
                new File(outFile).delete();
                Log.d(TAG, "Save to: " + outFile);
                muxer = new MediaMuxer(outFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                // The track can't be added until we have the codec specific data
            } catch (Exception e) {
                Log.i(TAG, "couldn't create muxer: " + e);
            }
        }

        InputStream istream = new ByteArrayInputStream(data);

        Random random = new Random(0);
        MediaCodec codec;
        try {
            codec = MediaCodec.createByCodecName(componentName);
        } catch (Exception e) {
            Log.e(TAG, "codec '" + componentName + "' failed construction.");
            return; /* does not get here, but avoids warning */
        }
        try {
            codec.configure(
                    format,
                    null /* surface */,
                    null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "codec '" + componentName + "' failed configuration.");
        }

        codec.start();
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        int numBytesSubmitted = 0;
        boolean doneSubmittingInput = false;
        int numBytesDequeued = 0;

        while (true) {
            int index;

            if (!doneSubmittingInput) {
                index = codec.dequeueInputBuffer(kTimeoutUs /* timeoutUs */);

                if (index != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    long timeUs =
                            (long) numBytesSubmitted * 1000000 / (2 * channelCount * sampleRate);
                    if (numBytesSubmitted >= kNumInputBytes) {
                        codec.queueInputBuffer(
                                index,
                                0 /* offset */,
                                0 /* size */,
                                timeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                        if (VERBOSE) {
                            Log.d(TAG, "queued input EOS.");
                        }

                        doneSubmittingInput = true;
                    } else {
                        int size = queueInputBuffer(
                                codec, codecInputBuffers, index, istream, MODE_RESOURCE, timeUs, random);

                        numBytesSubmitted += size;

                        if (VERBOSE) {
                            Log.d(TAG, "queued " + size + " bytes of input data.");
                        }
                    }
                }
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            index = codec.dequeueOutputBuffer(info, kTimeoutUs /* timeoutUs */);

            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            } else {
                if (muxer != null) {
                    ByteBuffer buffer = codec.getOutputBuffer(index);
                    if (muxidx < 0) {
                        MediaFormat trackFormat = codec.getOutputFormat();
                        muxidx = muxer.addTrack(trackFormat);
                        muxer.start();
                    }
                    muxer.writeSampleData(muxidx, buffer, info);
                }

                dequeueOutputBuffer(codec, codecOutputBuffers, index, info);

                numBytesDequeued += info.size;

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "dequeued output EOS.");
                    }
                    break;
                }

                if (VERBOSE) {
                    Log.d(TAG, "dequeued " + info.size + " bytes of output data.");
                }
            }
        }

        if (VERBOSE) {
            Log.d(TAG, "queued a total of " + numBytesSubmitted + "bytes, "
                    + "dequeued " + numBytesDequeued + " bytes.");
        }

        float desiredRatio = (float) outBitrate / (float) inBitrate;
        float actualRatio = (float) numBytesDequeued / (float) numBytesSubmitted;

        if (actualRatio < 0.9 * desiredRatio || actualRatio > 1.1 * desiredRatio) {
            Log.w(TAG, "desiredRatio = " + desiredRatio
                    + ", actualRatio = " + actualRatio);
        }

        codec.release();
        codec = null;
        if (muxer != null) {
            muxer.stop();
            muxer.release();
            muxer = null;
        }
    }

    private int queueInputBuffer(
            MediaCodec codec, ByteBuffer[] inputBuffers, int index,
            InputStream istream, int mode, long timeUs, Random random) {
        ByteBuffer buffer = inputBuffers[index];
        buffer.rewind();
        int size = buffer.limit();

        if ((mode & MODE_RESOURCE) != 0 && istream != null) {
            while (buffer.hasRemaining()) {
                try {
                    int next = istream.read();
                    if (next < 0) {
                        break;
                    }
                    buffer.put((byte) next);
                } catch (Exception ex) {
                    Log.i(TAG, "caught exception writing: " + ex);
                    break;
                }
            }
        } else if ((mode & MODE_RANDOM) != 0) {
            if ((mode & MODE_SILENTLEAD) != 0) {
                buffer.putInt(0);
                buffer.putInt(0);
                buffer.putInt(0);
                buffer.putInt(0);
            }
            while (true) {
                try {
                    int next = random.nextInt();
                    buffer.putInt(random.nextInt());
                } catch (BufferOverflowException ex) {
                    break;
                }
            }
        } else {
            byte[] zeroes = new byte[size];
            buffer.put(zeroes);
        }

        if ((mode & MODE_QUIET) != 0) {
            int n = buffer.limit();
            for (int i = 0; i < n; i += 2) {
                short s = buffer.getShort(i);
                s /= 8;
                buffer.putShort(i, s);
            }
        }

        codec.queueInputBuffer(index, 0 /* offset */, size, timeUs, 0 /* flags */);

        return size;
    }

    private void dequeueOutputBuffer(
            MediaCodec codec, ByteBuffer[] outputBuffers,
            int index, MediaCodec.BufferInfo info) {
        codec.releaseOutputBuffer(index, false /* render */);
    }
}
