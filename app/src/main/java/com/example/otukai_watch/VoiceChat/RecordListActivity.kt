package com.example.otukai_watch.VoiceChat

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import com.example.otukai_watch.MainActivity
import com.example.otukai_watch.R
import com.example.otukai_watch.VoiceChat.adapter.MyAdapter
import com.example.otukai_watch.VoiceChat.model.Recording
import com.example.otukai_watch.VoiceChat.model.Voice
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.item_recording.view.*
import kotlinx.android.synthetic.main.record_list.*
import java.io.File
import java.io.IOException

/**
 * レコードしたものをリスト表示する.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class RecordListActivity : AppCompatActivity(), MyAdapter.OnClickListener {
    private var mediaPlayer: MediaPlayer? = null
    private var lastProgress = 0
    private val mHandler = Handler()
    private var isPlaying = false
    private var last_index = -1
    private var myAdapter: MyAdapter? = null

    override fun onClickPlay(
        view: View,
        record: Recording,
        recordingList: ArrayList<Recording>,
        position: Int
    ) {
        playRecordItem(view, record, recordingList, position)
    }

    private fun playRecordItem(
        view: View,
        recordItem: Recording,
        recordingList: ArrayList<Recording>,
        position: Int
    ) {
        val recording = recordingList[position]

        if (isPlaying) {
            stopPlaying()
            if (position == last_index) {
                recording.isPlaying = false
                stopPlaying()
                myAdapter!!.notifyItemChanged(position)
            } else {
                markAllPaused(recordingList)
                recording.isPlaying = true
                myAdapter!!.notifyItemChanged(position)
                startPlaying(recording.uri, recordItem, view.seekBar, position)
                last_index = position
            }
            seekUpdate(view)
        } else {
            if (recording.isPlaying) {
                recording.isPlaying = false
                stopPlaying()
            } else {
                startPlaying(recording.uri, recordItem, view.seekBar, position)
                recording.isPlaying = true
                view.seekBar.max = mediaPlayer!!.duration
            }
            myAdapter!!.notifyItemChanged(position)
            last_index = position
        }

        manageSeekBar(view.seekBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_list)

        getAllRecordings()
    }

    @SuppressLint("WrongConstant")
    private fun getAllRecordings() {
        val recordArrayList = ArrayList<Recording>()
        val root = android.os.Environment.getExternalStorageDirectory()
        val path = root.absolutePath + "/AndroidCodility/Audios"


        val requestUrl: String = "https://pck.itok01.com/api/v1/voicelog"
        val voiceAPI: String = "https://pck.itok01.com/api/v1/voice"

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val httpAsync = (requestUrl + "?user=taro")
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        println(ex)
                    }
                    is Result.Success -> {
                        val data = result.get()
                        println(data)

                        val listMyData =
                            Types.newParameterizedType(List::class.java, Voice::class.java)
                        val listAdapter: JsonAdapter<Iterable<Voice>> =
                            moshi.adapter(listMyData)

                        for (voice in listAdapter.fromJson(data)!!) {
                            val fileName: String = voice.id.toString() + ".wav"
                            val fileUri: String = path + "/" + fileName

                            if (!File(fileUri).exists()) {
                                val httpAsync = (voiceAPI + "?user=taro&id=" + voice.id.toString())
                                    .httpDownload()
                                    .fileDestination { _, _ ->
                                        File(fileUri)
                                    }
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
                            recordArrayList.add(Recording(voice, fileUri, fileName, false))
                        }
                    }
                }
            }

        httpAsync.join()

        tvNoData.visibility = View.GONE
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this,
            androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
            false
        )
        myAdapter = MyAdapter(recordArrayList)
        myAdapter!!.setListener(this)
        recyclerView.adapter = myAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun seekUpdate(itemView: View) {
        if (mediaPlayer != null) {
            val mCurrentPosition = mediaPlayer!!.currentPosition
            itemView.seekBar.max = mediaPlayer!!.duration
            itemView.seekBar.progress = mCurrentPosition
            lastProgress = mCurrentPosition
        }
        mHandler.postDelayed(Runnable { seekUpdate(itemView) }, 100)
    }

    private fun manageSeekBar(seekBar: SeekBar?) {
        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer!!.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun stopPlaying() {
        try {
            mediaPlayer!!.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaPlayer = null
        isPlaying = false
    }

    private fun startPlaying(uri: String?, audio: Recording?, seekBar: SeekBar?, position: Int) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer!!.setDataSource(uri)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        } catch (e: IOException) {
            Log.e("LOG_TAG", "prepare() failed")
        }
//showing the pause button
        seekBar!!.max = mediaPlayer!!.duration
        isPlaying = true

        mediaPlayer!!.setOnCompletionListener(MediaPlayer.OnCompletionListener {
            audio!!.isPlaying = false
            myAdapter!!.notifyItemChanged(position)
        })
    }

    private fun markAllPaused(recordingList: ArrayList<Recording>) {
        for (i in recordingList.indices) {
            recordingList[i].isPlaying = false
            recordingList[i] = recordingList[i]
        }
        myAdapter!!.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopPlaying()
    }
}
