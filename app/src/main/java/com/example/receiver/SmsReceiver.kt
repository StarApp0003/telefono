package com.example.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.MessageItem
import com.example.data.model.MessageStatus
import com.example.data.repository.SmsRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        processIncomingSms(context, intent, pendingResult)
    }

    companion object {
        private const val TAG = "SmsReceiver"

        fun processIncomingSms(context: Context, intent: Intent, pendingResult: PendingResult?) {
            val action = intent.action
            if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
                action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
            ) {
                pendingResult?.finish()
                return
            }

            val appContext = context.applicationContext ?: context
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SMSConnect:SmsWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }

            try {
                wakeLock?.acquire(15_000L) // Mantener CPU despierta durante el procesamiento
            } catch (e: Throwable) {
                Log.w(TAG, "No se pudo adquirir wakeLock: ${e.message}")
            }

            try {
                // Ejecución sincrónica en hilo IO durante onReceive():
                // Esto garantiza que el sistema operativo mantenga el proceso en estado activo/prioritario
                // hasta que el SMS se guarde en Room y se dispare la notificación visual.
                runBlocking(Dispatchers.IO) {
                    val messages = extractSmsMessages(intent)
                    if (messages.isEmpty()) {
                        Log.w(TAG, "No se encontraron mensajes SMS en el Intent recibido")
                        return@runBlocking
                    }

                    val sender = messages[0].originatingAddress
                        ?: messages[0].displayOriginatingAddress
                        ?: "Desconocido"

                    val timestamp = if (messages[0].timestampMillis > 0) {
                        messages[0].timestampMillis
                    } else {
                        System.currentTimeMillis()
                    }

                    // Extraer información de ranura SIM y suscripción
                    val subId = when {
                        intent.hasExtra("subscription") -> intent.getIntExtra("subscription", -1)
                        intent.hasExtra("android.telephony.extra.SUBSCRIPTION_INDEX") ->
                            intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
                        else -> -1
                    }
                    val slot = when {
                        intent.hasExtra("slot") -> intent.getIntExtra("slot", 0)
                        intent.hasExtra("simSlot") -> intent.getIntExtra("simSlot", 0)
                        else -> 0
                    }

                    // Concatenar todas las partes del mensaje (SMS multipart/largos)
                    val bodyBuilder = StringBuilder()
                    for (sms in messages) {
                        bodyBuilder.append(sms.displayMessageBody ?: sms.messageBody ?: "")
                    }
                    val fullBody = bodyBuilder.toString()

                    val repository = SmsRepository(appContext)
                    val db = AppDatabase.getInstance(appContext)

                    // Comprobar si ya existe para evitar duplicación
                    val duplicate = db.messageDao().findDuplicate(sender, fullBody, timestamp)
                    if (duplicate != null) {
                        Log.d(TAG, "Mensaje SMS duplicado omitido")
                        return@runBlocking
                    }

                    val contactName = repository.resolveContactName(sender)
                    val threadId = sender.hashCode().toLong()

                    var sysId: Long = -1L

                    // Si somos la app predeterminada o la acción es SMS_DELIVER, guardar en el proveedor del sistema
                    if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || repository.isDefaultSmsApp()) {
                        try {
                            val realSysThreadId = try {
                                Telephony.Threads.getOrCreateThreadId(appContext, sender)
                            } catch (e: Throwable) {
                                threadId
                            }

                            val values = ContentValues().apply {
                                put(Telephony.Sms.ADDRESS, sender)
                                put(Telephony.Sms.BODY, fullBody)
                                put(Telephony.Sms.DATE, timestamp)
                                put(Telephony.Sms.READ, 0)
                                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                                put(Telephony.Sms.THREAD_ID, realSysThreadId)
                                if (subId != -1) {
                                    put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                                }
                            }
                            val insertedUri = appContext.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                            sysId = insertedUri?.lastPathSegment?.toLongOrNull() ?: -1L
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error guardando en el proveedor SMS del sistema: ${e.message}")
                        }
                    }

                    // Guardar en la base de datos local Room
                    val messageItem = MessageItem(
                        systemSmsId = sysId,
                        threadId = threadId,
                        address = sender,
                        contactName = contactName,
                        body = fullBody,
                        timestamp = timestamp,
                        isSentByUser = false,
                        status = MessageStatus.DELIVERED,
                        simSlot = slot,
                        subId = subId,
                        isRead = false
                    )
                    db.messageDao().insertMessage(messageItem)
                    Log.d(TAG, "SMS de $sender guardado con éxito en segundo plano")

                    // Emitir notificación flotante con sonido y vibración
                    if (repository.isNotificationsEnabled()) {
                        try {
                            val notificationHelper = NotificationHelper(appContext)
                            notificationHelper.showSmsNotification(
                                address = sender,
                                contactName = contactName,
                                messageBody = fullBody,
                                timestamp = timestamp,
                                threadId = threadId
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error emitiendo notificación de SMS: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error crítico procesando SMS entrante: ${e.message}", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Throwable) {
                    // Ignorar
                }
                try {
                    pendingResult?.finish()
                } catch (e: Throwable) {
                    // Ignorar
                }
            }
        }

        /**
         * Extrae los mensajes SMS del Intent de forma ultra-confiable,
         * soportando la API nativa de Telephony y fallback manual de PDUs crudos.
         */
        private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
            val list = mutableListOf<SmsMessage>()

            // 1. Intentar método oficial de Telephony
            try {
                val fromTelephony = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (!fromTelephony.isNullOrEmpty()) {
                    return fromTelephony.toList()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "getMessagesFromIntent falló: ${e.message}")
            }

            // 2. Extracción directa desde el Bundle de PDUs (crucial para cold-start o ROMs personalizadas)
            val bundle = intent.extras ?: return emptyList()
            val pdusObj = bundle.get("pdus") ?: return emptyList()
            val format = bundle.getString("format")

            val pdus: Array<*> = when (pdusObj) {
                is Array<*> -> pdusObj
                is Collection<*> -> pdusObj.toTypedArray()
                else -> return emptyList()
            }

            for (pdu in pdus) {
                val pduBytes = pdu as? ByteArray ?: continue
                try {
                    val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !format.isNullOrBlank()) {
                        SmsMessage.createFromPdu(pduBytes, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pduBytes)
                    }
                    if (sms != null) {
                        list.add(sms)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error decodificando PDU SMS: ${e.message}")
                }
            }

            return list
        }
    }
}
