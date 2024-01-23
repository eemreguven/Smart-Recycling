package com.mrguven.smartrecycling.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(
    tableName = "recycled_packaging_table",
    foreignKeys = [ForeignKey(
        entity = RecyclingProcess::class,
        parentColumns = ["id"],
        childColumns = ["recyclingProcessId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RecycledPackaging(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String,
    val count: Int,
    var recyclingProcessId: Long
)
