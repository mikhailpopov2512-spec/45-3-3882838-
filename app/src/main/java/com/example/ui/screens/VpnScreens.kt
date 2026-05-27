package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SubscriptionEntity
import com.example.data.local.VpnProfileEntity
import com.example.data.util.VpnConnectionService
import com.example.data.util.VpnLinkParser
import com.example.ui.viewmodel.VpnViewModel
import com.example.ui.util.Locales
import kotlinx.coroutines.launch

@Composable
fun DashboardTab(
    viewModel: VpnViewModel,
    lang: String,
    onNavigateToServers: () -> Unit
) {
    val connection by viewModel.connectionState.collectAsState()
    val ipInfo by viewModel.ipDetails.collectAsState()
    val bytesIn by viewModel.bytesReceived.collectAsState()
    val bytesOut by viewModel.bytesSent.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Pulsing circle connection animation states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // State Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    when (connection) {
                        "CONNECTED" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        "CONNECTING" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    }
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (connection) {
                                "CONNECTED" -> MaterialTheme.colorScheme.secondary
                                "CONNECTING" -> MaterialTheme.colorScheme.tertiary
                                else -> Color.Gray
                            }
                        )
                )
                Text(
                    text = Locales.get(
                        when (connection) {
                            "CONNECTED" -> "status_connected"
                            "CONNECTING" -> "status_connecting"
                            else -> "status_disconnected"
                        }, lang
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (connection) {
                        "CONNECTED" -> MaterialTheme.colorScheme.secondary
                        "CONNECTING" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Glowing Power Toggle Ring
        Box(
            modifier = Modifier
                .size(210.dp)
                .testTag("power_ring_box"),
            contentAlignment = Alignment.Center
        ) {
            // Pulse circle underlay
            if (connection == "CONNECTED" || connection == "CONNECTING") {
                Box(
                    modifier = Modifier
                        .size(170.dp * pulseScale)
                        .clip(CircleShape)
                        .background(
                            color = when (connection) {
                                "CONNECTED" -> MaterialTheme.colorScheme.secondary.copy(alpha = pulseAlpha)
                                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = pulseAlpha)
                            }
                        )
                )
            }

            // Outer Canvas border dash
            Canvas(modifier = Modifier.size(200.dp)) {
                val cycleColor = when (connection) {
                    "CONNECTED" -> Color(0xFF10B981)
                    "CONNECTING" -> Color(0xFFF59E0B)
                    else -> Color(0xFF475569)
                }
                drawCircle(
                    color = cycleColor.copy(alpha = 0.25f),
                    radius = size.width / 2,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Power button clickable shape
            Surface(
                onClick = {
                    if (activeProfile != null) {
                        viewModel.toggleConnection(activeProfile!!)
                    } else if (allProfiles.isNotEmpty()) {
                        viewModel.toggleConnection(allProfiles.first())
                    } else {
                        Toast.makeText(context, Locales.get("empty_servers", lang), Toast.LENGTH_LONG).show()
                        onNavigateToServers()
                    }
                },
                modifier = Modifier
                    .size(140.dp)
                    .shadow(16.dp, CircleShape)
                    .testTag("power_toggle_btn"),
                shape = CircleShape,
                color = when (connection) {
                    "CONNECTED" -> MaterialTheme.colorScheme.secondary
                    "CONNECTING" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when (connection) {
                    "CONNECTED", "CONNECTING" -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power VPN Toggle",
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (connection == "CONNECTED") "ON" else "OFF",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Config Brief Item Card
        val serverText = activeProfile?.name ?: Locales.get("no_server", lang)
        val serverProtocol = activeProfile?.protocol ?: "VLESS"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToServers() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeProfile != null) Icons.Default.DatasetLinked else Icons.Default.VpnKey,
                            contentDescription = "Server Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = Locales.get("selected_server", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = serverText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (activeProfile != null) {
                    IconButton(
                        onClick = { viewModel.executePing(activeProfile!!) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = "Quick Ping Check",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate Servers",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // IP Geolocation Details Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Locales.get("active_ip", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ipInfo.ip,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(ipInfo.ip))
                                Toast.makeText(context, Locales.get("copied", lang), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addLog("Refreshing active WAN endpoint...")
                            VpnConnectionService.stopVpn() // reset and fetch public IP info
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(Locales.get("ip_refetch", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Locales.get("location", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Country",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (ipInfo.countryName.isNotEmpty()) "${ipInfo.city}, ${ipInfo.countryName}" else "Unknown Subnet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Locales.get("provider", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "ISP Provider",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ipInfo.org.ifEmpty { "Resolving..." },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Live Traffic Metrics Grid Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = "Download flow",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Locales.get("traffic_rx", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatBytes(bytesIn),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignTop,
                            contentDescription = "Upload Flow",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Locales.get("traffic_tx", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatBytes(bytesOut),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Quick Optimization Button
        Button(
            onClick = { viewModel.runNetworkOptimization() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("optimize_system_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Optimize Core")
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = Locales.get("optimize_btn", lang),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Locales.get("optimize_tip", lang),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ServersTab(viewModel: VpnViewModel, lang: String) {
    val profiles by viewModel.allProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val filteredProfiles = profiles.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.host.contains(searchQuery, ignoreCase = true) ||
                it.protocol.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Server actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val clipText = clipboardManager.getText()?.text ?: ""
                        if (clipText.isNotEmpty()) {
                            val ok = viewModel.addProfileByLink(clipText)
                            if (ok) {
                                Toast.makeText(context, Locales.get("added_success", lang), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to parse: ${clipText.take(15)}...", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Clipboard empty!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste Link", modifier = Modifier.size(16.dp))
                        Text(Locales.get("paste_clipboard", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Button(
                    onClick = { showManualDialog = true },
                    modifier = Modifier
                        .weight(0.7f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Manual Form", modifier = Modifier.size(16.dp))
                        Text(Locales.get("manual_add", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar & Ping actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Locales.get("search_hint", lang), fontSize = 14.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Button(
                    onClick = { viewModel.pingAllProfiles() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Speed, contentDescription = "Speed Dial", modifier = Modifier.size(18.dp))
                        Text(Locales.get("ping_all", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Server items list
            if (filteredProfiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Empty configs representation indicator",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = Locales.get("empty_servers", lang),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProfiles, key = { it.id }) { profile ->
                        val isSelected = activeProfile?.id == profile.id
                        ServerItemRow(
                            profile = profile,
                            isSelected = isSelected,
                            onSelect = { viewModel.toggleConnection(profile) },
                            onPing = { viewModel.executePing(profile) },
                            onDelete = { viewModel.deleteProfile(profile) },
                            onCopy = {
                                val link = when (profile.protocol) {
                                    "VLESS" -> VpnLinkParser.toVlessLink(profile)
                                    "VMess" -> VpnLinkParser.toVmessLink(profile)
                                    "Shadowsocks" -> VpnLinkParser.toShadowsocksLink(profile)
                                    else -> VpnLinkParser.toTrojanLink(profile)
                                }
                                clipboardManager.setText(AnnotatedString(link))
                                Toast.makeText(context, Locales.get("copied", lang), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Manual form insertion dialog sheet
        if (showManualDialog) {
            ManualConfigDialog(
                lang = lang,
                onDismiss = { showManualDialog = false },
                onSave = { name, protocol, host, port, uuid, security, sni, path, flow ->
                    viewModel.addManualProfile(name, protocol, host, port.toIntOrNull() ?: 443, uuid, security, sni, path, flow)
                    showManualDialog = false
                    Toast.makeText(context, Locales.get("added_success", lang), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun ServerItemRow(
    profile: VpnProfileEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_card_${profile.id}")
            .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Color badges according to protocols
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (profile.protocol) {
                                "VLESS" -> Color(0xFF0ea5e9).copy(alpha = 0.15f)
                                "VMess" -> Color(0xFF8b5cf6).copy(alpha = 0.15f)
                                "Shadowsocks" -> Color(0xFFf59e0b).copy(alpha = 0.15f)
                                else -> Color(0xFFec4899).copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.protocol.take(3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = when (profile.protocol) {
                            "VLESS" -> Color(0xFF0284C7)
                            "VMess" -> Color(0xFF7c3aed)
                            "Shadowsocks" -> Color(0xFFd97706)
                            else -> Color(0xFFdb2777)
                        }
                    )
                }

                // Server detail info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = profile.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile.isSubProfile) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Cloud syncd",
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Text(
                        text = "${profile.host}:${profile.port}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions panel: Ping widget + copy + delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Interactive latency tracker
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .clickable { onPing() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Ping sign",
                            modifier = Modifier.size(12.dp),
                            tint = when {
                                profile.ping in 0..75 -> Color(0xFF10B981)
                                profile.ping in 76..180 -> Color(0xFFF59E0B)
                                profile.ping > 180 -> Color(0xFFEF4444)
                                else -> Color.Gray
                            }
                        )
                        Text(
                            text = if (profile.ping >= 0) "${profile.ping} ms" else "---",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                profile.ping in 0..75 -> Color(0xFF10B981)
                                profile.ping in 76..180 -> Color(0xFFF59E0B)
                                profile.ping > 180 -> Color(0xFFEF4444)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy Node link",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Trash delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionsTab(viewModel: VpnViewModel, lang: String) {
    val subscriptions by viewModel.allSubscriptions.collectAsState()
    val isRefreshing by viewModel.isRefreshingSub.collectAsState()
    var subName by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form to add a new subscription pool link
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Locales.get("add_sub_btn", lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextField(
                    value = subName,
                    onValueChange = { subName = it },
                    placeholder = { Text(Locales.get("sub_name_hint", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                TextField(
                    value = subUrl,
                    onValueChange = { subUrl = it },
                    placeholder = { Text(Locales.get("sub_url_hint", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Button(
                    onClick = {
                        if (subUrl.trim().startsWith("http", ignoreCase = true) || subUrl.contains("fallback")) {
                            viewModel.addSubscription(subName, subUrl.trim())
                            subName = ""
                            subUrl = ""
                        } else {
                            // Helper fallback instructions for local testing
                            viewModel.addLog("Entered URL seems mock or generic. Use fallback URL helper.")
                            viewModel.addSubscription(
                                subName.ifEmpty { "Premium Test Pool" },
                                "https://happvpn.com/fallback-test"
                            )
                            subName = ""
                            subUrl = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.AddLink, contentDescription = "Sub icon integration")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Locales.get("add_sub_btn", lang), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subscriptions List Title Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Locales.get("subs_title", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        // List representation
        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = "Empty subs",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = Locales.get("empty_subs", lang),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(subscriptions) { sub ->
                    SubscriptionItemRow(
                        sub = sub,
                        onSync = { viewModel.syncSubscription(sub) },
                        onDelete = { viewModel.deleteSubscription(sub) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionItemRow(
    sub: SubscriptionEntity,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Sub Dns label link",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sub.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sub.url,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSync) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync link database",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove subscription",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: VpnViewModel, lang: String) {
    val routing by viewModel.routingMode.collectAsState()
    val dnsBySpec by viewModel.dnsServer.collectAsState()
    val muxEnabled by viewModel.isMuxEnabled.collectAsState()
    val mtu by viewModel.mtuSize.collectAsState()
    val keepAlive by viewModel.keepAlive.collectAsState()
    val ipv6Enabled by viewModel.isIpv6Enabled.collectAsState()
    val darkTheme by viewModel.isDarkTheme.collectAsState()
    val clipManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Locales.get("settings_title", lang),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        // General settings list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Topic Router Options
                Column {
                    Text(Locales.get("routing_mode", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bypass LAN", "Global", "Bypass Local").forEach { mode ->
                            val isSel = routing == mode
                            val textLabel = when (mode) {
                                "Bypass LAN" -> "Bypass LAN"
                                "Global" -> "Global Proxy"
                                else -> "Bypass Country"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.05f
                                        )
                                    )
                                    .clickable { viewModel.setRoutingMode(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = textLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Topic DNS Selector Options
                Column {
                    Text(Locales.get("dns_server", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "AdGuard").forEach { dns ->
                            val isSel = dnsBySpec == dns
                            val dnsLabel = dns.split(" ").first()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.05f
                                        )
                                    )
                                    .clickable { viewModel.setDnsServer(dns) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dnsLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Visual theme option row toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(Locales.get("theme", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (darkTheme) Locales.get("theme_dark", lang) else Locales.get("theme_light", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { viewModel.toggleTheme() },
                        thumbContent = {
                            Icon(
                                imageVector = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme state indicator",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Languages localization manual switch options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(Locales.get("language", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (lang == "RU") "Русский (Russian)" else "English (EN)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("RU", "EN").forEach { lCode ->
                            val active = lang == lCode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.05f
                                        )
                                    )
                                    .clickable { viewModel.setLanguage(lCode) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optimization sliders & switches
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // MUX Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Locales.get("mux_title", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            Locales.get("mux_desc", lang),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = muxEnabled, onCheckedChange = { viewModel.toggleMux() })
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // MTU slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Locales.get("mtu_size", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${mtu} B", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = mtu.toFloat(),
                        onValueChange = { viewModel.setMtuSize(it.toInt()) },
                        valueRange = 1280f..1500f,
                        steps = 22
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Live Core IPv6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(Locales.get("ipv6_title", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Default interface dual-stack", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = ipv6Enabled, onCheckedChange = { viewModel.toggleIpv6() })
                }
            }
        }

        // MONOSPACE RETRO LOGS TERMINAL
        Text(
            text = Locales.get("terminal_head", lang),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)) // Pure terminal dark black
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Action row over logs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                        Text("happ_vpn_core.log", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF9CA3AF))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val textToCopy = viewModel.terminalLogs.joinToString("\n")
                                clipManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, Locales.get("copied", lang), Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(Locales.get("copy_logs", lang), color = Color(0xFF38BDF8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        TextButton(
                            onClick = { viewModel.terminalLogs.clear() },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(Locales.get("clear_logs", lang), color = Color(0xFFEF4444), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Divider(color = Color(0xFF1F2937), modifier = Modifier.padding(vertical = 4.dp))

                // Scrollable messages list
                val logsList = viewModel.terminalLogs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    reverseLayout = false
                ) {
                    items(logsList) { log ->
                        Text(
                            text = log,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                log.contains("[ERROR]", ignoreCase = true) || log.contains("TIMEOUT", ignoreCase = true) -> Color(0xFFF87171)
                                log.contains("SYSTEM", ignoreCase = true) || log.contains("🔍 RUNNING", ignoreCase = true) -> Color(0xFF60A5FA)
                                log.contains("OK", ignoreCase = true) || log.contains("Connected", ignoreCase = true) -> Color(0xFF34D399)
                                else -> Color(0xFFF3F4F6)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ManualConfigDialog(
    lang: String,
    onDismiss: () -> Unit,
    onSave: (name: String, protocol: String, host: String, port: String, uuid: String, security: String, sni: String, path: String, flow: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("VLESS") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var uuid by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("tls") }
    var sni by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var flow by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Locales.get("manual_dialog_title", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Name Row
                Column {
                    Text(Locales.get("name", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Protocol selector options
                Column {
                    Text("Protocol", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("VLESS", "VMess", "Shadowsocks", "Trojan").forEach { proto ->
                            val active = protocol == proto
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.05f
                                        )
                                    )
                                    .clickable { protocol = proto }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = proto.take(3).uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Host, port Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(Locales.get("host", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = host,
                            onValueChange = { host = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(Locales.get("port", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = port,
                            onValueChange = { port = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                // UUID / Key / Pass
                Column {
                    Text(Locales.get("uuid", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                if (protocol == "VLESS" || protocol == "VMess" || protocol == "Trojan") {
                    // Security settings
                    Column {
                        Text(Locales.get("security", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("tls", "none").forEach { sec ->
                                val active = security == sec
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.05f
                                            )
                                        )
                                        .clickable { security = sec }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sec.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // SNI, path fields representation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Locales.get("sni", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextField(
                                value = sni,
                                onValueChange = { sni = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(Locales.get("path", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextField(
                                value = path,
                                onValueChange = { path = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                if (protocol == "VLESS") {
                    // Flow Field selector
                    Column {
                        Text(Locales.get("flow", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = flow,
                            onValueChange = { flow = it },
                            placeholder = { Text("xtls-rprx-vision") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Dialog Buttons Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(Locales.get("cancel", lang))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSave(name, protocol, host, port, uuid, security, sni, path, flow) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(Locales.get("save", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Utility human-readable bytes printer
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0.0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.1f KB", kb)
    }
}
