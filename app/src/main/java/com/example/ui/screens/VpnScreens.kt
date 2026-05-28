package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubscriptionEntity
import com.example.data.local.VpnProfileEntity
import com.example.ui.viewmodel.VpnViewModel

// Theme Palette (Vibrant Dynamic Themes)
var isDarkThemeState by mutableStateOf(false)

val DarkBackground: Color get() = if (isDarkThemeState) Color(0xFF121214) else Color(0xFFF5F7FA)
val SurfaceCard: Color get() = if (isDarkThemeState) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
val GlowGreen: Color get() = if (isDarkThemeState) Color(0xFF00E676) else Color(0xFF10B981)
val ElectricBlue: Color get() = if (isDarkThemeState) Color(0xFF00B0FF) else Color(0xFF007BFF)
val BrightText: Color get() = if (isDarkThemeState) Color(0xFFFFFFFF) else Color(0xFF1A1A1E)
val MutedText: Color get() = if (isDarkThemeState) Color(0xFFA0A0A5) else Color(0xFF6B7280)

@Composable
fun VpnMainScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier,
    onRequestPrepareVpn: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE) }
    remember {
        isDarkThemeState = sharedPrefs.getBoolean("is_dark_theme", false)
        true
    }
    
    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Server List, 2: Subscriptions, 3: Settings
    
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 10.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Dashboard") },
                    label = { Text("Статус", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = SurfaceCard.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Servers") },
                    label = { Text("Серверы", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = SurfaceCard.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.RssFeed, contentDescription = "Subscriptions") },
                    label = { Text("Подписки", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = SurfaceCard.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Настройки", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = SurfaceCard.copy(alpha = 0.5f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> DashboardTab(viewModel, context, onRequestPrepareVpn)
                1 -> ServersTab(viewModel)
                2 -> SubscriptionsTab(viewModel)
                3 -> SettingsTab(viewModel)
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: VpnViewModel,
    context: Context,
    onRequestPrepareVpn: () -> Unit
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val bytesRx by viewModel.bytesReceived.collectAsStateWithLifecycle()
    val bytesTx by viewModel.bytesTransmitted.collectAsStateWithLifecycle()
    val durationSec by viewModel.durationSec.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "Radar glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title
        Text(
            text = "HAPP VPN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BrightText,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Glow Shield status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(230.dp * (if (isConnected || isConnecting) pulseScale else 1.0f))
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = if (isConnected) GlowGreen else ElectricBlue,
                        spotColor = if (isConnected) GlowGreen else ElectricBlue
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SurfaceCard,
                                SurfaceCard.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .border(
                        width = 4.dp,
                        color = when {
                            isConnected -> GlowGreen
                            isConnecting -> ElectricBlue.copy(alpha = 0.6f)
                            else -> MutedText.copy(alpha = 0.4f)
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        viewModel.toggleVpnConnection(context, onRequestPrepareVpn)
                    }
                    .testTag("connect_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Power else Icons.Filled.PowerOff,
                        contentDescription = "Power status",
                        tint = when {
                            isConnected -> GlowGreen
                            isConnecting -> ElectricBlue
                            else -> MutedText
                        },
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            isConnected -> "ПОДКЛЮЧЕНО"
                            isConnecting -> "СОЕДИНЕНИЕ..."
                            else -> "ПОДКЛЮЧИТЬ"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isConnected -> GlowGreen
                            isConnecting -> ElectricBlue
                            else -> BrightText
                        }
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatDuration(durationSec),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlowGreen
                        )
                    }
                }
            }
        }

        // Active selected server display card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Выбранный сервер",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedProfile?.name ?: "Выберите сервер во вкладке...",
                        color = BrightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedProfile != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${selectedProfile?.protocol} • ${selectedProfile?.server}:${selectedProfile?.port}",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedProfile != null) ElectricBlue.copy(alpha = 0.15f)
                            else SurfaceCard.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedProfile?.countryCode ?: "UN",
                        color = if (selectedProfile != null) ElectricBlue else MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Real-time Traffic throughput statistics panels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upload bytes statistics
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Upload speed",
                        tint = ElectricBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Отправлено", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = formatBytes(bytesTx),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Download bytes statistics
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Download speed",
                        tint = GlowGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Скачано", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = formatBytes(bytesRx),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// FORMAT HELPER
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0.00 KB"
    val k = 1024.0
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = Math.floor(Math.log(bytes.toDouble()) / Math.log(k)).toInt()
    val cleanIdx = i.coerceIn(0, 4)
    val value = bytes / Math.pow(k, cleanIdx.toDouble())
    return String.format("%.2f %s", value, sizes[cleanIdx])
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}


