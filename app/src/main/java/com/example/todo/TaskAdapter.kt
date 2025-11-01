package com.example.todo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var tasks: List<Task>,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: View = itemView.findViewById(R.id.taskColor)
        val titleText: TextView = itemView.findViewById(R.id.taskTitle)
        val tDate: TextView = itemView.findViewById(R.id.taskDueDate)
        val note: TextView = itemView.findViewById(R.id.taskDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleText.text = task.title
        holder.note.text = task.note
        holder.colorView.setBackgroundColor(task.color)

        val date = Date(task.createdAt)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tDate.text = formatter.format(date)

        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount() = tasks.size

    fun submitList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}

