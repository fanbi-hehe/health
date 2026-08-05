package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.health.data.local.entity.FoodLibrary
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodLibrary): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodLibrary>)

    @Update
    suspend fun update(food: FoodLibrary)

    @Delete
    suspend fun delete(food: FoodLibrary)

    @Query("SELECT * FROM food_library ORDER BY isCustom DESC, name ASC")
    fun getAllFoods(): Flow<List<FoodLibrary>>

    @Query("SELECT * FROM food_library WHERE name LIKE '%' || :query || '%' ORDER BY isCustom DESC, name ASC")
    fun searchFoods(query: String): Flow<List<FoodLibrary>>

    @Query("SELECT * FROM food_library WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomFoods(): Flow<List<FoodLibrary>>

    @Query("DELETE FROM food_library WHERE isCustom = 1")
    suspend fun deleteAllCustom()
}
