package com.mrguven.smartrecycling.di

import com.mrguven.smartrecycling.data.local.RecycledPackagingDao
import com.mrguven.smartrecycling.data.local.RecyclingProcessDao
import com.mrguven.smartrecycling.data.repository.RecyclingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Singleton
    @Provides
    fun provideMovieRepository(
        recyclingProcessDao: RecyclingProcessDao, recycledPackagingDao: RecycledPackagingDao
    ): RecyclingRepository {
        return RecyclingRepository(recyclingProcessDao, recycledPackagingDao)
    }
}
