package com.example.otukai_watch.OtukaiTimer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import com.example.otukai_watch.R


class timerActivity : AppCompatActivity() {

  val handler = Handler()  //一度だけ代入
  var timeValue = 0       //何度も代入


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_timer)

    /* テキストとボタンの変数定義 */
    val timeText = findViewById(R.id.timetext) as TextView
    val startButton = findViewById(R.id.start) as Button
    val stopButton = findViewById(R.id.stop) as Button
    val resetButton = findViewById(R.id.reset) as Button

    /* 1秒ごとに処理を実行する */
    val runnable = object : Runnable {
      override fun run() {
        timeValue++
        // TextViewを更新
        // ?.letを用いて、nullではない場合のみ更新
        timeToText(timeValue)?.let {
          // timeToText(timeValue)の値がlet内ではitとして使える
          timeText.text = it
        }
        handler.postDelayed(this, 1000)
      }
    }

    //startボタン機能
    startButton.setOnClickListener {
      handler.post(runnable)
    }

    //stopボタン機能
    stopButton.setOnClickListener {
      handler.removeCallbacks(runnable)
    }

    //resetボタン機能
    resetButton.setOnClickListener {
      handler.removeCallbacks(runnable)
      timeValue = 0
      // timeToTextの引数はデフォルト値が設定されているので、引数省略できる
      timeToText()?.let {
        timeText.text = it
      }
    }
  }

  // 数値を00:00:00形式の文字列に変換する関数
  // 引数timeにはデフォルト値0を設定、返却する型はnullableなString?型
  private fun timeToText(time: Int = 0): String? {
    return if (time < 0) {
      null
    } else if (time == 0) {
      "00:00:00"
    } else {
      val h = time / 3600
      val m = time % 3600 / 60
      val s = time % 60
      "%1$02d:%2$02d:%3$02d".format(h, m, s)
    }
  }
}
