package com.example.talktile_05.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Query("SELECT * FROM bookmarks WHERE book = :book AND chapter = :chapter ORDER BY createdAt DESC")
    suspend fun listForChapter(book: String, chapter: String): List<Bookmark>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}
