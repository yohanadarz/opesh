package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "habit_database"
    ).build()

    private val repository = HabitRepository(database.habitDao())
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow(prefs.getString("currency", "$") ?: "$")
    val currency: StateFlow<String> = _currency.asStateFlow()

    val uiState: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setCurrency(newCurrency: String) {
        prefs.edit().putString("currency", newCurrency).apply()
        _currency.value = newCurrency
    }

    fun addHabit(name: String, costAmount: Double, costFrequency: String, quitTimestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insert(Habit(name = name, costAmount = costAmount, costFrequency = costFrequency, quitTimestamp = quitTimestamp))
        }
    }

    fun deleteHabit(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}
