package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["address"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systemSmsId: Long = -1, // ID del SMS en el ContentProvider de Android si aplica
    val threadId: Long = 0,
    val address: String, // Número de teléfono del destinatario/remitente
    val contactName: String? = null,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSentByUser: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val simSlot: Int = 0, // 0 = SIM 1, 1 = SIM 2, -1 = Desconocido
    val subId: Int = -1, // Subscription ID de Android Telephony
    val isRead: Boolean = true
)
