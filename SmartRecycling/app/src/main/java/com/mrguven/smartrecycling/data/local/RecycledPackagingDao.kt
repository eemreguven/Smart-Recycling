package com.mrguven.smartrecycling.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecycledPackagingDao {
    @Insert
    suspend fun insertRecycledPackaging(recycledPackaging: RecycledPackaging)

    @Query("SELECT * FROM recycled_packaging_table WHERE recyclingProcessId = :recyclingProcessId")
    suspend fun getRecycledPackagingByProcessId(recyclingProcessId: Long): List<RecycledPackaging>

    @Query("DELETE FROM recycled_packaging_table WHERE recyclingProcessId = :recyclingProcessId")
    suspend fun deleteRecycledPackagingByProcessId(recyclingProcessId: Long)
}
