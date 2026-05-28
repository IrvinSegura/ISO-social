package com.nexus.feature.post.presentation.create

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputPath: String? = null

    fun startRecording(): String {
        val file = File(context.cacheDir, "post_audio_${System.currentTimeMillis()}.m4a")
        outputPath = file.absolutePath
        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputPath)
            prepare()
            start()
        }
        return outputPath!!
    }

    fun stopRecording(): String? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            outputPath
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            outputPath = null
            null
        }
    }

    fun deleteFile(path: String) = File(path).delete()

    fun release() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else MediaRecorder()
}
