package com.teoking.avsamples.ui.codec

import android.app.Application
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teoking.avsamples.logic.extractor_muxer.MediaMuxerProxy
import com.teoking.avsamples.util.playWithMedia2Player
import com.teoking.common.AudioRecordHelper
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.min

class AudioCodecViewModel(application: Application) : AndroidViewModel(application) {

    val descText = "This demo shows how to encode/decode audio with MediaCodec:\n" +
            "* Encode pcm audio from AudioRecord to AAC (record 5s)\n" +
            "* Play saved AAC with MediaPlayer\n" +
            "* Decode AAC to PCM and played with AudioTrack"

    private var isRecoding = false

    private val _recordingText = MutableLiveData<String>().apply {
        value = RECORDING_TEXT_DEFAULT
    }
    val recordingStateText: LiveData<String> = _recordingText

    private val _stateText = MutableLiveData<String>().apply {
        value = ""
    }
    val stateText: LiveData<String> = _stateText

    fun recordAndEncodeAudioToAac() {
        if (isRecoding) {
            return
        }

        isRecoding = true

        GlobalScope.launch(Dispatchers.Main) {
            // Start recording audio
            startRecord()

            for (second in MAX_RECORD_DURATION downTo 1) {
                _recordingText.value = "$RECORDING_TEXT_COUNT_DOWN $second"
                delay(1000)
            }
            // Stop recoding audio
            stopRecord()
            _recordingText.value = RECORDING_TEXT_FINISH_SAVING

            // Encoding and saving
            encodingAndSavingAudio(_stateText)

            delay(1000)

            _recordingText.value = RECORDING_TEXT_DEFAULT
            isRecoding = false
        }
    }

    private fun startRecord() = AudioRecordHelper.getInstance().start()
    private fun stopRecord() = AudioRecordHelper.getInstance().stop()

    private lateinit var muxer: MediaMuxerProxy
    private var trackIndex = -1

    private suspend fun encodingAndSavingAudio(stateText: MutableLiveData<String>) =
        withContext(Dispatchers.IO) {
            val bytes = AudioRecordHelper.getInstance().byte
            val sampleRate = AudioRecordHelper.getInstance().sampleRate
            val channelCount = AudioRecordHelper.getInstance().channelCount

            // Create out media format
            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
            // AACObjectLC = AAC LC (Low Complexity)
            // see: https://en.wikipedia.org/wiki/Advanced_Audio_Coding
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BITRATE)

            // Create encoder
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)

            val bytesSize = bytes.size
            var offset = 0
            var totalBytesRead = 0L
            var presentationTimeUs = 0L

            codec.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)
                    if (VERBOSE) Log.d(TAG, "onInputBufferAvailable: id=$inputBufferId, bs=$bytesSize, tr=$totalBytesRead, of=$offset, " +
                            "pt=$presentationTimeUs, r=${inputBuffer?.remaining()}")
                    inputBuffer?.apply {
                        val sizeInBytes = capacity()

                        val validSize = min(bytesSize - offset, sizeInBytes)
                        if (validSize > 0) {
                            put(bytes, offset, validSize)
                            codec.queueInputBuffer(inputBufferId, 0, validSize, presentationTimeUs, 0)
                            offset += validSize
                            totalBytesRead += validSize
                            // presentationTimeUs算法
                            presentationTimeUs = 1000000L * (totalBytesRead / 2) / sampleRate
                        } else {
                            // Handle EOS
                            handleEndOfStream(codec, inputBufferId)
                        }
                    }
                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    outputBufferId: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferId)!!
                    if (VERBOSE) Log.d(TAG, "onOutputBufferAvailable outputBuffer=$outputBuffer, info:{flags=${info.flags}, size=${info.size}}")

                    // Ignore codec config buffers
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (VERBOSE) Log.d(TAG, "audio encoder: codec config buffer");
                        codec.releaseOutputBuffer(outputBufferId, false)
                        return
                    }

                    // Using muxer writing sample to output file
                    // I tried writing by hand with Okio, but got broken media file.
                    if (info.size != 0) {
                        muxer.writeSampleData(trackIndex, outputBuffer, info)
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "audio encoder: EOS");

                        codec.stop()
                        codec.release()

                        muxer.release()
                    }
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    if (VERBOSE) Log.d(TAG, "onOutputFormatChanged $format")

                    // Create muxer in codec's callback
                    muxer = createMuxerProxy()
                    // Only one track here
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    if (VERBOSE) Log.d(TAG, "onError", e)
                }

            })

            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
        }


    private fun createMuxerProxy(): MediaMuxerProxy {
        File(outputAvcFilePath).delete()
        return MediaMuxerProxy(outputAvcFilePath)
    }

    private val outputAvcFilePath = getApplication<Application>().externalCacheDir!!.absolutePath + File.separator + "temp.mp4"

    private fun handleEndOfStream(codec: MediaCodec, inputBufferId: Int) {
        codec.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
    }

    fun playAacWithMediaPlayer() {
        viewModelScope.launch {
            // My encoded AAC audio (mp4) cannot be played with androidx.media2.MediaPlayer
            // error log:
            // W/VideoCapabilities: Unrecognized profile 2130706433 for video/avc
            // I/VideoCapabilities: Unsupported profile 4 for video/mp4v-es
            // I/ACodec: codec does not support config priority (err -2147483648)
            // I/ACodec: codec does not support config operating rate (err -2147483648)
             playWithMedia2Player(getApplication(), outputAvcFilePath)

            // But the android.media.MediaPlayer could play the audio
//            playWithLegacyMediaPlayer(outputAvcFilePath)
        }
    }

    fun decodeAacAndPlayWithAudioTrack() {

    }

    companion object {
        private const val RECORDING_TEXT_DEFAULT = "RECORD AUDIO AND ENCODE TO AAC"
        private const val RECORDING_TEXT_COUNT_DOWN = "RECORDING TIME LEFT "
        private const val RECORDING_TEXT_FINISH_SAVING = "RECORDING FINISH AND SAVING"
        private const val MAX_RECORD_DURATION = 5L
        private const val CODEC_TIMEOUT = 5000L
        private const val OUTPUT_AUDIO_BITRATE = 128 * 1024

        private const val VERBOSE = true
        private const val TAG = "AudioCodecDemo"
    }
}
