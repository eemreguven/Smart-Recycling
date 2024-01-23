package com.mrguven.smartrecycling.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycling_process_table")
data class RecyclingProcess(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo val date: Long,
    @ColumnInfo val containerName: String,
)
