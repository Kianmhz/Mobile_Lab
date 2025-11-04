package com.example.todo

data class Task(
    val id: Long,
    val title: String,
    val note: String,
    val color: Int,
    val createdAt: Long,
    val done: Boolean,
    val checkbox: Boolean = false,
    val imagePath: String?,
)
