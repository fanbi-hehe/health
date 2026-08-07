package com.example.health.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移链（version 1 → 8）。
 *
 * 演进历史：
 * v1 初始 7 表 → v2 新增 exercise_library（基础字段）→ v3 扩展动作库字段
 * → v4 chat_message 加 imagePath → v5 training_record 加 timestamp
 * → v6 新增 activity_record / daily_step_count
 * → v7 food_library 加 proteinPer100g
 * → v8 food_library 加 carbs/fat；diet_record 加宏量字段
 */
object Migrations {

    /** v1 → v2：新增动作库表（v2 仅基础字段）。 */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `exercise_library` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `bodyPart` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)"
            )
        }
    }

    /** v2 → v3：动作库扩展字段（NOT NULL 需要默认值）。 */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `equipment` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `muscleGroup` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `target` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `secondaryMuscles` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `instructions` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `instructionSteps` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `image` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `exercise_library` ADD COLUMN `gifUrl` TEXT NOT NULL DEFAULT ''")
        }
    }

    /** v3 → v4：聊天消息支持图片。 */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `imagePath` TEXT")
        }
    }

    /** v4 → v5：训练记录加时间戳（旧记录为 0，不影响按日期显示）。 */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `training_record` ADD COLUMN `timestamp` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** v5 → v6：新增运动记录表与每日步数表。 */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `activity_record` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`type` TEXT NOT NULL, `startTime` INTEGER NOT NULL, " +
                    "`durationMinutes` INTEGER NOT NULL, " +
                    "`avgHeartRate` INTEGER NOT NULL, `maxHeartRate` INTEGER NOT NULL, " +
                    "`caloriesKcal` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, " +
                    "`avgPace` TEXT, `routeJson` TEXT, `source` TEXT NOT NULL, `note` TEXT)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_step_count` (" +
                    "`date` TEXT NOT NULL, `steps` INTEGER NOT NULL, " +
                    "`caloriesKcal` INTEGER NOT NULL, PRIMARY KEY(`date`))"
            )
        }
    }

    /** v6 → v7：食物库加蛋白质。 */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `food_library` ADD COLUMN `proteinPer100g` REAL NOT NULL DEFAULT 0")
        }
    }

    /** v7 → v8：食物库加碳水/脂肪；饮食记录加宏量字段。 */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `food_library` ADD COLUMN `carbsPer100g` REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `food_library` ADD COLUMN `fatPer100g` REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `diet_record` ADD COLUMN `proteinG` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `diet_record` ADD COLUMN `carbsG` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `diet_record` ADD COLUMN `fatG` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8
    )
}
