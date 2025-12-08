package com.example.talktile_05.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_state")
data class ReadingState(
    @PrimaryKey val id: Int = 1,
    val book: String?,
    val chapter: String?,
    val page: Int,
    val paragraphIndex: Int
)
