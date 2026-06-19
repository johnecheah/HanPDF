package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- GEMINI API SCHEMAS ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64 encoding
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

// --- RETROFIT INTERFACE ---

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent") // Recommended default model for quick multimodal task
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- SERVICE ENGINE ---

object GeminiService {
    private const val TAG = "GeminiService"
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

    private val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    /**
     * Executes raw High-Precision OCR on a custom Bitmap.
     */
    suspend fun performOcr(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is holding default value or empty.")
            return@withContext "API Configuration Error: Please configure your GEMINI_API_KEY inside the Secrets panel. (Offline scan simulation is active below)."
        }

        try {
            // Convert bitmap to Base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "You are a professional OCR engine. Perform high-precision text recognition on this scanned text, identity card, or document page. Recognize ALL text contents verbatim. Retain original structure, rows, tables, and document labels (e.g. ID numbers, Names, dates, titles). Do not summarize, output ONLY the extracted document text. If there is no legible text, report 'No legible text was found in the scan.'"),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                )
            )

            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "OCR Error: Verification failed. Please make sure the scanning angle has clear lighting."
        } catch (e: Exception) {
            Log.e(TAG, "OCR Api failed", e)
            "OCR Analysis failed: ${e.localizedMessage}. Please secure network and credentials."
        }
    }

    /**
     * Executes visual layout and document analysis on edit pages.
     */
    suspend fun analyzeDocumentStructure(prompt: String, bitmap: Bitmap?): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Local Intelligence: Key not setup. Please enter your API key in the secrets panel."
        }
        try {
            val parts = mutableListOf<GeminiPart>()
            parts.add(GeminiPart(text = "Review this document layout/contents. $prompt. Suggest optimal edits, corrections, or summarize key elements elegantly."))
            
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image)))
            }

            val request = GeminiRequest(contents = listOf(GeminiContent(parts = parts)))
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No suggestions."
        } catch (e: Exception) {
            "Analysis error: ${e.localizedMessage}"
        }
    }
}
