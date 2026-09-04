package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notifications.NotificationHelper

/**
 * Receptor de inicio del dispositivo para garantizar que los canales de notificación
 * y las configuraciones de segundo plano estén listos de inmediato al reiniciar el teléfono.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Evento de inicio recibido: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            try {
                // Inicializar canales de notificación de alta prioridad
                NotificationHelper(context)
            } catch (e: Exception) {
                Log.w("BootReceiver", "Error al inicializar notificaciones en arranque: ${e.message}")
            }
        }
    }
}
