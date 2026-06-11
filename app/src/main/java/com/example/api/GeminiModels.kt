package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseFormat") val responseFormat: ResponseFormat? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "type") val type: String? = null,
    @Json(name = "text") val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    @Json(name = "mimeType") val mimeType: String
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String, // "1:1", "16:9", "9:16", "3:2"
    @Json(name = "imageSize") val imageSize: String // "1K", "2K", "4K"
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)
