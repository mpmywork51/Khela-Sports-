package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val backupUrl: String? = null,
    val category: String, // e.g., "Cricket", "Football", "FIFA World Cup"
    val logoUrl: String? = null,
    val headersJson: String? = null, // e.g., {"User-Agent": "MyCustomUA", "Referer": "http://stream.com"}
    val isLive: Boolean = true,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)
