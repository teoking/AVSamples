package com.teoking.avsamples.ui.recordplay

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.teoking.common.AudioRecordHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException

class AudioRecordPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val mAudioFile: File =
        File(getApplication<Application>().externalCacheDir!!.absolutePath + File.separator + "temp.wav")

    val descText =
        "This demo shows how to record by AudioRecord and then play it(pcm format) with AudioTrack."

    private val _stateText = MutableLiveData<String>().apply {
        value = ""
    }
    val stateText: LiveData<String> = _stateText

    private var isRecording = false

    fun recordPcmStart() {
        if (!AudioRecordHelper.getInstance().isRecording) {
            _stateText.value = "Recording audio..."
            AudioRecordHelper.getInstance().start()
        }
    }

    fun recordPcmStop() {
        if (AudioRecordHelper.getInstance().isRecording) {
            _stateText.value = "Saving audio..."
            AudioRecordHelper.getInstance().stop()
        }

        GlobalScope.launch(Dispatchers.Main) {
            saveRecordedAudio(mAudioFile, _stateText)
        }
    }

    private suspend fun saveRecordedAudio(outFile: File, stateText: MutableLiveData<String>) = withContext(Dispatchers.IO) {
        try {
            val bufferedSink = outFile.sink().buffer()
            bufferedSink.write(AudioRecordHelper.getInstance().byte)
            stateText.postValue("Saving audio to ${outFile.absolutePath}")
        } catch (e: IOException) {
            Log.d("AudioRecordPlayActivity", "record failed", e)
            stateText.postValue("Saving audio failed!")
        }
    }

    fun playPcm() {
        if (isRecording) {
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            isRecording = true
            playPcmInner(mAudioFile)
            isRecording = false
        }
    }

    private suspend fun playPcmInner(pcmFile: File) = withContext(Dispatchers.IO) {
        // 获取录制时使用的码率
        val sampleRateHz = AudioRecordHelper.getInstance().sampleRate
        // 获取录制后音频要使用的播放channel
        val channelConfig = AudioRecordHelper.getInstance().outChannel
        // 获取音频编码格式
        val audioFormat = AudioRecordHelper.getInstance().audioEncoding
        val minBufferSize =
            AudioTrack.getMinBufferSize(sampleRateHz, channelConfig, audioFormat)

        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .build()

        // STREAM_MODE下可以先play（此时并未真正play），在write数据后play
        player.play()

        // 读取pcm音频数据，写入player
        val buffer = Buffer()
        val source = pcmFile.source().buffer()
        var byteCount: Long
        while (source.read(buffer, minBufferSize.toLong()).also { byteCount = it } != -1L) {
            player.write(buffer.readByteArray(), 0, byteCount.toInt())
            player.flush()
        }

        // 写入完毕，也表示播放完成
        player.stop()
        player.release()
    }

}
