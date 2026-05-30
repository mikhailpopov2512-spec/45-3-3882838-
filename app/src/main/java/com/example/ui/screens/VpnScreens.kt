package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.*
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
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 12.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Dashboard") },
                    label = { Text("Статус", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = ElectricBlue.copy(alpha = 0.12f),
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Dns, contentDescription = "Servers") },
                    label = { Text("Серверы", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = ElectricBlue.copy(alpha = 0.12f),
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
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
                        indicatorColor = ElectricBlue.copy(alpha = 0.12f),
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
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
                        indicatorColor = ElectricBlue.copy(alpha = 0.12f),
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
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
                0 -> DashboardTab(
                    viewModel = viewModel,
                    context = context,
                    onRequestPrepareVpn = onRequestPrepareVpn,
                    onNavigateToSettings = { activeTab = 3 }
                )
                1 -> ServersTab(viewModel)
                2 -> SubscriptionsTab(viewModel)
                3 -> SettingsTab(
                    viewModel = viewModel,
                    onBack = { activeTab = 0 }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: VpnViewModel,
    context: Context,
    onRequestPrepareVpn: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val bytesRx by viewModel.bytesReceived.collectAsStateWithLifecycle()
    val bytesTx by viewModel.bytesTransmitted.collectAsStateWithLifecycle()
    val durationSec by viewModel.durationSec.collectAsStateWithLifecycle()

    val currentIp by viewModel.currentIp.collectAsStateWithLifecycle()
    val currentCountry by viewModel.currentCountry.collectAsStateWithLifecycle()
    val downloadSpeedKbps by viewModel.downloadSpeedKbps.collectAsStateWithLifecycle()
    val uploadSpeedKbps by viewModel.uploadSpeedKbps.collectAsStateWithLifecycle()

    val profilesList by viewModel.profiles.collectAsStateWithLifecycle()
    val subsList by viewModel.subscriptions.collectAsStateWithLifecycle()
    val vpnLogs by viewModel.vpnLogs.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Dialog state controllers
    var showAddSubDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "Radar glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val rotationTransition = rememberInfiniteTransition(label = "Spinning halo")
    val rotationAngle by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "haloAngle"
    )

    if (showAddSubDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddSubDialog = false },
            onResult = { name, url ->
                showAddSubDialog = false
                viewModel.addSubscription(name, url) { success ->
                    if (success) {
                        android.widget.Toast.makeText(context, "$name успешно добавлена!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Импортировано с базовыми настройками", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showQrScannerDialog) {
        QrScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onResult = { payload ->
                showQrScannerDialog = false
                viewModel.addSubscription("QR Sub", payload) { success ->
                    if (success) {
                        android.widget.Toast.makeText(context, "QR Конфигурация импортирована!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Профиль добавлен по QR коду", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-Fidelity Header Section matched to first photo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = BrightText,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "HAPP VPN",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrightText,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showAddSubDialog = true },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Subscription",
                    tint = BrightText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Concentric pulsing central trigger power dial from photo
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(260.dp)
                .fillMaxWidth(),
        ) {
            // Faint outer rings simulating depth
            Box(
                modifier = Modifier
                    .size(246.dp)
                    .clip(CircleShape)
                    .background(MutedText.copy(alpha = 0.03f))
                    .border(1.dp, MutedText.copy(alpha = 0.05f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(MutedText.copy(alpha = 0.04f))
                    .border(1.dp, MutedText.copy(alpha = 0.06f), CircleShape)
            )

            // Dynamic Spinning outer glowing neon ring when active
            if (isConnected || isConnecting) {
                Box(
                    modifier = Modifier
                        .size(228.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ElectricBlue,
                                    GlowGreen,
                                    ElectricBlue.copy(alpha = 0.2f),
                                    GlowGreen.copy(alpha = 0.2f),
                                    ElectricBlue
                                )
                            ),
                            shape = CircleShape
                        )
                        .rotate(rotationAngle)
                )
            }

            // Core Power Button Circle (Styled in Light/Gray Mode)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(175.dp * (if (isConnected || isConnecting) pulseScale else 1.0f))
                    .shadow(
                        elevation = if (isConnected) 16.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = if (isConnected) GlowGreen else MutedText,
                        spotColor = if (isConnected) GlowGreen else MutedText
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 4.dp,
                        color = when {
                            isConnected -> GlowGreen
                            isConnecting -> ElectricBlue
                            else -> Color(0xFFEAEAEE)
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        viewModel.toggleVpnConnection(context, onRequestPrepareVpn)
                    }
                    .testTag("connect_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "Power Connection Trigger",
                        tint = when {
                            isConnected -> GlowGreen
                            isConnecting -> ElectricBlue
                            else -> Color(0xFFCBCAD2)
                        },
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when {
                            isConnected -> "ОТКЛЮЧИТЬ"
                            isConnecting -> "СОЕДИНЕНИЕ"
                            else -> "ПОДКЛЮЧИТЬ"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isConnected -> GlowGreen
                            isConnecting -> ElectricBlue
                            else -> Color(0xFF8E8D96)
                        }
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDuration(durationSec),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GlowGreen
                        )
                    }
                }
            }
        }

        // Subscriptions Section (Displays exact mockup card named 'subscribe 1' styles)
        if (subsList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Нет активных подписок",
                        fontWeight = FontWeight.Bold,
                        color = BrightText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Создайте подписку кнопкой '+' вверху или Clipboard / QR-Code внизу.",
                        color = MutedText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                subsList.forEach { sub ->
                    val subProfiles = profilesList.filter { it.subscriptionId == sub.id }
                    DashboardSubscriptionItem(
                        sub = sub,
                        subProfiles = subProfiles,
                        selectedProfile = selectedProfile,
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }

        // Live IP detector and secure shield state card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) GlowGreen.copy(alpha = 0.12f)
                                else ElectricBlue.copy(alpha = 0.1f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Filled.Shield else Icons.Filled.PrivacyTip,
                            contentDescription = "Shield Indicator",
                            tint = if (isConnected) GlowGreen else ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column {
                        Text(
                            text = if (isConnected) "Ваш VPN IP" else "Твой Реальный IP",
                            color = MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = currentIp,
                            color = BrightText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentCountry,
                            color = if (isConnected) GlowGreen else MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ЗАЩИТА",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isConnected) "АКТИВНА" else "ОТКЛЮЧЕНА",
                        color = if (isConnected) GlowGreen else Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Active selected server display card with Auto-Select accelerator option
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Выбранная локация",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedProfile?.name ?: "Локация не выбрана",
                        color = BrightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedProfile != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${selectedProfile?.protocol} • ${selectedProfile?.server}:${selectedProfile?.port}",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    } else if (profilesList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Выберите сервер во вкладке «Серверы»",
                            color = ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // If profiles exist but none is selected, offer a quick tap button
                if (selectedProfile == null && profilesList.isNotEmpty()) {
                    Button(
                        onClick = {
                            val fastest = profilesList.filter { it.pingMs > 0 }.minByOrNull { it.pingMs }
                                ?: profilesList.firstOrNull()
                            if (fastest != null) {
                                viewModel.selectProfile(fastest)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("АВТО", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedProfile != null) ElectricBlue.copy(alpha = 0.15f)
                                else SurfaceCard.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
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
        }

        // Live Real-time speeds metrics gauge grid
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upload stats
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Upload direction",
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ОТПРАВКА", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isConnected) String.format("%.1f KB/s", uploadSpeedKbps) else "0.0 KB/s",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BrightText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatBytes(bytesTx),
                        fontSize = 11.sp,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Download stats
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Download direction",
                            tint = GlowGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("СКАЧИВАНИЕ", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isConnected) String.format("%.1f KB/s", downloadSpeedKbps) else "0.0 KB/s",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BrightText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatBytes(bytesRx),
                        fontSize = 11.sp,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Beautiful import actions rows from mockup image (Clipboard and QR-Code side-by-side)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clipboard import pill button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    viewModel.addSubscription("Clipboard Sub", text) { success ->
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Импорт из буфера завершен!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Ошибка разбора! Добавлен демонстрационный сервер.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Буфер обмена пуст!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Нет данных в буфере!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Предоставьте доступ к буферу обмена", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFE5E5EA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "Clipboard Paste",
                        tint = Color(0xFF5E5CE6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clipboard",
                        color = BrightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // QR-Code import pill button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showQrScannerDialog = true },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFE5E5EA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = "QR Scanner",
                        tint = Color(0xFF5E5CE6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QR-Code",
                        color = BrightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Ray Engine Live Logs Console Terminal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isConnected) GlowGreen.copy(alpha = 0.35f)
                            else if (isConnecting) ElectricBlue.copy(alpha = 0.35f)
                            else MutedText.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F13))
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnected) GlowGreen 
                                    else if (isConnecting) ElectricBlue 
                                    else MutedText.copy(alpha = 0.5f)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ЛОГИ ЯДРА (SING-BOX CORE v1.9)",
                            color = BrightText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "ОЧИСТИТЬ",
                        color = ElectricBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            viewModel.clearLogs()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFF070709), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val logScrollState = rememberScrollState()
                    LaunchedEffect(vpnLogs.size) {
                        try {
                            logScrollState.animateScrollTo(logScrollState.maxValue)
                        } catch (e: Exception) {
                            // Safe fallback
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(logScrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (vpnLogs.isEmpty()) {
                            Text(
                                text = "Ожидание событий...",
                                color = MutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            vpnLogs.forEach { logLine ->
                                val textColor = when {
                                    logLine.contains("[Core]", ignoreCase = true) -> GlowGreen
                                    logLine.contains("[System]", ignoreCase = true) -> GlowGreen
                                    logLine.contains("[Probe]", ignoreCase = true) -> Color(0xFFFFB300)
                                    logLine.contains("[Tunnel]", ignoreCase = true) -> ElectricBlue
                                    logLine.contains("[Route]", ignoreCase = true) -> Color(0xFFCE93D8)
                                    else -> MutedText
                                }
                                Text(
                                    text = logLine,
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSubscriptionItem(
    sub: com.example.data.local.SubscriptionEntity,
    subProfiles: List<com.example.data.local.VpnProfileEntity>,
    selectedProfile: com.example.data.local.VpnProfileEntity?,
    viewModel: VpnViewModel,
    context: Context
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    val rotationTransition = rememberInfiniteTransition(label = "Sync Spin")
    val rotationAngleSync by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "syncAngle"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left chevron and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand Status",
                        tint = MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sub.name,
                            color = BrightText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatAddedAt(sub.addedAt),
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                }

                // Action buttons: Sync, Ping test, three dots options
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            isSyncing = true
                            viewModel.addSubscription(sub.name, sub.url) { success ->
                                isSyncing = false
                                if (success) {
                                    android.widget.Toast.makeText(context, "Подписка ${sub.name} обновлена!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Локальные профили успешно восстановлены.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync",
                            tint = Color(0xFF5E5CE6),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isSyncing) rotationAngleSync else 0f)
                        )
                    }

                    IconButton(
                        onClick = {
                            android.widget.Toast.makeText(context, "Тестирование пинга серверов подписки...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.testAllPings(context)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Ping Test",
                            tint = GlowGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = MutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(SurfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Обновить", color = BrightText) },
                                onClick = {
                                    showMenu = false
                                    isSyncing = true
                                    viewModel.addSubscription(sub.name, sub.url) { _ -> isSyncing = false }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deleteSubscription(sub.id)
                                    android.widget.Toast.makeText(context, "Подписка удалена", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Expand profiles matching subscriptionId
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MutedText.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                if (subProfiles.isEmpty()) {
                    Text(
                        text = "В подписке нет доступных серверов.",
                        fontSize = 12.sp,
                        color = MutedText,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subProfiles.forEach { profile ->
                            val isSelected = selectedProfile?.id == profile.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF5E5CE6).copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { viewModel.selectProfile(profile) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Country flag symbol wrapper
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color(0xFF5E5CE6).copy(alpha = 0.15f)
                                                else MutedText.copy(alpha = 0.08f)
                                            )
                                    ) {
                                        Text(
                                            text = profile.countryCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) Color(0xFF5E5CE6) else BrightText
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = profile.name,
                                            color = if (isSelected) Color(0xFF5E5CE6) else BrightText,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "${profile.protocol} • ${profile.server}:${profile.port}",
                                            color = MutedText,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SignalCellularAlt,
                                        contentDescription = "Signal indicator",
                                        tint = when {
                                            profile.pingMs == -1 -> MutedText
                                            profile.pingMs < 100 -> GlowGreen
                                            else -> Color(0xFF5E5CE6)
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onResult: (name: String, url: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF5E5CE6))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Новая подписка", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Название подписки") },
                    placeholder = { Text("например: MyPremium", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrightText,
                        unfocusedTextColor = BrightText,
                        focusedBorderColor = Color(0xFF5E5CE6),
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f)
                    )
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Ссылка (URL) или Ключ доступа") },
                    placeholder = { Text("https://example.com/sub или key...", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrightText,
                        unfocusedTextColor = BrightText,
                        focusedBorderColor = Color(0xFF5E5CE6),
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank() && urlInput.isNotBlank()) {
                        onResult(nameInput, urlInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6)),
                enabled = nameInput.isNotBlank() && urlInput.isNotBlank()
            ) {
                Text("Добавить", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MutedText)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    
    val infiniteTransition = rememberInfiniteTransition(label = "Laser scanner")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = -85f,
        targetValue = 85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "laserOffset"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color(0xFF5E5CE6))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сканирование QR-кода", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Наведите камеру на QR-код подписки или конфигурации vless//, vmess//, ss//. Вы также можете вставить код вручную ниже.", fontSize = 12.sp, color = MutedText, textAlign = TextAlign.Center)
                
                // Visual simulation camera box
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(2.dp, Color(0xFF5E5CE6), RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = laserYOffset.dp)
                            .background(Color.Red)
                            .shadow(4.dp, spotColor = Color.Red)
                    )
                    
                    Icon(
                        Icons.Filled.CropFree,
                        contentDescription = null,
                        tint = Color(0xFF5E5CE6).copy(alpha = 0.3f),
                        modifier = Modifier.size(100.dp)
                    )
                }
                
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Код конфигурации или URL") },
                    placeholder = { Text("Вставьте ссылку или payload...", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrightText,
                        unfocusedTextColor = BrightText,
                        focusedBorderColor = Color(0xFF5E5CE6),
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f)
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            rawText = "vless://de2-frank@de2.happvpn.site:443?type=ws#🇩🇪 DE Frankfurt Gateway"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6).copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Демо-код", fontSize = 11.sp, color = Color(0xFF5E5CE6))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        onResult(rawText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6))
            ) {
                Text("Импорт", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MutedText)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(24.dp)
    )
}

fun formatAddedAt(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return format.format(date)
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

                Text(
                    "Или выберите бесплатный сервер обхода:",
                    fontSize = 11.sp,
                    color = MutedText,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            nameInput = "Bypass DE"
                            urlInput = "vless://de-sub@de1.happvpn.site:443#🇩🇪 DE Frankfurt VLESS"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue.copy(alpha = 0.15f),
                            contentColor = ElectricBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🇩🇪 DE Free", fontSize = 11.sp, color = ElectricBlue)
                    }
                    Button(
                        onClick = {
                            nameInput = "Bypass US"
                            urlInput = "vmess://eyJhZGQiOiJ1czEuaGFwcHZwaS5zaXRlIiwicG9ydCI6NDQzLCJpZCI6ImRlMSIsInBzIjoiVVMgTmV3WW9yayJ9"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue.copy(alpha = 0.15f),
                            contentColor = ElectricBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🇺🇸 US Free", fontSize = 11.sp, color = ElectricBlue)
                    }
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
fun SettingsTab(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE) }

    // State bindings
    var language by remember { mutableStateOf(sharedPrefs.getString("language", "Русский") ?: "Русский") }
    var appAppearance by remember { mutableStateOf(sharedPrefs.getString("app_appearance", "Auto") ?: "Auto") }
    var routingProfile by remember { mutableStateOf(sharedPrefs.getString("routing_mode", "all") ?: "all") }
    
    var fragmentationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("fragmentation_enabled", false)) }
    var muxEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("mux_enabled", false)) }
    var preferredIpType by remember { mutableStateOf(sharedPrefs.getString("preferred_ip_type", "Auto") ?: "Auto") }

    var allowLan by remember { mutableStateOf(sharedPrefs.getBoolean("allow_lan", false)) }
    var autoStartVpn by remember { mutableStateOf(sharedPrefs.getBoolean("auto_start_vpn", false)) }

    // Base tunnel configurations
    var dnsServer by remember { mutableStateOf(sharedPrefs.getString("dns_server", "1.1.1.1") ?: "1.1.1.1") }
    var mtuSize by remember { mutableStateOf(sharedPrefs.getInt("mtu_size", 1400)) }
    var realPingOnly by remember { mutableStateOf(sharedPrefs.getBoolean("real_ping_only", true)) }
    var dnsLeakProtected by remember { mutableStateOf(sharedPrefs.getBoolean("dns_leak_protected", true)) }

    // App lists for Per-App Proxy Settings
    val systemAppsList = remember { listOf("Chrome", "Telegram", "YouTube", "WhatsApp", "Instagram", "Firefox") }
    var proxySelectedApps by remember {
        mutableStateOf(sharedPrefs.getStringSet("proxy_selected_apps", setOf("Telegram", "Instagram")) ?: setOf("Telegram", "Instagram"))
    }

    // Dialog controllers
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showUiDetailsDialog by remember { mutableStateOf(false) }
    var showRoutingDialog by remember { mutableStateOf(false) }
    var showPerAppDialog by remember { mutableStateOf(false) }
    var showPreferredIpDialog by remember { mutableStateOf(false) }
    var showVpnDetailsDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showPingDialog by remember { mutableStateOf(false) }

    // Serialized backup outputs
    var rawSerializedBackup by remember { mutableStateOf("") }
    var pasteBackupInput by remember { mutableStateOf("") }
    var importStatusMsg by remember { mutableStateOf("") }

    // Intercept theme changer instantly on appearance modification
    fun changeAppearanceState(mode: String) {
        appAppearance = mode
        sharedPrefs.edit().putString("app_appearance", mode).apply()
        when (mode) {
            "Light theme" -> {
                isDarkThemeState = false
                sharedPrefs.edit().putBoolean("is_dark_theme", false).apply()
            }
            "Dark theme" -> {
                isDarkThemeState = true
                sharedPrefs.edit().putBoolean("is_dark_theme", true).apply()
            }
            else -> {
                isDarkThemeState = false
                sharedPrefs.edit().putBoolean("is_dark_theme", false).apply()
            }
        }
    }

    // Language selector Dialogue
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Выберите язык / Language", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Русский", "English", "Deutsch").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (language == lang) Color(0xFF5E5CE6).copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    language = lang
                                    sharedPrefs.edit().putString("language", lang).apply()
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang, color = BrightText, fontSize = 15.sp)
                            if (language == lang) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF5E5CE6))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Закрыть", color = Color(0xFF5E5CE6))
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Appearance selector Dialogue
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("App Appearance", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto", "Light theme", "Dark theme").forEach { arg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (appAppearance == arg) Color(0xFF5E5CE6).copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    changeAppearanceState(arg)
                                    showAppearanceDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(arg, color = BrightText, fontSize = 15.sp)
                            if (appAppearance == arg) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF5E5CE6))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Закрыть", color = Color(0xFF5E5CE6))
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // General UI Details Popup
    if (showUiDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showUiDetailsDialog = false },
            title = { Text("Параметры Интерфейса", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("HAPP VPN использует высокопроизводительный движок рендеринга Jetpack Compose на базе графического API Vulkan/OpenGL.", fontSize = 13.sp, color = BrightText)
                    Text("Шрифты:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrightText)
                    Text("• Метрика заголовков: Inter\n• Консоль трафика: JetBrains Mono", fontSize = 12.sp, color = MutedText)
                }
            },
            confirmButton = {
                Button(onClick = { showUiDetailsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6))) {
                    Text("ОК", color = Color.White)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Routing Profile selector
    if (showRoutingDialog) {
        AlertDialog(
            onDismissRequest = { showRoutingDialog = false },
            title = { Text("Профили маршрутизации", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to "Проксировать весь трафик (Global)",
                        "bypass_rf" to "Обход блокировок РФ (Bypass RU)",
                        "selective" to "Выборочная фильтрация сайтов (RKN Blocklist)"
                    ).forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (routingProfile == pair.first) Color(0xFF5E5CE6).copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    routingProfile = pair.first
                                    sharedPrefs.edit().putString("routing_mode", pair.first).apply()
                                    showRoutingDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pair.second, color = BrightText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            if (routingProfile == pair.first) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF5E5CE6), modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRoutingDialog = false }) {
                    Text("Отмена", color = Color(0xFF5E5CE6))
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Per-app Proxy selector
    if (showPerAppDialog) {
        AlertDialog(
            onDismissRequest = { showPerAppDialog = false },
            title = { Text("Прокси по приложениям", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VPN туннель будет включаться только для отмеченных приложений:", fontSize = 12.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    systemAppsList.forEach { appName ->
                        val isChecked = proxySelectedApps.contains(appName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) Color(0xFF5E5CE6).copy(alpha = 0.05f) else Color.Transparent)
                                .clickable {
                                    val newSet = proxySelectedApps.toMutableSet()
                                    if (isChecked) newSet.remove(appName) else newSet.add(appName)
                                    proxySelectedApps = newSet
                                    sharedPrefs.edit().putStringSet("proxy_selected_apps", newSet).apply()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(appName, color = BrightText, fontSize = 14.sp)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    val newSet = proxySelectedApps.toMutableSet()
                                    if (isChecked) newSet.remove(appName) else newSet.add(appName)
                                    proxySelectedApps = newSet
                                    sharedPrefs.edit().putStringSet("proxy_selected_apps", newSet).apply()
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5E5CE6))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPerAppDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6))) {
                    Text("Готово", color = Color.White)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Preferred IP selector
    if (showPreferredIpDialog) {
        AlertDialog(
            onDismissRequest = { showPreferredIpDialog = false },
            title = { Text("Preferred IP Type", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("IPv4 Only", "IPv6 Only", "Auto").forEach { arg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (preferredIpType == arg) Color(0xFF5E5CE6).copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    preferredIpType = arg
                                    sharedPrefs.edit().putString("preferred_ip_type", arg).apply()
                                    showPreferredIpDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(arg, color = BrightText, fontSize = 15.sp)
                            if (preferredIpType == arg) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF5E5CE6))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPreferredIpDialog = false }) {
                    Text("Закрыть", color = Color(0xFF5E5CE6))
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // MTU/DNS Leak dialogues
    if (showVpnDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showVpnDetailsDialog = false },
            title = { Text("Параметры соединения VPN", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = mtuSize.toString(),
                        onValueChange = {
                            val parsed = it.toIntOrNull() ?: 1400
                            mtuSize = parsed
                            sharedPrefs.edit().putInt("mtu_size", parsed).apply()
                        },
                        label = { Text("Размер пакета MTU") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrightText,
                            unfocusedTextColor = BrightText,
                            focusedBorderColor = Color(0xFF5E5CE6)
                        )
                    )

                    OutlinedTextField(
                        value = dnsServer,
                        onValueChange = {
                            dnsServer = it
                            sharedPrefs.edit().putString("dns_server", it).apply()
                        },
                        label = { Text("Сервер DNS") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrightText,
                            unfocusedTextColor = BrightText,
                            focusedBorderColor = Color(0xFF5E5CE6)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Защита от утечки DNS (DNS Leak)", color = BrightText, fontSize = 13.sp)
                        Switch(
                            checked = dnsLeakProtected,
                            onCheckedChange = {
                                dnsLeakProtected = it
                                sharedPrefs.edit().putBoolean("dns_leak_protected", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF5E5CE6))
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showVpnDetailsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6))) {
                    Text("Сохранить", color = Color.White)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Ping parameters controller
    if (showPingDialog) {
        AlertDialog(
            onDismissRequest = { showPingDialog = false },
            title = { Text("Параметры зондирования портов", color = BrightText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Показывать честный пинг без симуляции", color = BrightText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = realPingOnly,
                            onCheckedChange = {
                                realPingOnly = it
                                sharedPrefs.edit().putBoolean("real_ping_only", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF5E5CE6))
                        )
                    }
                    Text("Когда фича включена, пинг проверяет реальную скорость соединения сокета TCP без задержки.", fontSize = 11.sp, color = MutedText)
                }
            },
            confirmButton = {
                Button(onClick = { showPingDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6))) {
                    Text("Готово", color = Color.White)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Main Scaffold layout of Settings showing a beautiful layout matched 100% to photo!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // High-Fidelity App Top Bar matching the photo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = BrightText,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BrightText
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Category: UI Settings
            item {
                SettingsCategoryHeader("UI Settings")
            }
            item {
                SettingsClickableRow("Language", valueText = language) {
                    showLanguageDialog = true
                }
            }
            item {
                SettingsClickableRow("App appearance", valueText = appAppearance, hasUpDownArrows = true) {
                    showAppearanceDialog = true
                }
            }
            item {
                SettingsClickableRow("UI Settings") {
                    showUiDetailsDialog = true
                }
            }

            // Divider matching space
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Category: Tunnel Settings
            item {
                SettingsCategoryHeader("Tunnel Settings")
            }
            item {
                val profileText = when (routingProfile) {
                    "all" -> "Global"
                    "bypass_rf" -> "Bypass RU"
                    else -> "RKN Selective"
                }
                SettingsClickableRow("Routing profiles", valueText = profileText) {
                    showRoutingDialog = true
                }
            }
            item {
                SettingsClickableRow("Per-app Proxy Settings", valueText = "${proxySelectedApps.size} apps") {
                    showPerAppDialog = true
                }
            }
            item {
                SettingsSwitchRow("Enable Fragmentation", checked = fragmentationEnabled) {
                    fragmentationEnabled = it
                    sharedPrefs.edit().putBoolean("fragmentation_enabled", it).apply()
                }
            }
            item {
                SettingsSwitchRow("Enable Mux", checked = muxEnabled) {
                    muxEnabled = it
                    sharedPrefs.edit().putBoolean("mux_enabled", it).apply()
                }
            }
            item {
                SettingsClickableRow("Preferred IP Type", valueText = preferredIpType, hasUpDownArrows = true) {
                    showPreferredIpDialog = true
                }
            }

            // Divider matching space
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Category: Advanced Settings
            item {
                SettingsCategoryHeader("Advanced Settings")
            }
            item {
                SettingsClickableRow("VPN Settings", valueText = "MTU $mtuSize") {
                    showVpnDetailsDialog = true
                }
            }
            item {
                SettingsClickableRow("Subscription", valueText = "Manage") {
                    showSubscriptionDialog = true
                }
            }
            item {
                SettingsClickableRow("Ping", valueText = if (realPingOnly) "Real" else "Simulated") {
                    showPingDialog = true
                }
            }
            item {
                SettingsSwitchRow("Allow connections from the LAN", checked = allowLan) {
                    allowLan = it
                    sharedPrefs.edit().putBoolean("allow_lan", it).apply()
                }
            }
            item {
                SettingsSwitchRow("Auto-Start VPN", checked = autoStartVpn) {
                    autoStartVpn = it
                    sharedPrefs.edit().putBoolean("auto_start_vpn", it).apply()
                }
            }

            // BACKUPS INTEGRATION IN SETTINGS FOR EXTRA RECOVERY FUNCTIONS
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    focusedBorderColor = Color(0xFF5E5CE6),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E5CE6)),
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
                                focusedBorderColor = Color(0xFF5E5CE6),
                                unfocusedBorderColor = MutedText
                            )
                        )

                        if (importStatusMsg.isNotBlank()) {
                            Text(
                                importStatusMsg,
                                fontSize = 12.sp,
                                color = if (importStatusMsg.contains("успешно")) GlowGreen else Color(0xFF5E5CE6)
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
}

@Composable
fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF5352E4), // Beautiful clean lavender purple matching high fidelity photo!
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsClickableRow(
    title: String,
    valueText: String? = null,
    hasUpDownArrows: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = SurfaceCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = BrightText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (valueText != null) {
                    Text(
                        text = valueText,
                        color = MutedText,
                        fontSize = 14.sp
                    )
                }
                if (hasUpDownArrows) {
                    Icon(
                        imageVector = Icons.Filled.UnfoldMore,
                        contentDescription = null,
                        tint = MutedText.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MutedText.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = BrightText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF5E5CE6) // Purple matching photo
                )
            )
        }
    }
}
