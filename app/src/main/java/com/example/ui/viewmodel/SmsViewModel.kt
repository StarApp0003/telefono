package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactItem
import com.example.data.model.ConversationItem
import com.example.data.model.MessageItem
import com.example.data.repository.SmsRepository
import com.example.sms.SimInfo
import com.example.ui.util.SoundEffectHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val _searchQuery = MutableStateFlow("")
    private val _contactSearchQuery = MutableStateFlow("")
    private val _activeChatAddress = MutableStateFlow<String?>(null)
    private val _activeChatContactName = MutableStateFlow<String?>(null)
    private val _activeChatMessages = MutableStateFlow<List<MessageItem>>(emptyList())
    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
    private val _selectedSim = MutableStateFlow<SimInfo?>(null)
    private val _preferredSubId = MutableStateFlow(repository.getPreferredSubscriptionId())
    private val _isDefaultSmsApp = MutableStateFlow(repository.isDefaultSmsApp())
    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    private val _chatThemeColor = MutableStateFlow(repository.getChatThemeColor())
    private val _inChatSoundsEnabled = MutableStateFlow(repository.isInChatSoundsEnabled())
    private val _notificationsEnabled = MutableStateFlow(repository.isNotificationsEnabled())
    private val _isSyncing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SmsUiState> = combine(
        repository.getConversations(),
        _searchQuery,
        _activeChatMessages,
        _activeChatAddress,
        _contacts
    ) { conversations: List<ConversationItem>, query: String, messages: List<MessageItem>, activeAddr: String?, contacts: List<ContactItem> ->
        val filteredConversations = if (query.isBlank()) {
            conversations
        } else {
            conversations.filter { conv ->
                conv.address.contains(query, ignoreCase = true) ||
                (conv.contactName?.contains(query, ignoreCase = true) == true) ||
                conv.lastMessage.contains(query, ignoreCase = true)
            }
        }

        SmsUiState(
            conversations = filteredConversations,
            searchQuery = query,
            activeChatMessages = messages,
            activeChatAddress = activeAddr,
            activeChatContactName = _activeChatContactName.value,
            contacts = contacts,
            contactSearchQuery = _contactSearchQuery.value,
            availableSims = _availableSims.value,
            selectedSim = _selectedSim.value ?: _availableSims.value.firstOrNull(),
            preferredSubId = _preferredSubId.value,
            isDefaultSmsApp = _isDefaultSmsApp.value,
            themeMode = _themeMode.value,
            chatThemeColor = _chatThemeColor.value,
            inChatSoundsEnabled = _inChatSoundsEnabled.value,
            notificationsEnabled = _notificationsEnabled.value,
            isSyncing = _isSyncing.value,
            errorMessage = _errorMessage.value,
            successMessage = _successMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SmsUiState()
    )

    init {
        refreshSims()
        checkDefaultSmsApp()
        syncSms()
        loadContacts()
    }

    fun syncSms() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncDeviceSms()
            } catch (e: Exception) {
                // Log and swallow
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refreshSims() {
        val sims = repository.getAvailableSimCards()
        _availableSims.value = sims
        val preferred = repository.getPreferredSubscriptionId()
        _preferredSubId.value = preferred
        val foundPref = sims.find { it.subscriptionId == preferred }
        _selectedSim.value = foundPref ?: sims.firstOrNull()
    }

    fun checkDefaultSmsApp() {
        _isDefaultSmsApp.value = repository.isDefaultSmsApp()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onContactSearchQueryChanged(query: String) {
        _contactSearchQuery.value = query
        viewModelScope.launch {
            _contacts.value = repository.searchContacts(query)
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = repository.searchContacts(_contactSearchQuery.value)
        }
    }

    fun selectSim(sim: SimInfo) {
        _selectedSim.value = sim
    }

    fun setPreferredSim(subId: Int) {
        repository.setPreferredSubscriptionId(subId)
        _preferredSubId.value = subId
        refreshSims()
    }

    fun setThemeMode(mode: Int) {
        repository.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setChatThemeColor(colorId: Int) {
        repository.setChatThemeColor(colorId)
        _chatThemeColor.value = colorId
    }

    fun setInChatSoundsEnabled(enabled: Boolean) {
        repository.setInChatSoundsEnabled(enabled)
        _inChatSoundsEnabled.value = enabled
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
        _notificationsEnabled.value = enabled
    }

    private var chatMessagesJob: kotlinx.coroutines.Job? = null

    fun openConversation(address: String, threadId: Long = address.hashCode().toLong()) {
        chatMessagesJob?.cancel()
        _activeChatAddress.value = address
        _activeChatContactName.value = repository.resolveContactName(address)
        _activeChatMessages.value = emptyList() // Limpiar mensajes anteriores de inmediato

        val openTime = System.currentTimeMillis()
        var initialLoaded = false

        chatMessagesJob = viewModelScope.launch {
            repository.markConversationAsRead(address)
            repository.getMessagesForConversation(address, threadId).collect { messages ->
                if (_activeChatAddress.value == address) {
                    val prevMessages = _activeChatMessages.value
                    _activeChatMessages.value = messages

                    // Si llega un nuevo mensaje recibido mientras estamos dentro de este chat, sonar
                    if (initialLoaded && messages.size > prevMessages.size) {
                        val latest = messages.lastOrNull()
                        if (latest != null && !latest.isSentByUser && latest.timestamp >= (openTime - 5000)) {
                            if (_inChatSoundsEnabled.value) {
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
        _activeChatAddress.value = null
        _activeChatContactName.value = null
        _activeChatMessages.value = emptyList()
    }

    fun sendSms(
        destinationAddress: String,
        body: String,
        simInfo: SimInfo? = _selectedSim.value,
        onSuccess: (() -> Unit)? = null
    ) {
        if (destinationAddress.isBlank() || body.isBlank()) {
            _errorMessage.value = "Por favor ingresa un número de teléfono y el mensaje."
            return
        }

        // Sonido de envío estilo WhatsApp al accionar el envío
        if (_inChatSoundsEnabled.value) {
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
                _successMessage.value = "SMS enviado exitosamente"
                onSuccess?.invoke()
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Error desconocido al enviar SMS"
                _errorMessage.value = "Fallo de envío: $error"
            }
        }
    }

    fun deleteConversation(address: String, threadId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(address, threadId)
            if (_activeChatAddress.value == address) {
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
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
