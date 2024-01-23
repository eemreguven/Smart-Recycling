package com.mrguven.smartrecycling.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.local.RecyclingProcess
import com.mrguven.smartrecycling.data.repository.RecyclingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecyclingViewModel @Inject constructor(private val recyclingRepository: RecyclingRepository) : ViewModel(){
    private val _creationStatus = MutableLiveData<Boolean>()
    val creationStatus: LiveData<Boolean>
        get() = _creationStatus

    fun createRecyclingProcessWithPackaging(
        recyclingProcess: RecyclingProcess,
        recycledPackagingList: List<RecycledPackaging>
    ) {
        viewModelScope.launch {
            try {
                recyclingRepository.insertRecyclingProcessWithPackaging(recyclingProcess, recycledPackagingList)

                _creationStatus.value = true
            } catch (e: Exception) {
                _creationStatus.value = false
                Log.e("RecyclingViewModel", "Error creating Recycling Process with Packaging: ${e.message}")
            }
        }
    }
}