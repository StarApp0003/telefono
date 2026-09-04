package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receptor de SMS_DELIVER específico para cuando la aplicación es designada
 * como la App de SMS Predeterminada del sistema Android.
 * Requiere el permiso del sistema android.permission.BROADCAST_SMS.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        SmsReceiver.processIncomingSms(context, intent, pendingResult)
    }
}
