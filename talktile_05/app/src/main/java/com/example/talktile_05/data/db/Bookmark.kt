package com.example.talktile_05.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: String,
    val page: Int,
    val paragraphIndex: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
