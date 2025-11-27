package com.example.todo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TaskAdapter
    private lateinit var drawer: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var dbHelper: DbHelper

    // Launcher to get data back from AddNoteActivity
    private val addNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val title = result.data!!.getStringExtra("title").orEmpty()
            val desc = result.data!!.getStringExtra("desc").orEmpty()
            val date = result.data!!.getLongExtra("dateMillis", System.currentTimeMillis())
            val color = result.data!!.getIntExtra("color", 0xFF90CAF9.toInt())
            val imagePath = result.data!!.getStringExtra("imagePath")

            insert(title = title, note = desc, createdAt = date, color = color, imagePath = imagePath)
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
        adapter = TaskAdapter(emptyList())
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
    }

    // --- App Bar Menu (Search + Add) ---
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)

        val sv = searchItem?.actionView as? SearchView

        sv?.queryHint = "Search by title"
        // expand so typing works immediately (optional)
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
    inner class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "todo.db", null, 2) {
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
        dbHelper.writableDatabase.insert("tasks", null, cv)
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
            val imgp = it.getColumnIndexOrThrow("image_path")
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
