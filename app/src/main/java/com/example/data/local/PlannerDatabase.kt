package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.RoutineCompletionEntity
import com.example.data.model.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        RoutineBlockEntity::class,
        RoutineCompletionEntity::class,
        HabitEntity::class,
        HabitLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(PlannerConverters::class)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        fun getInstance(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "my_daily_planner.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
