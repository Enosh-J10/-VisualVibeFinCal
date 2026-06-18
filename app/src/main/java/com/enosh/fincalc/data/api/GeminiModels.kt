package com.enosh.fincalc.data.api

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    @SerializedName("contents") val contents: List<Content>,
    @SerializedName("generation_config") val generationConfig: GenerationConfig? = null,
    @SerializedName("system_instruction") val systemInstruction: Content? = null
)

data class Content(
    @SerializedName("role") val role: String? = null,
    @SerializedName("parts") val parts: List<Part>
)

data class Part(
    @SerializedName("text") val text: String? = null,
    @SerializedName("inline_data") val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("data") val data: String
)

data class GenerationConfig(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("topK") val topK: Int? = null,
    @SerializedName("topP") val topP: Double? = null,
    @SerializedName("max_output_tokens") val maxOutputTokens: Int? = null,
    @SerializedName("stop_sequences") val stopSequences: List<String>? = null
)

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>,
    @SerializedName("prompt_feedback") val promptFeedback: PromptFeedback? = null
)

data class Candidate(
    @SerializedName("content") val content: Content,
    @SerializedName("finish_reason") val finishReason: String?,
    @SerializedName("index") val index: Int?,
    @SerializedName("safety_ratings") val safetyRatings: List<SafetyRating>?
)

data class SafetyRating(
    @SerializedName("category") val category: String,
    @SerializedName("probability") val probability: String
)

data class PromptFeedback(
    @SerializedName("safety_ratings") val safetyRatings: List<SafetyRating>?
)
