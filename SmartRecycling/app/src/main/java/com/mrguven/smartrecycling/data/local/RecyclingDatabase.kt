package com.mrguven.smartrecycling.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecyclingProcess::class, RecycledPackaging::class], version = 1)
abstract class RecyclingDatabase : RoomDatabase() {
    abstract fun recyclingProcessDao(): RecyclingProcessDao
    abstract fun recycledPackagingDao(): RecycledPackagingDao
}
