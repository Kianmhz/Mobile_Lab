package com.example.todo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
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
        val taskColour = findViewById<Spinner>(R.id.taskColour)

        // Read incoming extras
        val mode = intent.getStringExtra("mode")
        val taskId = intent.getLongExtra("taskId", -1L)
        val incomingTitle = intent.getStringExtra("title")
        val incomingDesc = intent.getStringExtra("desc")
        val incomingDateMillis = intent.getLongExtra("dateMillis", 0L)
        val incomingColor = intent.getIntExtra("color", Color.TRANSPARENT)

        if (mode == "edit") {
            supportActionBar?.title = "Edit Task"

            // Title/Desc
            taskTitle.setText(incomingTitle.orEmpty())
            taskDesc.setText(incomingDesc.orEmpty())

            // Date
            if (incomingDateMillis != 0L) {
                val cal =
                    java.util.Calendar.getInstance().apply { timeInMillis = incomingDateMillis }
                taskDate.updateDate(
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH),
                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                )
            }

            val colorName = when (incomingColor) {
                Color.RED -> "Red"
                Color.BLUE -> "Blue"
                Color.GREEN -> "Green"
                Color.YELLOW -> "Yellow"
                Color.MAGENTA -> "Purple"
                else -> "None"
            }
            (0 until taskColour.count)
                .firstOrNull { taskColour.getItemAtPosition(it)?.toString() == colorName }
                ?.let { taskColour.setSelection(it) }
        } else {
            supportActionBar?.title = "Add Task"
        }

        val doneButton = findViewById<Button>(R.id.addNoteDoneButton)
        doneButton.setOnClickListener {
            val title = taskTitle.text?.toString()?.trim().orEmpty()
            val desc = taskDesc.text?.toString()?.trim().orEmpty()
            val colourVal = taskColour.selectedItem?.toString()
            val color = when (colourVal) {
                "Red" -> Color.RED
                "Blue" -> Color.BLUE
                "Green" -> Color.GREEN
                "Yellow" -> Color.YELLOW
                "Purple" -> Color.MAGENTA
                else -> Color.TRANSPARENT
            }

            if (title.isBlank()) {
                taskTitle.error = "Title cannot be empty"
                taskTitle.requestFocus()
                return@setOnClickListener
            }

            val cal = java.util.Calendar.getInstance().apply {
                set(taskDate.year, taskDate.month, taskDate.dayOfMonth, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val dateMillis = cal.timeInMillis

            // Return edited/new values
            val data = Intent().apply {
                putExtra("taskId", taskId)  // keep -1 if new
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
