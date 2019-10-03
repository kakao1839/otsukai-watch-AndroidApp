package com.example.otukai_watch

import android.media.MediaRecorder
import java.io.File

class Recorder {
    private var recorder: MediaRecorder? = null

    fun start(filePath: String) {
        var wavFile: File? = File(filePath)
        if (wavFile!!.exists()) {
            wavFile.delete()
        }
        wavFile = null
        try {
            recorder = MediaRecorder()
            recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            recorder!!.setOutputFile(filePath)
            recorder!!.prepare()
            recorder!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun stop() {
        try {
            recorder?.stop()
            recorder?.reset()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}