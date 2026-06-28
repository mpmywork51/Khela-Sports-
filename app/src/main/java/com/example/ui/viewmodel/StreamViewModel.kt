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

    private val _supabaseUrl = MutableStateFlow(prefs.getString("supabase_url", "") ?: "")
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseKey = MutableStateFlow(prefs.getString("supabase_key", "") ?: "")
    val supabaseKey: StateFlow<String> = _supabaseKey.asStateFlow()

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
        // Pre-populate updated defaults with the exact TV V Channel requested
        viewModelScope.launch {
            val hasPopulatedTvV = prefs.getBoolean("has_populated_tv_v_v3", false)
            if (!hasPopulatedTvV) {
                Log.d("StreamViewModel", "Pre-populating updated defaults with new TV V Channel link")
                repository.prepopulateDefaultChannels()
                prefs.edit().putBoolean("has_populated_tv_v_v3", true).apply()
            } else {
                val currentList = repository.allChannels.first()
                if (currentList.isEmpty()) {
                    Log.d("StreamViewModel", "Pre-populating default channels as fallback")
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

    fun updateSupabaseSettings(url: String, key: String) {
        _supabaseUrl.value = url
        _supabaseKey.value = key
        prefs.edit()
            .putString("supabase_url", url)
            .putString("supabase_key", key)
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
     * Trigger manual synchronization with Supabase API.
     */
    fun syncWithSupabase() {
        viewModelScope.launch {
            _syncStatus.value = "ক্রমান্বয়ে সিঙ্ক হচ্ছে... (Syncing...)"
            try {
                if (_supabaseUrl.value.isBlank() || _supabaseKey.value.isBlank()) {
                    _syncStatus.value = "ভুল: Supabase URL এবং Key প্রয়োজনীয়! (Error: Config required)"
                    return@launch
                }
                val count = repository.syncWithSupabase(_supabaseUrl.value, _supabaseKey.value)
                _syncStatus.value = "সফলভাবে $count টি চ্যানেল সিঙ্ক হয়েছে! (Success: Synced $count channels)"
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Supabase Sync Error", e)
                _syncStatus.value = "ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            }
        }
    }

    /**
     * Restore default pre-populated channels.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            _syncStatus.value = "রিসেট হচ্ছে..."
            try {
                repository.prepopulateDefaultChannels()
                _syncStatus.value = "ডিফল্ট চ্যানেলে রিসেট সফল হয়েছে!"
            } catch (e: Exception) {
                _syncStatus.value = "রিসেট ব্যর্থ হয়েছে: ${e.localizedMessage}"
            }
        }
    }

    fun addNewChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.insertChannel(channel)
        }
    }

    fun updateChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.updateChannel(channel)
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.deleteChannel(channel)
        }
    }

    fun deleteChannelById(id: Int) {
        viewModelScope.launch {
            repository.deleteChannelById(id)
        }
    }

    fun clearStatus() {
        _syncStatus.value = null
    }

    /**
     * Resolves the final stream URL based on active channel, server selection, and proxy configurations.
     */
    fun resolveStreamUrl(channel: ChannelEntity, serverIndex: Int): String {
        val rawUrl = if (serverIndex == 2 && !channel.backupUrl.isNullOrBlank()) {
            channel.backupUrl
        } else {
            channel.url
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
