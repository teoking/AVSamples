package com.teoking.avsamples.ui.codec

import android.app.Application
import android.media.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teoking.avsamples.util.playWithMedia2Player
import com.teoking.common.AudioRecordHelper
import kotlinx.coroutines.*
import java.io.File

class AudioCodecViewModel(application: Application) : AndroidViewModel(application) {

    val descText = "This demo shows how to encode/decode audio with MediaCodec:\n" +
            "* Encode pcm audio from AudioRecord to AAC (record 5s)\n" +
            "* Play saved AAC with MediaPlayer\n" +
            "* Decode AAC to PCM and played with AudioTrack\n" +
            "* CODE IS NOT STABLE!"

    private var isRecoding = false

    private val _recordingText = MutableLiveData<String>().apply {
        value = RECORDING_TEXT_DEFAULT
    }
    val recordingStateText: LiveData<String> = _recordingText

    private val _stateText = MutableLiveData<String>().apply {
        value = ""
    }
    val stateText: LiveData<String> = _stateText

    private var player: MyAudioTrack? = null

    override fun onCleared() {
        player?.release()
    }

    fun recordAndEncodeAudioToAac() {
        if (isRecoding) {
            return
        }

        isRecoding = true

        viewModelScope.launch {
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
            encodingAndSavingAudio()

            delay(1000)

            _recordingText.value = RECORDING_TEXT_DEFAULT
            isRecoding = false
        }
    }

    private fun startRecord() = AudioRecordHelper.getInstance().start()
    private fun stopRecord() = AudioRecordHelper.getInstance().stop()

    private val outputAvcFilePath =
        getApplication<Application>().externalCacheDir!!.absolutePath + File.separator + "encoded-c2.android.aac.encoder-48000Hz-1ch-131072-64bit.mp4"

    fun playAacWithMediaPlayer() {
        viewModelScope.launch {
            playWithMedia2Player(getApplication(), outputAvcFilePath)
        }
    }

    private fun encodingAndSavingAudio() {
        val bytes = AudioRecordHelper.getInstance().byte
        val sampleRate = AudioRecordHelper.getInstance().sampleRate
        val channelCount = AudioRecordHelper.getInstance().channelCount

        // Create out media format
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        )
        // AACObjectLC = AAC LC (Low Complexity)
        // see: https://en.wikipedia.org/wiki/Advanced_Audio_Coding
        mediaFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BITRATE)

        Thread(Runnable {
            EncoderUtil().encode(
                bytes,
                EncoderUtil.getCodecName(true, mediaFormat),
                mediaFormat,
                getApplication<Application>().externalCacheDir!!.absolutePath
            )
        }).start()
    }

    fun decodeAacAndPlayWithAudioTrack() {
        Thread {
            val shortArray = DecoderUtil.decodeToShortArray(outputAvcFilePath)!!
            if (shortArray.isNotEmpty()) {
                if (player == null) {
                    player = MyAudioTrack(
                        48000,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    player?.init()
                }
                player?.playAudioTrack(shortArray, 0, shortArray.size)
            }
        }.start()
    }

    companion object {
        private const val RECORDING_TEXT_DEFAULT = "Record audio and encode to AAC"
        private const val RECORDING_TEXT_COUNT_DOWN = "Recording time left "
        private const val RECORDING_TEXT_FINISH_SAVING = "Recording finish and saving"
        private const val MAX_RECORD_DURATION = 5L
        private const val CODEC_TIMEOUT = 5000L
        private const val OUTPUT_AUDIO_BITRATE = 128 * 1024

        private const val VERBOSE = true
        private const val TAG = "AudioCodecDemo"
    }
}
