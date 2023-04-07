package com.example.gnahomework

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: GameDataRepository
) : ViewModel() {
    fun getGames(): LiveData<List<GameData>> {
        return repository.getGames()
    }
}