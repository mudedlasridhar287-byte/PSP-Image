package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val enhancedPrompt: String? = null,
    val imageBytes: String, // Base64 representation of the generated image
    val aspectRatio: String, // "1:1", "16:9", "9:16", "3:2"
    val quality: String, // "Standard", "HD", "Ultra HD"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isShared: Boolean = false
)
