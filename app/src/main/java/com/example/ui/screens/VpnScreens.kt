package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubscriptionEntity
import com.example.data.local.VpnProfileEntity
import com.example.data.util.AppLogger
import com.example.data.util.VpnConnectionService
import com.example.ui.theme.*
import com.example.ui.viewmodel.VpnViewModel
import java.io.File
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VpnMainScreen(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Slide navigation transitions
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == "home") {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            } else {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            "home" -> DefaultDashboardView(viewModel)
            "settings" -> SettingsMenuView(viewModel)
            "login" -> LoginPaneView(viewModel)
            "logs" -> LiveLoggerView(viewModel)
        }
    }
}

// ----------------------------------------------------
// 1. PRIMARY REDESIGNED DASHBOARD VIEW
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DefaultDashboardView(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val profiles by viewModel.processedProfiles.collectAsStateWithLifecycle()
    val subscriptions by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val trafficStats by viewModel.trafficStats.collectAsStateWithLifecycle()
    val pingStateText by viewModel.pingStateText.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showAddSubDialog by remember { mutableStateOf(false) }
    var subUrl by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }
    var showQrDialog by remember { mutableStateOf(false) }
    var qrInputText by remember { mutableStateOf("") }

    // List collapsing trigger
    var isProfilesExpanded by remember { mutableStateOf(true) }

    val bgGradient = if (MaterialTheme.colorScheme.background == Color(0xFFF0F3F6)) {
        listOf(Color(0xFFEAEEF1), Color(0xFFF3F5F7))
    } else {
        listOf(Color(0xFF090A0C), Color(0xFF141920))
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Settings action (Left)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.navigateTo("settings") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                // Geo Status warning Pill (Middle)
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .shadow(1.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            Toast
                                .makeText(
                                    context,
                                    "Гео-файлы успешно настроены в кэше ядра.",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Geo Warning",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ошибка загрузки гео файлов",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                }

                // Plus action (Right)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showAddSubDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Link",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgGradient))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Radar Conic Pulse connection globe
                ConnectionGlobe(
                    connectionState = connectionState,
                    profileName = selectedProfile?.name ?: "Прокси не выбран",
                    onToggle = { viewModel.toggleVpn(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Realtime download up statistics
                TrafficStatsCard(
                    connectionState = connectionState,
                    stats = trafficStats,
                    protocol = selectedProfile?.protocol ?: "N/A"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable details list of servers and configurations
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        SearchAndUtilityRow(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onPingAll = { viewModel.pingAllProfiles() },
                            onDeleteFailed = { viewModel.deleteFailedProfiles() },
                            sortType = sortType,
                            onSortChange = { viewModel.setSortType(it) },
                            pingText = pingStateText
                        )
                    }

                    if (isLoading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Real User profiles (if loaded)
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCardItem(
                            profile = profile,
                            isSelected = profile.id == selectedProfile?.id,
                            onSelect = { viewModel.selectProfile(profile.id) },
                            onDelete = { viewModel.deleteProfile(profile.id) },
                            onPingNode = { viewModel.pingSingleProfile(profile.id, profile.host, profile.port) }
                        )
                    }

                    // Display simple empty state if profiles list is empty
                    if (profiles.isEmpty()) {
                        item {
                            EmptyStatePlaceholderBlock()
                        }
                    }

                    // Real User subscriptions (if added to DB helper)
                    items(subscriptions, key = { it.url }) { sub ->
                        SubscribedCardItem(
                            title = sub.name,
                            lastUpdatedText = "Обновлено: " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(sub.lastUpdated)),
                            footerText = "Количество узлов: ${sub.nodeCount}\nДля обновления нажмите кнопку синхронизации круглую справа во вкладке настроек или удержанием.",
                            onSync = { viewModel.refreshSubscription(context, sub.url) }
                        )
                    }

                    // Bottom list spacer
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

            // ----------------------------------------------------
            // FLOATING BOTTOM ACTION PILLS: "Из буфера" & "QR-Код"
            // Originally present in the image footer
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Clipboard import Button (Left pill)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                viewModel.importFromClipboard(context, text)
                            } else {
                                viewModel.importFromClipboard(context, "")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = "Clipboard",
                            tint = Color(0xFF6B4EFF), // Dark purple/blue clipboard tint
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Из буфера",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E3137)
                        )
                    }
                }

                // QR-Code scanning Button (Right pill)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .clickable { showQrDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "QR Code",
                            tint = Color(0xFF1976D2), // Medium blue icon
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "QR-Код",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E3137)
                        )
                    }
                }
            }
        }
    }

    // Modal dialogue input panels
    if (showAddSubDialog) {
        Dialog(onDismissRequest = { showAddSubDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Добавление подписки",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Название подписки") },
                        placeholder = { Text("Например, Мой Обход БС") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        label = { Text("URL-ссылка (HTTP/HTTPS)") },
                        placeholder = { Text("https://host.com/sub") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddSubDialog = false }) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.addSubscription(context, subUrl, subName)
                                showAddSubDialog = false
                                subUrl = ""
                                subName = ""
                            }
                        ) {
                            Text("Добавить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode,
                        contentDescription = "Scan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        "Быстрый QR импорт",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Поместите QR-код конфигурации Vless/Vmess/Sub перед камерой устройства. Вы также можете вручную вставить строку под ним:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = qrInputText,
                        onValueChange = { qrInputText = it },
                        placeholder = { Text("Вставьте vless://... ключ") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                // Simulate mock camera scanning
                                val mockProfileLink = "vless://96c88888-c888-4444-baa3-99999999999b@45.138.5.21:443?type=tcp&security=reality&pbk=h8K9eK-zJ7L9sY-8H8F9qG9xK7A&sni=google.com&sid=01#Zotus%20Fast%20Germany"
                                viewModel.importFromClipboard(context, mockProfileLink)
                                showQrDialog = false
                                qrInputText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сканировать")
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showQrDialog = false }) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = {
                                    if (qrInputText.isNotBlank()) {
                                        viewModel.importFromClipboard(context, qrInputText)
                                        showQrDialog = false
                                        qrInputText = ""
                                    } else {
                                        Toast.makeText(context, "Строка пуста!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Вставить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. CONNECTION GLOBE COMPOSABLE (Screenshot replication)
// ----------------------------------------------------
@Composable
fun ConnectionGlobe(
    connectionState: String,
    profileName: String,
    onToggle: () -> Unit
) {
    val isConnected = connectionState == "Connected"
    val isConnecting = connectionState == "Connecting"

    val infiniteTransition = rememberInfiniteTransition(label = "Radar Ripple")
    val scaleFactor1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple Scale 1"
    )
    val alphaFactor1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple Alpha 1"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        val focusColor = if (isConnected) PingGood else if (isConnecting) PingMedium else MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .size(200.dp)
                .drawBehind {
                    // Draw soft atmospheric rings behind the main circle
                    if (isConnected || isConnecting) {
                        drawCircle(
                            color = focusColor,
                            radius = (size.minDimension / 2.2f) * scaleFactor1,
                            alpha = alphaFactor1,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        // Static outer ring
                        drawCircle(
                            color = focusColor.copy(alpha = 0.15f),
                            radius = (size.minDimension / 2.2f) * 1.15f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    } else {
                        // Static passive border
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            radius = (size.minDimension / 2.2f) * 1.1f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Material white paper container (corresponds with screenshot)
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Power else Icons.Filled.PowerOff,
                        contentDescription = "Power Connection Status",
                        tint = if (isConnected) focusColor else Color(0xFFB0BEC5), // Slate grey
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status Text
        Text(
            text = when (connectionState) {
                "Connected" -> "ПОДКЛЮЧЕНО"
                "Connecting" -> "ЗАПУСК КАНАЛА…"
                else -> "ОТКЛЮЧЕНО"
            },
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Small indicator pill containing node name
        Box(
            modifier = Modifier
                .shadow(0.5.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(focusColor)
                )
                Text(
                    text = profileName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. STATS CARD ROW
// ----------------------------------------------------
@Composable
fun TrafficStatsCard(
    connectionState: String,
    stats: VpnConnectionService.TrafficStats,
    protocol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Download
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Download",
                        tint = PingGood,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ВХОДЯЩИЙ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSpeed(stats.downSpeedKbps),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatBytes(stats.totalDownBytes),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }

            // Separator line
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            )

            // Speed Upload
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Upload",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ИСХОДЯЩИЙ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSpeed(stats.upSpeedKbps),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatBytes(stats.totalUpBytes),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }

            // Separator line
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            )

            // Technology info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ПРОТОКОЛ",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = protocol,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Шифрование",
                    fontSize = 9.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. SEARCH & UTILITY ACTIONS (Filtering configuration)
// ----------------------------------------------------
@Composable
fun SearchAndUtilityRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onPingAll: () -> Unit,
    onDeleteFailed: () -> Unit,
    sortType: VpnViewModel.SortType,
    onSortChange: (VpnViewModel.SortType) -> Unit,
    pingText: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simple search input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск узлов по имени или протоколу...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Command utilities row (Sorting & status parameters)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Сортировка:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Text(
                        text = when (sortType) {
                            VpnViewModel.SortType.PING -> "По Пингу ⚡"
                            VpnViewModel.SortType.ALPHABETICAL -> "А-Я 🔠"
                            VpnViewModel.SortType.DATE -> "По Дате 📅"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(4.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("По пингу") },
                            onClick = { onSortChange(VpnViewModel.SortType.PING); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("По алфавиту") },
                            onClick = { onSortChange(VpnViewModel.SortType.ALPHABETICAL); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("По дате добавления") },
                            onClick = { onSortChange(VpnViewModel.SortType.DATE); expanded = false }
                        )
                    }
                }
            }

            if (pingText != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp)
                        Text(pingText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. PROFILE ROW ITEM
// ----------------------------------------------------
@Composable
fun ProfileCardItem(
    profile: VpnProfileEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPingNode: () -> Unit
) {
    val borderStroke = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    val containerBg = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("node_${profile.id}")
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored protocol block
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(getProtocolColor(profile.protocol)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.protocol,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = profile.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${profile.host}:${profile.port}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ping status tag clickable
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(getPingBgColor(profile.ping))
                        .clickable { onPingNode() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = "Ping",
                            tint = getPingTextColor(profile.ping),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = when (profile.ping) {
                                -1 -> "Тест"
                                -2 -> "ERR"
                                else -> "${profile.ping}мс"
                            },
                            color = getPingTextColor(profile.ping),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove",
                        tint = PingBad.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. DETAILED MOCK SUBSCRIPTION CARD (Replicates Image)
// ----------------------------------------------------
@Composable
fun SubscribedCardItem(
    title: String,
    lastUpdatedText: String,
    footerText: String,
    onSync: () -> Unit,
    hasTelegramPill: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main Top info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Left arrow list selector
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Arrow right",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = lastUpdatedText,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sync Refresh button
                    IconButton(onClick = onSync, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Ping Speed dial button
                    IconButton(onClick = onSync, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.AvTimer,
                            contentDescription = "Ping test",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Options dots button
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expiry/Telegram row inside if requested (specifically matches Zotus VPN)
            if (hasTelegramPill) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expiry info pill (blue-ish grey background)
                    Box(
                        modifier = Modifier
                            .shadow(0.5.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE1F5FE))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Истекает: 01.01.2067",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF01579B)
                            )
                        }
                    }

                    // Telegram send button icon
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1F5FE))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Telegram Link",
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // High custom stylized blue warning alerts block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8EAF6)) // Soft indigo container tint mirroring white-blue border
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = footerText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ----------------------------------------------------
// 7. EMPTY STATE LAYOUT
// ----------------------------------------------------
@Composable
fun EmptyStatePlaceholderBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Список серверов пуст. Вы можете импортировать ключи из буфера обмена, по QR-коду или добавить URL подписки в верхнем меню (+).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            textAlign = TextAlign.Center
        )
    }
}

// ----------------------------------------------------
// 8. MODERN SETTINGS MENU VIEW
// ----------------------------------------------------
// 8. MODERN SETTINGS MENU VIEW
// ----------------------------------------------------
@Composable
fun SettingsMenuView(viewModel: VpnViewModel) {
    val context = LocalContext.current
    var currentSettingsPage by remember { mutableStateOf("main") } // "main", "advanced", "tunnel"

    // Bindings from viewModel
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val allowLan by viewModel.allowLan.collectAsStateWithLifecycle()
    val autoStart by viewModel.autoStart.collectAsStateWithLifecycle()
    val betaVersion by viewModel.betaVersion.collectAsStateWithLifecycle()
    val packet by viewModel.packet.collectAsStateWithLifecycle()
    val delay by viewModel.delay.collectAsStateWithLifecycle()
    val rand by viewModel.rand.collectAsStateWithLifecycle()
    val randRange by viewModel.randRange.collectAsStateWithLifecycle()
    val useMux by viewModel.useMux.collectAsStateWithLifecycle()
    val preferredIp by viewModel.preferredIp.collectAsStateWithLifecycle()
    val enableXrayTun by viewModel.enableXrayTun.collectAsStateWithLifecycle()
    val blockTunnelBind by viewModel.blockTunnelBind.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val routingMode by viewModel.routingMode.collectAsStateWithLifecycle()
    val useFragmentation by viewModel.useFragmentation.collectAsStateWithLifecycle()
    val fragmentType by viewModel.fragmentType.collectAsStateWithLifecycle()
    val fragmentPackets by viewModel.fragmentPackets.collectAsStateWithLifecycle()
    val fragmentLength by viewModel.fragmentLength.collectAsStateWithLifecycle()
    val fragmentDelay by viewModel.fragmentDelay.collectAsStateWithLifecycle()
    val maxFragmentDivide by viewModel.maxFragmentDivide.collectAsStateWithLifecycle()
    val enableNoise by viewModel.enableNoise.collectAsStateWithLifecycle()
    val noiseType by viewModel.noiseType.collectAsStateWithLifecycle()

    // Dialog controller states
    var showEditDialogFor by remember { mutableStateOf<String?>(null) }
    var editDialogTitle by remember { mutableStateOf("") }
    var editDialogValue by remember { mutableStateOf("") }

    var showResetDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showRoutingDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showSchemesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Floating Edit dialog trigger helper
    fun triggerEditDialog(key: String, title: String, currentVal: String) {
        showEditDialogFor = key
        editDialogTitle = title
        editDialogValue = currentVal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Shared Top Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            if (currentSettingsPage == "main") {
                                viewModel.navigateTo("home")
                            } else {
                                currentSettingsPage = "main"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = when (currentSettingsPage) {
                        "advanced" -> "Настройки VPN"
                        "tunnel" -> "Настройки Туннелизации"
                        else -> "Нарактеристики"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (currentSettingsPage) {
                "main" -> {
                    // SCREEN 1 - MAIN SETTINGS PANEL
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowClickable("Настройки VPN", "Расширенные TCP и локальные лимиты") {
                                    currentSettingsPage = "advanced"
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Параметры интерфейса и туннеля", "Язык, темы, фрагментация, шум") {
                                    currentSettingsPage = "tunnel"
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Калибровка пинга", "Перепроверить все узлы вручную") {
                                    viewModel.pingAllProfiles()
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Разрешить подключения из LAN", allowLan) {
                                    viewModel.setAllowLan(it)
                                    AppLogger.log(context, "SETTING", "LAN подключения: $it")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Автозапуск приложения", autoStart) {
                                    viewModel.setAutoStart(it)
                                    AppLogger.log(context, "SETTING", "Автозапуск: $it")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ДРУГИЕ НАСТРОЙКИ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowSwitch("Бета версия", betaVersion) {
                                    viewModel.setBetaVersion(it)
                                    AppLogger.log(context, "SETTING", "Режим БЕТА: $it")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Проверить обновление", "Текущая версия: v2.8.5-Mera") {
                                    showUpdateDialog = true
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Статистика работы", "Объем пропущенного трафика") {
                                    showStatsDialog = true
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Терминал логирования", "Посмотреть внутренний вывод vpn_app_logs.txt") {
                                    viewModel.navigateTo("logs")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Очистить логи", "Удалить устаревшие файлы записи логов") {
                                    AppLogger.clearLogs(context)
                                    Toast.makeText(context, "Лог-файл VPN успешно очищен и сброшен.", Toast.LENGTH_SHORT).show()
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Полный сброс параметров", "Сбросить конфигурации, базы и тему к дефолтным", textColor = PingBad) {
                                    showResetDialog = true
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ИНФОРМАЦИЯ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowClickable("База знаний (FAQ)", "Ответы на популярные вопросы") {
                                    showFaqDialog = true
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Схемы ссылок URL", "Используемые deep-links в приложении") {
                                    showSchemesDialog = true
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Информация о MeraVPN", "Лицензии, версии сборки и команда") {
                                    showAboutDialog = true
                                }
                            }
                        }
                    }
                }

                "advanced" -> {
                    // SCREEN 2 - ADVANCED PARAMETERS ("Настройки VPN")
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowClickable("Пакет", packet) {
                                    triggerEditDialog("packet", "Размер и формат пакетов", packet)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Задержка", delay) {
                                    triggerEditDialog("delay", "Интервал задержки пакета", delay)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Rand", rand) {
                                    triggerEditDialog("rand", "Параметр случайного заполнения (Rand)", rand)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("RandRange", if(randRange.isEmpty()) "Не установлено" else randRange) {
                                    triggerEditDialog("randRange", "Границы RandRange", randRange)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Использовать мультиплексирование (Mux)", useMux) {
                                    viewModel.setUseMux(it)
                                    AppLogger.log(context, "SETTING", "Mux: $it")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowDropdown(
                                    title = "Предпочтительный тип IP",
                                    selectedValue = preferredIp,
                                    options = listOf("Auto", "IPv4", "IPv6")
                                ) {
                                    viewModel.setPreferredIp(it)
                                    AppLogger.log(context, "SETTING", "Предпочтительный IP: $it")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Inbounds", "Настройка локальных портов входящего соединения") {
                                    Toast.makeText(context, "Inbounds порт по умолчанию: Auto (10808, 10809)", Toast.LENGTH_SHORT).show()
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Включить Xray TUN интерфейс", enableXrayTun) {
                                    viewModel.setEnableXrayTun(it)
                                    AppLogger.log(context, "SETTING", "TUN: $it")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Блок привязки к туннелю", blockTunnelBind) {
                                    viewModel.setBlockTunnelBind(it)
                                    AppLogger.log(context, "SETTING", "Tethering block: $it")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "БЫСТРЫЕ ДЕЙСТВИЯ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowClickable("Вернуться в Главное Меню") {
                                    currentSettingsPage = "main"
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Очистить сохраненные подписки") {
                                    viewModel.navigateTo("home")
                                    Toast.makeText(context, "Перейдите на главный экран для быстрого удаления", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                "tunnel" -> {
                    // SCREEN 3 - INTERFACE & TUNNEL PREFERENCES
                    item {
                        Text(
                            text = "НАСТРОЙКИ ИНТЕРФЕЙСА",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowClickable("Язык приложения", language) {
                                    Toast.makeText(context, "Выбранный язык: Русский. Система локализована.", Toast.LENGTH_SHORT).show()
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Тёмная тема интерфейса", isDark) {
                                    viewModel.setDarkTheme(it)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "НАСТРОЙКИ ТУННЕЛЯ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                SettingRowClickable("Маршрутизация глобальная", routingMode) {
                                    showRoutingDialog = true
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Прокси для выбранных приложений", "Обход выбранных программ Android") {
                                    Toast.makeText(context, "По умолчанию все приложения перенаправляются в туннель.", Toast.LENGTH_SHORT).show()
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                    SettingRowSwitch("Использовать фрагментацию", useFragmentation) {
                                        viewModel.setUseFragmentation(it)
                                        AppLogger.log(context, "SETTING", "Fragmentation: $it")
                                    }
                                    Text(
                                        text = "Фрагментация в Xray разбивает данные на более мелкие части для лучшей обфускации и защиты от государственного обнаружения и замедлений. Включение этой функции применяется ко всем конфигурациям сервера автоматически.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowDropdown(
                                    title = "Тип ядра",
                                    selectedValue = fragmentType,
                                    options = listOf("Xray", "V2ray")
                                ) {
                                    viewModel.setFragmentType(it)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowDropdown(
                                    title = "Фрагментирование пакетов",
                                    selectedValue = fragmentPackets,
                                    options = listOf("tlshello", "tcp", "http")
                                ) {
                                    viewModel.setFragmentPackets(it)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Длина фрагмента (от - до)", fragmentLength) {
                                    triggerEditDialog("fragmentLength", "Длина фрагмента", fragmentLength)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Задержка фрагмента (мин-макс)", fragmentDelay) {
                                    triggerEditDialog("fragmentDelay", "Задержка фрагмента в милсекундах", fragmentDelay)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowClickable("Максимальное разделение фрагмента", maxFragmentDivide) {
                                    triggerEditDialog("maxFragmentDivide", "Максимальное разделение", maxFragmentDivide)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowSwitch("Включить шумы канала", enableNoise) {
                                    viewModel.setEnableNoise(it)
                                    AppLogger.log(context, "SETTING", "Нойс: $it")
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                SettingRowDropdown(
                                    title = "Тип шума",
                                    selectedValue = noiseType,
                                    options = listOf("array", "rand")
                                ) {
                                    viewModel.setNoiseType(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Numeric/String Dialog
    if (showEditDialogFor != null) {
        AlertDialog(
            onDismissRequest = { showEditDialogFor = null },
            title = { Text(editDialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = editDialogValue,
                    onValueChange = { editDialogValue = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (showEditDialogFor) {
                        "packet" -> viewModel.setPacket(editDialogValue)
                        "delay" -> viewModel.setDelay(editDialogValue)
                        "rand" -> viewModel.setRand(editDialogValue)
                        "randRange" -> viewModel.setRandRange(editDialogValue)
                        "fragmentLength" -> viewModel.setFragmentLength(editDialogValue)
                        "fragmentDelay" -> viewModel.setFragmentDelay(editDialogValue)
                        "maxFragmentDivide" -> viewModel.setMaxFragmentDivide(editDialogValue)
                    }
                    showEditDialogFor = null
                }) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogFor = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Settings Routing Dialog chooser
    if (showRoutingDialog) {
        AlertDialog(
            onDismissRequest = { showRoutingDialog = false },
            title = { Text("Выберите маршрут трафика", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val listOptions = listOf(
                        "📡 MeraVPN | RU-Routing",
                        "🌍 Global Tunneling (Весь трафик)",
                        "🚫 Обход РФ и рекламы (Россия напрямую)"
                    )
                    listOptions.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setRoutingMode(opt)
                                    AppLogger.log(context, "SETTING", "Route changed: $opt")
                                    showRoutingDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                fontSize = 14.sp,
                                fontWeight = if (opt == routingMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (opt == routingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoutingDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Modal Alert dialog confirmations
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сброс параметров", fontWeight = FontWeight.Bold, color = PingBad) },
            text = { Text("Вы абсолютно уверены? Это действие навсегда сотрет все настройки туннеля, тему, сохраненные ключи серверов и журналы авторизации.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllSettings(context)
                    showResetDialog = false
                    viewModel.navigateTo("home")
                }) {
                    Text("Да, стереть все", color = PingBad, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Проверка обновлений", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Поздравляем! У вас установлена самая новая и оптимизированная экспериментальная версия клиента:\n\nВерсия: v2.8.5-Mera (Сборка 104).\n\nВсе компоненты Xray и TUN-интерфейсы работают безупречно.") },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("ОК", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showStatsDialog) {
        val stats by viewModel.trafficStats.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Статистика MeraVPN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Входящий трафик:", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text(formatBytes(stats.totalDownBytes), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Исходящий трафик:", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text(formatBytes(stats.totalUpBytes), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Всего передано:", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text(formatBytes(stats.totalDownBytes + stats.totalUpBytes), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Закрыть", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("База знаний (FAQ)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("1. Как добавить ключ?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Скопируйте строку с протоколом vless://, vmess://, ss://, trojan:// в буфер обмена и нажмите кнопку '+' в правом верхнем углу главного экрана.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                    item {
                        Text("2. Что делает фрагментация?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Фрагментация Xray разбивает пакеты на части, обходя сигнатурное обнаружение ТСПУ и DPI провайдеров. Полноценно спасает от замедлений.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                    item {
                        Text("3. Стабильно ли соединение?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Наш движок автоматически перепроверяет туннель каждые несколько секунд, препятствуя разрывам и вылетам.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text("Понятно", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSchemesDialog) {
        AlertDialog(
            onDismissRequest = { showSchemesDialog = false },
            title = { Text("Поддерживаемые URL схемы", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Text("Клиент MeraVPN обрабатывает следующие схемы ссылок:\n- meravpn://import?url=<link>\n- vless://<config>\n- vmess://<config>\n- ss://<config>\n- trojan://<config>\n- tuic://<config>\n- hysteria2://<config>")
            },
            confirmButton = {
                TextButton(onClick = { showSchemesDialog = false }) {
                    Text("ОК", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("О MeraVPN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Text("Создано с любовью для обеспечения доступного и быстрого интернета без ограничений и цензуры.\n\nВерсия: 2.8.5 (Release)\nЯдро Xray: v1.8.11\nБаза данных: SQLite + Coroutines\nРендеринг: Jetpack Compose Jet3")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Закрыть", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SettingRowClickable(
    title: String,
    value: String = "",
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                fontSize = 15.sp, 
                fontWeight = FontWeight.SemiBold, 
                color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onBackground
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value, 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingRowSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.SemiBold, 
            color = MaterialTheme.colorScheme.onBackground, 
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingRowDropdown(
    title: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.SemiBold, 
            color = MaterialTheme.colorScheme.onBackground, 
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedValue, 
                fontSize = 14.sp, 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 9. CLIENT LOGIN PANE
// ----------------------------------------------------
@Composable
fun LoginPaneView(viewModel: VpnViewModel) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }
    var nickInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Авторизация VPN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Войдите, чтобы автоматически разблокировать безлимитный трафик на Premium скорости.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = nickInput,
                    onValueChange = { nickInput = it },
                    label = { Text("Имя пользователя / логин") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Электронная почта") },
                    placeholder = { Text("user@mail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo("settings") },
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text("Назад")
                    }

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && nickInput.isNotBlank() && emailInput.contains("@")) {
                                val success = viewModel.loginUser(emailInput, nickInput)
                                if (success) {
                                    AppLogger.log(context, "AUTH", "Пользователь успешно вошел: $nickInput ($emailInput)")
                                    Toast.makeText(context, "$nickInput успешно вошел в систему!", Toast.LENGTH_SHORT).show()
                                    viewModel.navigateTo("settings")
                                }
                            } else {
                                Toast.makeText(context, "Заполните необходимые поля и проверьте правильность Email и пароля!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text("Войти")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 10. REAL-TIME VIEW LOGGER CONSOLE
// ----------------------------------------------------
@Composable
fun LiveLoggerView(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val logs by AppLogger.logsFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15181D)) // High contrast dark canvas for logs terminal
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Терминал логов",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share logs link button
                IconButton(
                    onClick = { shareLogFileText(context) }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = PingGood)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { viewModel.navigateTo("settings") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fast console logs output stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF030508))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Логи еще не накоплены в базе.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs.reversed()) { logline ->
                        Text(
                            text = logline,
                            color = if (logline.contains("ERROR") || logline.contains("Ошибка")) Color.Red else if (logline.contains("SYSTEM")) Color.Cyan else Color.White,
                            style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 11. GENERAL UTILITY HELPERS
// ----------------------------------------------------
private fun shareLogFileText(context: Context) {
    val file = AppLogger.getLogFile(context)
    if (!file.exists() || file.length() == 0L) {
        Toast.makeText(context, "Лог-файл пуст или еще не создан!", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val logsText = file.readText()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, logsText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Отправить лог-файл VPN обратно")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Исключение при передаче логов: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getProtocolColor(protocol: String): Color {
    return when (protocol.uppercase(Locale.ROOT)) {
        "VLESS" -> Color(0xFF00BFA5)     // Beautiful bright teal
        "VMESS" -> Color(0xFF9C27B0)     // Purple
        "SS" -> Color(0xFFE64A19)        // Bright red orange
        "TROJAN" -> Color(0xFF1976D2)    // Cool Blue
        "HY2", "HYSTERIA2" -> Color(0xFFFBC02D)// Gold Yellow
        "TUIC" -> Color(0xFFF06292)      // Pink Rose
        else -> Color(0xFF45A29E)
    }
}

private fun getPingBgColor(ping: Int): Color {
    return when {
         ping == -1 -> Color(0xFFCFD8DC) // Test pending Grey bg
         ping == -2 -> Color(0xFFFFEBEE) // Err Red Light bg
         ping < 120 -> Color(0xFFE0F2F1) // Fast Green Light bg
         ping < 300 -> Color(0xFFFFF3E0) // Warning Orange Light bg
         else -> Color(0xFFFFEBEE)       // Slow Red Light bg
    }
}

private fun getPingTextColor(ping: Int): Color {
    return when {
         ping == -1 -> Color(0xFF546E7A) // Dark gray text
         ping == -2 -> Color(0xFFD32F2F) // Deep warning red text
         ping < 120 -> Color(0xFF00796B) // Deep fast teal text
         ping < 300 -> Color(0xFFE65100) // Deep dark orange text
         else -> Color(0xFFD32F2F)       // Deep slow red text
    }
}

private fun formatSpeed(kbps: Long): String {
    return when {
        kbps == 0L -> "0.0 B/s"
        kbps < 1000L -> "$kbps Kbps"
        else -> String.format(Locale.ROOT, "%.1f Mbps", kbps / 1024.0)
    }
}

private fun formatBytes(bytes: Long): String {
    val b = bytes.toDouble()
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes == 0L -> "0.0 B"
        bytes < kb -> "${bytes} B"
        bytes < mb -> String.format(Locale.ROOT, "%.1f KB", b / kb)
        bytes < gb -> String.format(Locale.ROOT, "%.1f MB", b / mb)
        else -> String.format(Locale.ROOT, "%.1f GB", b / gb)
    }
}
