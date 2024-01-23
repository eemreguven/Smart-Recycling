package com.mrguven.smartrecycling.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RecyclingProcessDao {
    @Insert
    suspend fun insertRecyclingProcess(recyclingProcess: RecyclingProcess) : Long

    @Transaction
    @Query("SELECT * FROM recycling_process_table")
    suspend fun getAllRecyclingProcessesWithPackaging(): List<RecyclingProcessWithPackaging>
}
