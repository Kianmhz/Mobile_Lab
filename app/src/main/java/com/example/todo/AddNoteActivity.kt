package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AddNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val taskTitle = findViewById<EditText>(R.id.tasktitle)
        val taskDate = findViewById<DatePicker>(R.id.taskDate)
        val taskDesc = findViewById<EditText>(R.id.taskDesc)

        val doneButton = findViewById<Button>(R.id.addNoteDoneButton)

        doneButton.setOnClickListener {
            val title = taskTitle.text?.toString()?.trim().orEmpty()
            val desc  = taskDesc.text?.toString()?.trim().orEmpty()

            if (title.isBlank()) {
                taskTitle.error = "Title cannot be empty"
                taskTitle.requestFocus()
                return@setOnClickListener
            }

            // Convert DatePicker (year, month, day) to epoch millis
            val cal = java.util.Calendar.getInstance().apply {
                set(taskDate.year, taskDate.month, taskDate.dayOfMonth, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val dateMillis = cal.timeInMillis

            // You can add a color picker later; for now default to a nice blue
            val color = 0xFF90CAF9.toInt()

            // Return to Main with data
            val data = Intent().apply {
                putExtra("title", title)
                putExtra("desc", desc)
                putExtra("dateMillis", dateMillis)
                putExtra("color", color)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }
}
