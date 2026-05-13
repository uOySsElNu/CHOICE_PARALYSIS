package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.OptionList
import com.choiceparalysis.turntable.data.repository.OptionListRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OptionListRepository(application)

    val lists: StateFlow<List<OptionList>> = repository.optionLists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createList(name: String, options: List<String>) {
        viewModelScope.launch {
            repository.saveList(
                OptionList(
                    name = name,
                    options = options
                )
            )
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            repository.deleteList(id)
        }
    }
}
