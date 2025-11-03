package com.example.todo

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val checkedTasks = mutableSetOf<Long>()

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

        holder.titleText.text = task.title
        holder.note.text = task.note
        holder.colorView.setBackgroundColor(task.color)

        val date = Date(task.createdAt)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tDate.text = formatter.format(date)

        // Prevent triggering listener while updating checked state
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = checkedTasks.contains(task.id)

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkedTasks.add(task.id)
            } else {
                checkedTasks.remove(task.id)
            }
        }

        // Click on the entire item (not checkbox) triggers edit
        holder.itemView.setOnClickListener {
            onItemClick(task)
        }

        // Small touch animation
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).start()
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    v.performClick()
                    false
                }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun submitList(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        notifyDataSetChanged()
    }

    fun getCheckedTaskIds(): List<Long> = checkedTasks.toList()

    fun clearChecked() {
        checkedTasks.clear()
        notifyDataSetChanged()
    }
}