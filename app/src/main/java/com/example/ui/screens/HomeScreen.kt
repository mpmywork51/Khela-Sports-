package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.database.ChannelEntity
import com.example.ui.viewmodel.StreamViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: StreamViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channels.collectAsState()
    val showWelcomeDialog by viewModel.showWelcomeDialog.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val serverIndex by viewModel.selectedServer.collectAsState()

    val rawContext = LocalContext.current
    val context = remember(rawContext) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            rawContext.createAttributionContext("default")
        } else {
            rawContext
        }
    }

    // Bottom Navigation State: 0 = লাইভ ভিডিও, 1 = টিভি চ্যানেল, 2 = সময়সূচী
    var currentBottomTab by remember { mutableStateOf(0) }

    // Live Video Categories (Inside Tab 0)
    var selectedLiveCategory by remember { mutableStateOf(0) } // 0 = সব খেলা, 1 = ক্রিকেট, 2 = ফুটবল, 3 = অন্যান্য
    val liveCategoryTitles = listOf("সব খেলা", "ক্রিকেট", "ফুটবল", "অন্যান্য")

    // Filter Sports Channels for Tab 0
    val filteredSportsChannels = remember(channels, selectedLiveCategory) {
        val nonTvChannels = channels.filter {
            val isTv = it.category.equals("টিভি চ্যানেল") || 
                       it.category.equals("TV Channel", ignoreCase = true) || 
                       it.category.contains("TV") || 
                       it.category.contains("টিভি") || 
                       it.category.contains("Channel", ignoreCase = true)
            !isTv
        }
        when (selectedLiveCategory) {
            0 -> nonTvChannels
            1 -> nonTvChannels.filter { it.category.equals("Cricket", ignoreCase = true) || it.category.contains("ক্রিকেট") }
            2 -> nonTvChannels.filter { it.category.equals("Football", ignoreCase = true) || it.category.contains("ফুটবল") }
            3 -> nonTvChannels.filter { 
                !it.category.equals("Cricket", ignoreCase = true) && 
                !it.category.contains("ক্রিকেট") && 
                !it.category.equals("Football", ignoreCase = true) && 
                !it.category.contains("ফুটবল")
            }
            else -> nonTvChannels
        }
    }

    // Filter TV Channels for Tab 1
    val filteredTvChannels = remember(channels) {
        channels.filter {
            it.category.equals("টিভি চ্যানেল") || 
            it.category.equals("TV Channel", ignoreCase = true) || 
            it.category.contains("TV") || 
            it.category.contains("টিভি") || 
            it.category.contains("Channel", ignoreCase = true)
        }
    }

    // Group TV Channels by Category for Tab 1 (as seen in video grouping)
    val tvChannelsGrouped = remember(filteredTvChannels) {
        filteredTvChannels.groupBy { it.category }
    }

    // Sports Tab counts
    val sportsCount = channels.count {
        val isTv = it.category.equals("টিভি চ্যানেল") || 
                   it.category.equals("TV Channel", ignoreCase = true) || 
                   it.category.contains("TV") || 
                   it.category.contains("টিভি") || 
                   it.category.contains("Channel", ignoreCase = true)
        !isTv
    }
    val cricketCount = channels.count { !it.category.contains("টিভি") && (it.category.equals("Cricket", ignoreCase = true) || it.category.contains("ক্রিকেট")) }
    val footballCount = channels.count { !it.category.contains("টিভি") && (it.category.equals("Football", ignoreCase = true) || it.category.contains("ফুটবল")) }
    val otherSportsCount = sportsCount - cricketCount - footballCount

    val liveCategoryCounts = listOf(sportsCount, cricketCount, footballCount, otherSportsCount)

    // Dark Slate Background Gradient
    val glassBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF040A10),
            Color(0xFF020406)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glassBackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 2. Header Bar - Centered Logo, Profile Left, Bell Right (Always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Avatar Circle (Left) -> Navigates to Admin Control Panel
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F00FF87))
                        .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.4f), CircleShape)
                        .clickable { onNavigateToAdmin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Admin Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Brand Title (Centered)
                Text(
                    text = "LiveKhela",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FF87),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                // Notification Bell Icon (Right)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1FFFFFFF))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Main Views switching based on Bottom Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentBottomTab) {
                    0 -> { // ------------------ TAB 0: লাইভ ভিডিও ------------------
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Category Row selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                liveCategoryTitles.forEachIndexed { index, title ->
                                    val isSelected = selectedLiveCategory == index
                                    val count = liveCategoryCounts[index]
                                    val tabText = "$title ($count)"

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isSelected) Color(0xFF00FF87) else Color(0x1F00FF87)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF00FF87) else Color(0x1A00FF87),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable { selectedLiveCategory = index }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF070B11) else Color.White
                                        )
                                    }
                                }
                            }

                            // Channels Match Cards Grid / Empty state
                            if (filteredSportsChannels.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateView(
                                        title = "এই মুহূর্তে কোন সচল ম্যাচ সম্প্রচার হচ্ছে না।",
                                        subtitle = "দয়া করে পরবর্তীতে খেলা শুরুর নির্দিষ্ট সময়ে চেক করুন।"
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(1),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredSportsChannels) { channel ->
                                        SportsMatchCard(
                                            channel = channel,
                                            onClick = {
                                                viewModel.selectChannel(channel)
                                                onNavigateToPlayer()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // ------------------ TAB 1: টিভি চ্যানেল ------------------
                        if (filteredTvChannels.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "কোন টিভি চ্যানেল পাওয়া যায়নি।",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3), // 3-column pure logo grid matching video
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic Grouped Layout as seen in video (Header category then item logos)
                                tvChannelsGrouped.forEach { (categoryName, list) ->
                                    item(span = { GridItemSpan(3) }) {
                                        Text(
                                            text = "$categoryName (${list.size})",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }

                                    items(list) { channel ->
                                        PureTvChannelLogoCard(
                                            channel = channel,
                                            onClick = {
                                                viewModel.selectChannel(channel)
                                                onNavigateToPlayer()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // ------------------ TAB 2: সময়সূচী ------------------
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ScheduleEmptyView()
                        }
                    }
                }
            }

            // 4. Custom Bottom Navigation Bar matching the video layout and oval pill shape precisely
            CustomBottomNavigationBar(
                currentTab = currentBottomTab,
                onTabSelected = { tabIndex ->
                    currentBottomTab = tabIndex
                }
            )
        }

        // Welcome overlay dialog
        if (showWelcomeDialog) {
            LiveKhelaWelcomeDialog(
                onJoinTelegram = {
                    try {
                        val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/livekhela"))
                        context.startActivity(telegramIntent)
                    } catch (e: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/livekhela"))
                        context.startActivity(webIntent)
                    }
                },
                onExit = {
                    viewModel.dismissWelcomeDialog()
                }
            )
        }
    }
}

/**
 * Custom Compose-based Bottom Navigation Bar.
 * Styled with absolute black background, translucent top border, and an elegant neon green oval indicator.
 */
@Composable
fun CustomBottomNavigationBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF03070C))
            .navigationBarsPadding()
            .border(width = 0.5.dp, color = Color(0x1AFFFFFF))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf("লাইভ ভিডিও", "টিভি চ্যানেল", "সময়সূচী")
        val icons = listOf(Icons.Default.PlayArrow, Icons.Default.PlayArrow, Icons.Default.DateRange)

        items.forEachIndexed { index, label ->
            val isSelected = currentTab == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 2.dp, horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Color(0xFF00FF87) else Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (index == 1) {
                        TvIcon(
                            tint = if (isSelected) Color(0xFF03070C) else Color.LightGray.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = label,
                            tint = if (isSelected) Color(0xFF03070C) else Color.LightGray.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF00FF87) else Color.LightGray.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Pure TV Logo Card: Styled as a rounded square button with subtle border, centering the brand logotype image.
 * Matches TV Channels screen from the video.
 */
@Composable
fun PureTvChannelLogoCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square shape matching video
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050D15))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp) // Keeps logo inset beautifully
                )
            } else {
                Text(
                    text = channel.name.take(3).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00FF87),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Empty state matches concentric circles radar with Bengali error texts
 */
@Composable
fun EmptyStateView(
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        // Concentric Circles Radar Icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerPt = size.center
                drawCircle(
                    color = Color(0xFF00FF87).copy(alpha = 0.25f),
                    radius = 20.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00FF87).copy(alpha = 0.15f),
                    radius = 42.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00FF87).copy(alpha = 0.08f),
                    radius = 64.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            TvIcon(
                tint = Color(0xFF00FF87),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            color = Color.LightGray.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * Schedule empty state view featuring Clock face and specific Bengali notice
 */
@Composable
fun ScheduleEmptyView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        // Custom drawn clock face with canvas hands
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerPt = size.center
                val radius = 40.dp.toPx()

                // Outer Ring
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.4f),
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Hand 1 (Hour hand)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = centerPt,
                    end = androidx.compose.ui.geometry.Offset(centerPt.x + radius * 0.4f, centerPt.y - radius * 0.2f),
                    strokeWidth = 3.dp.toPx()
                )

                // Hand 2 (Minute hand)
                drawLine(
                    color = Color(0xFF00FF87).copy(alpha = 0.7f),
                    start = centerPt,
                    end = androidx.compose.ui.geometry.Offset(centerPt.x, centerPt.y - radius * 0.65f),
                    strokeWidth = 2.dp.toPx()
                )

                // Center Dot
                drawCircle(
                    color = Color(0xFF00FF87),
                    radius = 3.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "নিকট ভবিষ্যতে কোন ম্যাচ সিডিউল করা নাই।",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "নতুন ম্যাচের সিডিউল আসার জন্য লাইভখেলার সাথে থাকুন।",
            color = Color.LightGray.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Embedded High-Performance Video Player with modular lifecycle wrapper.
 * Pure Compose overlay controllers replicate the layout perfectly without fight over XML default styles.
 */
@OptIn(UnstableApi::class)
@Composable
fun EmbeddedLivePlayer(
    channel: ChannelEntity,
    viewModel: StreamViewModel,
    serverIndex: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // Load configs
    val minBuffer by viewModel.minBufferMs.collectAsState()
    val maxBuffer by viewModel.maxBufferMs.collectAsState()
    val playbackBuffer by viewModel.bufferForPlaybackMs.collectAsState()
    val rebufferBuffer by viewModel.bufferForPlaybackAfterRebufferMs.collectAsState()

    // Player State
    val activeUrl = remember(channel, serverIndex) { viewModel.resolveStreamUrl(channel, serverIndex) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Initialize ExoPlayer
    LaunchedEffect(activeUrl, minBuffer, maxBuffer, playbackBuffer, rebufferBuffer) {
        errorMessage = null
        isBuffering = true

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBuffer, maxBuffer, playbackBuffer, rebufferBuffer)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setPreferredVideoMimeType("video/avc"))
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("LiveKhela/1.0 (Android; Embedded ExoPlayer Engine)")
            .setAllowCrossProtocolRedirects(true)

        channel.headersJson?.let { headersStr ->
            if (headersStr.isNotBlank()) {
                try {
                    val headersMap = mutableMapOf<String, String>()
                    val json = org.json.JSONObject(headersStr)
                    json.keys().forEach { key ->
                        headersMap[key] = json.getString(key)
                    }
                    httpDataSourceFactory.setDefaultRequestProperties(headersMap)
                } catch (e: Exception) {
                    Log.e("EmbeddedPlayer", "Failed parsing custom headers", e)
                }
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isPlaying = player.isPlaying
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "স্ট্রীম লোড করা সম্ভব হচ্ছে না।\n(Error: ${error.localizedMessage})"
            }
        })

        val mediaItem = MediaItem.fromUri(activeUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        exoPlayer = player
    }

    // Handle mute change reactively
    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    // Release player on Dispose
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Video 16:9 Aspect ratio Box layout
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.3f))
    ) {
        if (activeUrl.isNotBlank() && errorMessage == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false // Use our pure elegant Compose controllers overlay
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- Custom Controllers overlay matching video exactly ---

        // 1. Top Row Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Closed Pill button ("বন্ধ")
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "বন্ধ",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Channel name
            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // ON / OFF toggle simulator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isPlaying) Color(0xFF00FF87) else Color.Gray, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPlaying) "ON" else "OFF",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Server selector buttons (S1, S2)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(2.dp)
            ) {
                val s1Active = serverIndex == 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (s1Active) Color(0xFF00FF87) else Color.Transparent)
                        .clickable { viewModel.setServerIndex(1) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "S1",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (s1Active) Color(0xFF03070C) else Color.White
                    )
                }

                if (!channel.backupUrl.isNullOrBlank()) {
                    val s2Active = serverIndex == 2
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (s2Active) Color(0xFF00FF87) else Color.Transparent)
                            .clickable { viewModel.setServerIndex(2) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "S2",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (s2Active) Color(0xFF03070C) else Color.White
                        )
                    }
                }
            }
        }

        // 2. Middle Buffering Overlay
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF00FF87),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "বাফারিং হচ্ছে...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Error message overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            isBuffering = true
                            exoPlayer?.prepare()
                            exoPlayer?.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("পুনরায় চেষ্টা", color = Color(0xFF03070C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Bottom Controller Row Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause Icon
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Refresh, // Simplified play/pause toggler
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            if (isPlaying) {
                                exoPlayer?.pause()
                                isPlaying = false
                            } else {
                                exoPlayer?.play()
                                isPlaying = true
                            }
                        }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Volume / Mute Toggle Icon
                Icon(
                    imageVector = if (isMuted) Icons.Default.Close else Icons.Default.Notifications, // Simplified volume indicators
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { isMuted = !isMuted }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Green LIVE Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0E2214))
                        .border(0.5.dp, Color(0xFF00FF87), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFF00FF87), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        color = Color(0xFF00FF87),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Counter / timer simulation text
                Text(
                    text = "00:00",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            // Right-side zoom / aspect ratio toggle icon (replicates fullscreen icon action)
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Zoom Aspect Ratio",
                tint = Color.White,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
            )
        }
    }
}

