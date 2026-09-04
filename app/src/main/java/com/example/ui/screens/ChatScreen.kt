package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MessageItem
import com.example.data.model.MessageStatus
import com.example.sms.SimInfo
import com.example.ui.theme.ChatThemeOption
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.util.DateTimeUtils
import com.example.ui.viewmodel.SmsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    address: String,
    state: SmsUiState,
    onBack: () -> Unit,
    onSendMessage: (String, SimInfo?) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onDeleteConversation: (String, Long) -> Unit,
    onSelectSim: (SimInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textMessage by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<MessageItem?>(null) }
    var selectedMessageForDetails by remember { mutableStateOf<MessageItem?>(null) }
    var showDeleteConvoDialog by remember { mutableStateOf(false) }

    val isDark = when (state.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val chatTheme = remember(state.chatThemeColor) {
        ChatThemeOption.getById(state.chatThemeColor)
    }

    val listState = rememberLazyListState()

    val messages = state.activeChatMessages
    val contactName = state.activeChatContactName ?: address

    // Desplazar automáticamente al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Avatar palette generator
    val avatarColors = remember(address) {
        val palettes = listOf(
            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
            listOf(Color(0xFF0D9488), Color(0xFF0F766E)),
            listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
            listOf(Color(0xFFDB2777), Color(0xFFBE185D)),
            listOf(Color(0xFFD97706), Color(0xFFB45309)),
            listOf(Color(0xFF059669), Color(0xFF047857))
        )
        palettes[Math.abs(address.hashCode()) % palettes.size]
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Copiar número con feedback rápido
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Teléfono", address))
                                Toast.makeText(context, "Número copiado: $address", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        // Avatar estilizado con degradado
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(avatarColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contactName.trim().take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contactName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                state.selectedSim?.let { sim ->
                                    Text(
                                        text = "• SIM ${sim.slotIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Botón para llamar directamente
                    IconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$address"))
                            context.startActivity(dialIntent)
                        },
                        modifier = Modifier.testTag("chat_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = stringResource(R.string.call_contact),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Menú de opciones
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("chat_more_menu")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.call_contact)) },
                            onClick = {
                                showMenu = false
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$address"))
                                context.startActivity(dialIntent)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_number)) },
                            onClick = {
                                showMenu = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Teléfono", address))
                                Toast.makeText(context, context.getString(R.string.number_copied), Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_conversation), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConvoDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Contenedor principal de mensajes o estado inicial
            if (messages.isEmpty()) {
                // Estado vacío atractivo con sugerencias de inicio rápido
                EmptyChatWelcome(
                    contactName = contactName,
                    address = address,
                    avatarColors = avatarColors,
                    onSelectChip = { suggestion ->
                        textMessage = suggestion
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                // Lista de Mensajes con separadores de fecha
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var lastHeaderDate = ""

                    messages.forEachIndexed { index, msg ->
                        val currentDateHeader = DateTimeUtils.formatHeaderDate(msg.timestamp)
                        if (currentDateHeader != lastHeaderDate) {
                            lastHeaderDate = currentDateHeader
                            item(key = "date_header_${msg.timestamp}_$index") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        tonalElevation = 1.dp
                                    ) {
                                        Text(
                                            text = currentDateHeader,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "msg_${msg.id}") {
                            MessageBubble(
                                message = msg,
                                isMultiSim = state.availableSims.size > 1,
                                chatTheme = chatTheme,
                                isDark = isDark,
                                onLongClick = { selectedMessageForOptions = msg },
                                onRetrySend = {
                                    onSendMessage(msg.body, state.selectedSim)
                                }
                            )
                        }
                    }
                }
            }

            // Barra de Selección Rápida de SIM (si el dispositivo cuenta con Dual SIM)
            if (state.availableSims.size > 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
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
                                text = "Enviar mediante:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availableSims.forEach { sim ->
                                val isSelected = (state.selectedSim?.subscriptionId == sim.subscriptionId) ||
                                        (state.selectedSim == null && sim.slotIndex == 0)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clickable { onSelectSim(sim) }
                                        .testTag("chat_sim_selector_${sim.slotIndex}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SIM ${sim.slotIndex + 1}: ${sim.carrierName.ifBlank { "Operador" }.take(10)}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Barra Inferior de Entrada de Mensaje (Dock Moderno con Selector de Emojis)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Panel Expansible de Emojis Rápidos
                    AnimatedVisibility(
                        visible = showEmojiPicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                val emojiCategories = listOf(
                                    listOf("😀", "😂", "🥰", "😍", "😊", "😎", "🤔", "🥺"),
                                    listOf("👍", "🙏", "❤️", "🔥", "✨", "🎉", "👏", "🙌"),
                                    listOf("👋", "🤝", "💪", "💯", "👀", "🚀", "💡", "⭐")
                                )
                                emojiCategories.forEachIndexed { rowIndex, rowList ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        rowList.forEach { emoji ->
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Transparent,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        textMessage += emoji
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = emoji,
                                                        fontSize = 20.sp,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Contador dinámico de caracteres y partes SMS
                    if (textMessage.isNotBlank()) {
                        val length = textMessage.length
                        val parts = if (length <= 160) 1 else ((length - 1) / 153) + 1
                        val maxForSingle = if (parts == 1) 160 else parts * 153
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "$length/$maxForSingle • $parts SMS",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cápsula contenedora de entrada moderna con botón de emojis integrado
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón de emoji a la izquierda
                                IconButton(
                                    onClick = { showEmojiPicker = !showEmojiPicker },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("chat_emoji_button")
                                ) {
                                    Icon(
                                        imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                                        contentDescription = if (showEmojiPicker) stringResource(R.string.close_emoji_picker) else stringResource(R.string.emoji_picker_title),
                                        tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Campo de texto para escribir el mensaje
                                OutlinedTextField(
                                    value = textMessage,
                                    onValueChange = { textMessage = it },
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.type_message),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    maxLines = 5,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val canSend = textMessage.trim().isNotBlank()

                        Surface(
                            shape = CircleShape,
                            color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (canSend) 3.dp else 0.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canSend) {
                                    val toSend = textMessage.trim()
                                    textMessage = ""
                                    showEmojiPicker = false
                                    onSendMessage(toSend, state.selectedSim)
                                }
                                .testTag("chat_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.send),
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Opciones al Mantener Presionado un Mensaje
    selectedMessageForOptions?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = {
                Text(
                    text = "Opciones del mensaje",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg.body,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SMS", msg.body))
                                Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.copy_message), fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForDetails = msg
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.message_details), fontWeight = FontWeight.Medium)
                    }

                    if (msg.isSentByUser) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSendMessage(msg.body, state.selectedSim)
                                    selectedMessageForOptions = null
                                    Toast.makeText(context, "Reenviando SMS...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = stringResource(R.string.resend_message), fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeleteMessage(msg.id)
                                selectedMessageForOptions = null
                                Toast.makeText(context, "Mensaje eliminado", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.delete),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForOptions = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Modal de Detalles del Mensaje
    selectedMessageForDetails?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForDetails = null },
            title = {
                Text(
                    text = stringResource(R.string.message_details),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(label = "Tipo", value = if (msg.isSentByUser) "Mensaje saliente (Enviado)" else "Mensaje entrante (Recibido)")
                    DetailRow(label = "Destinatario / Remitente", value = msg.address)
                    DetailRow(label = "Fecha y hora", value = DateTimeUtils.formatMessageTime(msg.timestamp))
                    DetailRow(label = "Estado", value = when (msg.status) {
                        MessageStatus.PENDING -> "Pendiente"
                        MessageStatus.SENDING -> "Enviando..."
                        MessageStatus.SENT -> "Enviado al operador móvil"
                        MessageStatus.DELIVERED -> "Entregado con éxito"
                        MessageStatus.FAILED -> "Error de envío"
                    })
                    DetailRow(label = "Ranura SIM", value = if (msg.simSlot >= 0) "SIM ${msg.simSlot + 1}" else "Automática / Sistema")
                    DetailRow(label = "Caracteres", value = "${msg.body.length} caracteres (${if (msg.body.length <= 160) "1 SMS" else "${((msg.body.length - 1)/153) + 1} partes"})")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForDetails = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de Confirmación para Borrar la Conversación
    if (showDeleteConvoDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConvoDialog = false },
            title = { Text(stringResource(R.string.delete_conversation), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_delete_conversation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConvoDialog = false
                        onDeleteConversation(address, address.hashCode().toLong())
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConvoDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyChatWelcome(
    contactName: String,
    address: String,
    avatarColors: List<Color>,
    onSelectChip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(avatarColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contactName.trim().take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = contactName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (contactName != address) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.chat_starter_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Respuestas rápidas sugeridas:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        val suggestions = listOf(
            "¡Hola! 👋",
            "¿Cómo estás?",
            "¿Qué tal todo?",
            "Avísame cuando puedas",
            "¿Estás disponible?"
        )

        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { chipText ->
                SuggestionChip(
                    onClick = { onSelectChip(chipText) },
                    label = { Text(chipText, fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageItem,
    isMultiSim: Boolean,
    chatTheme: ChatThemeOption,
    isDark: Boolean,
    onLongClick: () -> Unit,
    onRetrySend: () -> Unit
) {
    val isSent = message.isSentByUser
    val alignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart

    // Formas de burbuja asimétricas y pulidas
    val bubbleShape = if (isSent) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val textColor = if (isSent) {
        chatTheme.onSentColor
    } else {
        if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    }

    val metadataColor = if (isSent) {
        chatTheme.onSentColor.copy(alpha = 0.8f)
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = alignment
    ) {
        val bubbleModifier = if (isSent) {
            Modifier
                .widthIn(min = 80.dp, max = 290.dp)
                .shadow(elevation = 1.5.dp, shape = bubbleShape)
                .clip(bubbleShape)
                .background(Brush.horizontalGradient(chatTheme.gradientColors))
        } else {
            Modifier
                .widthIn(min = 80.dp, max = 290.dp)
                .shadow(elevation = 0.5.dp, shape = bubbleShape)
                .clip(bubbleShape)
                .background(if (isDark) chatTheme.receivedBubbleDark else chatTheme.receivedBubbleLight)
        }

        Box(
            modifier = bubbleModifier.testTag("chat_bubble_${message.id}")
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 6.dp
                )
            ) {
                Text(
                    text = message.body,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de ranura SIM en burbuja si aplica
                    if (isMultiSim && message.simSlot >= 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSent) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "S${message.simSlot + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = metadataColor,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Hora del mensaje
                    Text(
                        text = DateTimeUtils.formatMessageTime(message.timestamp),
                        fontSize = 10.sp,
                        color = metadataColor
                    )

                    // Estado del mensaje saliente
                    if (isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.PENDING -> {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Pendiente",
                                    tint = metadataColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.SENDING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.2.dp,
                                    color = textColor
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Enviado",
                                    tint = metadataColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Entregado",
                                    tint = Color(0xFF86EFAC),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.FAILED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onRetrySend() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error al enviar",
                                        tint = StatusError,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Reintentar",
                                        fontSize = 10.sp,
                                        color = StatusError,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
