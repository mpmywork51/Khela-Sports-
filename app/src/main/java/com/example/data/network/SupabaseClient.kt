package com.example.data.network

import android.util.Log
import com.example.data.database.ChannelEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

object SupabaseClient {
    private val client = OkHttpClient()

    /**
     * Fetches channels dynamically from a Supabase table.
     * Expects a standard Supabase REST API URL and Anon API Key.
     */
    suspend fun fetchChannelsFromSupabase(
        supabaseUrl: String,
        supabaseKey: String,
        tableName: String = "channels"
    ): List<ChannelEntity> {
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) {
            throw IllegalArgumentException("Supabase URL and API Key cannot be blank")
        }

        // Standard Supabase REST endpoint: https://[project-ref].supabase.co/rest/v1/[table]?select=*
        val cleanUrl = supabaseUrl.trimEnd('/')
        val url = if (cleanUrl.contains("/rest/v1/")) {
            "$cleanUrl?select=*"
        } else {
            "$cleanUrl/rest/v1/$tableName?select=*"
        }

        Log.d("SupabaseClient", "Syncing from URL: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned unsuccessful code: ${response.code} - ${response.message}")
            }

            val bodyString = response.body?.string() ?: ""
            Log.d("SupabaseClient", "Response Body length: ${bodyString.length}")
            if (bodyString.isBlank()) return emptyList()

            val jsonArray = JSONArray(bodyString)
            val channels = mutableListOf<ChannelEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name", "Unknown Channel")
                
                // Read streaming link (can fall back to various common fields)
                val streamUrl = obj.optString("url", obj.optString("stream_url", ""))
                if (streamUrl.isBlank()) continue

                val backupUrl = obj.optString("backup_url", obj.optString("backupUrl", null))
                val category = obj.optString("category", "General")
                val logoUrl = obj.optString("logo_url", obj.optString("logoUrl", null))
                val headersJson = obj.optString("headers_json", obj.optString("headersJson", null))
                val isLive = obj.optBoolean("is_live", obj.optBoolean("isLive", true))
                val orderIndex = obj.optInt("order_index", obj.optInt("orderIndex", i))

                channels.add(
                    ChannelEntity(
                        name = name,
                        url = streamUrl,
                        backupUrl = backupUrl,
                        category = category,
                        logoUrl = logoUrl,
                        headersJson = headersJson,
                        isLive = isLive,
                        orderIndex = orderIndex
                    )
                )
            }
            return channels
        }
    }
}
