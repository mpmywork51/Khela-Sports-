package com.example.data.network

import android.util.Log
import com.example.data.database.ChannelEntity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object FirebaseDbClient {
    private val client = OkHttpClient()

    /**
     * Fetches channels dynamically from Firebase Realtime Database.
     * Supports both JSON Array and JSON Object (Map) structures.
     */
    suspend fun fetchChannelsFromFirebase(firebaseUrl: String): List<ChannelEntity> {
        if (firebaseUrl.isBlank()) {
            throw IllegalArgumentException("Firebase Realtime Database URL cannot be blank")
        }

        // Clean and prepare the Firebase REST URL
        val cleanUrl = firebaseUrl.trim().trimEnd('/')
        val url = if (cleanUrl.endsWith(".json")) {
            cleanUrl
        } else {
            "$cleanUrl/channels.json"
        }

        Log.d("FirebaseDbClient", "Fetching from Firebase: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Firebase returned error: ${response.code} - ${response.message}")
            }

            val bodyString = response.body?.string() ?: ""
            Log.d("FirebaseDbClient", "Firebase Response: $bodyString")

            if (bodyString.isBlank() || bodyString == "null") {
                return emptyList()
            }

            val channels = mutableListOf<ChannelEntity>()

            try {
                if (bodyString.trim().startsWith("[")) {
                    // Parse as JSON Array
                    val jsonArray = JSONArray(bodyString)
                    for (i in 0 until jsonArray.length()) {
                        if (jsonArray.isNull(i)) continue
                        val obj = jsonArray.getJSONObject(i)
                        channels.add(parseChannelObject(obj, i))
                    }
                } else if (bodyString.trim().startsWith("{")) {
                    // Parse as JSON Object (Map format)
                    val jsonObject = JSONObject(bodyString)
                    val keys = jsonObject.keys()
                    var index = 0
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val valueObj = jsonObject.get(key)
                        if (valueObj is JSONObject) {
                            channels.add(parseChannelObject(valueObj, index++))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseDbClient", "Error parsing Firebase data", e)
                throw IOException("Parsing error: ${e.message}")
            }

            return channels.sortedBy { it.orderIndex }
        }
    }

    /**
     * Uploads the entire list of channels to Firebase Realtime Database.
     */
    suspend fun uploadChannelsToFirebase(firebaseUrl: String, channels: List<ChannelEntity>) {
        if (firebaseUrl.isBlank()) {
            throw IllegalArgumentException("Firebase Realtime Database URL cannot be blank")
        }

        val cleanUrl = firebaseUrl.trim().trimEnd('/')
        val url = if (cleanUrl.endsWith(".json")) {
            cleanUrl
        } else {
            "$cleanUrl/channels.json"
        }

        Log.d("FirebaseDbClient", "Uploading to Firebase: $url")

        val jsonArray = JSONArray()
        channels.forEachIndexed { i, channel ->
            val obj = JSONObject()
            obj.put("id", channel.id)
            obj.put("name", channel.name)
            obj.put("url", channel.url)
            obj.put("backupUrl", channel.backupUrl ?: "")
            obj.put("category", channel.category)
            obj.put("logoUrl", channel.logoUrl ?: "")
            obj.put("headersJson", channel.headersJson ?: "")
            obj.put("isLive", channel.isLive)
            obj.put("isFavorite", channel.isFavorite)
            obj.put("orderIndex", channel.orderIndex)
            obj.put("server3", channel.server3 ?: "")
            obj.put("server4", channel.server4 ?: "")
            obj.put("server5", channel.server5 ?: "")
            jsonArray.put(obj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonArray.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Firebase upload failed: ${response.code} - ${response.message}")
            }
            Log.d("FirebaseDbClient", "Successfully uploaded ${channels.size} channels to Firebase")
        }
    }

    private fun parseChannelObject(obj: JSONObject, defaultIndex: Int): ChannelEntity {
        val name = obj.optString("name", "Unnamed Channel")
        val url = obj.optString("url", obj.optString("stream_url", ""))
        val backupUrl = obj.optString("backupUrl", obj.optString("backup_url", ""))
        val category = obj.optString("category", "টিভি চ্যানেল")
        val logoUrl = obj.optString("logoUrl", obj.optString("logo_url", ""))
        val headersJson = obj.optString("headersJson", obj.optString("headers_json", ""))
        val isLive = obj.optBoolean("isLive", obj.optBoolean("is_live", true))
        val isFavorite = obj.optBoolean("isFavorite", obj.optBoolean("is_favorite", false))
        val orderIndex = obj.optInt("orderIndex", obj.optInt("order_index", defaultIndex))
        val server3 = obj.optString("server3", obj.optString("server_3", ""))
        val server4 = obj.optString("server4", obj.optString("server_4", ""))
        val server5 = obj.optString("server5", obj.optString("server_5", ""))

        return ChannelEntity(
            name = name,
            url = url,
            backupUrl = if (backupUrl.isBlank()) null else backupUrl,
            category = category,
            logoUrl = if (logoUrl.isBlank()) null else logoUrl,
            headersJson = if (headersJson.isBlank()) null else headersJson,
            isLive = isLive,
            isFavorite = isFavorite,
            orderIndex = orderIndex,
            server3 = if (server3.isBlank()) null else server3,
            server4 = if (server4.isBlank()) null else server4,
            server5 = if (server5.isBlank()) null else server5
        )
    }
}
