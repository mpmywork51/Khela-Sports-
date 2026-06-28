package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChannelEntity
import com.example.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StreamViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("livekhela_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val repository = ChannelRepository(database.channelDao(), application)

    // Dynamic state flow from local database
    val channels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current streaming and player configuration
    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    private val _selectedServer = MutableStateFlow(1) // 1 = Main URL, 2 = Backup URL
    val selectedServer: StateFlow<Int> = _selectedServer.asStateFlow()

    // Preferences and customizable fields
    private val _useProxy = MutableStateFlow(prefs.getBoolean("use_proxy", false))
    val useProxy: StateFlow<Boolean> = _useProxy.asStateFlow()

    private val _proxyUrl = MutableStateFlow(
        prefs.getString("proxy_url", "https://livekhela-proxy.herokuapp.com/proxy?url=") ?: ""
    )
    val proxyUrl: StateFlow<String> = _proxyUrl.asStateFlow()

    private val _firebaseUrl = MutableStateFlow(
        prefs.getString("firebase_url", "https://ai-studio-applet-webapp-8d448-default-rtdb.firebaseio.com/") ?: "https://ai-studio-applet-webapp-8d448-default-rtdb.firebaseio.com/"
    )
    val firebaseUrl: StateFlow<String> = _firebaseUrl.asStateFlow()

    // High performance buffer configurations
    private val _minBufferMs = MutableStateFlow(prefs.getInt("min_buffer_ms", 5000))
    val minBufferMs: StateFlow<Int> = _minBufferMs.asStateFlow()

    private val _maxBufferMs = MutableStateFlow(prefs.getInt("max_buffer_ms", 15000))
    val maxBufferMs: StateFlow<Int> = _maxBufferMs.asStateFlow()

    private val _bufferForPlaybackMs = MutableStateFlow(prefs.getInt("buffer_for_playback_ms", 1500))
    val bufferForPlaybackMs: StateFlow<Int> = _bufferForPlaybackMs.asStateFlow()

    private val _bufferForPlaybackAfterRebufferMs = MutableStateFlow(
        prefs.getInt("buffer_for_playback_after_rebuffer_ms", 3000)
    )
    val bufferForPlaybackAfterRebufferMs: StateFlow<Int> = _bufferForPlaybackAfterRebufferMs.asStateFlow()

    // Onboarding welcome dialog visible logic (matching LiveKhela video!)
    private val _showWelcomeDialog = MutableStateFlow(prefs.getBoolean("show_welcome_dialog_v1", true))
    val showWelcomeDialog: StateFlow<Boolean> = _showWelcomeDialog.asStateFlow()

    // Synchronization / status logs
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    init {
        // Automatically sync from Firebase if URL is configured, otherwise clear if requested
        viewModelScope.launch {
            if (_firebaseUrl.value.isNotBlank()) {
                Log.d("StreamViewModel", "Auto syncing from Firebase at startup")
                try {
                    repository.syncWithFirebase(_firebaseUrl.value)
                } catch (e: Exception) {
                    Log.e("StreamViewModel", "Startup Firebase Sync Failed", e)
                }
            } else {
                val currentList = repository.allChannels.first()
                if (currentList.isEmpty()) {
                    Log.d("StreamViewModel", "No local channels. Waiting for user input.")
                    repository.prepopulateDefaultChannels()
                }
            }
        }
    }

    fun selectChannel(channel: ChannelEntity?, serverIndex: Int = 1) {
        _selectedChannel.value = channel
        _selectedServer.value = serverIndex
    }

    fun setServerIndex(serverIndex: Int) {
        _selectedServer.value = serverIndex
    }

    fun dismissWelcomeDialog() {
        _showWelcomeDialog.value = false
        prefs.edit().putBoolean("show_welcome_dialog_v1", false).apply()
    }

    fun setWelcomeDialogShown(shown: Boolean) {
        _showWelcomeDialog.value = shown
        prefs.edit().putBoolean("show_welcome_dialog_v1", shown).apply()
    }

    fun updateProxySettings(useProxy: Boolean, proxyUrl: String) {
        _useProxy.value = useProxy
        _proxyUrl.value = proxyUrl
        prefs.edit()
            .putBoolean("use_proxy", useProxy)
            .putString("proxy_url", proxyUrl)
            .apply()
    }

    fun updateFirebaseSettings(url: String) {
        _firebaseUrl.value = url
        prefs.edit()
            .putString("firebase_url", url)
            .apply()
    }

    fun updateBufferSettings(min: Int, max: Int, playback: Int, rebuffer: Int) {
        _minBufferMs.value = min
        _maxBufferMs.value = max
        _bufferForPlaybackMs.value = playback
        _bufferForPlaybackAfterRebufferMs.value = rebuffer
        prefs.edit()
            .putInt("min_buffer_ms", min)
            .putInt("max_buffer_ms", max)
            .putInt("buffer_for_playback_ms", playback)
            .putInt("buffer_for_playback_after_rebuffer_ms", rebuffer)
            .apply()
    }

    /**
     * Trigger manual synchronization (download) from Firebase Realtime Database.
     */
    fun syncWithFirebase() {
        viewModelScope.launch {
            _syncStatus.value = "ক্রমান্বয়ে সিঙ্ক হচ্ছে... (Syncing from Firebase...)"
            try {
                if (_firebaseUrl.value.isBlank()) {
                    _syncStatus.value = "ভুল: Firebase URL প্রয়োজনীয়! (Error: Firebase URL required)"
                    return@launch
                }
                val count = repository.syncWithFirebase(_firebaseUrl.value)
                _syncStatus.value = "সফলভাবে $count টি চ্যানেল ডাউনলোড হয়েছে! (Success: Synced $count channels)"
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Firebase Sync Error", e)
                _syncStatus.value = "সিঙ্ক ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            }
        }
    }

    /**
     * Trigger manual upload to Firebase Realtime Database.
     */
    fun uploadToFirebase() {
        viewModelScope.launch {
            _syncStatus.value = "ফায়ারবেজে আপলোড হচ্ছে... (Uploading to Firebase...)"
            try {
                if (_firebaseUrl.value.isBlank()) {
                    _syncStatus.value = "ভুল: Firebase URL প্রয়োজনীয়! (Error: Firebase URL required)"
                    return@launch
                }
                repository.uploadToFirebase(_firebaseUrl.value)
                _syncStatus.value = "সফলভাবে ফায়ারবেজে ডাটা আপলোড হয়েছে! (Success: Uploaded to Firebase)"
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Firebase Upload Error", e)
                _syncStatus.value = "আপলোড ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            }
        }
    }

    private suspend fun uploadToFirebaseSilent() {
        if (_firebaseUrl.value.isNotBlank()) {
            try {
                repository.uploadToFirebase(_firebaseUrl.value)
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Auto Firebase upload failed", e)
            }
        }
    }

    /**
     * Restore default pre-populated channels (empty by request).
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            _syncStatus.value = "রিসেট হচ্ছে..."
            try {
                repository.prepopulateDefaultChannels()
                if (_firebaseUrl.value.isNotBlank()) {
                    uploadToFirebaseSilent()
                }
                _syncStatus.value = "চ্যানেল তালিকা রিসেট সম্পন্ন হয়েছে!"
            } catch (e: Exception) {
                _syncStatus.value = "রিসেট ব্যর্থ হয়েছে: ${e.localizedMessage}"
            }
        }
    }

    fun addNewChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.insertChannel(channel)
            uploadToFirebaseSilent()
        }
    }

    fun updateChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.updateChannel(channel)
            uploadToFirebaseSilent()
        }
    }

    fun moveChannelUp(channel: ChannelEntity) {
        viewModelScope.launch {
            val list = channels.value.toMutableList()
            val index = list.indexOfFirst { it.id == channel.id }
            if (index > 0) {
                val temp = list[index]
                list[index] = list[index - 1]
                list[index - 1] = temp
                list.forEachIndexed { i, ch ->
                    repository.updateChannel(ch.copy(orderIndex = i))
                }
                uploadToFirebaseSilent()
            }
        }
    }

    fun moveChannelDown(channel: ChannelEntity) {
        viewModelScope.launch {
            val list = channels.value.toMutableList()
            val index = list.indexOfFirst { it.id == channel.id }
            if (index >= 0 && index < list.size - 1) {
                val temp = list[index]
                list[index] = list[index + 1]
                list[index + 1] = temp
                list.forEachIndexed { i, ch ->
                    repository.updateChannel(ch.copy(orderIndex = i))
                }
                uploadToFirebaseSilent()
            }
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.deleteChannel(channel)
            uploadToFirebaseSilent()
        }
    }

    fun deleteChannelById(id: Int) {
        viewModelScope.launch {
            repository.deleteChannelById(id)
            uploadToFirebaseSilent()
        }
    }

    fun clearStatus() {
        _syncStatus.value = null
    }

    /**
     * Resolves the final stream URL based on active channel, server selection, and proxy configurations.
     */
    fun resolveStreamUrl(channel: ChannelEntity, serverIndex: Int): String {
        val rawUrl = when (serverIndex) {
            2 -> channel.backupUrl ?: channel.url
            3 -> channel.server3 ?: channel.url
            4 -> channel.server4 ?: channel.url
            5 -> channel.server5 ?: channel.url
            else -> channel.url
        }

        return if (_useProxy.value && _proxyUrl.value.isNotBlank()) {
            val prefix = _proxyUrl.value
            // Ensure proper concatenation
            if (prefix.endsWith("=")) {
                "$prefix${encodeUrlParam(rawUrl)}"
            } else {
                "$prefix$rawUrl"
            }
        } else {
            rawUrl
        }
    }

    private fun encodeUrlParam(url: String): String {
        return try {
            java.net.URLEncoder.encode(url, "UTF-8")
        } catch (e: Exception) {
            url
        }
    }
}
