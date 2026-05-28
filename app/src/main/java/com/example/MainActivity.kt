package com.example

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.data.local.AppDatabase
import com.example.data.repository.VpnRepository
import com.example.ui.screens.VpnMainScreen
import com.example.ui.screens.isDarkThemeState
import com.example.ui.viewmodel.VpnViewModel
import android.content.Context

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { VpnRepository(db.vpnDao()) }
    
    private val viewModel: VpnViewModel by viewModels {
        VpnViewModel.Factory(repository)
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selected = viewModel.selectedProfile.value
            if (selected != null) {
                viewModel.startVpnService(this, selected)
            } else {
                Toast.makeText(this, "Сервер не выбран", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Доступ к VPN отклонен", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load persistent theme state before compose initialization
        val sharedPrefs = getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        isDarkThemeState = sharedPrefs.getBoolean("is_dark_theme", false)
        
        enableEdgeToEdge()
        
        setContent {
            VpnMainScreen(
                viewModel = viewModel,
                onRequestPrepareVpn = {
                    checkAndRequestVpnPermission()
                }
            )
        }
    }

    private fun checkAndRequestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            val selected = viewModel.selectedProfile.value
            if (selected != null) {
                viewModel.startVpnService(this, selected)
            }
        }
    }
}
