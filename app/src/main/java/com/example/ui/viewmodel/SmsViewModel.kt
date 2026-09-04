package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactItem
import com.example.data.model.ConversationItem
import com.example.data.model.MessageItem
import com.example.data.repository.SmsRepository
import com.example.notifications.NotificationHelper
import com.example.sms.SimInfo
import com.example.ui.util.SoundEffectHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsUiState(
    val conversations: List<ConversationItem> = emptyList(),
    val searchQuery: String = "",
    val activeChatMessages: List<MessageItem> = emptyList(),
    val activeChatAddress: String? = null,
    val activeChatContactName: String? = null,
    val contacts: List<ContactItem> = emptyList(),
    val contactSearchQuery: String = "",
    val availableSims: List<SimInfo> = emptyList(),
    val selectedSim: SimInfo? = null,
    val preferredSubId: Int = -1,
    val isDefaultSmsApp: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = true,
    val themeMode: Int = 0,
    val chatThemeColor: Int = 0,
    val inChatSoundsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)
    private var allConversations: List<ConversationItem> = emptyList()
    private var chatMessagesJob: Job? = null

    private val _uiState = MutableStateFlow(
        SmsUiState(
            preferredSubId = repository.getPreferredSubscriptionId(),
            isDefaultSmsApp = repository.isDefaultSmsApp(),
            isBatteryOptimizationIgnored = checkBatteryOptimization(),
            themeMode = repository.getThemeMode(),
            chatThemeColor = repository.getChatThemeColor(),
            inChatSoundsEnabled = repository.isInChatSoundsEnabled(),
            notificationsEnabled = repository.isNotificationsEnabled()
        )
    )
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()

    init {
        refreshSims()
        checkDefaultSmsApp()
        refreshBatteryOptimizationStatus()
        observeConversations()
        syncSms()
        loadContacts()
    }

    private fun observeConversations() {
        viewModelScope.launch {
            repository.getConversations().collect { conversations ->
                allConversations = conversations
                val query = _uiState.value.searchQuery
                _uiState.update {
                    it.copy(conversations = filterConversations(conversations, query))
                }
            }
        }
    }

    private fun filterConversations(list: List<ConversationItem>, query: String): List<ConversationItem> {
        if (query.isBlank()) return list
        return list.filter { conv ->
            conv.address.contains(query, ignoreCase = true) ||
            (conv.contactName?.contains(query, ignoreCase = true) == true) ||
            conv.lastMessage.contains(query, ignoreCase = true)
        }
    }

    fun checkBatteryOptimization(): Boolean {
        val app = getApplication<Application>()
        val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(app.packageName) ?: true
        } else {
            true
        }
    }

    fun refreshBatteryOptimizationStatus() {
        val isIgnored = checkBatteryOptimization()
        _uiState.update { it.copy(isBatteryOptimizationIgnored = isIgnored) }
    }

    fun testBackgroundNotification() {
        val notificationHelper = NotificationHelper(getApplication())
        notificationHelper.showTestNotification()
    }

    fun syncSms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                repository.syncDeviceSms()
            } catch (e: Exception) {
                // Ignore failure during background refresh
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun refreshSims() {
        val sims = repository.getAvailableSimCards()
        val preferred = repository.getPreferredSubscriptionId()
        val foundPref = sims.find { it.subscriptionId == preferred } ?: sims.firstOrNull()
        _uiState.update {
            it.copy(
                availableSims = sims,
                preferredSubId = preferred,
                selectedSim = foundPref
            )
        }
    }

    fun checkDefaultSmsApp() {
        val isDefault = repository.isDefaultSmsApp()
        _uiState.update { it.copy(isDefaultSmsApp = isDefault) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                conversations = filterConversations(allConversations, query)
            )
        }
    }

    fun onContactSearchQueryChanged(query: String) {
        _uiState.update { it.copy(contactSearchQuery = query) }
        viewModelScope.launch {
            val results = repository.searchContacts(query)
            _uiState.update { it.copy(contacts = results) }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            val results = repository.searchContacts(_uiState.value.contactSearchQuery)
            _uiState.update { it.copy(contacts = results) }
        }
    }

    fun selectSim(sim: SimInfo) {
        _uiState.update { it.copy(selectedSim = sim) }
    }

    fun setPreferredSim(subId: Int) {
        repository.setPreferredSubscriptionId(subId)
        val sims = _uiState.value.availableSims
        val found = sims.find { it.subscriptionId == subId } ?: sims.firstOrNull()
        _uiState.update {
            it.copy(preferredSubId = subId, selectedSim = found)
        }
    }

    fun setThemeMode(mode: Int) {
        repository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setChatThemeColor(colorId: Int) {
        repository.setChatThemeColor(colorId)
        _uiState.update { it.copy(chatThemeColor = colorId) }
    }

    fun setInChatSoundsEnabled(enabled: Boolean) {
        repository.setInChatSoundsEnabled(enabled)
        _uiState.update { it.copy(inChatSoundsEnabled = enabled) }
    }

    fun playTestInChatSound(isSentSound: Boolean) {
        if (isSentSound) {
            SoundEffectHelper.playSentSound()
        } else {
            SoundEffectHelper.playReceivedSound()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun openConversation(address: String, threadId: Long = address.hashCode().toLong()) {
        chatMessagesJob?.cancel()
        val contactName = repository.resolveContactName(address)
        _uiState.update {
            it.copy(
                activeChatAddress = address,
                activeChatContactName = contactName,
                activeChatMessages = emptyList()
            )
        }

        val openTime = System.currentTimeMillis()
        var initialLoaded = false

        chatMessagesJob = viewModelScope.launch {
            repository.markConversationAsRead(address)
            repository.getMessagesForConversation(address, threadId).collect { messages ->
                if (_uiState.value.activeChatAddress == address) {
                    val prevMessages = _uiState.value.activeChatMessages
                    _uiState.update { it.copy(activeChatMessages = messages) }

                    // Si llega un nuevo mensaje recibido mientras estamos dentro del chat
                    if (initialLoaded && messages.size > prevMessages.size) {
                        val latest = messages.lastOrNull()
                        if (latest != null && !latest.isSentByUser && latest.timestamp >= (openTime - 5000)) {
                            if (_uiState.value.inChatSoundsEnabled) {
                                SoundEffectHelper.playReceivedSound()
                            }
                        }
                    }
                    initialLoaded = true
                }
            }
        }
    }

    fun closeActiveConversation() {
        chatMessagesJob?.cancel()
        chatMessagesJob = null
        _uiState.update {
            it.copy(
                activeChatAddress = null,
                activeChatContactName = null,
                activeChatMessages = emptyList()
            )
        }
    }

    fun sendSms(
        destinationAddress: String,
        body: String,
        simInfo: SimInfo? = _uiState.value.selectedSim,
        onSuccess: (() -> Unit)? = null
    ) {
        if (destinationAddress.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa un número de teléfono y el mensaje.") }
            return
        }

        // Sonido de envío inmediato
        if (_uiState.value.inChatSoundsEnabled) {
            SoundEffectHelper.playSentSound()
        }

        viewModelScope.launch {
            val subId = simInfo?.subscriptionId?.takeIf { it != -1 }
            val simSlot = simInfo?.slotIndex ?: 0
            val result = repository.sendRealSms(
                destinationAddress = destinationAddress,
                messageBody = body,
                subscriptionId = subId,
                simSlot = simSlot
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "SMS enviado exitosamente") }
                onSuccess?.invoke()
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Error desconocido al enviar SMS"
                _uiState.update { it.copy(errorMessage = "Fallo de envío: $error") }
            }
        }
    }

    fun deleteConversation(address: String, threadId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(address, threadId)
            if (_uiState.value.activeChatAddress == address) {
                closeActiveConversation()
            }
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
