package com.example.data.repository

import android.content.Context
import com.example.data.database.ChannelDao
import com.example.data.database.ChannelEntity
import com.example.data.network.FirebaseDbClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
     * Synchronizes local database with remote Firebase Realtime Database.
     */
    suspend fun syncWithFirebase(firebaseUrl: String): Int = withContext(Dispatchers.IO) {
        val remoteChannels = FirebaseDbClient.fetchChannelsFromFirebase(firebaseUrl)
        if (remoteChannels.isNotEmpty()) {
            channelDao.deleteAllChannels()
            channelDao.insertChannels(remoteChannels)
        } else {
            // If Firebase has nothing (e.g. empty DB or null), we clean local DB as well
            channelDao.deleteAllChannels()
        }
        remoteChannels.size
    }

    /**
     * Uploads the entire local list of channels to remote Firebase Realtime Database.
     */
    suspend fun uploadToFirebase(firebaseUrl: String) = withContext(Dispatchers.IO) {
        val localChannels = channelDao.getAllChannels().first()
        FirebaseDbClient.uploadChannelsToFirebase(firebaseUrl, localChannels)
    }

    /**
     * Pre-populates default channels (sports streams) matching the LiveKhela layout
     */
    suspend fun prepopulateDefaultChannels() = withContext(Dispatchers.IO) {
        channelDao.deleteAllChannels()
    }
}
