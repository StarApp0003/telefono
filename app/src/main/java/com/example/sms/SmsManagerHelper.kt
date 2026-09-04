package com.example.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.receiver.SmsStatusReceiver

class SmsManagerHelper(private val context: Context) {

    companion object {
        private const val TAG = "SmsManagerHelper"
    }

    /**
     * Obtiene la lista de tarjetas SIM activas en el dispositivo.
     */
    fun getActiveSimCards(): List<SimInfo> {
        val simList = mutableListOf<SimInfo>()
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null && hasPhoneStatePermission) {
                val activeList: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
                if (!activeList.isNullOrEmpty()) {
                    for (info in activeList) {
                        val displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}"
                        val carrierName = info.carrierName?.toString() ?: ""
                        val number = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                subscriptionManager.getPhoneNumber(info.subscriptionId)
                            } else {
                                @Suppress("DEPRECATION")
                                info.number ?: ""
                            }
                        } catch (e: Exception) {
                            ""
                        }

                        val isDefault = if (Build.VERSION.SDK_INT >= 24) {
                            SubscriptionManager.getDefaultSmsSubscriptionId() == info.subscriptionId
                        } else {
                            false
                        }

                        simList.add(
                            SimInfo(
                                subscriptionId = info.subscriptionId,
                                slotIndex = info.simSlotIndex,
                                displayName = displayName,
                                carrierName = carrierName,
                                number = number,
                                isDefault = isDefault
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso no otorgado para leer SIMs: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error detectando SIMs: ${e.message}")
        }

        // Si no se detectaron SIMs o no hay permiso, devolvemos una entrada genérica por defecto
        if (simList.isEmpty()) {
            simList.add(
                SimInfo(
                    subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId(),
                    slotIndex = 0,
                    displayName = "SIM Principal",
                    carrierName = "Operador móvil",
                    isDefault = true
                )
            )
        }

        return simList
    }

    /**
     * Envía un mensaje SMS real al destinatario usando la SIM especificada por subscriptionId.
     */
    fun sendSms(
        destinationNumber: String,
        messageText: String,
        localMessageId: Long,
        subscriptionId: Int? = null
    ): Boolean {
        return try {
            val smsManager: SmsManager = if (subscriptionId != null && subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val baseManager = context.getSystemService(SmsManager::class.java)
                    baseManager.createForSubscriptionId(subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            // Crear Intent y PendingIntent para seguimiento de estado 'Enviado' (SENT)
            val sentIntent = Intent(context, SmsStatusReceiver::class.java).apply {
                action = SmsStatusReceiver.ACTION_SMS_SENT
                data = Uri.parse("sms_connect://sent/$localMessageId")
                putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, localMessageId)
                putExtra(SmsStatusReceiver.EXTRA_ADDRESS, destinationNumber)
            }
            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                localMessageId.toInt(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Crear Intent y PendingIntent para seguimiento de estado 'Entregado' (DELIVERED)
            val deliveredIntent = Intent(context, SmsStatusReceiver::class.java).apply {
                action = SmsStatusReceiver.ACTION_SMS_DELIVERED
                data = Uri.parse("sms_connect://delivered/$localMessageId")
                putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, localMessageId)
                putExtra(SmsStatusReceiver.EXTRA_ADDRESS, destinationNumber)
            }
            val deliveredPendingIntent = PendingIntent.getBroadcast(
                context,
                (localMessageId + 100000).toInt(),
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Dividir el mensaje si excede el tamaño máximo de 160 caracteres (Multipart)
            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                val deliveredIntents = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    sentIntents.add(sentPendingIntent)
                    deliveredIntents.add(deliveredPendingIntent)
                }
                smsManager.sendMultipartTextMessage(
                    destinationNumber,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                )
            } else {
                smsManager.sendTextMessage(
                    destinationNumber,
                    null,
                    messageText,
                    sentPendingIntent,
                    deliveredPendingIntent
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS a $destinationNumber: ${e.message}", e)
            false
        }
    }
}
