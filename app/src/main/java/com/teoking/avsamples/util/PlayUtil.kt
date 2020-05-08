package com.teoking.avsamples.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media2.MediaPlayer
import androidx.media2.SessionPlayer
import androidx.media2.UriMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

const val TAG = "PlayUtil"
const val VERBOSE = true

private lateinit var media2Player: MediaPlayer
private lateinit var legacyPlayer: android.media.MediaPlayer

suspend fun playWithMedia2Player(context: Context, filePath: String) = withContext(Dispatchers.IO) {
    if (!::media2Player.isInitialized) {
        media2Player = MediaPlayer(context)
    } else {
        media2Player.reset()
    }
    val mediaItem = UriMediaItem.Builder(context, Uri.fromFile(File(filePath))).build()
    val mediaItemResult = media2Player.setMediaItem(mediaItem).get()

    if (VERBOSE) Log.d(TAG, "[media2 player] setMediaItem result: ${mediaItemResult.resultCode}")
    if (mediaItemResult.resultCode == SessionPlayer.PlayerResult.RESULT_CODE_SUCCESS) {
        val prepareResult = media2Player.prepare().get()
        if (VERBOSE) Log.d(TAG, "[media2 player] prepare result: ${prepareResult.resultCode}")

        if (prepareResult.resultCode == SessionPlayer.PlayerResult.RESULT_CODE_SUCCESS) {
            val result = media2Player.play().get()
            if (VERBOSE) Log.d(TAG, "[media2 player] play result: ${result.resultCode}")
        }
    }
}

suspend fun playWithLegacyMediaPlayer(filePath: String) = withContext(Dispatchers.IO) {
    if (!::media2Player.isInitialized) {
        legacyPlayer = android.media.MediaPlayer()
    } else {
        legacyPlayer.reset()
    }

    legacyPlayer.setDataSource(filePath)
    legacyPlayer.prepare()
    legacyPlayer.start()
}

