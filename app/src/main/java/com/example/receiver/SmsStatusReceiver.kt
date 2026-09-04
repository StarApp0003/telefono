package com.example.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receptor de difusiones para confirmar el estado de envío (SENT) y entrega (DELIVERED) de los SMS.
 */
class SmsStatusReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SMS_SENT = "com.example.smsconnect.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.smsconnect.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_ADDRESS = "extra_address"
        private const val TAG = "SmsStatusReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return

        val action = intent.action
        val resultCode = resultCode

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                when (action) {
                    ACTION_SMS_SENT -> {
                        val status = when (resultCode) {
                            Activity.RESULT_OK -> MessageStatus.SENT
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE,
                            SmsManager.RESULT_ERROR_NO_SERVICE,
                            SmsManager.RESULT_ERROR_NULL_PDU,
                            SmsManager.RESULT_ERROR_RADIO_OFF -> MessageStatus.FAILED
                            else -> MessageStatus.FAILED
                        }
                        Log.d(TAG, "Estado de envío para mensaje $messageId: $status (code $resultCode)")
                        db.messageDao().updateMessageStatus(messageId, status)
                    }
                    ACTION_SMS_DELIVERED -> {
                        val status = if (resultCode == Activity.RESULT_OK) {
                            MessageStatus.DELIVERED
                        } else {
                            MessageStatus.FAILED
                        }
                        Log.d(TAG, "Estado de entrega para mensaje $messageId: $status (code $resultCode)")
                        db.messageDao().updateMessageStatus(messageId, status)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando estado de SMS: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
