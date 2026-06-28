package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChannelEntity
import com.example.ui.viewmodel.StreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: StreamViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channels.collectAsState()
    val useProxy by viewModel.useProxy.collectAsState()
    val proxyUrl by viewModel.proxyUrl.collectAsState()
    val supabaseUrl by viewModel.supabaseUrl.collectAsState()
    val supabaseKey by viewModel.supabaseKey.collectAsState()

    val minBuffer by viewModel.minBufferMs.collectAsState()
    val maxBuffer by viewModel.maxBufferMs.collectAsState()
    val playbackBuffer by viewModel.bufferForPlaybackMs.collectAsState()
    val rebufferBuffer by viewModel.bufferForPlaybackAfterRebufferMs.collectAsState()

    val syncStatus by viewModel.syncStatus.collectAsState()

    // Temporary form inputs for Add Channel
    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var inputBackupUrl by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Football") }
    var inputHeadersJson by remember { mutableStateOf("") }
    var inputLogoUrl by remember { mutableStateOf("") }

    // Admin Inputs for Settings (bind to state)
    var proxyInput by remember(proxyUrl) { mutableStateOf(proxyUrl) }
    var supabaseUrlInput by remember(supabaseUrl) { mutableStateOf(supabaseUrl) }
    var supabaseKeyInput by remember(supabaseKey) { mutableStateOf(supabaseKey) }

    // Buffer inputs
    var minBufferInput by remember(minBuffer) { mutableStateOf(minBuffer.toString()) }
    var maxBufferInput by remember(maxBuffer) { mutableStateOf(maxBuffer.toString()) }
    var playbackBufferInput by remember(playbackBuffer) { mutableStateOf(playbackBuffer.toString()) }
    var rebufferBufferInput by remember(rebufferBuffer) { mutableStateOf(rebufferBuffer.toString()) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF070B11)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("admin_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "LiveKhela এডমিন প্যানেল",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = { viewModel.resetToDefaults() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00FF87))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রিসেট (Reset)", fontSize = 12.sp)
                }
            }

            // Sync Status Banner if active
            if (syncStatus != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F46)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = syncStatus!!,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.clearStatus() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00FF87))
                        ) {
                            Text("ঠিক আছে", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Supabase Dynamic Links Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2F46))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Supabase ডাইনামিক ডাটাবেজ লিংক",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "এখানে Supabase এর ডাটাবেজ REST URL এবং API Key দিয়ে সরাসরি রিয়েল-টাইম চ্যানেল তালিকা সিঙ্ক করতে পারেন।",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = supabaseUrlInput,
                            onValueChange = { supabaseUrlInput = it },
                            label = { Text("Supabase API URL") },
                            placeholder = { Text("https://xxxx.supabase.co") },
                            modifier = Modifier.fillMaxWidth().testTag("supabase_url_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF87),
                                focusedLabelColor = Color(0xFF00FF87),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = supabaseKeyInput,
                            onValueChange = { supabaseKeyInput = it },
                            label = { Text("Supabase Anon Key") },
                            modifier = Modifier.fillMaxWidth().testTag("supabase_key_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF87),
                                focusedLabelColor = Color(0xFF00FF87),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            singleLine = true
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateSupabaseSettings(supabaseUrlInput, supabaseKeyInput)
                                    viewModel.syncWithSupabase()
                                },
                                modifier = Modifier.weight(1f).testTag("sync_supabase_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                            ) {
                                Text("সিঙ্ক করুন (Sync Now)", color = Color(0xFF070B11), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateSupabaseSettings(supabaseUrlInput, supabaseKeyInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2F46))
                            ) {
                                Text("সংরক্ষণ", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 2. Reverse Proxy Bypass Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2F46))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "রিভার্স প্রক্সি বাইপাস (Reverse Proxy)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = useProxy,
                                onCheckedChange = { viewModel.updateProxySettings(it, proxyInput) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF070B11),
                                    checkedTrackColor = Color(0xFF00FF87)
                                )
                            )
                        }

                        Text(
                            text = "এটি সক্রিয় করলে সমস্ত অনিরাপদ HTTP এবং .ts স্ট্রিম আপনার নিজের হোস্টেড নিরাপদ HTTPS রিভার্স প্রক্সি সার্ভার দিয়ে রি-রুট হবে।",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = proxyInput,
                            onValueChange = { proxyInput = it },
                            label = { Text("Proxy End Point (with ?url=)") },
                            placeholder = { Text("https://my-proxy.com/proxy?url=") },
                            modifier = Modifier.fillMaxWidth().testTag("proxy_url_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF87),
                                focusedLabelColor = Color(0xFF00FF87),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.updateProxySettings(useProxy, proxyInput)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2F46))
                        ) {
                            Text("প্রক্সি সেটিংস আপডেট করুন", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // 3. High Performance Custom Buffer Tuning
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2F46))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ExoPlayer উচ্চ ক্ষমতা সম্পন্ন বাফার টিউনিং",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ExoPlayer এর বাফার এবং থ্রেশহোল্ড প্যারামিটার টিউন করুন (মিলি-সেকেন্ডে)। এটি অতিরিক্ত ট্রাফিকের সময় বাফারিং সমস্যা সম্পূর্ণ নির্মূল করবে।",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = minBufferInput,
                                onValueChange = { minBufferInput = it },
                                label = { Text("Min Buffer (ms)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                            )
                            OutlinedTextField(
                                value = maxBufferInput,
                                onValueChange = { maxBufferInput = it },
                                label = { Text("Max Buffer (ms)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = playbackBufferInput,
                                onValueChange = { playbackBufferInput = it },
                                label = { Text("Playback (ms)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                            )
                            OutlinedTextField(
                                value = rebufferBufferInput,
                                onValueChange = { rebufferBufferInput = it },
                                label = { Text("Rebuffer (ms)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                            )
                        }

                        Button(
                            onClick = {
                                val min = minBufferInput.toIntOrNull() ?: 5000
                                val max = maxBufferInput.toIntOrNull() ?: 15000
                                val play = playbackBufferInput.toIntOrNull() ?: 1500
                                val reb = rebufferBufferInput.toIntOrNull() ?: 3000
                                viewModel.updateBufferSettings(min, max, play, reb)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("update_buffers_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                        ) {
                            Text("বাফার সেটিংস আপডেট করুন (Save Buffers)", color = Color(0xFF070B11), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // 4. Manual Channels List (CRUD Header)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "চ্যানেল তালিকা (${channels.size} টি)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF070B11), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("যোগ করুন", color = Color(0xFF070B11), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Dynamic channel items display
                channels.forEach { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131D2A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Category: ${channel.category} | ${if (channel.isLive) "LIVE" else "OFFLINE"}",
                                color = Color(0xFF00FF87),
                                fontSize = 11.sp
                            )
                            Text(
                                text = channel.url,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteChannel(channel) },
                            modifier = Modifier.testTag("delete_channel_${channel.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF4E4E)
                            )
                        }
                    }
                }
            }
        }

        // Add Channel Dialog Box Overlay
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color(0xFF0F1722),
                title = {
                    Text("নতুন লাইভ চ্যানেল যোগ করুন", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("চ্যানেলের নাম (Name)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            label = { Text("প্রধান স্ট্রীম লিংক (m3u8, ts, mp4)") },
                            placeholder = { Text("http://example.com/stream.m3u8") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = inputBackupUrl,
                            onValueChange = { inputBackupUrl = it },
                            label = { Text("বিকল্প ব্যাকআপ লিংক (Server 2)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        // Category Dropdown options
                        OutlinedTextField(
                            value = inputCategory,
                            onValueChange = { inputCategory = it },
                            label = { Text("বিভাগ (Cricket, Football, General)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = inputHeadersJson,
                            onValueChange = { inputHeadersJson = it },
                            label = { Text("কাস্টম রিকোয়েস্ট হেডার (JSON)") },
                            placeholder = { Text("{\"User-Agent\": \"AndroidPlayer\"}") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = inputLogoUrl,
                            onValueChange = { inputLogoUrl = it },
                            label = { Text("থাম্বনেইল লোগো লিংক (Logo/Thumbnail URL)") },
                            placeholder = { Text("https://example.com/logo.png") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputName.isNotBlank() && inputUrl.isNotBlank()) {
                                viewModel.addNewChannel(
                                    ChannelEntity(
                                        name = inputName,
                                        url = inputUrl,
                                        backupUrl = if (inputBackupUrl.isBlank()) null else inputBackupUrl,
                                        category = inputCategory,
                                        logoUrl = if (inputLogoUrl.isBlank()) null else inputLogoUrl,
                                        headersJson = if (inputHeadersJson.isBlank()) null else inputHeadersJson,
                                        isLive = true
                                    )
                                )
                                showAddDialog = false
                                // Clear inputs
                                inputName = ""
                                inputUrl = ""
                                inputBackupUrl = ""
                                inputHeadersJson = ""
                                inputLogoUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                    ) {
                        Text("সংরক্ষণ", color = Color(0xFF070B11), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("বাতিল", color = Color.LightGray)
                    }
                }
            )
        }
    }
}
