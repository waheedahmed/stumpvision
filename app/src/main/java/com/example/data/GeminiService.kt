package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    val temperature: Float? = 0.4f,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun getAnalysis(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiService = retrofit.create(GeminiService::class.java)

    /**
     * Call the Gemini API to get coaching feedback/insights.
         */
    suspend fun fetchBowlingCoaching(deliveriesSummary: String, bowlerType: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel."
        }
        
        val systemPrompt = "You are StumpVision Coach, a elite cricket bowling analyst and master coach. Your tone is insightful, supportive, and tactical. You analyze deliveries and suggest concrete adjustments (e.g., wrist position, bowling crease alignment, release point height, speed variations)."
        val prompt = "Analyze this bowling spell for a bowler of type '$bowlerType'. Deliveries data: $deliveriesSummary. Provide brief, elite, bulleted takeaways (under 120 words total): 1. Seam/Spin Consistency, 2. Length Fatigue (is pitch mapping drifting short or full?), 3. Quick Action Item."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiConfig(temperature = 0.3f)
        )

        return try {
            val response = apiService.getAnalysis(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No insights available for this spell yet."
        } catch (e: Exception) {
            "Analysis Error: ${e.localizedMessage ?: "Could not complete request"}"
        }
    }
}
