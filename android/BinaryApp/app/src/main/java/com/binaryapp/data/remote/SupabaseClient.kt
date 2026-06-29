package com.binaryapp.data.remote

import android.content.Context
import com.google.gson.Gson
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Properties

object SupabaseClient {
    private val client = OkHttpClient()
    val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private var supabaseUrl: String = ""
    private var supabaseKey: String = ""

    fun initialize(context: Context) {
        try {
            val properties = Properties()
            val assetManager = context.assets
            val inputStream = assetManager.open("supabase.properties")
            properties.load(inputStream)
            supabaseUrl = properties.getProperty("supabase_url", "").trim().removeSuffix("/")
            supabaseKey = properties.getProperty("supabase_key", "").trim()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getHeaders(): Headers {
        return Headers.Builder()
            .add("apikey", supabaseKey)
            .add("Authorization", "Bearer $supabaseKey")
            .add("Content-Type", "application/json")
            .build()
    }

    fun buildUrl(path: String, queryParams: Map<String, String> = emptyMap()): HttpUrl {
        val baseUrl = supabaseUrl.toHttpUrlOrNull() ?: throw IllegalStateException("Invalid Supabase URL: '$supabaseUrl'")
        val builder = baseUrl.newBuilder()
            .addPathSegment("rest")
            .addPathSegment("v1")
            .addPathSegment(path)
        
        for ((key, value) in queryParams) {
            builder.addQueryParameter(key, value)
        }
        return builder.build()
    }

    suspend fun get(path: String, queryParams: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = buildUrl(path, queryParams)
        val request = Request.Builder()
            .url(url)
            .headers(getHeaders())
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response: $bodyStr")
            }
            bodyStr
        }
    }

    suspend fun post(path: String, jsonBody: String, preferRepresentation: Boolean = false, upsert: Boolean = false): String = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val headersBuilder = getHeaders().newBuilder()
        
        var preferHeader = ""
        if (preferRepresentation) preferHeader += "return=representation"
        if (upsert) {
            if (preferHeader.isNotEmpty()) preferHeader += ","
            preferHeader += "resolution=merge-duplicates"
        }
        if (preferHeader.isNotEmpty()) {
            headersBuilder.add("Prefer", preferHeader)
        }
        val request = Request.Builder()
            .url(url)
            .headers(headersBuilder.build())
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response: $bodyStr")
            }
            bodyStr
        }
    }

    suspend fun patch(path: String, jsonBody: String, queryParams: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = buildUrl(path, queryParams)
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .headers(getHeaders())
            .patch(body)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response: $bodyStr")
            }
            bodyStr
        }
    }

    suspend fun delete(path: String, queryParams: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = buildUrl(path, queryParams)
        val request = Request.Builder()
            .url(url)
            .headers(getHeaders())
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response: $bodyStr")
            }
            bodyStr
        }
    }
}
