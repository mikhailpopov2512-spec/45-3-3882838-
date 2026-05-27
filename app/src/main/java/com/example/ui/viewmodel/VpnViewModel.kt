package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.VpnRepository
import com.example.data.util.VpnConnectionService
import com.example.data.util.AppLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = VpnDatabaseHelper.getInstance(application)
    private val repository: VpnRepository
    private val sharedPrefs = application.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)

    val allSubscriptions: StateFlow<List<SubscriptionEntity>> = dbHelper.subscriptionsFlow
    private val rawProfiles: StateFlow<List<VpnProfileEntity>> = dbHelper.profilesFlow

    // Database reactive feeds
    val allUsers: StateFlow<List<UserAccountEntity>> = dbHelper.usersFlow
    val supportMessages: StateFlow<List<SupportMessageEntity>> = dbHelper.supportMessagesFlow
    val announcements: StateFlow<List<AnnouncementEntity>> = dbHelper.announcementsFlow

    private val _loginUserAccount = MutableStateFlow<UserAccountEntity?>(null)
    val loginUserAccount: StateFlow<UserAccountEntity?> = _loginUserAccount

    private val _rememberedUsersText = MutableStateFlow(
        sharedPrefs.getString("remembered_logins_list", "Михаил Попов,Старший администратор,Младший администратор,Роскомнадзор") ?: "Михаил Попов,Старший администратор,Младший администратор,Роскомнадзор"
    )
    val rememberedUsersList: StateFlow<List<String>> = _rememberedUsersText.map { text ->
        if (text.isBlank()) emptyList() else text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Михаил Попов", "Старший администратор", "Младший администратор", "Роскомнадзор"))

    private val _captchaQuestion = MutableStateFlow("")
    val captchaQuestion: StateFlow<String> = _captchaQuestion

    private val _captchaAnswer = MutableStateFlow("")
    val captchaAnswer: StateFlow<String> = _captchaAnswer

    fun generateNewCaptcha() {
        val a = (2..9).random()
        val b = (2..9).random()
        _captchaQuestion.value = "Решите простой пример: $a + $b"
        _captchaAnswer.value = (a + b).toString()
    }

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

    // Integrated Theme, Navigation & User Login State Models
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen

    data class UserProfile(val email: String, val username: String, val planType: String)
    private val _userProfile = MutableStateFlow<UserProfile?>(
        sharedPrefs.getString("user_email", null)?.let { email ->
            UserProfile(email, sharedPrefs.getString("user_name", "User") ?: "User", "Premium Ultra Pro")
        }
    )
    val userProfile: StateFlow<UserProfile?> = _userProfile

    // Persistent settings matched with user screenshots
    private val _allowLan = MutableStateFlow(sharedPrefs.getBoolean("allow_lan", false))
    val allowLan: StateFlow<Boolean> = _allowLan

    private val _autoStart = MutableStateFlow(sharedPrefs.getBoolean("auto_start", false))
    val autoStart: StateFlow<Boolean> = _autoStart

    private val _betaVersion = MutableStateFlow(sharedPrefs.getBoolean("beta_version", true))
    val betaVersion: StateFlow<Boolean> = _betaVersion

    private val _packet = MutableStateFlow(sharedPrefs.getString("packet", "1A,2B,3C") ?: "1A,2B,3C")
    val packet: StateFlow<String> = _packet

    private val _delay = MutableStateFlow(sharedPrefs.getString("delay", "1-3") ?: "1-3")
    val delay: StateFlow<String> = _delay

    private val _rand = MutableStateFlow(sharedPrefs.getString("rand", "10-200") ?: "10-200")
    val rand: StateFlow<String> = _rand

    private val _randRange = MutableStateFlow(sharedPrefs.getString("rand_range", "") ?: "")
    val randRange: StateFlow<String> = _randRange

    private val _useMux = MutableStateFlow(sharedPrefs.getBoolean("use_mux", false))
    val useMux: StateFlow<Boolean> = _useMux

    private val _preferredIp = MutableStateFlow(sharedPrefs.getString("preferred_ip", "Auto") ?: "Auto")
    val preferredIp: StateFlow<String> = _preferredIp

    private val _enableXrayTun = MutableStateFlow(sharedPrefs.getBoolean("enable_xray_tun", false))
    val enableXrayTun: StateFlow<Boolean> = _enableXrayTun

    private val _blockTunnelBind = MutableStateFlow(sharedPrefs.getBoolean("block_tunnel_bind", false))
    val blockTunnelBind: StateFlow<Boolean> = _blockTunnelBind

    private val _language = MutableStateFlow(sharedPrefs.getString("language", "Русский") ?: "Русский")
    val language: StateFlow<String> = _language

    private val _routingMode = MutableStateFlow(sharedPrefs.getString("routing_mode", "📡 MeraVPN | RU-Routing") ?: "📡 MeraVPN | RU-Routing")
    val routingMode: StateFlow<String> = _routingMode

    private val _useFragmentation = MutableStateFlow(sharedPrefs.getBoolean("use_fragmentation", true))
    val useFragmentation: StateFlow<Boolean> = _useFragmentation

    private val _fragmentType = MutableStateFlow(sharedPrefs.getString("fragment_type", "Xray") ?: "Xray")
    val fragmentType: StateFlow<String> = _fragmentType

    private val _fragmentPackets = MutableStateFlow(sharedPrefs.getString("fragment_packets", "tlshello") ?: "tlshello")
    val fragmentPackets: StateFlow<String> = _fragmentPackets

    private val _fragmentLength = MutableStateFlow(sharedPrefs.getString("fragment_length", "10-30") ?: "10-30")
    val fragmentLength: StateFlow<String> = _fragmentLength

    private val _fragmentDelay = MutableStateFlow(sharedPrefs.getString("fragment_delay", "1-3") ?: "1-3")
    val fragmentDelay: StateFlow<String> = _fragmentDelay

    private val _maxFragmentDivide = MutableStateFlow(sharedPrefs.getString("max_fragment_divide", "10-20") ?: "10-20")
    val maxFragmentDivide: StateFlow<String> = _maxFragmentDivide

    private val _enableNoise = MutableStateFlow(sharedPrefs.getBoolean("enable_noise", true))
    val enableNoise: StateFlow<Boolean> = _enableNoise

    private val _noiseType = MutableStateFlow(sharedPrefs.getString("noise_type", "array") ?: "array")
    val noiseType: StateFlow<String> = _noiseType

    // Setting update handlers
    fun setAllowLan(value: Boolean) {
        _allowLan.value = value
        sharedPrefs.edit().putBoolean("allow_lan", value).apply()
    }

    fun setAutoStart(value: Boolean) {
        _autoStart.value = value
        sharedPrefs.edit().putBoolean("auto_start", value).apply()
    }

    fun setBetaVersion(value: Boolean) {
        _betaVersion.value = value
        sharedPrefs.edit().putBoolean("beta_version", value).apply()
    }

    fun setPacket(value: String) {
        _packet.value = value
        sharedPrefs.edit().putString("packet", value).apply()
    }

    fun setDelay(value: String) {
        _delay.value = value
        sharedPrefs.edit().putString("delay", value).apply()
    }

    fun setRand(value: String) {
        _rand.value = value
        sharedPrefs.edit().putString("rand", value).apply()
    }

    fun setRandRange(value: String) {
        _randRange.value = value
        sharedPrefs.edit().putString("rand_range", value).apply()
    }

    fun setUseMux(value: Boolean) {
        _useMux.value = value
        sharedPrefs.edit().putBoolean("use_mux", value).apply()
    }

    fun setPreferredIp(value: String) {
        _preferredIp.value = value
        sharedPrefs.edit().putString("preferred_ip", value).apply()
    }

    fun setEnableXrayTun(value: Boolean) {
        _enableXrayTun.value = value
        sharedPrefs.edit().putBoolean("enable_xray_tun", value).apply()
    }

    fun setBlockTunnelBind(value: Boolean) {
        _blockTunnelBind.value = value
        sharedPrefs.edit().putBoolean("block_tunnel_bind", value).apply()
    }

    fun setLanguage(value: String) {
        _language.value = value
        sharedPrefs.edit().putString("language", value).apply()
    }

    fun setRoutingMode(value: String) {
        _routingMode.value = value
        sharedPrefs.edit().putString("routing_mode", value).apply()
    }

    fun setUseFragmentation(value: Boolean) {
        _useFragmentation.value = value
        sharedPrefs.edit().putBoolean("use_fragmentation", value).apply()
    }

    fun setFragmentType(value: String) {
        _fragmentType.value = value
        sharedPrefs.edit().putString("fragment_type", value).apply()
    }

    fun setFragmentPackets(value: String) {
        _fragmentPackets.value = value
        sharedPrefs.edit().putString("fragment_packets", value).apply()
    }

    fun setFragmentLength(value: String) {
        _fragmentLength.value = value
        sharedPrefs.edit().putString("fragment_length", value).apply()
    }

    fun setFragmentDelay(value: String) {
        _fragmentDelay.value = value
        sharedPrefs.edit().putString("fragment_delay", value).apply()
    }

    fun setMaxFragmentDivide(value: String) {
        _maxFragmentDivide.value = value
        sharedPrefs.edit().putString("max_fragment_divide", value).apply()
    }

    fun setEnableNoise(value: Boolean) {
        _enableNoise.value = value
        sharedPrefs.edit().putBoolean("enable_noise", value).apply()
    }

    fun setNoiseType(value: String) {
        _noiseType.value = value
        sharedPrefs.edit().putString("noise_type", value).apply()
    }

    fun resetAllSettings(context: Context) {
        viewModelScope.launch {
            // Delete all profiles from local DB helper
            repository.deleteAllProfiles()
            
            // Clear preferences
            sharedPrefs.edit().clear().apply()
            
            // Reapply defaults
            _allowLan.value = false
            _autoStart.value = false
            _betaVersion.value = true
            _packet.value = "1A,2B,3C"
            _delay.value = "1-3"
            _rand.value = "10-200"
            _randRange.value = ""
            _useMux.value = false
            _preferredIp.value = "Auto"
            _enableXrayTun.value = false
            _blockTunnelBind.value = false
            _language.value = "Русский"
            _routingMode.value = "📡 MeraVPN | RU-Routing"
            _useFragmentation.value = true
            _fragmentType.value = "Xray"
            _fragmentPackets.value = "tlshello"
            _fragmentLength.value = "10-30"
            _fragmentDelay.value = "1-3"
            _maxFragmentDivide.value = "10-20"
            _enableNoise.value = true
            _noiseType.value = "array"
            _selectedProfileId.value = -1
            _isDarkTheme.value = false

            Toast.makeText(context, "Все настройки и база серверов сброшены!", Toast.LENGTH_LONG).show()
        }
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
        sharedPrefs.edit().putBoolean("dark_theme", dark).apply()
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun loginUser(email: String, username: String): Boolean {
        if (email.isNotBlank() && username.isNotBlank()) {
            _userProfile.value = UserProfile(email, username, "Premium Ultra Pro")
            sharedPrefs.edit()
                .putString("user_email", email)
                .putString("user_name", username)
                .apply()
            return true
        }
        return false
    }

    fun logoutUser() {
        _userProfile.value = null
        sharedPrefs.edit()
            .remove("user_email")
            .remove("user_name")
            .apply()
    }

    init {
        AppLogger.init(application)
        AppLogger.log(application, "SYSTEM", "Приложение запущено. ViewModel успешно создана.")
        
        repository = VpnRepository(dbHelper)

        // Restore login cache if exists
        val cacheName = sharedPrefs.getString("user_name", null)
        val cacheRole = sharedPrefs.getString("user_role", null)
        if (cacheName != null && cacheRole != null) {
            _loginUserAccount.value = UserAccountEntity(cacheName, cacheRole, cacheRole == "ROSKOMNADZOR")
        }

        generateNewCaptcha()
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
            AppLogger.log(context, "IMPORT", "Попытка импорта из буфера обмена, но буфер оказался пуст.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.log(context, "IMPORT", "Запуск импорта ключей из буфера обмена...")
                val count = repository.importFromText(text)
                if (count > 0) {
                    Toast.makeText(context, "Импортировано профилей: $count", Toast.LENGTH_SHORT).show()
                    AppLogger.log(context, "IMPORT", "Завершено. Успешно импортировано профилей: $count")
                    pingAllProfiles()
                } else {
                    Toast.makeText(context, "Не найдено Vless ключей или подписок", Toast.LENGTH_LONG).show()
                    AppLogger.log(context, "IMPORT", "Завершено. Разрешенных ключей или подписок в буфере не обнаружено.")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка импорта: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                AppLogger.log(context, "IMPORT", "Критическая ошибка при импорте: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubscription(context: Context, url: String, name: String) {
        if (url.isBlank() || !url.startsWith("http")) {
            Toast.makeText(context, "Введите прямую HTTP/HTTPS ссылку", Toast.LENGTH_SHORT).show()
            AppLogger.log(context, "SUBSCRIPTION", "Попытка добавления неверной ссылки подписки: $url")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.log(context, "SUBSCRIPTION", "Добавление новой подписки. Название: $name, Ссылка: $url")
                repository.updateSubscription(url, name)
                Toast.makeText(context, "Подписка добавлена!", Toast.LENGTH_SHORT).show()
                AppLogger.log(context, "SUBSCRIPTION", "Подписка '$name' успешно загружена и добавлена.")
                pingAllProfiles()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                AppLogger.log(context, "SUBSCRIPTION", "Ошибка при добавлении подписки '$name': ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSubscription(url: String) {
        viewModelScope.launch {
            AppLogger.log(getApplication(), "SUBSCRIPTION", "Удаление подписки по ссылке: $url")
            repository.deleteSubscriptionByUrl(url)
        }
    }

    fun refreshSubscription(context: Context, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.log(context, "SUBSCRIPTION", "Обновление подписки со ссылки: $url")
                repository.updateSubscription(url)
                Toast.makeText(context, "Подписка обновлена!", Toast.LENGTH_SHORT).show()
                AppLogger.log(context, "SUBSCRIPTION", "Подписка успешно обновлена.")
                pingAllProfiles()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                AppLogger.log(context, "SUBSCRIPTION", "Ошибка при обновлении подписки: ${e.message}")
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

    // --- ADMIN & SUPPORT SYSTEM IMPLEMENTATION ---

    fun adminLogin(username: String, pass: String, context: Context): Boolean {
        val user = dbHelper.authenticateUser(username, pass)
        if (user != null) {
            if (user.isBlocked) {
                Toast.makeText(context, "Имя пользователя заблокировано главным администратором Роскомнадзора!", Toast.LENGTH_LONG).show()
                AppLogger.log(context, "AUTH", "Попытка входа в заблокированный аккаунт: ${user.username}")
                return false
            }
            _loginUserAccount.value = user
            _userProfile.value = UserProfile(
                email = "${user.role.lowercase()}@meravpn.io",
                username = user.username,
                planType = getPlanForRole(user.role)
            )
            
            sharedPrefs.edit()
                .putString("user_email", "${user.role.lowercase()}@meravpn.io")
                .putString("user_name", user.username)
                .putString("user_role", user.role)
                .apply()

            Toast.makeText(context, "Добро пожаловать, ${user.username}!", Toast.LENGTH_SHORT).show()
            AppLogger.log(context, "AUTH", "Успешная авторизация пользователя: ${user.username} [${user.role}]")
            return true
        } else {
            Toast.makeText(context, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun adminRegister(username: String, pass: String, role: String, context: Context): Boolean {
        if (username.isBlank() || pass.isBlank()) {
            Toast.makeText(context, "Логин и пароль не могут быть пустыми!", Toast.LENGTH_SHORT).show()
            return false
        }
        val isBlocked = (role == "ROSKOMNADZOR") // Seeding default blocked easter egg
        val newUser = UserAccountEntity(username, role, isBlocked)
        val success = dbHelper.insertUser(newUser, pass)
        if (success) {
            Toast.makeText(context, "Аккаунт $username ($role) зарегистрирован!", Toast.LENGTH_SHORT).show()
            AppLogger.log(context, "AUTH", "Регистрация успешна: $username с ролью $role")
            return true
        } else {
            Toast.makeText(context, "Ошибка: имя пользователя уже занято!", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun adminLogout() {
        _loginUserAccount.value = null
        _userProfile.value = null
        sharedPrefs.edit()
            .remove("user_email")
            .remove("user_name")
            .remove("user_role")
            .apply()
    }

    fun adminSetBlock(username: String, isBlocked: Boolean, context: Context) {
        dbHelper.setUserBlockState(username, isBlocked)
        Toast.makeText(context, "Статус блокировки $username изменен: " + if(isBlocked) "Блок" else "Активен", Toast.LENGTH_SHORT).show()
        AppLogger.log(context, "ADMIN_CTRL", "Изменен статус блокировки пользователя $username на isBlocked=$isBlocked")
    }

    fun sendSupportMessage(sender: String, text: String, context: Context) {
        if (text.isBlank()) return
        dbHelper.insertSupportMessage(sender, text)
        Toast.makeText(context, "Вопрос обратной связи отправлен модераторам!", Toast.LENGTH_SHORT).show()
        AppLogger.log(context, "SUPPORT", "Пользователь $sender отправил тикет: $text")
    }

    fun replySupportMessage(id: Int, replyBy: String, replyText: String, replyRole: String, context: Context) {
        if (replyText.isBlank()) return
        dbHelper.replyToSupportMessage(id, replyBy, replyText, replyRole)
        Toast.makeText(context, "Ответ на тикет #$id отправлен!", Toast.LENGTH_SHORT).show()
        AppLogger.log(context, "SUPPORT", "Модератор $replyBy [$replyRole] ответил на тикет #$id: $replyText")
    }

    fun sendGlobalAnnouncement(sender: String, text: String, context: Context) {
        if (text.isBlank()) return
        dbHelper.insertAnnouncement(sender, text)
        Toast.makeText(context, "Уведомление отправлено всем пользователям!", Toast.LENGTH_SHORT).show()
        AppLogger.log(context, "BROADCAST", "$sender разослал уведомление: $text")
    }

    fun getPlanForRole(role: String): String {
        return when (role) {
            "CREATOR" -> "Главный Создатель | Михаил Попов"
            "SENIOR_ADMIN" -> "Старшая Администрация"
            "JUNIOR_ADMIN" -> "Младший Модератор"
            "ROSKOMNADZOR" -> "Блокировщик РКН"
            else -> "Покупатель"
        }
    }

    fun getRoleLabel(role: String): String {
        return when (role) {
            "CREATOR" -> "Создатель (Владелец)"
            "SENIOR_ADMIN" -> "Старший админ"
            "JUNIOR_ADMIN" -> "Младший модератор"
            "ROSKOMNADZOR" -> "Роскомнадзор"
            else -> "Пользователь"
        }
    }
}
