package com.teoking.avsamples.ui.video.hwdecode

import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * Scheduler will wait for proper time to reach the presentation time of a frame. This implements
 * the video FPS control.
 */
private class FpsScheduler {

    private var prevPresentUs = 0L
    private var prevSystemTimeUs = 0L

    // Simple presentation time control implementation.
    // Not consider the frame rate, speed, e.g. factors.
    fun waitForPresent(presentationTimeUs: Long) {
        // first frame should present immediately
        // other frame should wait to the presentation time
        if (prevPresentUs == 0L) {
            prevPresentUs = presentationTimeUs
            prevSystemTimeUs = System.nanoTime() / 1000
        } else {
            val nowUs = System.nanoTime() / 1000
            var frameDelta = presentationTimeUs - prevPresentUs
            if (frameDelta < 0) {
                frameDelta = 0
            }
            val frameTimeUs = prevSystemTimeUs + frameDelta
            val sleepUs = frameTimeUs - nowUs

            if (sleepUs > 0) {
                val realSleepMillis = sleepUs / 1000L
                val realSleepNanos = (sleepUs % 1000).toInt() * 1000
                Thread.sleep(realSleepMillis, realSleepNanos)
                Log.d(TAG, "wait for present: millis=$realSleepMillis, nanos=$realSleepNanos")
            } else {
                Log.d(TAG, "present now: sleepUs=$sleepUs")
            }
        }
    }

    fun reset() {
        prevPresentUs = 0L
        prevSystemTimeUs = 0L
    }

    companion object {
        private const val TAG = "FpsScheduler"
    }
}

/**
 * A hardware decoder for decoding video. And render the output frame to the surface.
 */
class HwVideoDecoder(private val surface: Surface) {

    private val mediaExtractor: MediaExtractor = MediaExtractor()
    private lateinit var mediaCodec: MediaCodec
    private val bufferInfo = MediaCodec.BufferInfo()
    private var videoColorFormat: Int = -1
    private var isPaused = true
    private val pauseLock = Object()
    private var isReachEOS = false
    private lateinit var decodeThread: Thread
    private val scheduler = FpsScheduler()
    private var mVideoSampleNumber: Int = 0

    constructor(afd: AssetFileDescriptor, surface: Surface) : this(surface) {
        mediaExtractor.setDataSource(afd)
        init()
    }

    private fun init() {
        // Select the first video track we find, ignore the rest.
        for (i in 0..mediaExtractor.trackCount) {
            val format = mediaExtractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "media file mine=$mime")
            if (mime?.startsWith(VIDEO_MIME_PREFIX) == true) {
                //select the track
                mediaExtractor.selectTrack(i)

                videoColorFormat = try {
                    format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
                } catch (e: NullPointerException) {
                    -1
                }

                // Create codec with the mime
                mediaCodec = MediaCodec.createDecoderByType(mime)
                // Config codec
                mediaCodec.configure(format, surface, null, 0)

                break
            }
        }

        mediaCodec.start()
    }

    fun start() {
        decodeThread = thread(name = THREAD_NAME) {
            try {
                doDecode()
            } catch (e: Exception) {
                Log.w(TAG, "decode thread exit with: ${e.message}")
            }
        }
    }

    fun playOrPause() {
        isPaused = !isPaused
        if (!isPaused) {
            if (isReachEOS) {
                mediaExtractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                scheduler.reset()
            }
            synchronized(pauseLock) {
                pauseLock.notify()
            }
        }
    }

    fun stop() {
        mediaCodec.stop()
        mediaCodec.reset()
        mediaCodec.release()

        mediaExtractor.release()
    }

    private fun doDecode() {
        var inputDone = false
        var outputDone = false
        while (true) {
            // Handle pause
            synchronized(pauseLock) {
                while (isPaused) {
                    Log.d(TAG, "Pause decoder thread!")
                    pauseLock.wait()
                }
            }

            // deal with inputBuffer
            if (!inputDone) {
                // Retrieve the index of an input buffer to be filled with valid data
                // or -1 if no such buffer is currently available.
                val inputBufIndex: Int = mediaCodec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufIndex >= 0) {
                    // retrieve the available buffer to be filled
                    val inputBuf: ByteBuffer? = mediaCodec.getInputBuffer(inputBufIndex)

                    inputBuf?.let {
                        // Retrieve the current encoded sample
                        // and store it in the byte buffer starting at the given offset.
                        val sampleSize: Int = mediaExtractor.readSampleData(it, 0)
                        if (sampleSize < 0) {
                            // End of stream -- send empty frame with EOS flag set.
                            mediaCodec.queueInputBuffer(
                                inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            // submit it to the codec.
                            val presentationTimeUs: Long = mediaExtractor.sampleTime
                            mediaCodec.queueInputBuffer(
                                inputBufIndex, 0, sampleSize,
                                presentationTimeUs, 0 /*flags*/
                            )
                            // Advance to the next sample.
                            mediaExtractor.advance()
                        }
                    }
                } else {
                    Log.w(TAG, "input buffer not available")
                }
            }

            // deal with outputBuffer
            if (!outputDone) {
                // Dequeue an output buffer, block at most TIMEOUT_USEC microseconds.
                // return the index of an output buffer that has been successfully decoded
                val outputBufIndex: Int = mediaCodec.dequeueOutputBuffer(
                    bufferInfo,
                    TIMEOUT_US
                )
                when (outputBufIndex) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(
                        TAG,
                        "no output from decoder available"
                    )
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Log.d(TAG, "output buffer changed")
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.d(
                        TAG,
                        "output format changed to " + mediaCodec.outputFormat
                    )
                    else -> {
                        if (outputBufIndex < 0) {
                            Log.d(TAG, "outputBufIndex < 0")
                            return
                        }

                        // time control based on PTS
                        if (bufferInfo.size != 0) {
                            scheduler.waitForPresent(bufferInfo.presentationTimeUs)
                        }

                        //--------------------outputBufIndex >= 0---------------------------------
                        // send the buffer to the output surface.
                        // surface will return the buffer to the codec
                        // once the buffer is no longer used
                        Log.i(
                            TAG,
                            "submitted frame " + mVideoSampleNumber + ", pts=" + bufferInfo.presentationTimeUs
                        )
                        // count the sample number
                        mVideoSampleNumber++
                        Log.d(TAG, "send buffer to surface, index=$outputBufIndex")
                        // I do not deal with the buffer, just tell the codec to render it to the surface
                        if (bufferInfo.presentationTimeUs != 0L) {
                            mediaCodec.releaseOutputBuffer(outputBufIndex, bufferInfo.presentationTimeUs * 1000)
                        } else {
                            mediaCodec.releaseOutputBuffer(outputBufIndex, false)
                        }

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            Log.d(TAG, "output end --- EOS")
                            // stop the thread at the end of the video, or loop playback if necessary
                            mVideoSampleNumber = 0
                            inputDone = false
                            mediaCodec.flush()
                            Log.d(
                                TAG,
                                "pausing the thread. inputDone = " + inputDone +
                                        ", presentation time = " + bufferInfo.presentationTimeUs
                            )
                            isPaused = true
                            isReachEOS = true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "HardwareDecoder"
        private const val THREAD_NAME = "HwDecoder"
        private const val VIDEO_MIME_PREFIX = "video/"
        private const val TIMEOUT_US = 100000L
    }
}