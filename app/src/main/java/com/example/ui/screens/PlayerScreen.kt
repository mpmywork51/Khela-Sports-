package com.example.ui.screens

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.data.database.ChannelEntity
import com.example.ui.viewmodel.StreamViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: StreamViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawContext = LocalContext.current
    val context = remember(rawContext) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            rawContext.createAttributionContext("default")
        } else {
            rawContext
        }
    }
    val channel by viewModel.selectedChannel.collectAsState()
    val serverIndex by viewModel.selectedServer.collectAsState()

    val minBuffer by viewModel.minBufferMs.collectAsState()
    val maxBuffer by viewModel.maxBufferMs.collectAsState()
    val playbackBuffer by viewModel.bufferForPlaybackMs.collectAsState()
    val rebufferBuffer by viewModel.bufferForPlaybackAfterRebufferMs.collectAsState()

    // Resolved URL
    val activeUrl = channel?.let { viewModel.resolveStreamUrl(it, serverIndex) } ?: ""

    // Player States
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentResolution by remember { mutableStateOf("বাছাই করা হচ্ছে... (Detecting)") }

    // Recreate/Re-init ExoPlayer when URL, Server or custom buffers change
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(activeUrl, minBuffer, maxBuffer, playbackBuffer, rebufferBuffer) {
        if (activeUrl.isBlank()) return@LaunchedEffect

        errorMessage = null
        isBuffering = true

        Log.d("PlayerScreen", "Initializing ExoPlayer for URL: $activeUrl")
        Log.d("PlayerScreen", "Buffers: min=$minBuffer, max=$maxBuffer, playback=$playbackBuffer, rebuffer=$rebufferBuffer")

        // 1. Setup Custom high-performance Buffer Load Control as requested!
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuffer,                  // minBufferMs (Default 5000)
                maxBuffer,                  // maxBufferMs (Default 15000)
                playbackBuffer,             // bufferForPlaybackMs (Default 1500)
                rebufferBuffer              // bufferForPlaybackAfterRebufferMs (Default 3000)
            )
            .build()

        // 2. Setup ABR Track Selector
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setPreferredVideoMimeType("video/avc"))
        }

        // 3. Setup Custom Data Source with custom User-Agent and referer headers
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("LiveKhela/1.0 (Android; Advanced ExoPlayer Engine)")
            .setAllowCrossProtocolRedirects(true)

        // Inject dynamic headers from the ChannelEntity if present
        channel?.headersJson?.let { headersStr ->
            if (headersStr.isNotBlank()) {
                try {
                    val headersMap = mutableMapOf<String, String>()
                    val json = org.json.JSONObject(headersStr)
                    json.keys().forEach { key ->
                        headersMap[key] = json.getString(key)
                    }
                    httpDataSourceFactory.setDefaultRequestProperties(headersMap)
                    Log.d("PlayerScreen", "Custom headers successfully injected: $headersMap")
                } catch (e: Exception) {
                    Log.e("PlayerScreen", "Failed parsing custom headers", e)
                }
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)

        // 4. Construct ExoPlayer
        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Set Up Listener
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = player.isPlaying
                isBuffering = playbackState == Player.STATE_BUFFERING

                if (playbackState == Player.STATE_READY) {
                    val format = player.videoFormat
                    currentResolution = if (format != null) {
                        "${format.width}x${format.height} @ ${(format.frameRate).toInt()}fps"
                    } else {
                        "অ্যাডাপ্টিভ (ABR Active)"
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                Log.e("PlayerScreen", "ExoPlayer Error: ", error)
                errorMessage = "স্ট্রীম লোড করা সম্ভব হচ্ছে না। সার্ভার পরিবর্তন করে দেখুন।\n(Error: ${error.localizedMessage})"
            }
        })

        // Prepare and play
        val mediaItem = MediaItem.fromUri(activeUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        exoPlayer = player
    }
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                break
            }
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Aspect Ratio & Resize modes
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showRatioIndicator by remember { mutableStateOf(false) }
    var ratioIndicatorText by remember { mutableStateOf("সাধারণ (Fit)") }

    LaunchedEffect(showRatioIndicator) {
        if (showRatioIndicator) {
            kotlinx.coroutines.delay(2000)
            showRatioIndicator = false
        }
    }

    // Safely release player and reset orientation on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d("PlayerScreen", "Releasing ExoPlayer and restoring Portrait orientation")
            exoPlayer?.release()
            exoPlayer = null
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Glass Background Gradient
    val glassBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0C1420),
            Color(0xFF080D16),
            Color(0xFF04060A)
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glassBackgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Bar (Hidden in full landscape mode)
                if (!isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel?.name ?: "লাইভ স্ট্রিম",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = channel?.category ?: "স্পোর্টস",
                                color = Color(0xFF00FF87), // Bright accent green
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Server Switch labels
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1FFFFFFF)) // Glass server background
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val s1Color = if (serverIndex == 1) Color(0xFF00FF87) else Color(0x0FFFFFFF)
                            val s1TextColor = if (serverIndex == 1) Color(0xFF070B11) else Color.White
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(s1Color)
                                    .clickable { viewModel.setServerIndex(1) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "S1",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s1TextColor
                                )
                            }

                            if (!channel?.backupUrl.isNullOrBlank()) {
                                val s2Color = if (serverIndex == 2) Color(0xFF00FF87) else Color(0x0FFFFFFF)
                                val s2TextColor = if (serverIndex == 2) Color(0xFF070B11) else Color.White
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(s2Color)
                                        .clickable { viewModel.setServerIndex(2) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "S2",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s2TextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Video Player Container (Becomes full screen in landscape, otherwise 16:9)
                Box(
                    modifier = if (isLandscape) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9.5f)
                            .background(Color.Black)
                            .border(BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.2f)))
                    }
                ) {
                    if (activeUrl.isNotBlank() && errorMessage == null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = true
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

                    // Floating Back Button inside player when in fullscreen landscape
                    if (isLandscape) {
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    // Aspect ratio feedback indicator overlay
                    if (showRatioIndicator) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = ratioIndicatorText,
                                color = Color(0xFF00FF87),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Custom floating Aspect Ratio and Rotation Controllers (Always available on top right/bottom right)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Resizing / Zoom Square Button
                        IconButton(
                            onClick = {
                                val nextMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                        ratioIndicatorText = "জুম (Zoom)"
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                                        ratioIndicatorText = "ফুল স্ক্রিন (Stretch)"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }
                                    else -> {
                                        ratioIndicatorText = "সাধারণ (Fit)"
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                                resizeMode = nextMode
                                showRatioIndicator = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow, // Beautiful generic square play as resize symbol
                                contentDescription = "Resize mode",
                                tint = Color(0xFF00FF87),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Rotate Screen Button
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation = if (isLandscape) {
                                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, // Rotation symbol
                                contentDescription = "Rotate Screen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Overlays: Buffering
                    if (isBuffering && errorMessage == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00FF87),
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "বাফারিং হচ্ছে...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "উচ্চ ক্ষমতার গতি সচল হচ্ছে",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Overlays: Error state
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFF4E4E),
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = errorMessage!!,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        val current = exoPlayer
                                        if (current != null) {
                                            current.prepare()
                                            current.play()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("পুনরায় চেষ্টা করুন (Retry)", color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Remaining details (Diagnostics & Help Text) are hidden in landscape fullscreen mode
                if (!isLandscape) {
                    // Stream Diagnostics Info (Super High-Performance Glass dashboard display)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x1FFFFFFF)), // Translucent glass
                        border = BorderStroke(1.dp, Color(0x1A00FF87)), // Soft glowing green border
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Diagnostics",
                                    tint = Color(0xFF00FF87),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "হাই-পারফরম্যান্স স্ট্রিমিং ডায়াগনস্টিকস",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            DiagnosticRow(
                                label = "স্ট্রীম সোর্স:",
                                value = if (serverIndex == 1) "Server 1 (Primary)" else "Server 2 (Backup)"
                            )
                            DiagnosticRow(
                                label = "রেজোলিউশন / মোড:",
                                value = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "$currentResolution (Fit)"
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "$currentResolution (Zoom)"
                                    else -> "$currentResolution (Stretch)"
                                }
                            )
                            DiagnosticRow(
                                label = "একাধিক বিটরেট (ABR):",
                                value = "সক্রিয় (Active Auto-Fallback)"
                            )
                            DiagnosticRow(
                                label = "হার্ডওয়্যার ডিকোডিং:",
                                value = "GPU তরান্বিত (Direct MediaCodec Core)"
                            )
                            DiagnosticRow(
                                label = "প্রক্সিমাইড বাইপাস:",
                                value = if (viewModel.useProxy.value) "সক্রিয় (Proxy Active)" else "নিষ্ক্রিয় (Direct Connect)"
                            )
                            DiagnosticRow(
                                label = "কাস্টম বাফার সাইজ:",
                                value = "Min: ${minBuffer / 1000}s, Max: ${maxBuffer / 1000}s"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Help instruction text
                    Text(
                        text = "* মোবাইল ঘুরিয়ে বা ডানদিকের রোটেট বাটন চেপে ফুল স্ক্রিন মোডে খেলা দেখতে পারেন। যেকোনো বাফারিংয়ের ক্ষেত্রে S1 ও S2 সার্ভার পরিবর্তন করে পরীক্ষা করুন।",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
