package com.example.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Servicio requerido por Android para las aplicaciones de SMS predeterminadas,
 * para manejar respuestas rápidas sin interfaz ("RESPOND_VIA_MESSAGE").
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
