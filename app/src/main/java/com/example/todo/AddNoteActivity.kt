package com.example.todo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private var imageUri: Uri? = null

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
        val imagePreview = findViewById<ImageView>(R.id.imagePreview)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnChooseImage = findViewById<Button>(R.id.btnChooseImage)
        val doneButton = findViewById<Button>(R.id.addNoteDoneButton)

        // --- Handle modes (add vs edit) ---
        val mode = intent.getStringExtra("mode")
        val taskId = intent.getLongExtra("taskId", -1L)
        val incomingTitle = intent.getStringExtra("title")
        val incomingDesc = intent.getStringExtra("desc")
        val incomingDateMillis = intent.getLongExtra("dateMillis", 0L)
        val incomingColor = intent.getIntExtra("color", Color.TRANSPARENT)
        val incomingImagePath = intent.getStringExtra("imagePath")

        if (mode == "edit") {
            supportActionBar?.title = "Edit Task"
            taskTitle.setText(incomingTitle.orEmpty())
            taskDesc.setText(incomingDesc.orEmpty())

            if (incomingDateMillis != 0L) {
                val cal = Calendar.getInstance().apply { timeInMillis = incomingDateMillis }
                taskDate.updateDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
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

            if (!incomingImagePath.isNullOrEmpty()) {
                imageUri = Uri.parse(incomingImagePath)
                imagePreview.setImageURI(imageUri)
            }
        } else {
            supportActionBar?.title = "Add Task"
        }

        // --- Image pickers ---
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                imagePreview.setImageURI(it)
            }
        }

        val takePhotoLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
                bitmap?.let {
                    val file = File(cacheDir, "task_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    imageUri = Uri.fromFile(file)
                    imagePreview.setImageBitmap(it)
                }
            }

        btnChooseImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnTakePhoto.setOnClickListener { takePhotoLauncher.launch(null) }

        // --- Done button ---
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

            val cal = Calendar.getInstance().apply {
                set(taskDate.year, taskDate.month, taskDate.dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateMillis = cal.timeInMillis

            val data = Intent().apply {
                putExtra("taskId", taskId)
                putExtra("title", title)
                putExtra("desc", desc)
                putExtra("dateMillis", dateMillis)
                putExtra("color", color)
                putExtra("imagePath", imageUri?.toString())
            }

            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
