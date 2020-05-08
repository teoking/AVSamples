package com.teoking.avsamples.logic.extractor_muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer

class MediaMuxerProxy(outputFilePath: String) {

    private val muxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    fun release() {
        muxer.release()
    }

    fun addTrack(format: MediaFormat): Int {
        return muxer.addTrack(format)
    }

    fun stop() {
        muxer.stop()
    }

    fun start() {
        muxer.start()
    }

    fun writeSampleData(
        currentTrackIndex: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        muxer.writeSampleData(currentTrackIndex, byteBuffer, bufferInfo)
    }
}