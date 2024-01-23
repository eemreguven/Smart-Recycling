package com.mrguven.smartrecycling.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.local.RecyclingProcess
import com.mrguven.smartrecycling.data.local.RecyclingProcessWithPackaging
import com.mrguven.smartrecycling.data.repository.RecyclingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class RecyclingHistoryViewModel @Inject constructor(private val recyclingRepository: RecyclingRepository) : ViewModel(){

    private val _recyclingProcesses = MutableLiveData<List<RecyclingProcessWithPackaging>>()
    val recyclingProcesses: LiveData<List<RecyclingProcessWithPackaging>> get() = _recyclingProcesses

    private val _recycledPackagingList = MutableLiveData<List<RecycledPackaging>>()
    val recycledPackagingList : LiveData<List<RecycledPackaging>> get() = _recycledPackagingList

    private val _toggleState = MutableLiveData<Boolean>()
    val toggleState: LiveData<Boolean>
        get() = _toggleState

    init {
        _toggleState.value = false
    }

    fun setToggleState(newState: Boolean) {
        _toggleState.value = newState
    }

    fun loadRecyclingProcesses() {
        viewModelScope.launch {
            _recyclingProcesses.value = recyclingRepository.getAllRecyclingProcessesWithPackaging()
        }
    }

    fun loadRecycledPackagingList(){
        viewModelScope.launch {
            _recycledPackagingList.value = recyclingRepository.getRecycledPackagingSummedByTitle()
        }
    }
}
