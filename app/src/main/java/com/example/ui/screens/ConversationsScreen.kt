package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ConversationItem
import com.example.data.model.MessageStatus
import com.example.sms.SimInfo
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.util.DateTimeUtils
import com.example.ui.viewmodel.SmsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    state: SmsUiState,
    onSearchQueryChange: (String) -> Unit,
    onConversationClick: (String, Long) -> Unit,
    onNewMessageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onSyncSms: () -> Unit,
    onDeleteConversation: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<ConversationItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                onSearchQueryChange("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cerrar búsqueda"
                            )
                        }
                    }
                },
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_conversations),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 19.sp
                                )
                                val unreadCount = state.conversations.count { it.unreadCount > 0 }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${state.conversations.size} conversaciones",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (unreadCount > 0) {
                                        Text(
                                            text = " • $unreadCount no leídas",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("search_icon_button")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                        }

                        IconButton(
                            onClick = onSyncSms,
                            modifier = Modifier.testTag("sync_icon_button")
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sincronizar SMS")
                            }
                        }

                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.testTag("settings_icon_button")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Configuración")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewMessageClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_new_message")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_message)
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!state.isDefaultSmsApp) {
                DefaultAppBanner(
                    onRequestDefault = onRequestDefaultSms,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.availableSims.isNotEmpty()) {
                SimStatusBar(sims = state.availableSims, selectedSim = state.selectedSim)
            }

            if (state.conversations.isEmpty()) {
                EmptyConversationsView(
                    isSearch = state.searchQuery.isNotBlank(),
                    onNewMessageClick = onNewMessageClick
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.conversations,
                        key = { "${it.address}_${it.threadId}" }
                    ) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.address, conversation.threadId) },
                            onLongClick = { conversationToDelete = conversation }
                        )
                    }
                }
            }
        }
    }

    conversationToDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text(stringResource(R.string.delete_conversation)) },
            text = { Text(stringResource(R.string.confirm_delete_conversation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation(conversation.address, conversation.threadId)
                        conversationToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DefaultAppBanner(
    onRequestDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SMS Connect no es la app predeterminada",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Configúrala para enviar y recibir SMS con el sistema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRequestDefault,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_request_default_sms")
            ) {
                Text(text = "Activar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimStatusBar(
    sims: List<SimInfo>,
    selectedSim: SimInfo?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (sims.size > 1) "Dual SIM Activo (${sims.size} tarjetas)" else "SIM Móvil Activa",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sims.forEach { sim ->
                    val isSelected = selectedSim?.subscriptionId == sim.subscriptionId
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = "SIM ${sim.slotIndex + 1}: ${if (sim.carrierName.isNotBlank()) sim.carrierName else "Red"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationRow(
    conversation: ConversationItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayName = conversation.contactName ?: conversation.address
    val initials = displayName.take(2).uppercase()

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conversation_row_${conversation.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conversation.photoUri != null) {
                AsyncImage(
                    model = conversation.photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                val avatarBg = remember(conversation.address) {
                    val colors = listOf(PrimaryBlue, SecondaryTeal, Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF59E0B))
                    colors[Math.abs(conversation.address.hashCode()) % colors.size]
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = DateTimeUtils.formatRelativeTime(conversation.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageStatusIcon(status = conversation.lastMessageStatus)

                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    when (status) {
        MessageStatus.PENDING -> {
            Text(text = "🕒 ", fontSize = 11.sp)
        }
        MessageStatus.SENDING -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(10.dp),
                strokeWidth = 1.2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(14.dp)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = StatusSuccess,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(14.dp)
            )
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
fun EmptyConversationsView(
    isSearch: Boolean,
    onNewMessageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearch) Icons.Default.Search else Icons.Default.Sms,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isSearch) "No se encontraron mensajes" else stringResource(R.string.no_conversations),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isSearch) "Prueba a buscar con otro número o palabra clave." else stringResource(R.string.start_new_chat_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearch) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNewMessageClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_new_message_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.new_message))
                }
            }
        }
    }
}
