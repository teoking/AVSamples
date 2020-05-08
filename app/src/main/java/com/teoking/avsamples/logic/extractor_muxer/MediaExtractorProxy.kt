package com.teoking.avsamples.logic.extractor_muxer

import android.content.res.AssetFileDescriptor
import android.media.DrmInitData
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.util.*

class MediaExtractorProxy(mediaFileDescriptor: AssetFileDescriptor) {

    private val mExtractor: MediaExtractor = MediaExtractor()
    private var mAudioTrackIndex = -1
    private var mVideoTrackIndex = -1

    init {
        mExtractor.setDataSource(mediaFileDescriptor)
        for (i in 0 until getTrackCount()) {
            val format = getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio")) {
                mAudioTrackIndex = i
            } else if (mime.startsWith("video")) {
                mVideoTrackIndex = i
            }
        }
    }

    fun getTrackCount(): Int {
        return mExtractor.trackCount
    }

    fun getTrackFormat(index: Int): MediaFormat {
        return mExtractor.getTrackFormat(index)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCasInfo(index: Int): MediaExtractor.CasInfo? {
        return mExtractor.getCasInfo(index)
    }

    fun getCachedDuration(): Long {
        return mExtractor.cachedDuration
    }

    fun getDrmInitData(): DrmInitData? {
        return mExtractor.drmInitData
    }

    fun getPsshInfo(): MutableMap<UUID, ByteArray>? {
        return mExtractor.psshInfo
    }

    fun getSampleFlags(): Int {
        return mExtractor.sampleFlags
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getSampleSize(): Long {
        return mExtractor.sampleSize
    }

    fun getSampleTime(): Long {
        return mExtractor.sampleTime
    }

    fun getSampleTrackIndex(): Int {
        return mExtractor.sampleTrackIndex
    }

    fun release() {
        mExtractor.release()
    }

    private fun readSampleData(byteBuffer: ByteBuffer, offset: Int): Int {
        return mExtractor.readSampleData(byteBuffer, offset)
    }

    private fun advance() {
        mExtractor.advance()
    }

    private fun selectTracks(index: Int) {
        mExtractor.selectTrack(index)
    }

    fun extractMp4MediaInfo(): String {
        val result = StringBuilder()
        for (i in 0 until mExtractor.trackCount) {
            val format = getTrackFormat(i)
            result.append("MediaFormat $i =").append(format.toString()).appendln("")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                result.appendln("CasInfo: ${getCasInfo(i)}")
            }
        }
        result.appendln("CachedDuration: ${getCachedDuration()}")
        result.appendln("DrmInitData: ${getDrmInitData()}")
        result.appendln("PsshInfo: ${getPsshInfo()}")
        result.appendln("SampleFlags: ${getSampleFlags()}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            result.appendln("SampleSize: ${getSampleSize()}")
        }
        result.appendln("SampleTime: ${getSampleTime()}")
        result.appendln("SampleTrackIndex: ${getSampleTrackIndex()}")

        return result.toString()
    }

    private fun isAudioTrack(index: Int): Boolean {
        return mAudioTrackIndex != - 1 && index == mAudioTrackIndex
    }

    fun getInputBuffer(trackIndex: Int, byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo): Pair<Int, Boolean> {
        selectTracks(trackIndex)
        val size = readSampleData(byteBuffer, 0)
        val isAudioTrack = isAudioTrack(getSampleTrackIndex())
        Log.d("tedted", "isAudioTrack=$isAudioTrack, size=$size")
        bufferInfo.flags = getSampleFlags()
        bufferInfo.offset = 0
        bufferInfo.size = size
        bufferInfo.presentationTimeUs = getSampleTime()
        advance()
        return Pair(size, isAudioTrack)
    }
}