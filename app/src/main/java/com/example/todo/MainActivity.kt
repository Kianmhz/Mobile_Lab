package com.example.todo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var adapter: TaskAdapter
    private lateinit var drawer: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var dbHelper: DbHelper

    // --- SENSOR CODE ---
    private lateinit var sensorManager: SensorManager
    private var isDarkMode = false
    private var accelCurrent = 0f
    private var accelLast = 0f
    private var shake = 0f

    // Launcher to get data back from AddNoteActivity (both add + edit)
    private val addNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!
            val taskId = data.getLongExtra("taskId", -1L)
            val title = data.getStringExtra("title").orEmpty()
            val desc = data.getStringExtra("desc").orEmpty()
            val date = data.getLongExtra("dateMillis", System.currentTimeMillis())
            val color = data.getIntExtra("color", 0xFF90CAF9.toInt())
            val imagePath = data.getStringExtra("imagePath")

            if (taskId >= 0) {
                // EDIT
                updateTask(
                    id = taskId,
                    title = title,
                    note = desc,
                    createdAt = date,
                    color = color,
                    imagePath = imagePath
                )
            } else {
                // ADD
                insert(
                    title = title,
                    note = desc,
                    createdAt = date,
                    color = color,
                    imagePath = imagePath
                )
            }
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        dbHelper = DbHelper(this)

        // RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.tasks_arr)
        adapter = TaskAdapter(emptyList()) { task ->
            // tap to edit
            val intent = Intent(this, AddNoteActivity::class.java).apply {
                putExtra("mode", "edit")
                putExtra("taskId", task.id)
                putExtra("title", task.title)
                putExtra("desc", task.note)
                putExtra("dateMillis", task.createdAt)
                putExtra("color", task.color)
                putExtra("imagePath", task.imagePath)
            }
            addNoteLauncher.launch(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        refresh()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_add_note -> addNoteLauncher.launch(Intent(this, AddNoteActivity::class.java))
            }
            drawer.closeDrawers()
            true
        }

        // --- SENSOR CODE: Initialize sensors ---
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Light sensor
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Accelerometer
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // ------------------ SENSOR LISTENER ------------------
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {

            // --- LIGHT SENSOR: Auto dark mode ---
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                if (lux < 10 && !isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    isDarkMode = true
                } else if (lux >= 10 && isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    isDarkMode = false
                }
            }

            // --- ACCELEROMETER: Shake to add task ---
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                accelLast = accelCurrent
                accelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = accelCurrent - accelLast
                shake = shake * 0.9f + delta

                if (shake > 12) {  // shake threshold
                    addNoteLauncher.launch(Intent(this, AddNoteActivity::class.java))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // --- App Bar Menu (Search + Add + Delete) ---
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val sv = searchItem?.actionView as? SearchView

        sv?.queryHint = "Search by title"
        searchItem?.expandActionView()
        sv?.isIconified = false

        sv?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean {
                adapter.submitList(if (q.isNullOrBlank()) allTasks() else searchTasksByTitle(q))
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.submitList(if (newText.isNullOrBlank()) allTasks() else searchTasksByTitle(newText))
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                addNoteLauncher.launch(Intent(this, AddNoteActivity::class.java))
                true
            }

            R.id.action_delete -> {
                deleteCheckedTasks()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // ------------------ DATABASE ------------------
    inner class DbHelper(ctx: Context) :
        SQLiteOpenHelper(ctx, "todo.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE tasks(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    note TEXT DEFAULT '',
                    color INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    done INTEGER NOT NULL DEFAULT 0,
                    image_path TEXT DEFAULT ''
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_title ON tasks(title)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
            db.execSQL("DROP TABLE IF EXISTS tasks")
            onCreate(db)
        }
    }

    private fun insert(
        title: String,
        note: String,
        createdAt: Long,
        color: Int,
        done: Boolean = false,
        imagePath: String? = null
    ) {
        val cv = ContentValues().apply {
            put("title", title)
            put("note", note)
            put("color", color)
            put("created_at", createdAt)
            put("done", if (done) 1 else 0)
            put("image_path", imagePath)
        }
        dbHelper.writableDatabase.insert("tasks", null, cv)
    }

    private fun updateTask(
        id: Long,
        title: String,
        note: String,
        createdAt: Long,
        color: Int,
        imagePath: String?,
        done: Boolean = false
    ) {
        val cv = ContentValues().apply {
            put("title", title)
            put("note", note)
            put("color", color)
            put("created_at", createdAt)
            put("done", if (done) 1 else 0)
            put("image_path", imagePath)
        }
        // IMPORTANT: this is UPDATE, not insert
        dbHelper.writableDatabase.update("tasks", cv, "_id = ?", arrayOf(id.toString()))
    }

    private fun all(): List<Map<String, Any>> =
        query("SELECT * FROM tasks ORDER BY created_at DESC", null)

    private fun search(q: String): List<Map<String, Any>> =
        query(
            "SELECT * FROM tasks WHERE title LIKE ? COLLATE NOCASE ORDER BY created_at DESC",
            arrayOf("%$q%")
        )

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun query(sql: String, args: Array<String>?): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val c = db.rawQuery(sql, args)
        val res = mutableListOf<Map<String, Any>>()
        c.use {
            val id = it.getColumnIndexOrThrow("_id")
            val t = it.getColumnIndexOrThrow("title")
            val n = it.getColumnIndexOrThrow("note")
            val col = it.getColumnIndexOrThrow("color")
            val ts = it.getColumnIndexOrThrow("created_at")
            val dn = it.getColumnIndexOrThrow("done")
            val img = it.getColumnIndexOrThrow("image_path")
            while (it.moveToNext()) {
                res += mapOf(
                    "_id" to it.getLong(id),
                    "title" to it.getString(t),
                    "note" to it.getString(n),
                    "color" to it.getInt(col),
                    "created_at" to it.getLong(ts),
                    "done" to (it.getInt(dn) == 1),
                    "image_path" to it.getString(img)
                )
            }
        }
        return res
    }

    private fun deleteCheckedTasks() {
        val checkedIds = adapter.getCheckedTaskIds()
        if (checkedIds.isEmpty()) return
        val db = dbHelper.writableDatabase
        checkedIds.forEach { id ->
            db.delete("tasks", "_id = ?", arrayOf(id.toString()))
        }
        adapter.clearChecked()
        refresh()
    }

    private fun allTasks(): List<Task> {
        return all().map {
            Task(
                id = it["_id"] as Long,
                title = it["title"] as String,
                note = it["note"] as String,
                color = it["color"] as Int,
                createdAt = it["created_at"] as Long,
                done = it["done"] as Boolean,
                imagePath = it["image_path"] as String?
            )
        }
    }

    private fun searchTasksByTitle(q: String): List<Task> {
        return search(q).map {
            Task(
                id = it["_id"] as Long,
                title = it["title"] as String,
                note = it["note"] as String,
                color = it["color"] as Int,
                createdAt = it["created_at"] as Long,
                done = it["done"] as Boolean,
                imagePath = it["image_path"] as String?
            )
        }
    }

    private fun refresh() {
        adapter.submitList(allTasks())
    }
}
