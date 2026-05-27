package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import com.example.ui.util.Locales
import com.example.ui.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val connectionState by viewModel.connectionState.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                var currentTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Shield VPN Icon",
                                        tint = if (connectionState == "CONNECTED") {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Text(
                                        text = Locales.get("app_title", currentLanguage),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            },
                            actions = {
                                // Language switch quick access action
                                TextButton(
                                    onClick = {
                                        val nextLang = if (currentLanguage == "RU") "EN" else "RU"
                                        viewModel.setLanguage(nextLang)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Translate, contentDescription = "Lang Toggle", modifier = Modifier.size(16.dp))
                                        Text(
                                            text = currentLanguage,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Quick Quick Speed Ping Scanning diagnostics
                                IconButton(onClick = { viewModel.runNetworkOptimization() }) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Optimization sweeps",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_navigation"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                label = { Text(Locales.get("tab_dashboard", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Dashboard tap"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                label = { Text(Locales.get("tab_servers", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 1) Icons.Default.Dns else Icons.Outlined.Dns,
                                        contentDescription = "Servers tab"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_servers")
                            )

                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                label = { Text(Locales.get("tab_subs", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 2) Icons.Default.CloudQueue else Icons.Outlined.CloudQueue,
                                        contentDescription = "Subscriptions tab"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_subs")
                            )

                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                label = { Text(Locales.get("tab_settings", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings tab"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                            },
                            label = "tab_fade_transition"
                        ) { tab ->
                            when (tab) {
                                0 -> DashboardTab(
                                    viewModel = viewModel,
                                    lang = currentLanguage,
                                    onNavigateToServers = { currentTab = 1 }
                                )
                                1 -> ServersTab(
                                    viewModel = viewModel,
                                    lang = currentLanguage
                                )
                                2 -> SubscriptionsTab(
                                    viewModel = viewModel,
                                    lang = currentLanguage
                                )
                                3 -> SettingsTab(
                                    viewModel = viewModel,
                                    lang = currentLanguage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
