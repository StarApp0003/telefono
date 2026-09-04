package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.ChatThemeOption
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.SmsUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SmsUiState,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onSetPreferredSim: (Int) -> Unit,
    onThemeModeChange: (Int) -> Unit,
    onChatThemeColorChange: (Int) -> Unit,
    onInChatSoundsChange: (Boolean) -> Unit,
    onTestSound: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = when (state.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val currentChatTheme = ChatThemeOption.getById(state.chatThemeColor)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Personalización de Chats (Colores de burbujas)
            SettingsHeader(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.chat_customization_title),
                subtitle = stringResource(R.string.chat_bubble_color_desc)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.chat_bubble_color_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona el estilo visual que prefieras para tus conversaciones:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selector de temas de color en chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ChatThemeOption.ALL_THEMES.forEach { theme ->
                            val isSelected = state.chatThemeColor == theme.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    theme.primaryColor.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, theme.primaryColor)
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onChatThemeColorChange(theme.id) }
                                    .testTag("chat_theme_option_${theme.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(theme.gradientColors)
                                            )
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = theme.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vista previa en vivo del chat
                    Text(
                        text = stringResource(R.string.preview_chat_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Burbuja recibida de muestra
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                                    color = if (isDark) currentChatTheme.receivedBubbleDark else currentChatTheme.receivedBubbleLight,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = stringResource(R.string.preview_incoming_msg),
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "10:42 AM",
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }

                            // Burbuja enviada de muestra
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                                    color = Color.Transparent,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .background(
                                            Brush.horizontalGradient(currentChatTheme.gradientColors),
                                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = stringResource(R.string.preview_outgoing_msg),
                                            fontSize = 13.sp,
                                            color = currentChatTheme.onSentColor
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(
                                                text = "10:43 AM",
                                                fontSize = 10.sp,
                                                color = currentChatTheme.onSentColor.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = currentChatTheme.onSentColor.copy(alpha = 0.9f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Sonidos en el Chat (Estilo WhatsApp)
            SettingsHeader(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.chat_sounds_title),
                subtitle = "Efectos auditivos en tiempo real durante la conversación"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sonidos al enviar y recibir",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.chat_sounds_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.inChatSoundsEnabled,
                            onCheckedChange = onInChatSoundsChange,
                            modifier = Modifier.testTag("in_chat_sounds_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Escucha una prueba de los sonidos nativos:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onTestSound(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_sent_sound_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMade,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.test_sent_sound), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalButton(
                            onClick = { onTestSound(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_received_sound_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallReceived,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.test_received_sound), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 3. Aplicación Predeterminada
            SettingsHeader(
                icon = Icons.Default.Sms,
                title = stringResource(R.string.default_sms_app),
                subtitle = "Gestión principal de mensajería del sistema"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isDefaultSmsApp) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isDefaultSmsApp) stringResource(R.string.already_default) else "Establecer como app principal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (state.isDefaultSmsApp) "SMS Connect gestiona todos los mensajes del sistema." else "Permite responder rápido, recibir SMS y gestionar el historial nativo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!state.isDefaultSmsApp) {
                        Button(
                            onClick = onRequestDefaultSms,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("settings_set_default_button")
                        ) {
                            Text("Activar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Activo",
                                tint = StatusSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 4. Gestión de Tarjetas SIM
            SettingsHeader(
                icon = Icons.Default.SimCard,
                title = stringResource(R.string.sim_settings_title),
                subtitle = "Configura la línea por defecto para enviar mensajes"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.preferred_sim),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSetPreferredSim(-1) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.preferredSubId == -1,
                            onClick = { onSetPreferredSim(-1) },
                            modifier = Modifier.testTag("sim_pref_auto")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = stringResource(R.string.auto_sim), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Preguntar o usar la SIM activa del sistema", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    state.availableSims.forEach { sim ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSetPreferredSim(sim.subscriptionId) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.preferredSubId == sim.subscriptionId,
                                onClick = { onSetPreferredSim(sim.subscriptionId) },
                                modifier = Modifier.testTag("sim_pref_${sim.slotIndex}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SIM ${sim.slotIndex + 1}: ${sim.displayName} (${sim.carrierName.ifBlank { "Operador" }})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                if (sim.number.isNotBlank()) {
                                    Text(text = sim.number, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Tema del Sistema
            SettingsHeader(
                icon = Icons.Default.ColorLens,
                title = stringResource(R.string.theme_title),
                subtitle = "Modo claro, oscuro o sincronizado con el sistema"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val themes = listOf(
                        0 to stringResource(R.string.theme_system),
                        1 to stringResource(R.string.theme_light),
                        2 to stringResource(R.string.theme_dark)
                    )
                    themes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onThemeModeChange(mode) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                modifier = Modifier.testTag("theme_radio_$mode")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 6. Notificaciones en Segundo Plano
            SettingsHeader(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notifications_settings),
                subtitle = "Alertas en la barra de estado de Android"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.enable_notifications), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = stringResource(R.string.enable_notifications_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = onNotificationsChange,
                        modifier = Modifier.testTag("notifications_switch")
                    )
                }
            }

            // 7. Permisos del Dispositivo
            SettingsHeader(
                icon = Icons.Default.PhoneAndroid,
                title = "Permisos del Dispositivo",
                subtitle = "Estado de las autorizaciones otorgadas"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PermissionStatusRow(
                        title = "Mensajes SMS (Envío y Lectura)",
                        description = "Requerido para enviar, recibir y leer SMS",
                        granted = isPermissionGranted(context, Manifest.permission.SEND_SMS) &&
                                isPermissionGranted(context, Manifest.permission.READ_SMS)
                    )
                    PermissionStatusRow(
                        title = "Contactos",
                        description = "Requerido para ver nombres y fotos en chats",
                        granted = isPermissionGranted(context, Manifest.permission.READ_CONTACTS)
                    )
                    PermissionStatusRow(
                        title = "Teléfono / SIMs",
                        description = "Requerido para detectar tarjetas SIM duales",
                        granted = isPermissionGranted(context, Manifest.permission.READ_PHONE_STATE)
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        PermissionStatusRow(
                            title = "Notificaciones",
                            description = "Requerido para avisos de mensajes entrantes",
                            granted = isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = onRequestPermissions,
                            modifier = Modifier.testTag("request_permissions_button")
                        ) {
                            Text("Solicitar permisos", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                val appSettings = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(appSettings)
                            },
                            modifier = Modifier.testTag("open_system_settings_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.open_settings))
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 8. Acerca de
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = "SMS Connect", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = stringResource(R.string.version), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Personalización visual y efectos de sonido en tiempo real", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (granted) "Concedido" else "Denegado",
            tint = if (granted) StatusSuccess else StatusError,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (granted) "Permitido" else "No otorgado",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) StatusSuccess else StatusError
        )
    }
}

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
