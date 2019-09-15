package com.example.otukai_watch.ToDoList

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.otukai_watch.MainActivity
import com.example.otukai_watch.R
import com.example.otukai_watch.com.example.otukai_watch.toDo.DBHandler
import com.example.otukai_watch.com.example.otukai_watch.toDo.DTO.ToDo
import kotlinx.android.synthetic.main.yarukoto_main.*

class ToDoListPage : AppCompatActivity() {

    lateinit var dbHandler: DBHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yarukoto_main)
        setSupportActionBar(tbTestToolbar)
        title = "Dashboard"
        dbHandler = DBHandler(this)
        rv_dashboard.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.yarukoto_dialog_dashboard, null)
            val toDoName = view.findViewById<EditText>(R.id.ev_todo)
            dialog.setView(view)
            dialog.setPositiveButton("Add") { _: DialogInterface, _: Int ->
                if (toDoName.text.isNotEmpty()) {
                    val toDo = ToDo()
                    toDo.name = toDoName.text.toString()
                    dbHandler.addToDo(toDo)
                    refreshList()
                }
            }

            StartOtukaiButton.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
