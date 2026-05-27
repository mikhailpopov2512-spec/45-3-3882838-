package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.screens.VpnMainScreen
import com.example.ui.theme.HappVpnTheme
import com.example.ui.viewmodel.VpnViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val active = viewModel.selectedProfile.value
            if (active != null) {
                viewModel.connectVpn(active)
            }
        } else {
            Toast.makeText(this, "Для подключения VPN необходимо системное разрешение!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vpnPermissionIntent.collectLatest { intent ->
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                        viewModel.clearPermissionIntent()
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestNotificationPermission = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            HappVpnTheme {
                VpnMainScreen(viewModel)
            }
        }
    }
}
