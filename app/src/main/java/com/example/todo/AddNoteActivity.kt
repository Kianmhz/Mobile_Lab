package com.example.todo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

        // Launchers for image picking
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                imagePreview.setImageURI(it)
            }
        }

        val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                // Save bitmap to internal storage
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

            val cal = Calendar.getInstance().apply {
                set(taskDate.year, taskDate.month, taskDate.dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateMillis = cal.timeInMillis

            val data = Intent().apply {
                putExtra("title", title)
                putExtra("desc", desc)
                putExtra("dateMillis", dateMillis)
                putExtra("color", color)
                putExtra("imagePath", imageUri?.toString()) // save URI string
            }

            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
