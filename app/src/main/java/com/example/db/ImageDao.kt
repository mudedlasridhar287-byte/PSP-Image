package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM generated_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<GeneratedImage>>

    @Query("SELECT * FROM generated_images WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteImages(): Flow<List<GeneratedImage>>

    @Query("SELECT * FROM generated_images WHERE prompt LIKE :searchQuery OR enhancedPrompt LIKE :searchQuery ORDER BY timestamp DESC")
    fun searchImages(searchQuery: String): Flow<List<GeneratedImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImage): Long

    @Update
    suspend fun updateImage(image: GeneratedImage)

    @Delete
    suspend fun deleteImage(image: GeneratedImage)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteImageById(id: Int)

    @Query("SELECT * FROM generated_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: Int): GeneratedImage?
}
