package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.RoutineCompletionEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    // Tasks
    @Query("SELECT * FROM tasks ORDER BY dateKey ASC, timeMinutes ASC, id DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    // Routine Blocks
    @Query("SELECT * FROM routine_blocks ORDER BY startMinutes ASC, sortOrder ASC")
    fun observeAllRoutines(): Flow<List<RoutineBlockEntity>>

    @Query("SELECT * FROM routine_blocks ORDER BY startMinutes ASC")
    suspend fun getAllRoutinesOnce(): List<RoutineBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineBlockEntity>)

    @Update
    suspend fun updateRoutine(routine: RoutineBlockEntity)

    @Query("DELETE FROM routine_blocks WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: Long)

    @Query("DELETE FROM routine_blocks")
    suspend fun clearAllRoutines()

    // Routine Completions
    @Query("SELECT * FROM routine_completions")
    fun observeAllRoutineCompletions(): Flow<List<RoutineCompletionEntity>>

    @Query("SELECT * FROM routine_completions")
    suspend fun getAllRoutineCompletionsOnce(): List<RoutineCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineCompletion(completion: RoutineCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineCompletions(completions: List<RoutineCompletionEntity>)

    @Query("DELETE FROM routine_completions WHERE routineId = :routineId AND dateKey = :dateKey")
    suspend fun deleteRoutineCompletion(routineId: Long, dateKey: String)

    @Query("DELETE FROM routine_completions")
    suspend fun clearAllRoutineCompletions()

    // Habits
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY id ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()

    // Habit Logs
    @Query("SELECT * FROM habit_logs")
    fun observeAllHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllHabitLogsOnce(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateKey = :dateKey")
    suspend fun getHabitLog(habitId: Long, dateKey: String): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLog(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteHabitLogsForHabit(habitId: Long)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllHabitLogs()

    @Transaction
    suspend fun restoreSnapshot(
        tasks: List<TaskEntity>,
        routines: List<RoutineBlockEntity>,
        routineCompletions: List<RoutineCompletionEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>
    ) {
        clearAllTasks()
        clearAllRoutines()
        clearAllRoutineCompletions()
        clearAllHabits()
        clearAllHabitLogs()

        insertTasks(tasks)
        insertRoutines(routines)
        insertRoutineCompletions(routineCompletions)
        insertHabits(habits)
        insertHabitLogs(habitLogs)
    }
}
