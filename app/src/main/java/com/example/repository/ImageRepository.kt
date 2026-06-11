package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Base64
import com.example.api.*
import com.example.db.GeneratedImage
import com.example.db.ImageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class ImageRepository(
    private val context: Context,
    private val imageDao: ImageDao
) {
    val allImages: Flow<List<GeneratedImage>> = imageDao.getAllImages()
    val favoriteImages: Flow<List<GeneratedImage>> = imageDao.getFavoriteImages()

    fun searchImages(query: String): Flow<List<GeneratedImage>> {
        return imageDao.searchImages("%$query%")
    }

    suspend fun insert(image: GeneratedImage): Long = withContext(Dispatchers.IO) {
        imageDao.insertImage(image)
    }

    suspend fun update(image: GeneratedImage) = withContext(Dispatchers.IO) {
        imageDao.updateImage(image)
    }

    suspend fun delete(image: GeneratedImage) = withContext(Dispatchers.IO) {
        imageDao.deleteImage(image)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        imageDao.deleteImageById(id)
    }

    /**
     * Enhances a prompt using gemini-3.5-flash.
     */
    suspend fun enhancePrompt(prompt: String, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Self-contained fallback enhancement
            return@withContext "A cinematic, ultra-detailed digital masterpiece of $prompt, futuristic concept art, intricate neon details, global illumination, dramatic synthwave lighting, 8k resolution, trending on ArtStation."
        }

        try {
            val systemInstruction = "You are a creative prompt engineering expert. Your job is to transform standard short user prompts into high-quality, descriptive, artistic image generation prompts. Add details about style, lighting, atmosphere, and composition to make it visually stunning. Return ONLY the enhanced prompt. Do NOT write any introduction, headers, quotes, or conversational text. Speak directly in the prompt language."
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = "Please enhance this image generation prompt: $prompt")))
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                generationConfig = GenerationConfig(
                    temperature = 0.8f,
                    topP = 0.95f
                )
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            val enhancedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!enhancedText.isNullOrBlank()) {
                enhancedText.trim()
            } else {
                "A vivid, premium digital artwork depicting $prompt, cinematic lighting, dramatic shadows, highly detailed textures, vibrant color grading."
            }
        } catch (e: Exception) {
            "A cosmic futuristic rendering of $prompt, 8k resolution, vaporwave color scheme, glowing cybernetic highlights, dramatic studio volumetric lighting."
        }
    }

    /**
     * Generates an image using gemini-3.1-flash-image-preview.
     * Falls back to a beautiful procedural art generator if the API key is invalid or if there's an issue.
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: String,
        quality: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Procedural aesthetic art generation fallback
            return@withContext generateProceduralAesthetics(prompt, aspectRatio, quality)
        }

        try {
            val imageSize = when (quality) {
                "Standard" -> "512px"
                "HD" -> "1K"
                "Ultra HD" -> "2K"
                else -> "1K"
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig(
                        aspectRatio = aspectRatio,
                        imageSize = imageSize
                    ),
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )

            // High-quality image generation model
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.1-flash-image-preview",
                apiKey = apiKey,
                request = request
            )

            // Look for inlineData inside the response candidate parts
            val base64Data = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
            if (!base64Data.isNullOrEmpty()) {
                return@withContext base64Data
            }

            // If candidates are empty, try falling back to the wider general image model
            val secondaryResponse = RetrofitClient.service.generateContent(
                model = "gemini-2.5-flash-image",
                apiKey = apiKey,
                request = request
            )
            val base64Secondary = secondaryResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
            if (!base64Secondary.isNullOrEmpty()) {
                return@withContext base64Secondary
            }

            // Fallback to beautiful procedural art if no image data was found
            return@withContext generateProceduralAesthetics(prompt, aspectRatio, quality)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to gorgeous procedural art on error
            return@withContext generateProceduralAesthetics(prompt, aspectRatio, quality)
        }
    }

    /**
     * Generates a high-quality stylized futuristic abstract artwork dynamically based on the prompt's hash.
     * Overlays digital-art details, UI framing, neon stars/nodes, and beautiful gradients.
     */
    private fun generateProceduralAesthetics(prompt: String, aspectRatio: String, quality: String): String {
        val width: Int
        val height: Int
        when (aspectRatio) {
            "16:9" -> { width = 1024; height = 576 }
            "9:16" -> { width = 576; height = 1024 }
            "3:2" -> { width = 960; height = 640 }
            else -> { width = 768; height = 768 } // 1:1
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Generate color scheme from prompt hash
        val hash = prompt.hashCode()
        val randomSeed = abs(hash)

        // Cosmic tech purple/deep blue theme
        val baseHue = (randomSeed % 60) + 260f // Purples/Indigo
        val hsvStart = floatArrayOf(baseHue, 0.9f, 0.15f)
        val hsvEnd = floatArrayOf((baseHue + 40f) % 360f, 0.95f, 0.05f)
        val hsvAccent = floatArrayOf((baseHue - 60f + 360f) % 360f, 0.85f, 0.85f)

        val colorStart = Color.HSVToColor(hsvStart)
        val colorEnd = Color.HSVToColor(hsvEnd)
        val colorAccent = Color.HSVToColor(hsvAccent)

        // Modern background with gradient
        val gradient = RadialGradient(
            width / 2f, height / 2f,
            width * 0.8f,
            colorStart, colorEnd,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // DRAW DESIGN ELEMENTS BASED ON PROMPT

        // 1. Cyber Stars & Nebula dust
        paint.color = Color.WHITE
        for (i in 0 until 40 + (randomSeed % 30)) {
            val starX = ((randomSeed * 37 + i * 197) % width).toFloat()
            val starY = ((randomSeed * 47 + i * 293) % height).toFloat()
            val alpha = 50 + ((randomSeed + i) % 200)
            paint.alpha = alpha
            val size = 1f + ((randomSeed + i) % 4)
            canvas.drawCircle(starX, starY, size, paint)
        }

        // 2. Futuristic geometric wireframes/nodes (e.g. Constellations)
        val numNodes = 6 + (randomSeed % 8)
        val nodesX = FloatArray(numNodes)
        val nodesY = FloatArray(numNodes)
        for (i in 0 until numNodes) {
            nodesX[i] = ((randomSeed * 13 + i * 383) % (width - 100) + 50).toFloat()
            nodesY[i] = ((randomSeed * 31 + i * 491) % (height - 100) + 50).toFloat()
        }

        // Draw connections
        paint.strokeWidth = 2f
        for (i in 0 until numNodes) {
            for (j in i + 1 until numNodes) {
                val dx = nodesX[i] - nodesX[j]
                val dy = nodesY[i] - nodesY[j]
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble())
                if (dist < width * 0.4) {
                    val alpha = (255 * (1f - dist / (width * 0.4))).toInt().coerceIn(0, 70)
                    paint.color = colorAccent
                    paint.alpha = alpha
                    canvas.drawLine(nodesX[i], nodesY[i], nodesX[j], nodesY[j], paint)
                }
            }
        }

        // Draw node endpoints as glowing cyber dots
        paint.style = Paint.Style.FILL
        for (i in 0 until numNodes) {
            paint.color = colorAccent
            paint.alpha = 150
            canvas.drawCircle(nodesX[i], nodesY[i], 8f, paint)
            paint.color = Color.WHITE
            paint.alpha = 230
            canvas.drawCircle(nodesX[i], nodesY[i], 3f, paint)
        }

        // 3. Central glowing complex fractal planetary or portal ring
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        paint.style = Paint.Style.STROKE
        val ringCount = 3 + (randomSeed % 4)
        for (r in 0 until ringCount) {
            val radius = (width * 0.15f) + r * 35f
            paint.color = colorAccent
            paint.alpha = (100 - r * 15).coerceAtLeast(10)
            paint.strokeWidth = 3f + r * 2f

            // Add futuristic gaps/dashes in the rings
            val rotationAngle = (randomSeed * (r + 1) * 15 % 360).toFloat()
            canvas.rotate(rotationAngle)

            // Draw circular dashes or concentric ellipses
            if (r % 2 == 0) {
                val numSegments = 12
                val sweep = 15f
                for (s in 0 until numSegments) {
                    canvas.drawArc(
                        -radius, -radius, radius, radius,
                        s * (360f / numSegments), sweep,
                        false, paint
                    )
                }
            } else {
                canvas.drawCircle(0f, 0f, radius, paint)
            }
        }

        // 4. Abstract math waves (sine waves) running through the composition
        paint.strokeWidth = 4f
        paint.color = colorAccent
        paint.alpha = 120
        val waveAmplitude = 30f + (randomSeed % 40)
        val waveFrequency = 0.005f + (randomSeed % 10) * 0.001f
        var lastX = -width / 2f
        var lastY = waveAmplitude * sin(lastX * waveFrequency)
        for (x in (-width / 2).toInt()..(width / 2).toInt() step 5) {
            val nextX = x.toFloat()
            // rotate coordinates slightly
            val angleRad = Math.toRadians((randomSeed % 45).toDouble()).toFloat()
            val computedY = waveAmplitude * cos(nextX * waveFrequency + randomSeed % 10)
            canvas.drawLine(lastX, lastY, nextX, computedY, paint)
            lastX = nextX
            lastY = computedY
        }
        canvas.restore()

        // 5. Minimal futuristic UI Framing & Overlay text
        paint.style = Paint.Style.STROKE
        paint.color = colorAccent
        paint.alpha = 60
        paint.strokeWidth = 2f
        // Inner tech bounding box
        canvas.drawRect(30f, 30f, width - 30f, height - 30f, paint)

        // Corner crosshairs
        val crossLen = 30f
        // Top-left
        canvas.drawLine(40f, 40f, 40f + crossLen, 40f, paint)
        canvas.drawLine(40f, 40f, 40f, 40f + crossLen, paint)
        // Top-right
        canvas.drawLine(width - 40f, 40f, width - 40f - crossLen, 40f, paint)
        canvas.drawLine(width - 40f, 40f, width - 40f, 40f + crossLen, paint)
        // Bottom-left
        canvas.drawLine(40f, height - 40f, 40f + crossLen, height - 40f, paint)
        canvas.drawLine(40f, height - 40f, 40f, height - 40f - crossLen, paint)
        // Bottom-right
        canvas.drawLine(width - 40f, height - 40f, width - 40f - crossLen, height - 40f, paint)
        canvas.drawLine(width - 40f, height - 40f, width - 40f, height - 40f - crossLen, paint)

        // Tech specs watermark
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = 140
        paint.textSize = 20f
        paint.isAntiAlias = true
        canvas.drawText("PSP IMAGE v2.0 // OFFLINE ENGINE", 60f, 75f, paint)
        canvas.drawText("RATIO: $aspectRatio // QUAL: $quality", 60f, 105f, paint)

        // Draw prompt preview at bottom carefully clipped/elided
        paint.color = Color.WHITE
        paint.alpha = 200
        paint.textSize = 24f
        val displayPrompt = if (prompt.length > 55) prompt.substring(0, 52) + "..." else prompt
        canvas.drawText("> \"$displayPrompt\"", 60f, height - 70f, paint)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
