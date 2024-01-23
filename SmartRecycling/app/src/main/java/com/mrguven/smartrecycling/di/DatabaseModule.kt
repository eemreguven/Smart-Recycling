package com.mrguven.smartrecycling.di

import android.app.Application
import androidx.room.Room
import com.mrguven.smartrecycling.data.local.RecycledPackagingDao
import com.mrguven.smartrecycling.data.local.RecyclingDatabase
import com.mrguven.smartrecycling.data.local.RecyclingProcessDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideDatabase(application: Application): RecyclingDatabase {
        return Room.databaseBuilder(application, RecyclingDatabase::class.java, "recycling_database")
            .build()
    }

    @Provides
    fun provideRecyclingProcessDao(database: RecyclingDatabase): RecyclingProcessDao {
        return database.recyclingProcessDao()
    }

    @Provides
    fun provideRecycledPackagingDao(database: RecyclingDatabase): RecycledPackagingDao {
        return database.recycledPackagingDao()
    }
}