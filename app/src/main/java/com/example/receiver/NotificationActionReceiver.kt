package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.data.repository.SmsRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receptor para las acciones directas de la notificación (Responder directamente y Marcar como leído).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DIRECT_REPLY = "com.example.smsconnect.ACTION_DIRECT_REPLY"
        const val ACTION_MARK_AS_READ = "com.example.smsconnect.ACTION_MARK_AS_READ"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
        val repository = SmsRepository(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DIRECT_REPLY -> {
                        val remoteInput = RemoteInput.getResultsFromIntent(intent)
                        val replyText = remoteInput?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()
                        if (!replyText.isNullOrBlank()) {
                            val subId = repository.getPreferredSubscriptionId()
                            repository.sendRealSms(
                                destinationAddress = address,
                                messageBody = replyText,
                                subscriptionId = if (subId != -1) subId else null
                            )
                            // Actualizar la notificación informando que el mensaje fue enviado
                            val notificationHelper = NotificationHelper(context)
                            val contactName = repository.resolveContactName(address)
                            val threadId = intent.getLongExtra(EXTRA_THREAD_ID, 0L)
                            notificationHelper.showSmsNotification(
                                address = address,
                                contactName = contactName,
                                messageBody = "Tú: $replyText",
                                timestamp = System.currentTimeMillis(),
                                threadId = threadId
                            )
                        }
                    }
                    ACTION_MARK_AS_READ -> {
                        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        repository.markConversationAsRead(address)
                        if (notificationId != -1) {
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.cancelNotification(notificationId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Log and swallow
            } finally {
                pendingResult.finish()
            }
        }
    }
}
