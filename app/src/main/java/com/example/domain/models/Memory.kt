package com.example.domain.models

data class Memory(
    val id: Long = 0,
    val factText: String,
    val source: String = "auto",
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "Personal",
    val isImportant: Boolean = false
)
