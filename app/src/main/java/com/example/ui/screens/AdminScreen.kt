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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val firebaseUrl by viewModel.firebaseUrl.collectAsState()

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
    var inputServer3 by remember { mutableStateOf("") }
    var inputServer4 by remember { mutableStateOf("") }
    var inputServer5 by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Football") }
    var inputHeadersJson by remember { mutableStateOf("") }
    var inputLogoUrl by remember { mutableStateOf("") }

    // State for Editing Channels
    var showEditDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }
    var editBackupUrl by remember { mutableStateOf("") }
    var editServer3 by remember { mutableStateOf("") }
    var editServer4 by remember { mutableStateOf("") }
    var editServer5 by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editHeadersJson by remember { mutableStateOf("") }
    var editLogoUrl by remember { mutableStateOf("") }

    // Admin authentication states
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("admin_auth", android.content.Context.MODE_PRIVATE) }
    var isAdminLoggedIn by remember { mutableStateOf(sharedPrefs.getBoolean("is_logged_in", false)) }
    var authEmailInput by remember { mutableStateOf("") }
    var authPasswordInput by remember { mutableStateOf("") }
    var authErrorMsg by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Admin Inputs for Settings (bind to state)
    var proxyInput by remember(proxyUrl) { mutableStateOf(proxyUrl) }
    var firebaseUrlInput by remember(firebaseUrl) { mutableStateOf(firebaseUrl) }

    // Buffer inputs
    var minBufferInput by remember(minBuffer) { mutableStateOf(minBuffer.toString()) }
    var maxBufferInput by remember(maxBuffer) { mutableStateOf(maxBuffer.toString()) }
    var playbackBufferInput by remember(playbackBuffer) { mutableStateOf(playbackBuffer.toString()) }
    var rebufferBufferInput by remember(rebufferBuffer) { mutableStateOf(rebufferBuffer.toString()) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF070B11)
    ) {
        if (!isAdminLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF00FF87)),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF070B11), CircleShape)
                                .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security Lock",
                                tint = Color(0xFF00FF87),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "এডমিন সিকিউরিটি লক",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "শুধুমাত্র অনুমোদিত এডমিন লগইন করতে পারবেন। আপনার একাউন্ট হ্যাকিং প্রতিরোধে এটি অত্যন্ত সুরক্ষিত।",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )

                        if (authErrorMsg != null) {
                            Text(
                                text = authErrorMsg!!,
                                color = Color(0xFFFF4E4E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedTextField(
                            value = authEmailInput,
                            onValueChange = { authEmailInput = it; authErrorMsg = null },
                            label = { Text("ইমেইল এড্রেস (Admin Email)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FF87),
                                focusedLabelColor = Color(0xFF00FF87)
                            )
                        )

                        OutlinedTextField(
                            value = authPasswordInput,
                            onValueChange = { authPasswordInput = it; authErrorMsg = null },
                            label = { Text("পাসওয়ার্ড (Security Password)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Text(
                                        text = if (passwordVisible) "লুকান" else "দেখুন",
                                        color = Color(0xFF00FF87),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FF87),
                                focusedLabelColor = Color(0xFF00FF87)
                            )
                        )

                        Button(
                            onClick = {
                                val email = authEmailInput.trim()
                                val pass = authPasswordInput.trim()
                                if (email == "mp.mywork51@gmail.com" && pass == "LiveKhelaAdmin#2026$!") {
                                    isAdminLoggedIn = true
                                    sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                                    authErrorMsg = null
                                } else {
                                    authErrorMsg = "ভুল ইমেইল অথবা পাসওয়ার্ড! অনুগ্রহ করে আবার চেষ্টা করুন।"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                        ) {
                            Text(
                                text = "নিরাপদ লগইন (Secure Login)",
                                color = Color(0xFF070B11),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        TextButton(onClick = onBack) {
                            Text("ফিরে যান (Back to Home)", color = Color.LightGray)
                        }
                    }
                }
            }
        } else {
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
                        text = "LiveKhela এডমিন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = { viewModel.resetToDefaults() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00FF87))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("রিসেট", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = {
                            isAdminLoggedIn = false
                            sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4E4E))
                    ) {
                        Text("লগআউট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                // 1. Firebase Realtime Database Section
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
                            text = "গুগল ফায়ারবেজ রিয়েল-টাইম ডাটাবেজ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "এখানে Firebase Realtime Database এর REST URL দিন। এডমিন প্যানেলের পরিবর্তনগুলো সাথে সাথে সকল ইউজারের ডিভাইসে সিঙ্ক হয়ে যাবে।",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = firebaseUrlInput,
                            onValueChange = { firebaseUrlInput = it },
                            label = { Text("Firebase Database URL") },
                            placeholder = { Text("https://your-project-id.firebaseio.com") },
                            modifier = Modifier.fillMaxWidth().testTag("firebase_url_field"),
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
                                    viewModel.updateFirebaseSettings(firebaseUrlInput)
                                    viewModel.syncWithFirebase()
                                },
                                modifier = Modifier.weight(1f).testTag("sync_firebase_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                            ) {
                                Text("সিঙ্ক ডাউনলোড", color = Color(0xFF070B11), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateFirebaseSettings(firebaseUrlInput)
                                    viewModel.uploadToFirebase()
                                },
                                modifier = Modifier.weight(1f).testTag("upload_firebase_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2F46))
                            ) {
                                Text("সার্ভারে আপলোড", color = Color.White, fontSize = 11.sp)
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
                channels.forEachIndexed { index, channel ->
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Reorder UP
                            IconButton(
                                onClick = { viewModel.moveChannelUp(channel) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) Color.White else Color.Gray.copy(alpha = 0.3f)
                                )
                            }

                            // Reorder DOWN
                            IconButton(
                                onClick = { viewModel.moveChannelDown(channel) },
                                enabled = index < channels.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (index < channels.size - 1) Color.White else Color.Gray.copy(alpha = 0.3f)
                                )
                            }

                            // EDIT
                            IconButton(
                                onClick = {
                                    editingChannel = channel
                                    editName = channel.name
                                    editUrl = channel.url
                                    editBackupUrl = channel.backupUrl ?: ""
                                    editServer3 = channel.server3 ?: ""
                                    editServer4 = channel.server4 ?: ""
                                    editServer5 = channel.server5 ?: ""
                                    editCategory = channel.category
                                    editLogoUrl = channel.logoUrl ?: ""
                                    editHeadersJson = channel.headersJson ?: ""
                                    showEditDialog = true
                                },
                                modifier = Modifier.size(32.dp).testTag("edit_channel_${channel.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF00FF87),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // DELETE
                            IconButton(
                                onClick = { viewModel.deleteChannel(channel) },
                                modifier = Modifier.size(32.dp).testTag("delete_channel_${channel.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF4E4E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
                            label = { Text("প্রধান স্ট্রীম লিংক / Server 1 (m3u8, ts, mp4)") },
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

                        OutlinedTextField(
                            value = inputServer3,
                            onValueChange = { inputServer3 = it },
                            label = { Text("সার্ভার ৩ লিংক (Server 3)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = inputServer4,
                            onValueChange = { inputServer4 = it },
                            label = { Text("সার্ভার ৪ লিংক (Server 4)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = inputServer5,
                            onValueChange = { inputServer5 = it },
                            label = { Text("সার্ভার ৫ লিংক (Server 5)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        // Category options
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
                                        server3 = if (inputServer3.isBlank()) null else inputServer3,
                                        server4 = if (inputServer4.isBlank()) null else inputServer4,
                                        server5 = if (inputServer5.isBlank()) null else inputServer5,
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
                                inputServer3 = ""
                                inputServer4 = ""
                                inputServer5 = ""
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

        // Edit Channel Dialog Box Overlay
        if (showEditDialog && editingChannel != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = Color(0xFF0F1722),
                title = {
                    Text("চ্যানেল এডিট করুন (Edit Channel)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("চ্যানেলের নাম (Name)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editUrl,
                            onValueChange = { editUrl = it },
                            label = { Text("প্রধান স্ট্রীম লিংক / Server 1 (m3u8, ts, mp4)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editBackupUrl,
                            onValueChange = { editBackupUrl = it },
                            label = { Text("বিকল্প ব্যাকআপ লিংক (Server 2)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editServer3,
                            onValueChange = { editServer3 = it },
                            label = { Text("সার্ভার ৩ লিংক (Server 3)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editServer4,
                            onValueChange = { editServer4 = it },
                            label = { Text("সার্ভার ৪ লিংক (Server 4)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editServer5,
                            onValueChange = { editServer5 = it },
                            label = { Text("সার্ভার ৫ লিংক (Server 5)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("বিভাগ (Cricket, Football, General)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editHeadersJson,
                            onValueChange = { editHeadersJson = it },
                            label = { Text("কাস্টম রিকোয়েস্ট হেডার (JSON)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )

                        OutlinedTextField(
                            value = editLogoUrl,
                            onValueChange = { editLogoUrl = it },
                            label = { Text("থাম্বনেইল লোগো লিংক (Logo/Thumbnail URL)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank() && editUrl.isNotBlank()) {
                                editingChannel?.let { original ->
                                    viewModel.updateChannel(
                                        original.copy(
                                            name = editName,
                                            url = editUrl,
                                            backupUrl = if (editBackupUrl.isBlank()) null else editBackupUrl,
                                            server3 = if (editServer3.isBlank()) null else editServer3,
                                            server4 = if (editServer4.isBlank()) null else editServer4,
                                            server5 = if (editServer5.isBlank()) null else editServer5,
                                            category = editCategory,
                                            logoUrl = if (editLogoUrl.isBlank()) null else editLogoUrl,
                                            headersJson = if (editHeadersJson.isBlank()) null else editHeadersJson
                                        )
                                    )
                                }
                                showEditDialog = false
                                editingChannel = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                    ) {
                        Text("সংরক্ষণ", color = Color(0xFF070B11), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditDialog = false
                        editingChannel = null
                    }) {
                        Text("বাতিল", color = Color.LightGray)
                    }
                }
            )
        }
    }
}
