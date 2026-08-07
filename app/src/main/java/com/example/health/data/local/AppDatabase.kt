package com.example.health.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.health.data.local.dao.AdviceLogDao
import com.example.health.data.local.dao.ActivityRecordDao
import com.example.health.data.local.dao.BodyWeightDao
import com.example.health.data.local.dao.ChatMessageDao
import com.example.health.data.local.dao.DailyStepCountDao
import com.example.health.data.local.dao.DietRecordDao
import com.example.health.data.local.dao.ExerciseLibraryDao
import com.example.health.data.local.dao.FoodLibraryDao
import com.example.health.data.local.dao.MealTemplateDao
import com.example.health.data.local.dao.TrainingRecordDao
import com.example.health.data.local.entity.AdviceLog
import com.example.health.data.local.entity.ActivityRecord
import com.example.health.data.local.entity.BodyWeight
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.local.entity.DailyStepCount
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
        ActivityRecord::class,
        DailyStepCount::class,
        FoodLibrary::class,
        MealTemplate::class,
        ExerciseLibrary::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dietRecordDao(): DietRecordDao
    abstract fun trainingRecordDao(): TrainingRecordDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun adviceLogDao(): AdviceLogDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun dailyStepCountDao(): DailyStepCountDao
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
                    // 真实 Migration：升级保留数据；无匹配 Migration 时抛异常而非清库
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
