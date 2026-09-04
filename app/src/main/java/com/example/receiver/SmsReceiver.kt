package com.example.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.MessageItem
import com.example.data.model.MessageStatus
import com.example.data.repository.SmsRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (!messages.isNullOrEmpty()) {
                    val sender = messages[0].originatingAddress ?: "Desconocido"
                    val timestamp = messages[0].timestampMillis

                    // Unir partes del mensaje si viene dividido
                    val bodyBuilder = StringBuilder()
                    for (sms in messages) {
                        bodyBuilder.append(sms.messageBody ?: "")
                    }
                    val fullBody = bodyBuilder.toString()

                    val repository = SmsRepository(context)
                    val db = AppDatabase.getInstance(context)
                    val contactName = repository.resolveContactName(sender)
                    val threadId = sender.hashCode().toLong()

                    // Guardar en la base de datos local
                    val messageItem = MessageItem(
                        threadId = threadId,
                        address = sender,
                        contactName = contactName,
                        body = fullBody,
                        timestamp = timestamp,
                        isSentByUser = false,
                        status = MessageStatus.DELIVERED,
                        isRead = false
                    )
                    db.messageDao().insertMessage(messageItem)

                    // Si somos la app SMS predeterminada y el broadcast es SMS_DELIVER, guardar en el Inbox del sistema
                    if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || repository.isDefaultSmsApp()) {
                        try {
                            val values = ContentValues().apply {
                                put(Telephony.Sms.ADDRESS, sender)
                                put(Telephony.Sms.BODY, fullBody)
                                put(Telephony.Sms.DATE, timestamp)
                                put(Telephony.Sms.READ, 0)
                                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                                put(Telephony.Sms.THREAD_ID, threadId)
                            }
                            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo insertar en el proveedor de SMS del sistema: ${e.message}")
                        }
                    }

                    // Mostrar notificación si está activado
                    if (repository.isNotificationsEnabled()) {
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.showSmsNotification(
                            address = sender,
                            contactName = contactName,
                            messageBody = fullBody,
                            timestamp = timestamp,
                            threadId = threadId
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando SMS recibido: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
