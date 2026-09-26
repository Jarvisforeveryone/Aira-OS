package com.example.domain.models

data class Reminder(
    val id: Long = 0,
    val title: String,
    val timestamp: Long,
    val isCompleted: Boolean = false,
    val category: String = "General"
)
