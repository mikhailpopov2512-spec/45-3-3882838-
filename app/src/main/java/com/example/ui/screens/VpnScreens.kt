package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubscriptionEntity
import com.example.data.local.VpnProfileEntity
import com.example.data.util.VpnConnectionService
import com.example.ui.theme.*
import com.example.ui.viewmodel.VpnViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnMainScreen(viewModel: VpnViewModel) {
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

    var showAddSubDialog by remember { mutableStateOf(false) }
    var subUrl by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }

    var showManageSubsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Shield Logo",
                            tint = CyberCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                "HAPP VPN",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Quantum Connect",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberTeal,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Clipboard Fast Import Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                viewModel.importFromClipboard(context, text)
                            } else {
                                viewModel.importFromClipboard(context, "")
                            }
                        },
                        modifier = Modifier.testTag("import_clipboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentPaste,
                            contentDescription = "Вставить ключ",
                            tint = CyberCyan
                        )
                    }

                    // Add Subscription URL Dialog Trigger
                    IconButton(
                        onClick = { showAddSubDialog = true },
                        modifier = Modifier.testTag("add_subscription_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddLink,
                            contentDescription = "Добавить подписку",
                            tint = CyberCyan
                        )
                    }

                    // Manage Subscripions Trigger Button
                    IconButton(onClick = { showManageSubsDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FolderSpecial,
                            contentDescription = "Инфо о подписках",
                            tint = CyberLightGray
                        )
                    }

                    // Auto-Switch to Best Ping Node Button
                    IconButton(
                        onClick = { viewModel.autoSwitchToBest(context) },
                        modifier = Modifier.testTag("auto_switch_best")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Выбрать лучший пинг",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        },
        containerColor = CyberBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Neon connection ripple ring / visual button
            ConnectionGlobe(
                connectionState = connectionState,
                profileName = selectedProfile?.name ?: "Узлы не добавлены",
                onToggle = { viewModel.toggleVpn(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Speeds & statistics dashboard
            TrafficStatsCard(
                connectionState = connectionState,
                stats = trafficStats,
                protocol = selectedProfile?.protocol ?: "N/A"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Filtering, search & utilities row
            SearchAndUtilityRow(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onPingAll = { viewModel.pingAllProfiles() },
                onDeleteFailed = { viewModel.deleteFailedProfiles() },
                sortType = sortType,
                onSortChange = { viewModel.setSortType(it) },
                pingText = pingStateText
            )

            Spacer(modifier = Modifier.height(10.dp))

            // List of Node keys & profiles
            Text(
                text = "Доступные прокси узлы (${profiles.size})",
                style = MaterialTheme.typography.bodyLarge,
                color = CyberLightGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = CyberCyan,
                    trackColor = CyberCard
                )
            }

            if (profiles.isEmpty()) {
                EmptyStateLayout()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCardItem(
                            profile = profile,
                            isSelected = profile.id == selectedProfile?.id,
                            onSelect = { viewModel.selectProfile(profile.id) },
                            onDelete = { viewModel.deleteProfile(profile.id) },
                            onPingNode = { viewModel.pingSingleProfile(profile.id, profile.host, profile.port) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Modal: Add Subscription URL Input
    if (showAddSubDialog) {
        Dialog(onDismissRequest = { showAddSubDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Импорт подписки",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Название (например, Сервер 1)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberLightGray,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = CyberLightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        label = { Text("URL-ссылка на подписку") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberLightGray,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = CyberLightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddSubDialog = false }) {
                            Text("Отмена", color = CyberLightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.addSubscription(context, subUrl, subName)
                                showAddSubDialog = false
                                subUrl = ""
                                subName = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBg)
                        ) {
                            Text("Добавить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Manage subscriptions and manual list updating
    if (showManageSubsDialog) {
        Dialog(onDismissRequest = { showManageSubsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Управление подписками",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showManageSubsDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = CyberLightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (subscriptions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Нет добавленных подписок",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CyberLightGray
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(subscriptions, key = { it.url }) { sub ->
                                SubscriptionItemRow(
                                    sub = sub,
                                    onUpdate = { viewModel.refreshSubscription(context, sub.url) },
                                    onDelete = { viewModel.deleteSubscription(sub.url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Glowing pulsing concentric ring dashboard control
@Composable
fun ConnectionGlobe(
    connectionState: String,
    profileName: String,
    onToggle: () -> Unit
) {
    val isConnected = connectionState == "Connected"
    val isConnecting = connectionState == "Connecting"

    // Pulse rings transition logic
    val infiniteTransition = rememberInfiniteTransition(label = "Radar Ripple")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple Scale"
    )
    val alphaFactor by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple Alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        val rippleColor = if (isConnected) PingGood else if (isConnecting) PingMedium else CyberCyan

        Box(
            modifier = Modifier
                .size(190.dp)
                .drawBehind {
                    // Draw outer radar pulse loops
                    if (isConnected || isConnecting) {
                        drawCircle(
                            color = rippleColor,
                            radius = (size.minDimension / 2.0f) * scaleFactor,
                            alpha = alphaFactor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Internal globe capsule
            Box(
                modifier = Modifier
                    .size(145.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberCard.copy(alpha = 0.95f),
                                CyberBg
                            )
                        )
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Power else Icons.Filled.PowerOff,
                        contentDescription = "Connection Button State",
                        tint = rippleColor,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (connectionState) {
                            "Connected" -> "ПОДКЛЮЧЕНО"
                            "Connecting" -> "ЗАПУСК…"
                            else -> "ОТКЛЮЧЕНО"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile connection info card label
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, rippleColor.copy(alpha = 0.5f)),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Dns,
                    contentDescription = "Active Server",
                    tint = rippleColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = profileName,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Active connection packet rate dashboards
@Composable
fun TrafficStatsCard(
    connectionState: String,
    stats: VpnConnectionService.TrafficStats,
    protocol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(0.5.dp, CyberTeal.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberLightGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatSpeed(stats.downSpeedKbps),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatBytes(stats.totalDownBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberLightGray,
                    fontSize = 10.sp
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(45.dp)
                    .background(CyberTeal.copy(alpha = 0.3f))
            )

            // Speed Upload
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Upload",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ИСХОДЯЩИЙ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberLightGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatSpeed(stats.upSpeedKbps),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatBytes(stats.totalUpBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberLightGray,
                    fontSize = 10.sp
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(45.dp)
                    .background(CyberTeal.copy(alpha = 0.3f))
            )

            // Tunnel Active Tech Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ПРОТОКОЛ",
                    fontSize = 10.sp,
                    color = CyberLightGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = protocol,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ШИФРОВАНИЕ",
                    fontSize = 9.sp,
                    color = CyberTeal
                )
            }
        }
    }
}

// Interactive node filters, text matching search queries, sorting operations
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
        // Search text input bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск по имени или протоколу", color = CyberLightGray.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = CyberLightGray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberCard,
                unfocusedContainerColor = CyberCard,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberTeal.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Command utility row (Ping All, Remove Dead, Sorting filter capsules)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ping All Button
                Button(
                    onClick = onPingAll,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBg),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).testTag("ping_all_button")
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = "Flash", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Тест Ping", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Delete Failed Button
                Button(
                    onClick = onDeleteFailed,
                    colors = ButtonDefaults.buttonColors(containerColor = PingBad, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).testTag("delete_failed_button")
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clean", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Удалить нераб.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Sorting selection list
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Сортировка:", fontSize = 11.sp, color = CyberLightGray)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Text(
                        text = when (sortType) {
                            VpnViewModel.SortType.PING -> "По Пингу"
                            VpnViewModel.SortType.ALPHABETICAL -> "А-Я"
                            VpnViewModel.SortType.DATE -> "По Дате"
                        },
                        fontSize = 12.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(4.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CyberCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("По пингу", color = Color.White) },
                            onClick = { onSortChange(VpnViewModel.SortType.PING); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("По алфавиту", color = Color.White) },
                            onClick = { onSortChange(VpnViewModel.SortType.ALPHABETICAL); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("По дате добавления", color = Color.White) },
                            onClick = { onSortChange(VpnViewModel.SortType.DATE); expanded = false }
                        )
                    }
                }
            }
        }

        // Animated dynamic ping status tag
        AnimatedVisibility(
            visible = pingText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberTeal.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pingText ?: "",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Individual Proxy Node list rows
@Composable
fun ProfileCardItem(
    profile: VpnProfileEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPingNode: () -> Unit
) {
    val borderStroke = if (isSelected) BorderStroke(1.5.dp, CyberCyan) else BorderStroke(0.5.dp, CyberLightGray.copy(alpha = 0.15f))
    val containerBg = if (isSelected) CyberCard.copy(alpha = 0.9f) else CyberCard.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("node_${profile.id}")
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Label flag, Name protocols & host parameters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Geometric flag block reflecting standard servers
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(getProtocolColor(profile.protocol)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.protocol,
                        color = CyberBg,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name and Address parameters
                Column {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${profile.host}:${profile.port}",
                        color = CyberLightGray.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Latency indicator, trigger-on-tap manual ping test and fast delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ping delay pill
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
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Ping",
                            tint = getPingTextColor(profile.ping),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = when (profile.ping) {
                                -1 -> "Тест"
                                -2 -> "ERR"
                                else -> "${profile.ping}мс"
                            },
                            color = getPingTextColor(profile.ping),
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Node deletion button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Удалить узел",
                        tint = PingBad.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Single subscription detail row
@Composable
fun SubscriptionItemRow(
    sub: SubscriptionEntity,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sub.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Text(
                    text = sub.url,
                    color = CyberLightGray.copy(alpha = 0.5f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 9.sp
                )
                Text(
                    text = "Узлов в базе: ${sub.nodeCount}",
                    color = CyberCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onUpdate, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Update", tint = CyberCyan, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = PingBad, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyStateLayout() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.DynamicFeed,
            contentDescription = "Empty Nodes",
            tint = CyberTeal.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Список прокси-узлов пуст",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            "Скопируйте Vless-ключи или ссылку на подписку во внешнем VPN и вставьте их, нажав на иконку в правом углу сверху.",
            color = CyberLightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Helper: Custom color selectors for protocols
private fun getProtocolColor(protocol: String): Color {
    return when (protocol.uppercase(Locale.ROOT)) {
        "VLESS" -> Color(0xFF00FFCC)
        "VMESS" -> Color(0xFF9400D3)
        "SS" -> Color(0xFFFF4500)
        "TROJAN" -> Color(0xFF1E90FF)
        "HYSTERIA2", "HY2" -> Color(0xFFFFD700)
        "TUIC" -> Color(0xFFFA8072)
        else -> CyberCyan
    }
}

// Helpers: Latency color representations
private fun getPingBgColor(ping: Int): Color {
    return when {
        ping == -1 -> CyberTeal.copy(alpha = 0.15f)
        ping == -2 -> PingBad.copy(alpha = 0.15f)
        ping < 120 -> PingGood.copy(alpha = 0.15f)
        ping < 300 -> PingMedium.copy(alpha = 0.15f)
        else -> PingBad.copy(alpha = 0.15f)
    }
}

private fun getPingTextColor(ping: Int): Color {
    return when {
        ping == -1 -> CyberLightGray
        ping == -2 -> PingBad
        ping < 120 -> PingGood
        ping < 300 -> PingMedium
        else -> PingBad
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
