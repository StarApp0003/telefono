package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.receiver.NotificationActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_SMS_ID = "sms_messages_channel"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val GROUP_KEY_SMS = "com.example.smsconnect.SMS_GROUP"
        const val SUMMARY_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_sms)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_SMS_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSmsNotification(
        address: String,
        contactName: String?,
        messageBody: String,
        timestamp: Long,
        threadId: Long
    ) {
        val title = contactName?.takeIf { it.isNotBlank() } ?: address
        val notificationId = address.hashCode()

        // Intent para abrir el chat al pulsar la notificación
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ADDRESS", address)
            putExtra("EXTRA_THREAD_ID", threadId)
            data = Uri.parse("sms_connect://conversation/$address")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción de respuesta directa (Direct Reply)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(context.getString(R.string.reply))
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DIRECT_REPLY
            putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
            putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            context.getString(R.string.reply),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Acción 'Marcar como leído'
        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_READ
            putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.checkbox_on_background,
            context.getString(R.string.mark_as_read),
            markReadPendingIntent
        ).build()

        val senderPerson = Person.Builder()
            .setName(title)
            .setKey(address)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Yo").build()
        ).addMessage(
            messageBody,
            timestamp,
            senderPerson
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SMS_ID)
            .setSmallIcon(R.drawable.ic_sms_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setGroup(GROUP_KEY_SMS)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())

            // Crear notificación sumaria para agrupar múltiples mensajes
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_SMS_ID)
                .setSmallIcon(R.drawable.ic_sms_notification)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText("Nuevos mensajes SMS"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY_SMS)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        } catch (e: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido en Android 13+
        }
    }

    fun cancelNotification(notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}
