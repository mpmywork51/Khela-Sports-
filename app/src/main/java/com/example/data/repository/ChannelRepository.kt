package com.example.data.repository

import android.content.Context
import com.example.data.database.ChannelDao
import com.example.data.database.ChannelEntity
import com.example.data.network.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val context: Context
) {
    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()

    suspend fun insertChannel(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        channelDao.insertChannel(channel)
    }

    suspend fun updateChannel(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        channelDao.updateChannel(channel)
    }

    suspend fun deleteChannel(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        channelDao.deleteChannel(channel)
    }

    suspend fun deleteChannelById(id: Int) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelById(id)
    }

    suspend fun clearAllChannels() = withContext(Dispatchers.IO) {
        channelDao.deleteAllChannels()
    }

    /**
     * Synchronizes local database with remote Supabase table.
     */
    suspend fun syncWithSupabase(supabaseUrl: String, supabaseKey: String): Int = withContext(Dispatchers.IO) {
        val remoteChannels = SupabaseClient.fetchChannelsFromSupabase(supabaseUrl, supabaseKey)
        if (remoteChannels.isNotEmpty()) {
            channelDao.deleteAllChannels()
            channelDao.insertChannels(remoteChannels)
        }
        remoteChannels.size
    }

    /**
     * Pre-populates default channels (sports streams) matching the LiveKhela layout
     */
    suspend fun prepopulateDefaultChannels() = withContext(Dispatchers.IO) {
        channelDao.deleteAllChannels()
    }
}