/**
 * Sports match card exactly matching BAN vs NZ template: teams on left/right, watch button, glowing border
 */
@Composable
fun SportsMatchCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    val nameWithoutParen = channel.name.replace(Regex("\\(.*\\)"), "").trim()
    val teams = remember(nameWithoutParen) {
        if (nameWithoutParen.contains(" vs ", ignoreCase = true)) {
            nameWithoutParen.split(Regex(" vs ", RegexOption.IGNORE_CASE))
        } else if (nameWithoutParen.contains(" vs. ", ignoreCase = true)) {
            nameWithoutParen.split(Regex(" vs\\. ", RegexOption.IGNORE_CASE))
        } else {
            null
        }
    }

    val logos = remember(channel.logoUrl) {
        if (!channel.logoUrl.isNullOrBlank()) {
            if (channel.logoUrl.contains(",")) {
                channel.logoUrl.split(",").map { it.trim() }
            } else if (channel.logoUrl.contains("|")) {
                channel.logoUrl.split("|").map { it.trim() }
            } else {
                listOf(channel.logoUrl.trim())
            }
        } else {
            emptyList()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "border_glow")
    val glowFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_fraction"
    )
    val glowColor = remember(glowFraction) {
        androidx.compose.ui.graphics.lerp(
            Color(0xFF00FF87),
            Color(0xFF00E5FF),
            glowFraction
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = glowColor, // Animating glowing neon color border
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050D15))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0E2214))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = channel.category,
                        color = Color(0xFF00FF87),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1AFF0000))
                        .border(1.dp, Color(0xFFFF3333), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFFFF3333), shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            color = Color(0xFFFF3333),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Teams VS layout
            if (teams != null && teams.size >= 2) {
                val team1 = teams[0].trim()
                val team2 = teams[1].trim()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Team 1 Logo and Text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Black, shape = CircleShape)
                                .border(1.2.dp, Color(0xFF00FF87).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val logo1 = logos.getOrNull(0)
                            if (!logo1.isNullOrBlank()) {
                                AsyncImage(
                                    model = logo1,
                                    contentDescription = team1,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Sport",
                                    tint = Color(0xFF00FF87),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = team1,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // VS Ring
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color(0xFF00FF87), CircleShape)
                            .background(Color(0xFF03070C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "vs",
                            color = Color(0xFF00FF87),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Team 2 Text and Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = team2,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Black, shape = CircleShape)
                                .border(1.2.dp, Color(0xFF00FF87).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val logo2 = logos.getOrNull(1) ?: logos.getOrNull(0)
                            if (!logo2.isNullOrBlank()) {
                                AsyncImage(
                                    model = logo2,
                                    contentDescription = team2,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Sport",
                                    tint = Color(0xFF00FF87),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Unified standard layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black, shape = CircleShape)
                                .border(1.dp, Color(0xFF00FF87), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = channel.name,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = channel.name.take(2).uppercase(),
                                    color = Color(0xFF00FF87),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF03070C), CircleShape)
                            .border(1.dp, Color(0xFF00FF87), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFF00FF87),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom row of Sports Match card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (channel.name.contains("(")) {
                        channel.name.substringAfter("(").replace(")", "")
                    } else {
                        "সরাসরি খেলা উপভোগ করুন"
                    },
                    color = Color.LightGray.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // "দেখুন" (Watch) play button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00FF87))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watch",
                            tint = Color(0xFF03070C),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "দেখুন",
                            color = Color(0xFF03070C),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Welcome dialog exactly matches the layout in the video: close button, glow border, star spark icon on top, proper Bengali text
 */
@Composable
fun LiveKhelaWelcomeDialog(
    onJoinTelegram: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.25.dp,
                    color = Color(0xFF00FF87).copy(alpha = 0.5f), // Green glow outline
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFB02050A)) // Semi-translucent dark glass
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Close 'X' button on top-right corner precisely matching video layout!
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close welcome dialog",
                        tint = Color.LightGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Star four-point spark top logo matching video
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0x1F00FF87), shape = CircleShape)
                            .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w / 2, 0f)
                                quadraticTo(w / 2, h / 2, w, h / 2)
                                quadraticTo(w / 2, h / 2, w / 2, h)
                                quadraticTo(w / 2, h / 2, 0f, h / 2)
                                quadraticTo(w / 2, h / 2, w / 2, 0f)
                            }
                            drawPath(path, Color(0xFF00FF87))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "LiveKhela-এ স্বাগতম",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "আপনাকে স্বাগতম আমাদের খেলার ওয়েবসাইটে সব ধরণের লাইভ খেলা উপভোগ করতে আমাদের সাথেই থাকুন।",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // "টেলিগ্রামে জয়েন হন" Button with send paper plane icon inside
                    Button(
                        onClick = onJoinTelegram,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send, // Serves as Telegram paper plane icon
                                contentDescription = "Telegram",
                                tint = Color(0xFF03070C),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "টেলিগ্রামে জয়েন হন",
                                color = Color(0xFF03070C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Outlined or Text button "বাহির হন"
                    Button(
                        onClick = onExit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "বাহির হন",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // TV Body
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Antennas
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.2f),
            end = androidx.compose.ui.geometry.Offset(w * 0.15f, 0f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.2f),
            end = androidx.compose.ui.geometry.Offset(w * 0.85f, 0f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

