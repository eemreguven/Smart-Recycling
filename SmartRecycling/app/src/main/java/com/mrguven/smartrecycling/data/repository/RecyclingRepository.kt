package com.mrguven.smartrecycling.data.repository

import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.local.RecycledPackagingDao
import com.mrguven.smartrecycling.data.local.RecyclingProcess
import com.mrguven.smartrecycling.data.local.RecyclingProcessDao
import com.mrguven.smartrecycling.data.local.RecyclingProcessWithPackaging
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RecyclingRepository @Inject constructor(
    private val recyclingProcessDao: RecyclingProcessDao,
    private val recycledPackagingDao: RecycledPackagingDao
) {

    suspend fun insertRecyclingProcessWithPackaging(
        recyclingProcess: RecyclingProcess,
        recycledPackagingList: List<RecycledPackaging>
    ) {
        val processId = recyclingProcessDao.insertRecyclingProcess(recyclingProcess)
        recycledPackagingList.forEach {
            it.recyclingProcessId = processId
            recycledPackagingDao.insertRecycledPackaging(it)
        }
    }

    suspend fun insertRecyclingProcess(recyclingProcess: RecyclingProcess) {
        recyclingProcessDao.insertRecyclingProcess(recyclingProcess)
    }

    suspend fun getAllRecyclingProcessesWithPackaging(): List<RecyclingProcessWithPackaging> {
        return recyclingProcessDao.getAllRecyclingProcessesWithPackaging()
    }

    suspend fun insertRecycledItem(recycledPackaging: RecycledPackaging) {
        recycledPackagingDao.insertRecycledPackaging(recycledPackaging)
    }

    suspend fun getRecycledItemsByProcessId(recyclingProcessId: Long): List<RecycledPackaging> {
        return recycledPackagingDao.getRecycledPackagingByProcessId(recyclingProcessId)
    }

    suspend fun deleteRecycledItemsByProcessId(recyclingProcessId: Long) {
        recycledPackagingDao.deleteRecycledPackagingByProcessId(recyclingProcessId)
    }

    suspend fun getRecycledPackagingSummedByTitle(): List<RecycledPackaging> {
        val  allProcesses = recyclingProcessDao.getAllRecyclingProcessesWithPackaging()

        val allPackaging = allProcesses.flatMap { it.recycledPackagingList}

        val groupedByTitle = allPackaging.groupBy { it.title }

        return groupedByTitle.map { (title, packagingList) ->
            RecycledPackaging(
                title = title,
                type = packagingList.firstOrNull()?.type ?: "",
                count = packagingList.sumBy { it.count },
                recyclingProcessId = packagingList.firstOrNull()?.recyclingProcessId ?: 0
            )
        }
    }
}
