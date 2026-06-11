package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.db.AppDatabase
import com.example.db.GeneratedImage
import com.example.repository.ImageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed interface GenerationState {
    object Idle : GenerationState
    object Loading : GenerationState
    data class Success(val images: List<GeneratedImage>) : GenerationState
    data class Error(val message: String) : GenerationState
}

data class UserProfile(
    val username: String,
    val email: String,
    val avatarSeed: String = "avatar_alpha",
    val isPremium: Boolean = false
)

class PSPViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ImageRepository(application, db.imageDao())

    // Secret Management Keys
    val geminiApiKey = MutableStateFlow(BuildConfig.GEMINI_API_KEY)

    // Design & Theming States
    val isDarkMode = MutableStateFlow(true)
    val selectedAspectRatio = MutableStateFlow("1:1") // "1:1", "16:9", "9:16", "3:2"
    val selectedQuality = MutableStateFlow("HD") // "Standard", "HD", "Ultra HD"
    val promptInput = MutableStateFlow("")
    val generationCount = MutableStateFlow(1) // Premium supports up to 4

    // Auth Simulation
    private val _userProfile = MutableStateFlow<UserProfile?>(
        UserProfile(
            username = "CosmicCreator",
            email = "creator@psp-image.ai",
            avatarSeed = "alpha_pro",
            isPremium = true // Default premium for gorgeous initial experience
        )
    )
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Database Reactive Flows
    val allImages: StateFlow<List<GeneratedImage>> = repository.allImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteImages: StateFlow<List<GeneratedImage>> = repository.favoriteImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedImages: StateFlow<List<GeneratedImage>> = _searchQuery
        .combine(allImages) { query, images ->
            if (query.isBlank()) {
                images
            } else {
                images.filter {
                    it.prompt.contains(query, ignoreCase = true) ||
                            (it.enhancedPrompt?.contains(query, ignoreCase = true) == true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Generation State
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // Prompt Enhancer State
    val isEnhancing = MutableStateFlow(false)

    // Current Generation History local session accumulator
    private val _currentSessionImages = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val currentSessionImages: StateFlow<List<GeneratedImage>> = _currentSessionImages.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAspectRatio(ratio: String) {
        selectedAspectRatio.value = ratio
    }

    fun setQuality(qual: String) {
        selectedQuality.value = qual
    }

    fun setGenerationCount(count: Int) {
        val user = _userProfile.value
        if (count > 1 && user?.isPremium == false) {
            // Cap at 1 for non-premium
            generationCount.value = 1
        } else {
            generationCount.value = count.coerceIn(1, 4)
        }
    }

    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    fun togglePremium() {
        _userProfile.update { current ->
            current?.copy(isPremium = !current.isPremium)
        }
    }

    fun login(username: String, email: String) {
        _userProfile.value = UserProfile(
            username = username.ifBlank { "User" },
            email = email.ifBlank { "user@example.com" },
            isPremium = true
        )
    }

    fun logout() {
        _userProfile.value = null
    }

    /**
     * Enhances prompt reactively
     */
    fun enhanceCurrentPrompt() {
        val currentPrompt = promptInput.value
        if (currentPrompt.isBlank()) return

        viewModelScope.launch {
            isEnhancing.value = true
            val enhanced = repository.enhancePrompt(currentPrompt, geminiApiKey.value)
            promptInput.value = enhanced
            isEnhancing.value = false
        }
    }

    /**
     * Triggers parallel or single image generation
     */
    fun generateImages() {
        val prompt = promptInput.value
        if (prompt.isBlank()) return

        val count = generationCount.value
        val ratio = selectedAspectRatio.value
        val qual = selectedQuality.value
        val apiKey = geminiApiKey.value

        viewModelScope.launch {
            _generationState.value = GenerationState.Loading
            _currentSessionImages.value = emptyList()

            try {
                // Execute generation requests in parallel
                val deferredList = (1..count).map {
                    async {
                        val base64 = repository.generateImage(prompt, ratio, qual, apiKey)
                        GeneratedImage(
                            prompt = prompt,
                            enhancedPrompt = if (prompt.length > 50) prompt else null,
                            imageBytes = base64,
                            aspectRatio = ratio,
                            quality = qual
                        )
                    }
                }

                val results = deferredList.awaitAll()
                val savedImages = mutableListOf<GeneratedImage>()

                results.forEach { image ->
                    val id = repository.insert(image)
                    savedImages.add(image.copy(id = id.toInt()))
                }

                _currentSessionImages.value = savedImages
                _generationState.value = GenerationState.Success(savedImages)
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "An unknown generation error occurred")
            }
        }
    }

    fun setGenerationStateToIdle() {
        _generationState.value = GenerationState.Idle
    }

    fun toggleFavorite(image: GeneratedImage) {
        viewModelScope.launch {
            repository.update(image.copy(isFavorite = !image.isFavorite))
        }
    }

    fun deleteImage(image: GeneratedImage) {
        viewModelScope.launch {
            repository.delete(image)
            // If the image was in the current session list, update it
            _currentSessionImages.update { current ->
                current.filter { it.id != image.id }
            }
        }
    }

    fun generateRandomPrompt(): String {
        val subjects = listOf("A mysterious cyber-ninja", "A giant celestial jellyfish", "An overgrown lost temple of glass", "A steampunk airship navigating a neon nebula", "A glowing robotic cat", "A futuristic solarpunk library", "An astronaut playing piano on Mars", "A majestic crystal dragon")
        val environments = listOf("in deep space surrounded by auroras", "drifting inside a cybernetic simulation", "overlooking a Tokyo cyberpunk skyline at night", "amidst emerald forest luminescence", "submerged in a fluorescent bioluminescent ocean", "surrounded by gravity-defying floating waterfalls")
        val styles = listOf("midjourney aesthetic, cinematic volumentric mood", "holographic vector render, retrofuturistic synthwave", "8k octane render, dramatic unreal engine shadows", "detailed surreal liquid-glass fantasy art", "ultra-realistic studio cyberpunk portrait")
        
        return "${subjects.random()} ${environments.random()}, ${styles.random()}."
    }

    fun loadRandomPromptText() {
        promptInput.value = generateRandomPrompt()
    }

    /**
     * Native share dialog for images using temp File Provider
     */
    fun shareImageNative(image: GeneratedImage) {
        val context = getApplication<Application>()
        try {
            val imageBytes = Base64.decode(image.imageBytes, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return

            // Save to internal cache dir publicly shared via FileProvider
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "psp_generate_${image.id}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Generated via PSP Image: \"${image.prompt}\"")
                }
                val chooser = Intent.createChooser(shareIntent, "Share generated art").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
