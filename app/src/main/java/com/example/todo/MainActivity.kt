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
import android.widget.Button
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

    // DB
    private lateinit var dbHelper: DbHelper

    // Launcher to get data back from AddNoteActivity (handles both add and edit)
    private val addNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val taskId = result.data!!.getLongExtra("taskId", -1L)
            val title = result.data!!.getStringExtra("title").orEmpty()
            val desc  = result.data!!.getStringExtra("desc").orEmpty()
            val date  = result.data!!.getLongExtra("dateMillis", System.currentTimeMillis())
            val color = result.data!!.getIntExtra("color", 0xFF90CAF9.toInt())

            if (taskId >= 0) {
                // EDIT mode
                updateTask(taskId, title, desc, date, color)
            } else {
                // ADD mode
                insert(title = title, note = desc, createdAt = date, color = color)
            }
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Drawer
        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        // DB helper inside Main
        dbHelper = DbHelper(this)

        // RecyclerView with click listener for editing
        val recyclerView = findViewById<RecyclerView>(R.id.tasks_arr)
        adapter = TaskAdapter(mutableListOf()) { task ->
            // When a task is clicked, open AddNoteActivity in edit mode
            val intent = Intent(this, AddNoteActivity::class.java).apply {
                putExtra("mode", "edit")
                putExtra("taskId", task.id)
                putExtra("title", task.title)
                putExtra("desc", task.note)
                putExtra("dateMillis", task.createdAt)
                putExtra("color", task.color)
            }
            addNoteLauncher.launch(intent)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Delete button
        val deleteButton = findViewById<Button>(R.id.btnDeleteSelected)
        deleteButton.setOnClickListener {
            deleteCheckedTasks()
        }

        // SearchView directly in layout
        val searchView = findViewById<SearchView>(R.id.searchBar)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean {
                adapter.submitList(if (q.isNullOrBlank()) allTasks() else searchTasksByTitle(q))
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.submitList(if (newText.isNullOrBlank()) allTasks() else searchTasksByTitle(newText))
                return true
            }
        })

        refresh()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_add_note -> {
                    val intent = Intent(this, AddNoteActivity::class.java).apply {
                        putExtra("mode", "add")
                    }
                    addNoteLauncher.launch(intent)
                }
            }
            drawer.closeDrawers()
            true
        }
    }

    // --- App Bar Menu (Add button only now) ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                val intent = Intent(this, AddNoteActivity::class.java).apply {
                    putExtra("mode", "add")
                }
                addNoteLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ------------------ SQLite ------------------

    inner class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "todo.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE tasks(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    note TEXT DEFAULT '',
                    color INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    done INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            // Optional index for faster LIKE searches on title
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_title ON tasks(title)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
            db.execSQL("DROP TABLE IF EXISTS tasks")
            onCreate(db)
        }
    }

    private fun insert(title: String, note: String, createdAt: Long, color: Int, done: Boolean = false) {
        val cv = ContentValues().apply {
            put("title", title)
            put("note", note)
            put("color", color)
            put("created_at", createdAt)
            put("done", if (done) 1 else 0)
        }
        dbHelper.writableDatabase.insert("tasks", null, cv)
    }

    private fun updateTask(
        id: Long,
        title: String,
        note: String,
        createdAt: Long,
        color: Int,
        done: Boolean = false
    ) {
        val cv = ContentValues().apply {
            put("title", title)
            put("note", note)
            put("color", color)
            put("created_at", createdAt)
            put("done", if (done) 1 else 0)
        }
        dbHelper.writableDatabase.update("tasks", cv, "_id = ?", arrayOf(id.toString()))
    }

    private fun all(): List<Map<String, Any>> =
        query("SELECT * FROM tasks ORDER BY created_at DESC", null)

    // Case-insensitive title search
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
            val t  = it.getColumnIndexOrThrow("title")
            val n  = it.getColumnIndexOrThrow("note")
            val col= it.getColumnIndexOrThrow("color")
            val ts = it.getColumnIndexOrThrow("created_at")
            val dn = it.getColumnIndexOrThrow("done")
            while (it.moveToNext()) {
                res += mapOf(
                    "_id" to it.getLong(id),
                    "title" to it.getString(t),
                    "note" to it.getString(n),
                    "color" to it.getInt(col),
                    "created_at" to it.getLong(ts),
                    "done" to (it.getInt(dn) == 1)
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

    // Map all rows → Task
    private fun allTasks(): List<Task> {
        return all().map {
            Task(
                id = it["_id"] as Long,
                title = it["title"] as String,
                note = it["note"] as String,
                color = it["color"] as Int,
                createdAt = it["created_at"] as Long,
                done = it["done"] as Boolean
            )
        }
    }

    // Map search rows → Task
    private fun searchTasksByTitle(q: String): List<Task> {
        return search(q).map {
            Task(
                id = it["_id"] as Long,
                title = it["title"] as String,
                note = it["note"] as String,
                color = it["color"] as Int,
                createdAt = it["created_at"] as Long,
                done = it["done"] as Boolean
            )
        }
    }

    private fun refresh() {
        adapter.submitList(allTasks())
    }
}