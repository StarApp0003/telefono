package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.receiver.NotificationActionReceiver
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.NewMessageScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SMSConnectTheme
import com.example.ui.viewmodel.SmsViewModel
import kotlinx.coroutines.launch

sealed class Screen {
    data object Conversations : Screen()
    data class Chat(val address: String, val threadId: Long) : Screen()
    data object NewMessage : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: SmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Conversations) }

            val permissionsToRequest = remember {
                val list = mutableListOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_PHONE_STATE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                list.toTypedArray()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val anyGranted = results.values.any { it }
                if (anyGranted) {
                    viewModel.syncSms()
                    viewModel.loadContacts()
                    viewModel.refreshSims()
                }
            }

            val defaultSmsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.checkDefaultSmsApp()
                viewModel.syncSms()
            }

            fun requestDefaultSmsApp() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                            if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                defaultSmsLauncher.launch(intent)
                                return
                            }
                        }
                    }
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                    }
                    defaultSmsLauncher.launch(intent)
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar("No se pudo abrir el selector de app de SMS predeterminada")
                    }
                }
            }

            LaunchedEffect(Unit) {
                val hasAll = permissionsToRequest.all {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
                }
                if (!hasAll) {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.checkDefaultSmsApp()
                        viewModel.refreshSims()
                        viewModel.syncSms()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let { error ->
                    snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
                    viewModel.clearErrorMessage()
                }
            }

            LaunchedEffect(uiState.successMessage) {
                uiState.successMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                    viewModel.clearSuccessMessage()
                }
            }

            LaunchedEffect(intent) {
                handleIntent(intent) { targetAddress ->
                    viewModel.openConversation(targetAddress)
                    currentScreen = Screen.Chat(targetAddress, targetAddress.hashCode().toLong())
                }
            }

            val isDarkTheme = when (uiState.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            SMSConnectTheme(darkTheme = isDarkTheme) {
                BackHandler(enabled = currentScreen !is Screen.Conversations) {
                    if (currentScreen is Screen.Chat) {
                        viewModel.closeActiveConversation()
                    }
                    currentScreen = Screen.Conversations
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Conversations -> {
                            ConversationsScreen(
                                state = uiState,
                                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                                onConversationClick = { address, threadId ->
                                    viewModel.openConversation(address, threadId)
                                    currentScreen = Screen.Chat(address, threadId)
                                },
                                onNewMessageClick = {
                                    viewModel.onContactSearchQueryChanged("")
                                    currentScreen = Screen.NewMessage
                                },
                                onSettingsClick = {
                                    currentScreen = Screen.Settings
                                },
                                onRequestDefaultSms = { requestDefaultSmsApp() },
                                onSyncSms = {
                                    permissionLauncher.launch(permissionsToRequest)
                                    viewModel.syncSms()
                                },
                                onDeleteConversation = { addr, threadId ->
                                    viewModel.deleteConversation(addr, threadId)
                                },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }

                        is Screen.Chat -> {
                            ChatScreen(
                                address = screen.address,
                                state = uiState,
                                onBack = {
                                    viewModel.closeActiveConversation()
                                    currentScreen = Screen.Conversations
                                },
                                onSendMessage = { text, sim ->
                                    viewModel.sendSms(
                                        destinationAddress = screen.address,
                                        body = text,
                                        simInfo = sim
                                    )
                                },
                                onDeleteMessage = { id ->
                                    viewModel.deleteMessage(id)
                                },
                                onDeleteConversation = { addr, threadId ->
                                    viewModel.deleteConversation(addr, threadId)
                                    currentScreen = Screen.Conversations
                                },
                                onSelectSim = { sim ->
                                    viewModel.selectSim(sim)
                                },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }

                        is Screen.NewMessage -> {
                            NewMessageScreen(
                                state = uiState,
                                onBack = {
                                    currentScreen = Screen.Conversations
                                },
                                onContactSearchQueryChange = { query ->
                                    viewModel.onContactSearchQueryChanged(query)
                                },
                                onSendMessage = { dest, text, sim ->
                                    viewModel.sendSms(
                                        destinationAddress = dest,
                                        body = text,
                                        simInfo = sim
                                    ) {
                                        viewModel.openConversation(dest)
                                        currentScreen = Screen.Chat(dest, dest.hashCode().toLong())
                                    }
                                },
                                onSelectContactForChat = { dest ->
                                    viewModel.openConversation(dest)
                                    currentScreen = Screen.Chat(dest, dest.hashCode().toLong())
                                },
                                onSelectSim = { sim ->
                                    viewModel.selectSim(sim)
                                },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }

                        is Screen.Settings -> {
                            SettingsScreen(
                                state = uiState,
                                onBack = {
                                    currentScreen = Screen.Conversations
                                },
                                onRequestDefaultSms = { requestDefaultSmsApp() },
                                onSetPreferredSim = { subId ->
                                    viewModel.setPreferredSim(subId)
                                },
                                onThemeModeChange = { mode ->
                                    viewModel.setThemeMode(mode)
                                },
                                onChatThemeColorChange = { colorId ->
                                    viewModel.setChatThemeColor(colorId)
                                },
                                onInChatSoundsChange = { enabled ->
                                    viewModel.setInChatSoundsEnabled(enabled)
                                },
                                onTestSound = { isSent ->
                                    viewModel.playTestInChatSound(isSent)
                                },
                                onNotificationsChange = { enabled ->
                                    viewModel.setNotificationsEnabled(enabled)
                                },
                                onRequestPermissions = {
                                    permissionLauncher.launch(permissionsToRequest)
                                },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?, onAddressFound: (String) -> Unit) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_SENDTO == action || Intent.ACTION_VIEW == action) {
            data?.let { uri ->
                val scheme = uri.scheme
                if (scheme == "sms" || scheme == "smsto" || scheme == "mms" || scheme == "mmsto") {
                    val ssp = uri.schemeSpecificPart
                    val address = ssp?.substringBefore("?") ?: ""
                    if (address.isNotBlank()) {
                        onAddressFound(address)
                        return
                    }
                }
            }
        }

        val notifAddress = intent.getStringExtra(NotificationActionReceiver.EXTRA_ADDRESS)
        if (!notifAddress.isNullOrBlank()) {
            onAddressFound(notifAddress)
        }
    }
}
