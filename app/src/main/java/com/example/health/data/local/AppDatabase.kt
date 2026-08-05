package com.example.health.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.health.data.local.converter.Converters
import com.example.health.data.local.dao.AdviceLogDao
import com.example.health.data.local.dao.BodyWeightDao
import com.example.health.data.local.dao.ChatMessageDao
import com.example.health.data.local.dao.DietRecordDao
import com.example.health.data.local.dao.ExerciseLibraryDao
import com.example.health.data.local.dao.FoodLibraryDao
import com.example.health.data.local.dao.MealTemplateDao
import com.example.health.data.local.dao.TrainingRecordDao
import com.example.health.data.local.entity.AdviceLog
import com.example.health.data.local.entity.BodyWeight
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.local.entity.DietRecord
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.local.entity.MealTemplate
import com.example.health.data.local.entity.TrainingRecord

@Database(
    entities = [
        DietRecord::class,
        TrainingRecord::class,
        BodyWeight::class,
        ChatMessage::class,
        AdviceLog::class,
        FoodLibrary::class,
        MealTemplate::class,
        ExerciseLibrary::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dietRecordDao(): DietRecordDao
    abstract fun trainingRecordDao(): TrainingRecordDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun adviceLogDao(): AdviceLogDao
    abstract fun foodLibraryDao(): FoodLibraryDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_assistant.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
