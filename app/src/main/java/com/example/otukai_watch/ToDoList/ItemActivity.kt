package com.example.otukai_watch.ToDoList

import android.annotation.SuppressLint
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import com.example.otukai_watch.R
import com.example.otukai_watch.ToDoList.DTO.ToDoItem
import android.content.Intent
import com.example.otukai_watch.OtukaiTimer.timerActivity
import com.example.otukai_watch.ToDoList.Task
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_item.*
import java.util.*

class ItemActivity : AppCompatActivity() {

  lateinit var dbHandler: DBHandler
  var todoId: Long = -1

  var list: MutableList<Task>? = null
  var adapter : ItemAdapter? = null
  var touchHelper : ItemTouchHelper? = null

  // API URL
  val requestUrl = "https://pck.itok01.com/api/v1/task"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_item)
    supportActionBar?.title = intent.getStringExtra(INTENT_TODO_NAME)
    todoId = intent.getLongExtra(INTENT_TODO_ID, -1)
    dbHandler = DBHandler(this)

    startOtukai.setOnClickListener {
      //Intentクラスのインスタンスを生成
      val intent = Intent(this,timerActivity::class.java)
      //アクティビティを起動する
      startActivity(intent)
    }

    rv_item.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    // 追加
    fab_item.setOnClickListener {
      val dialog = AlertDialog.Builder(this)
      dialog.setTitle("やることを追加")
      val view = layoutInflater.inflate(R.layout.dialog_dashboard, null)
      val toDoName = view.findViewById<EditText>(R.id.ev_todo)
      dialog.setView(view)
      dialog.setPositiveButton("追加") { _: DialogInterface, _: Int ->
        if (toDoName.text.isNotEmpty()) {
          val item = ToDoItem()
          item.itemName = toDoName.text.toString()
          item.toDoId = todoId
          item.isCompleted = false
          dbHandler.addToDoItem(item)
          refreshList()

          val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
          val requestAdapter = moshi.adapter(Task::class.java)
          val header: HashMap<String, String> = hashMapOf("Content-Type" to "application/json")

          val task = Task(
            user = "taro",
            item = toDoName.text.toString(),
            done = 0
          )

          val httpAsync = requestUrl
            .httpPost()
            .header(header)
            .body(requestAdapter.toJson(task))
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
      }
      dialog.setNegativeButton("キャンセル") { _: DialogInterface, _: Int ->

      }
      dialog.show()
    }
    // ドラッグで移動するやーつ
    touchHelper =
      ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
          p0: androidx.recyclerview.widget.RecyclerView,
          p1: androidx.recyclerview.widget.RecyclerView.ViewHolder,
          p2: androidx.recyclerview.widget.RecyclerView.ViewHolder
        ): Boolean {
          val sourcePosition = p1.adapterPosition
          val targetPosition = p2.adapterPosition
          Collections.swap(list,sourcePosition,targetPosition)
          adapter?.notifyItemMoved(sourcePosition,targetPosition)
          return true
        }

        override fun onSwiped(p0: androidx.recyclerview.widget.RecyclerView.ViewHolder, p1: Int) {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

      })

    touchHelper?.attachToRecyclerView(rv_item)

  }

  // 編集
  fun updateItem(item: ToDoItem) {
    val dialog = AlertDialog.Builder(this)
    dialog.setTitle("やることを編集")
    val view = layoutInflater.inflate(R.layout.dialog_dashboard, null)
    val toDoName = view.findViewById<EditText>(R.id.ev_todo)
    toDoName.setText(item.itemName)
    dialog.setView(view)
    dialog.setPositiveButton("上書き") { _: DialogInterface, _: Int ->
      if (toDoName.text.isNotEmpty()) {
        item.itemName = toDoName.text.toString()
        item.toDoId = todoId
        item.isCompleted = false
        dbHandler.updateToDoItem(item)
        refreshList()
      }
    }
    dialog.setNegativeButton("キャンセル") { _: DialogInterface, _: Int ->

    }
    dialog.show()
  }

  override fun onResume() {
    refreshList()
    super.onResume()
  }

  private fun refreshList() {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    //スリープ処理挟む
    Thread.sleep(2000L)

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
            val listMyData = Types.newParameterizedType(List::class.java, Task::class.java)
            val listAdapter: JsonAdapter<MutableList<Task>> = moshi.adapter(listMyData)

            list = listAdapter.fromJson(data);

            adapter = ItemAdapter(this, list!!)
            rv_item.adapter = adapter
          }
        }
      }

    httpAsync.join()
  }

  class ItemAdapter(val activity: ItemActivity, val list: MutableList<Task>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
      return ViewHolder(
        LayoutInflater.from(activity).inflate(
          R.layout.rv_child_item,
          p0,
          false
        )
      )
    }

    override fun getItemCount(): Int {
      return list.size
    }


    // checkboxの処理 (-> 削除)
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, p1: Int) {
      holder.itemName.text = list[p1].item
      holder.itemName.isChecked = (list[p1].done != 0)
      holder.delete.setOnClickListener {
        val dialog = AlertDialog.Builder(activity)
        dialog.setTitle("完了")
        dialog.setMessage("やることをリストから削除します。")
        dialog.setPositiveButton("削除") { _: DialogInterface, _: Int ->
          val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
          val requestAdapter = moshi.adapter(Task::class.java)
          val header: HashMap<String, String> = hashMapOf("Content-Type" to "application/json")

          val task = Task(
            user = list[p1].user,
            item = list[p1].item,
            done = 0
          )

          val httpAsync = "https://pck.itok01.com/api/v1/task"
            .httpDelete()
            .header(header)
            .body(requestAdapter.toJson(task))
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

          activity.refreshList()
        }
        dialog.setNegativeButton("キャンセル") { _: DialogInterface, _: Int ->

        }
        dialog.show()
      }
      holder.edit.setOnClickListener {
        //activity.updateItem(list[p1])
      }

      holder.move.setOnTouchListener { v, event ->
        if(event.actionMasked== MotionEvent.ACTION_DOWN){
          activity.touchHelper?.startDrag(holder)
        }
        false
      }
    }

    // config
    class ViewHolder(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
      val itemName: CheckBox = v.findViewById(R.id.cb_item)
      val delete: CheckBox = v.findViewById(R.id.cb_item)
      val edit: ImageView = v.findViewById(R.id.iv_edit)
      // val delete: ImageView = v.findViewById(R.id.iv_delete)
      val move: ImageView = v.findViewById(R.id.iv_move)
    }


  }

}
