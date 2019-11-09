package com.example.otukai_watch.VoiceChat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

import android.transition.TransitionManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.example.otukai_watch.R
import com.github.kittinunf.fuel.core.DataPart
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.InlineDataPart
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpUpload
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_voice.*
import java.io.File
import java.io.IOException

/**
 * レコードの最初の画面
 */
@RequiresApi(api = Build.VERSION_CODES.M)
class VoiceActivity : AppCompatActivity(), View.OnClickListener {

  private var mRecorder: MediaRecorder? = null
  private var mPlayer: MediaPlayer? = null
  private var fileName: String? = null
  private var lastProgress = 0
  private val mHandler = Handler()
  private val RECORD_AUDIO_REQUEST_CODE = 101
  private var isPlaying = false

  override fun onClick(view: View?) {
    when (view!!.id) {
      R.id.imgBtRecord -> {
        prepareRecording()
        startRecording()
      }

      R.id.imgBtStop -> {
        prepareStop()
        stopRecording()
      }

      R.id.imgViewPlay -> {
        if (!isPlaying && fileName != null) {
          isPlaying = true
          startPlaying()
        } else {
          isPlaying = false
          stopPlaying()
        }
      }

      R.id.imgBtRecordList -> {
        startActivity(Intent(this, RecordListActivity::class.java))
      }
    }
  }

  //API URL
  val requestUrl = "https://pck.itok01.com/api/v1/voice?user=taro&id=1"


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_voice)

    imgBtRecord.setOnClickListener {
      imgBtRecord.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      getPermissionToRecordAudio()
    }

    imgBtRecord.setOnClickListener(this)
    imgBtStop.setOnClickListener(this)
    imgViewPlay.setOnClickListener(this)
    imgBtRecordList.setOnClickListener(this)
  }

  private fun prepareStop() {
    TransitionManager.beginDelayedTransition(llRecorder)
    imgBtRecord.visibility = View.VISIBLE
    imgBtStop.visibility = View.GONE
    llPlay.visibility = View.VISIBLE
  }


  private fun prepareRecording() {
    TransitionManager.beginDelayedTransition(llRecorder)
    imgBtRecord.visibility = View.GONE
    imgBtStop.visibility = View.VISIBLE
    llPlay.visibility = View.GONE
  }

  private fun stopPlaying() {
    try {
      mPlayer!!.release()
    } catch (e: Exception) {
      e.printStackTrace()
    }

    mPlayer = null
    //showing the play button
    imgViewPlay.setImageResource(R.drawable.ic_play_circle)
    chronometer.stop()
  }

  //レコード開始
  private fun startRecording() {
    mRecorder = MediaRecorder()
    mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
    mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
    val root = android.os.Environment.getExternalStorageDirectory()
    val file = File(root.absolutePath + "/AndroidCodility/Audios")
    if (!file.exists()) {
      file.mkdirs()
    }

    fileName = root.absolutePath + "/AndroidCodility/Audios/" + (System.currentTimeMillis().toString() + ".wav")
    Log.d("filename", fileName)
    mRecorder!!.setOutputFile(fileName)
    mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

    try {
      mRecorder!!.prepare()
      mRecorder!!.start()
    } catch (e: IOException) {
      e.printStackTrace()
    }

    lastProgress = 0
    seekBar.progress = 0
    stopPlaying()
    // making the imageView a stop button starting the chronometer
    chronometer.base = SystemClock.elapsedRealtime()
    chronometer.start()

  }


  private fun stopRecording() {
    try {
      mRecorder!!.stop()
      mRecorder!!.release()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    mRecorder = null
    //starting the chronometer
    chronometer.stop()
    chronometer.base = SystemClock.elapsedRealtime()
    //showing the play button
    Toast.makeText(this, "Recording saved successfully.", Toast.LENGTH_SHORT).show()

    val voiceAPI: String = "https://pck.itok01.com/api/v1/voice"

    val httpAsync = voiceAPI
      .httpUpload()
      .add(FileDataPart(File(fileName), name = "file"))
      .add(InlineDataPart("taro", "user"))
      .add(InlineDataPart("1", "side"))
      .responseString { request, response, result ->
        when (result) {
          is Result.Failure -> {
            val ex = result.getException()
            println(ex)
          }
          is Result.Success -> {
            val data = result.get()
            println(data)
          }
        }
      }

    httpAsync.join()
  }


  private fun startPlaying() {
    mPlayer = MediaPlayer()
    try {
      mPlayer!!.setDataSource(fileName)
      mPlayer!!.prepare()
      mPlayer!!.start()
    } catch (e: IOException) {
      Log.e("LOG_TAG", "prepare() failed")
    }

    //making the imageView pause button
    imgViewPlay.setImageResource(R.drawable.ic_pause_circle)

    seekBar.progress = lastProgress
    mPlayer!!.seekTo(lastProgress)
    seekBar.max = mPlayer!!.duration
    seekBarUpdate()
    chronometer.start()

    mPlayer!!.setOnCompletionListener(MediaPlayer.OnCompletionListener {
      imgViewPlay.setImageResource(R.drawable.ic_play_circle)
      isPlaying = false
      chronometer.stop()
      chronometer.base = SystemClock.elapsedRealtime()
      mPlayer!!.seekTo(0)
    })

    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (mPlayer != null && fromUser) {
          mPlayer!!.seekTo(progress)
          chronometer.base = SystemClock.elapsedRealtime() - mPlayer!!.currentPosition
          lastProgress = progress
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}

      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    })
  }

  private var runnable: Runnable = Runnable { seekBarUpdate() }

  private fun seekBarUpdate() {
    if (mPlayer != null) {
      val mCurrentPosition = mPlayer!!.currentPosition
      seekBar.progress = mCurrentPosition
      lastProgress = mCurrentPosition
    }
    mHandler.postDelayed(runnable, 100)
  }


  private fun getPermissionToRecordAudio() {
    // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid checking the build version since Context.checkSelfPermission(...) is only available in Marshmallow
    // 2) Always check for permission (even if permission has already been granted) since the user can revoke permissions at any time through Settings
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
      || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
      || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissions(
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.RECORD_AUDIO,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), RECORD_AUDIO_REQUEST_CODE
      )
    }
  }

  // Callback with the request from calling requestPermissions(...)
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    // Make sure it's our original READ_CONTACTS request
    if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
      if (grantResults.size == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
        //Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show()
        finishAffinity()
      }
    }
  }
}
