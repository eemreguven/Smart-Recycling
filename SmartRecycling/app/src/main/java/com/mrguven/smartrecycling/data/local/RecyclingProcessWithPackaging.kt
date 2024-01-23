package com.mrguven.smartrecycling.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class RecyclingProcessWithPackaging(
    @Embedded val recyclingProcess: RecyclingProcess,
    @Relation(
        parentColumn = "id",
        entityColumn = "recyclingProcessId"
    )
    val recycledPackagingList: List<RecycledPackaging>
)
