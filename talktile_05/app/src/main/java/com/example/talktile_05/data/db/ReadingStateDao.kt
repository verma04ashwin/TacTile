package com.example.talktile_05.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReadingStateDao {

    @Query("SELECT * FROM reading_state WHERE id = 1")
    suspend fun getState(): ReadingState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReadingState)
}
