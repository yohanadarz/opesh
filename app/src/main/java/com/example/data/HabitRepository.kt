package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun insert(habit: Habit) = habitDao.insertHabit(habit)

    suspend fun deleteById(id: Int) = habitDao.deleteHabitById(id)
}
