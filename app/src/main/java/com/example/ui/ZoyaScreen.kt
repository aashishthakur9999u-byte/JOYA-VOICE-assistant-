package com.example.ui

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border

import androidx.compose.foundation.BorderStroke
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.collectAsState

import android.content.Context
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear



import com.example.live.VoiceRegistry
import com.example.live.VoiceProfile

@Composable
fun ZoyaScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var apiKey by remember {
        val saved = prefs.getString("api_key", "") ?: ""
        if (saved.isEmpty()) {
            val buildKey = com.example.BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
                prefs.edit().putString("api_key", buildKey).apply()
                mutableStateOf(buildKey)
            } else {
                mutableStateOf("")
            }
        } else {
            mutableStateOf(saved)
        }
    }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null) }
    var showMenu by remember { mutableStateOf(false) }

    var operationMode by remember { mutableStateOf(prefs.getString("operation_mode", "HYBRID") ?: "HYBRID") }
    val isOfflineMode by ZoyaForegroundService.isOfflineModeState.collectAsState()
    val latestUserTranscript by ZoyaForegroundService.latestUserTranscript.collectAsState()
    val latestAssistantResponse by ZoyaForegroundService.latestAssistantResponse.collectAsState()

    var selectedVoiceId by remember { mutableStateOf(prefs.getString("selected_voice", "Aoede") ?: "Aoede") }
    var turboSpeed by remember { mutableStateOf(prefs.getBoolean("turbo_speed", true)) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val currentVoice = remember(selectedVoiceId) { VoiceRegistry.getVoice(selectedVoiceId) }

    fun startServiceDirectly() {
        val intent = Intent(context, ZoyaForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
        serviceStarted = true
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            startServiceDirectly()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun requestPermissionsAndStart() {
        val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasMic && hasContacts && hasPhone) {
            startServiceDirectly()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        ZoyaForegroundService.onStateChange = { state ->
            zoyaState = state
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Z.O.Y.A. 2050",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00F0FF).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "CYBER 4D",
                                color = Color(0xFF00F0FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Text("⚙", color = Color.White, fontSize = 20.sp)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF0A0F24).copy(alpha = 0.98f))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("🎙️ Voice Studio (7 Real Voices)", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showVoiceDialog = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("⚡ Fast Response Speed", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showSpeedDialog = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("📋 View 2050 Hologram Logs", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onNavigateToChat()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("🔑 Gemini API Key (For Online Mode)", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showApiKeyDialog = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("📱 Accessibility Automation", color = Color.White) },
                            onClick = {
                                showMenu = false
                                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF070B19), Color(0xFF0A1128), Color(0xFF04060E))
                    )
                )
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Item 1: 2050 Operation Mode Selector (Auto / Online / Offline)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val modes = listOf("HYBRID" to "⚡ AUTO", "ONLINE" to "🌐 ONLINE", "OFFLINE" to "🛡️ OFFLINE")
                        modes.forEach { (modeKey, label) ->
                            val isSelected = operationMode == modeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.25f) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        if (isSelected) 1.dp else 0.dp,
                                        if (isSelected) Color(0xFF00F0FF) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        operationMode = modeKey
                                        prefs.edit().putString("operation_mode", modeKey).apply()
                                        val s = ZoyaForegroundService.activeService
                                        if (s != null) {
                                            if (modeKey == "OFFLINE") {
                                                s.switchToOfflineMode()
                                            } else {
                                                s.switchToOnlineMode()
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Item 2: Voice Studio Badge
                item {
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable { showVoiceDialog = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎙️ ${currentVoice.name} (${currentVoice.style})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (turboSpeed) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (turboSpeed) "⚡ 2050 TURBO" else "NATURAL",
                                color = if (turboSpeed) Color(0xFF00E676) else Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Item 3: Full 3D/4D Anime Hologram Avatar Viewport
                item {
                    ZoyaAvatar3D(
                        zoyaState = zoyaState,
                        isOfflineMode = isOfflineMode || operationMode == "OFFLINE",
                        onAvatarClick = {
                            val service = ZoyaForegroundService.activeService
                            if (service != null) {
                                service.triggerListen()
                            } else {
                                requestPermissionsAndStart()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Item 4: Realtime Hologram Live Subtitles HUD
                if (latestUserTranscript.isNotEmpty() || latestAssistantResponse.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B132B).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            if (latestUserTranscript.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("YOU:", color = Color(0xFFFF007F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(latestUserTranscript, color = Color.White, fontSize = 13.sp)
                                }
                            }
                            if (latestAssistantResponse.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ZOYA:", color = Color(0xFF00F0FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(latestAssistantResponse, color = Color(0xFF00F0FF), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Item 5: 2050 Cyber Quick Action Matrix
                item {
                    Text(
                        "⚡ 2050 QUANTUM COMMAND MATRIX",
                        color = Color(0xFF00F0FF).copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberCommandChip("📞 Call", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            s?.offlineVoiceEngine?.processOfflineCommand("call")
                        }
                        CyberCommandChip("🔦 Torch", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            s?.offlineVoiceEngine?.processOfflineCommand("torch on")
                        }
                        CyberCommandChip("🔊 Volume", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            s?.offlineVoiceEngine?.processOfflineCommand("volume badhao")
                        }
                        CyberCommandChip("🔆 Bright", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            s?.offlineVoiceEngine?.processOfflineCommand("brightness 80")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberCommandChip("🔋 Battery", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            s?.offlineVoiceEngine?.processOfflineCommand("battery status")
                        }
                        CyberCommandChip("⏰ Time", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            s?.offlineVoiceEngine?.processOfflineCommand("what time is it")
                        }
                        CyberCommandChip("💬 Logs", Modifier.weight(1f)) {
                            onNavigateToChat()
                        }
                        CyberCommandChip("🎙️ Mic", Modifier.weight(1f)) {
                            val s = ZoyaForegroundService.activeService
                            if (s != null) s.triggerListen() else requestPermissionsAndStart()
                        }
                    }
                }

                // Item 6: Main Activation / Disconnect Button
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!serviceStarted) {
                        androidx.compose.material3.Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_zoya_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00F0FF).copy(alpha = 0.2f),
                                contentColor = Color(0xFF00F0FF)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF00F0FF)),
                            onClick = {
                                requestPermissionsAndStart()
                            }
                        ) {
                            Text(
                                "⚡ ACTIVATE ZOYA 2050 (ONLINE & OFFLINE)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val s = ZoyaForegroundService.activeService
                                    s?.triggerListen()
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00F0FF).copy(alpha = 0.2f),
                                    contentColor = Color(0xFF00F0FF)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("🎙️ Speak Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            androidx.compose.material3.Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val intent = Intent(context, ZoyaForegroundService::class.java)
                                    context.stopService(intent)
                                    serviceStarted = false
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFEF9A9A)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Disconnect", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }    
    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key") },
            text = {
                Column {
                    Text("Enter your Gemini API key to use Z.O.Y.A.")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIza...") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            if (tempKey.isNotEmpty()) {
                                androidx.compose.material3.IconButton(onClick = { tempKey = "" }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear text"
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get your API key here",
                        color = Color(0xFF00B0FF),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.edit().putString("api_key", tempKey).apply()
                        apiKey = tempKey
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = {
                Column {
                    Text("Voice Studio", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text("Choose from 7 real Gemini neural voices", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VoiceRegistry.VOICES) { voice ->
                        val isSelected = voice.id.equals(selectedVoiceId, ignoreCase = true)
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVoiceId = voice.id
                                    prefs.edit().putString("selected_voice", voice.id).apply()
                                    if (ZoyaForegroundService.activeService != null) {
                                        ZoyaForegroundService.activeService?.restartSession()
                                    }
                                    showVoiceDialog = false
                                    android.widget.Toast.makeText(context, "Voice switched to ${voice.name}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (voice.gender == "Female") Color(0xFFFF4081).copy(alpha = 0.2f) else Color(0xFF00B0FF).copy(alpha = 0.2f),
                                            androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (voice.gender == "Female") "👩" else "👨",
                                        fontSize = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = voice.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = voice.gender,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = voice.style,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = voice.description,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        lineHeight = 14.sp
                                    )
                                }
                                if (isSelected) {
                                    Text("✓", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Close", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Text("⚡ Speed & Latency Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (turboSpeed) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (turboSpeed) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                turboSpeed = true
                                prefs.edit().putBoolean("turbo_speed", true).apply()
                                if (ZoyaForegroundService.activeService != null) {
                                    ZoyaForegroundService.activeService?.restartSession()
                                }
                                showSpeedDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡ Turbo Speed (Recommended)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                if (turboSpeed) Text("✓", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Fastest turnaround (80ms chunks). Zoya gives instantaneous, concise answers and executes commands without hesitation.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }

                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (!turboSpeed) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (!turboSpeed) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                turboSpeed = false
                                prefs.edit().putBoolean("turbo_speed", false).apply()
                                if (ZoyaForegroundService.activeService != null) {
                                    ZoyaForegroundService.activeService?.restartSession()
                                }
                                showSpeedDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌿 Natural Flow Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                if (!turboSpeed) Text("✓", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Delivers warm, relaxed conversational pacing with slightly more expressive cadence.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Done", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

}



@Composable
fun ZoyaOrb(state: ZoyaState) {
    val radiusScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0.5f) }
    val rotateAngle = remember { Animatable(0f) }
    
    // Ring rotations
    val ring1Angle = remember { Animatable(0f) }
    val ring2Angle = remember { Animatable(120f) }
    val ring3Angle = remember { Animatable(240f) }
    val ring4Angle = remember { Animatable(45f) }

    LaunchedEffect(state) {
        when (state) {
            ZoyaState.IDLE -> {
                radiusScale.animateTo(1f, animationSpec = tween(1000))
                glowAlpha.animateTo(
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            ZoyaState.LISTENING -> {
                radiusScale.animateTo(1.1f, animationSpec = tween(500))
                glowAlpha.animateTo(
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            ZoyaState.THINKING -> {
                radiusScale.animateTo(1.05f, animationSpec = tween(400))
                glowAlpha.animateTo(
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            ZoyaState.SPEAKING -> {
                radiusScale.animateTo(1.2f, animationSpec = tween(200))
                glowAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }

    // Continuous rotation for rings
    LaunchedEffect(Unit) {
        launch {
            ring1Angle.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            ring2Angle.animateTo(
                targetValue = 360f + 120f,
                animationSpec = infiniteRepeatable(
                    animation = tween(7000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            ring3Angle.animateTo(
                targetValue = 360f + 240f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5500, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            ring4Angle.animateTo(
                targetValue = -360f + 45f, // reverse rotation
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = size.minDimension / 4f
            val currentRadius = baseRadius * radiusScale.value
            
            // Core colors based on state
            val coreInnerColor = when (state) {
                ZoyaState.IDLE -> Color(0xFF80D8FF)
                ZoyaState.LISTENING -> Color(0xFFB388FF)
                ZoyaState.THINKING -> Color(0xFFFFD180)
                ZoyaState.SPEAKING -> Color(0xFF69F0AE)
                else -> Color.LightGray
            }
            
            val coreOuterColor = when (state) {
                ZoyaState.IDLE -> Color(0xFF00B0FF)
                ZoyaState.LISTENING -> Color(0xFF651FFF)
                ZoyaState.THINKING -> Color(0xFFFF9100)
                ZoyaState.SPEAKING -> Color(0xFF00E676)
                else -> Color.Gray
            }

            // 1. Ambient Background Glow
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(coreOuterColor.copy(alpha = glowAlpha.value * 0.5f), Color.Transparent),
                    center = center,
                    radius = currentRadius * 2.5f
                ),
                radius = currentRadius * 2.5f
            )

            // 2. The Glass Sphere (Core)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreInnerColor.copy(alpha = 0.8f),
                        coreOuterColor.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.5f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(center.x - currentRadius * 0.3f, center.y - currentRadius * 0.3f),
                    radius = currentRadius * 1.2f
                ),
                radius = currentRadius
            )
            
            // Inner Core Highlight for 3D effect
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                center = androidx.compose.ui.geometry.Offset(center.x - currentRadius * 0.4f, center.y - currentRadius * 0.4f),
                radius = currentRadius * 0.3f
            )

            // 3. Neon Orbital Rings
            val ringRadiusX = currentRadius * 1.8f
            val ringRadiusY = currentRadius * 0.6f
            
            // Helper function to draw a 3D-ish ring
            fun drawNeonRing(angle: Float, startColor: Color, endColor: Color, strokeWidth: Float) {
                rotate(angle, center) {
                    drawOval(
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(startColor, endColor, startColor, Color.Transparent, startColor),
                            center = center
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - ringRadiusX, center.y - ringRadiusY),
                        size = androidx.compose.ui.geometry.Size(ringRadiusX * 2, ringRadiusY * 2),
                        style = Stroke(width = strokeWidth)
                    )
                    // Glow for the ring
                    drawOval(
                        color = startColor.copy(alpha = 0.3f),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - ringRadiusX, center.y - ringRadiusY),
                        size = androidx.compose.ui.geometry.Size(ringRadiusX * 2, ringRadiusY * 2),
                        style = Stroke(width = strokeWidth * 3)
                    )
                }
            }

            // Draw Rings
            val speedMultiplier = if (state == ZoyaState.THINKING || state == ZoyaState.SPEAKING) 2f else 1f
            
            // Red/Pink Ring
            drawNeonRing(ring1Angle.value * speedMultiplier, Color(0xFFFF1744), Color(0xFFD50000), 4f)
            
            // Green/Yellow Ring
            drawNeonRing(ring2Angle.value * speedMultiplier, Color(0xFF00E676), Color(0xFF76FF03), 4f)
            
            // Blue/Cyan Ring
            drawNeonRing(ring3Angle.value * speedMultiplier, Color(0xFF00E5FF), Color(0xFF2979FF), 4f)
            
            // Outer subtle glass ring
            drawNeonRing(ring4Angle.value * speedMultiplier, Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f), 2f)
            
            // 4. Outer Glass Dome Reflection
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.3f)),
                    center = center,
                    radius = currentRadius * 2.2f
                ),
                radius = currentRadius * 2.2f,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()

    androidx.compose.material3.Scaffold(
        containerColor = Color(0xFF1E1E2E),
        topBar = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Button(
                    onClick = onNavigateBack,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF80D8FF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back", color = Color.Black)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "State: ${ZoyaForegroundService.currentState.name}",
                color = Color(0xFF00E5FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.Button(
                onClick = { ZoyaForegroundService.activeService?.reconnectSession() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF80D8FF)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Reconnect", color = Color.Black)
            }
        }
    }
}

@Composable
fun CyberCommandChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF00F0FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
