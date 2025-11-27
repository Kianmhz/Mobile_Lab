package com.example.todo

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(private var tasks: List<Task>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val checkedIds = mutableSetOf<Long>()

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: View = itemView.findViewById(R.id.taskColor)
        val titleText: TextView = itemView.findViewById(R.id.taskTitle)
        val tDate: TextView = itemView.findViewById(R.id.taskDueDate)
        val note: TextView = itemView.findViewById(R.id.taskDescription)
        val checkbox: CheckBox = itemView.findViewById(R.id.taskCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // ---- Fill UI ----
        holder.titleText.text = task.title
        holder.note.text = task.note
        holder.colorView.setBackgroundColor(task.color)

        val date = Date(task.createdAt)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tDate.text = formatter.format(date)

        // ---- Bind checkbox state ----
        holder.checkbox.setOnCheckedChangeListener(null) // prevents unwanted triggers
        holder.checkbox.isChecked = checkedIds.contains(task.id)

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkedIds.add(task.id)
            else checkedIds.remove(task.id)
        }

        // Animations on touch (your original code)
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    true
                }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun submitList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    // ---- Public selection API ----
    fun getCheckedTaskIds(): List<Long> = checkedIds.toList()

    fun clearChecked() {
        checkedIds.clear()
        notifyDataSetChanged()
    }
}
