package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * Receptor requerido por Android para apps de SMS predeterminadas para manejar mensajes WAP Push / MMS.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            Log.d("MmsReceiver", "WAP PUSH recibido en SMS Connect")
        }
    }
}
