package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VpnDatabaseHelper
import com.example.data.local.VpnProfileEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.repository.VpnRepository
import com.example.data.util.VpnConnectionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VpnRepository
    private val sharedPrefs = application.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)

    val allSubscriptions: StateFlow<List<SubscriptionEntity>>
    private val rawProfiles: StateFlow<List<VpnProfileEntity>>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    enum class SortType { PING, ALPHABETICAL, DATE }
    private val _sortType = MutableStateFlow(SortType.DATE)
    val sortType: StateFlow<SortType> = _sortType

    private val _selectedProfileId = MutableStateFlow(sharedPrefs.getInt("selected_id", -1))
    val selectedProfileId: StateFlow<Int> = _selectedProfileId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _pingStateText = MutableStateFlow<String?>(null)
    val pingStateText: StateFlow<String?> = _pingStateText

    val connectionState = VpnConnectionService.connectionState
    val connectedProfileName = VpnConnectionService.connectedProfileName
    val trafficStats = VpnConnectionService.trafficStats

    init {
        val dbHelper = VpnDatabaseHelper.getInstance(application)
        repository = VpnRepository(dbHelper)

        allSubscriptions = repository.allSubscriptions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        rawProfiles = repository.allProfiles.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
    }

    val processedProfiles: StateFlow<List<VpnProfileEntity>> = combine(
        rawProfiles, _searchQuery, _sortType
    ) { profiles, query, sort ->
        var list = if (query.isBlank()) {
            profiles
        } else {
            profiles.filter { it.name.contains(query, ignoreCase = true) || it.protocol.contains(query, ignoreCase = true) }
        }

        list = when (sort) {
            SortType.PING -> list.sortedWith { a, b ->
                val pA = if (a.ping < 0) 99999 else a.ping
                val pB = if (b.ping < 0) 99999 else b.ping
                pA.compareTo(pB)
            }
            SortType.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
            SortType.DATE -> list.sortedBy { it.dateAdded }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProfile: StateFlow<VpnProfileEntity?> = combine(
        rawProfiles, _selectedProfileId
    ) { profiles, id ->
        profiles.find { it.id == id } ?: profiles.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sort: SortType) {
        _sortType.value = sort
    }

    fun selectProfile(id: Int) {
        _selectedProfileId.value = id
        sharedPrefs.edit().putInt("selected_id", id).apply()
        if (connectionState.value == "Connected") {
            val prof = rawProfiles.value.find { it.id == id }
            if (prof != null) {
                connectVpn(prof)
            }
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteProfileById(id)
            if (_selectedProfileId.value == id) {
                val next = rawProfiles.value.firstOrNull { it.id != id }
                selectProfile(next?.id ?: -1)
            }
        }
    }

    fun importFromClipboard(context: Context, text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val count = repository.importFromText(text)
                if (count > 0) {
                    Toast.makeText(context, "Импортировано профилей: $count", Toast.LENGTH_SHORT).show()
                    pingAllProfiles()
                } else {
                    Toast.makeText(context, "Не найдено Vless ключей или подписок", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка импорта: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubscription(context: Context, url: String, name: String) {
        if (url.isBlank() || !url.startsWith("http")) {
            Toast.makeText(context, "Введите прямую HTTP/HTTPS ссылку", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateSubscription(url, name)
                Toast.makeText(context, "Подписка добавлена!", Toast.LENGTH_SHORT).show()
                pingAllProfiles()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSubscription(url: String) {
        viewModelScope.launch {
            repository.deleteSubscriptionByUrl(url)
        }
    }

    fun refreshSubscription(context: Context, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateSubscription(url)
                Toast.makeText(context, "Подписка обновлена!", Toast.LENGTH_SHORT).show()
                pingAllProfiles()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка обновления", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFailedProfiles() {
        viewModelScope.launch {
            repository.deleteUnreachableProfiles()
        }
    }

    fun pingAllProfiles() {
        val list = processedProfiles.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            _pingStateText.value = "Запуск пинга…"
            try {
                repository.testAllPings(list) { current, total ->
                    _pingStateText.value = "Тест пинга: $current / $total"
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                _pingStateText.value = null
            }
        }
    }

    fun pingSingleProfile(profileId: Int, host: String, port: Int) {
        viewModelScope.launch {
            repository.testSinglePing(profileId, host, port)
        }
    }

    fun autoSwitchToBest(context: Context) {
        val list = rawProfiles.value.filter { it.ping > 0 }
        if (list.isEmpty()) {
            Toast.makeText(context, "Нет рабочих узлов с пингом!", Toast.LENGTH_SHORT).show()
            return
        }
        val best = list.minByOrNull { it.ping }
        if (best != null) {
            selectProfile(best.id)
            Toast.makeText(context, "Автопереключение: выбран узел с пингом ${best.ping} мс!", Toast.LENGTH_SHORT).show()
        }
    }

    fun connectVpn(profile: VpnProfileEntity) {
        val intent = Intent(getApplication(), VpnConnectionService::class.java).apply {
            action = "CONNECT"
            putExtra("profile_name", profile.name)
        }
        getApplication<Application>().startService(intent)
    }

    fun disconnectVpn() {
        val intent = Intent(getApplication(), VpnConnectionService::class.java).apply {
            action = "DISCONNECT"
        }
        getApplication<Application>().startService(intent)
    }

    fun toggleVpn(context: Context) {
        if (connectionState.value == "Connected") {
            disconnectVpn()
        } else {
            val prof = selectedProfile.value
            if (prof == null) {
                Toast.makeText(context, "Добавьте или выберите профиль", Toast.LENGTH_SHORT).show()
                return
            }
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                _vpnPermissionIntent.value = vpnIntent
            } else {
                connectVpn(prof)
            }
        }
    }

    private val _vpnPermissionIntent = MutableStateFlow<Intent?>(null)
    val vpnPermissionIntent: StateFlow<Intent?> = _vpnPermissionIntent

    fun clearPermissionIntent() {
        _vpnPermissionIntent.value = null
    }
}