@Composable
fun ServersTab(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val profilesList by viewModel.profiles.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()

    var customName by remember { mutableStateOf("") }
    var customHost by remember { mutableStateOf("") }
    var customPort by remember { mutableStateOf("443") }
    var customProtocol by remember { mutableStateOf("VLESS") } // VLESS, VMESS, SHADOWSOCKS, TROJAN
    var showAddForm by remember { mutableStateOf(false) }

    // EXPLICIT GROUPING BY PROTOCOL TYPE per requested constraint
    val groupedProfiles = remember(profilesList) {
        profilesList.groupBy { it.protocol.uppercase() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Доступные серверы",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightText
                )
                
                IconButton(onClick = { showAddForm = !showAddForm }) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Manual Form Toggle",
                        tint = ElectricBlue
                    )
                }
            }
        }

        if (showAddForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Добавить сервер вручную", color = BrightText, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Название (например: Германия-01)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightText,
                                unfocusedTextColor = BrightText,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = MutedText
                            )
                        )

                        OutlinedTextField(
                            value = customHost,
                            onValueChange = { customHost = it },
                            label = { Text("Хост / IP") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightText,
                                unfocusedTextColor = BrightText,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = MutedText
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customPort,
                                onValueChange = { customPort = it },
                                label = { Text("Порт") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = BrightText,
                                    unfocusedTextColor = BrightText,
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = MutedText
                                )
                            )

                            // Protocol options selector row
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Протокол", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val protocols = listOf("VLESS", "VMESS", "SS", "TROJAN")
                                    protocols.forEach { proto ->
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (customProtocol == proto || (photoMatches(
                                                            customProtocol,
                                                            proto
                                                        ))
                                                    ) ElectricBlue else SurfaceCard.copy(alpha = 0.5f)
                                                )
                                                .clickable {
                                                    customProtocol = if (proto == "SS") "SHADOWSOCKS" else proto
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(proto, color = BrightText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (customName.isNotBlank() && customHost.isNotBlank()) {
                                    viewModel.addCustomProfile(
                                        name = customName,
                                        host = customHost,
                                        port = customPort.toIntOrNull() ?: 443,
                                        protocol = customProtocol
                                    )
                                    customName = ""
                                    customHost = ""
                                    customPort = "443"
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_custom_server_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text("Сохранить", color = BrightText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Empty state check
        if (groupedProfiles.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.NetworkCheck, "Empty List", tint = MutedText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Список серверов пуст", color = BrightText, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Импортируйте подписку или добавьте сервер вручную, чтобы начать работу с VPN.",
                        color = MutedText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            // RENDER EXPLICIT GROUPS BY PROTOCOLS
            val sortedProtocols = listOf("VLESS", "VMESS", "SHADOWSOCKS", "TROJAN")
            sortedProtocols.forEach { protocolName ->
                val list = groupedProfiles[protocolName]
                if (list != null && list.isNotEmpty()) {
                    item {
                        Text(
                            text = when (protocolName) {
                                "VMESS" -> "VMess Серверы"
                                "VLESS" -> "VLESS Серверы"
                                "SHADOWSOCKS" -> "Shadowsocks Серверы"
                                "TROJAN" -> "Trojan Серверы"
                                else -> protocolName
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(list) { profile ->
                        ServerCardItem(
                            profile = profile,
                            isSelected = selectedProfile?.id == profile.id,
                            onSelect = { viewModel.selectProfile(profile) },
                            onPingTest = { viewModel.testProfilePing(context, profile) },
                            onDelete = { viewModel.deleteProfile(profile.id) }
                        )
                    }
                }
            }
        }
    }
}

fun photoMatches(custom: String, proto: String): Boolean {
    if (custom == "SHADOWSOCKS" && proto == "SS") return true
    return custom == proto
}

@Composable
fun ServerCardItem(
    profile: VpnProfileEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPingTest: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) ElectricBlue else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceCard.copy(alpha = 0.9f) else SurfaceCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Latency Badge country symbol
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.1f))
                ) {
                    Text(
                        profile.countryCode,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = profile.name,
                        color = BrightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.server}:${profile.port}",
                        color = MutedText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Right elements: Latency metric and settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Ping trigger button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard.copy(alpha = 0.5f))
                        .clickable { onPingTest() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SignalCellularAlt,
                            contentDescription = "Ping Icon",
                            tint = when {
                                profile.pingMs == -1 -> MutedText
                                profile.pingMs < 100 -> GlowGreen
                                else -> ElectricBlue
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (profile.pingMs == -1) "?" else "${profile.pingMs} ms",
                            color = when {
                                profile.pingMs == -1 -> MutedText
                                profile.pingMs < 100 -> GlowGreen
                                else -> BrightText
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Delete profile button
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Server",
                        tint = MutedText.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionsTab(viewModel: VpnViewModel) {
    val subsList by viewModel.subscriptions.collectAsStateWithLifecycle()

    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Мои Подписки", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)

        // URL Subscription input panel builder
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Обновить или добавить подписку", color = BrightText, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Название подписки (например: Premium)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrightText,
                        unfocusedTextColor = BrightText,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = MutedText
                    )
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Ссылка на подписку (URL) или ключ/конфиг (vless://, vmess://, ss://, trojan:// или Base64)") },
                    placeholder = { Text("Вставьте ссылку или ключ доступа...", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrightText,
                        unfocusedTextColor = BrightText,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = MutedText
                    )
                )

                if (statusMessage.isNotBlank()) {
                    Text(
                        statusMessage,
                        fontSize = 12.sp,
                        color = if (statusMessage.contains("успешно")) GlowGreen else ElectricBlue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && urlInput.isNotBlank()) {
                            isLoading = true
                            statusMessage = "Загрузка серверов..."
                            viewModel.addSubscription(nameInput, urlInput) { success ->
                                isLoading = false
                                if (success) {
                                    statusMessage = "Подписка успешно обновлена!"
                                    nameInput = ""
                                    urlInput = ""
                                } else {
                                    statusMessage = "Ошибка подключения. Созданы базовые конфигурации."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_sub_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        disabledContainerColor = SurfaceCard.copy(alpha = 0.5f)
                    ),
                    enabled = !isLoading
                ) {
                    Text("Импортировать", color = BrightText, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subscriptions List Title
        Text("Список активных подписок", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)

        if (subsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Список подписок пуст", color = MutedText, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(subsList) { sub ->
                    SubscriptionCardItem(
                        subscription = sub,
                        onDelete = { viewModel.deleteSubscription(sub.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionCardItem(
    subscription: SubscriptionEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Web,
                    contentDescription = "Subscription type",
                    tint = ElectricBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(subscription.name, color = BrightText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subscription.url, color = MutedText, fontSize = 11.sp, maxLines = 1)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete sub",
                    tint = MutedText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE) }
    
    // Persistent values
    var isDark by remember { mutableStateOf(sharedPrefs.getBoolean("is_dark_theme", false)) }
    var dnsServer by remember { mutableStateOf(sharedPrefs.getString("dns_server", "1.1.1.1") ?: "1.1.1.1") }
    var mtuSize by remember { mutableStateOf(sharedPrefs.getInt("mtu_size", 1400)) }
    var realPingOnly by remember { mutableStateOf(sharedPrefs.getBoolean("real_ping_only", true)) }
    
    // For Backup actions
    var rawSerializedBackup by remember { mutableStateOf("") }
    var pasteBackupInput by remember { mutableStateOf("") }
    var importStatusMsg by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrightText)
        }

        // DESIGN SYSTEM & THEME
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Внешний вид", color = BrightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Темная тема оформления", color = BrightText, fontSize = 14.sp)
                            Text("Переключить режим приложения", color = MutedText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { checked ->
                                isDark = checked
                                isDarkThemeState = checked
                                sharedPrefs.edit().putBoolean("is_dark_theme", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }
                }
            }
        }

        // CONNECTION TUNNEL SETTINGS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Параметры соединения", color = BrightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // DNS Server Option
                    Column {
                        Text("DNS Сервер", color = BrightText, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dnsOptions = listOf("1.1.1.1", "8.8.8.8", "Система")
                            dnsOptions.forEach { option ->
                                val selected = when (option) {
                                    "Система" -> dnsServer == "system"
                                    else -> dnsServer == option
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) ElectricBlue else SurfaceCard.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            val value = if (option == "Система") "system" else option
                                            dnsServer = value
                                            sharedPrefs.edit().putString("dns_server", value).apply()
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Color.Transparent else MutedText.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = option,
                                        color = if (selected) Color.White else BrightText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // MTU Configuration
                    Column {
                        Text("Размер MTU (MTU Size)", color = BrightText, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mtuSize.toString(),
                            onValueChange = { newVal ->
                                val intVal = newVal.toIntOrNull() ?: 1400
                                mtuSize = intVal
                                sharedPrefs.edit().putInt("mtu_size", intVal).apply()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightText,
                                unfocusedTextColor = BrightText,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = MutedText
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Честный пинг серверов", color = BrightText, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Скрывать симуляцию, показывать только реальный отклик портов", color = MutedText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = realPingOnly,
                            onCheckedChange = { checked ->
                                realPingOnly = checked
                                sharedPrefs.edit().putBoolean("real_ping_only", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }
                }
            }
        }

        // BACKUPS INTEGRATION IN SETTINGS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Резервное копирование (Бэкап)", color = BrightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Экспортируйте или импортируйте резервной YAML/JSON код настроек.", color = MutedText, fontSize = 11.sp)

                    if (rawSerializedBackup.isNotBlank()) {
                        OutlinedTextField(
                            value = rawSerializedBackup,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = GlowGreen),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = MutedText
                            )
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.exportBackupData { json ->
                                rawSerializedBackup = json
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (rawSerializedBackup.isBlank()) "Создать бэкап" else "Обновить бэкап",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MutedText.copy(alpha = 0.2f)))

                    OutlinedTextField(
                        value = pasteBackupInput,
                        onValueChange = { pasteBackupInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("Вставьте JSON код бэкапа...", color = MutedText) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = BrightText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MutedText
                        )
                    )

                    if (importStatusMsg.isNotBlank()) {
                        Text(
                            importStatusMsg,
                            fontSize = 12.sp,
                            color = if (importStatusMsg.contains("успешно")) GlowGreen else ElectricBlue
                        )
                    }

                    Button(
                        onClick = {
                            if (pasteBackupInput.isNotBlank()) {
                                viewModel.importBackupData(pasteBackupInput) { success ->
                                    if (success) {
                                        importStatusMsg = "Конфигурация успешно восстановлена!"
                                        pasteBackupInput = ""
                                    } else {
                                        importStatusMsg = "Ошибка импорта. Проверьте ваш JSON."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Импортировать бэкап", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
