package com.example.data.model

data class ConversationItem(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val photoUri: String? = null,
    val simSlot: Int = 0,
    val lastMessageStatus: MessageStatus = MessageStatus.SENT,
    val isLastMessageSentByUser: Boolean = false
)
