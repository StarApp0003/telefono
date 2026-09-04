package com.example.data.repository

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.model.ContactItem
import com.example.data.model.ConversationItem
import com.example.data.model.MessageItem
import com.example.data.model.MessageStatus
import com.example.sms.SimInfo
import com.example.sms.SmsManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val messageDao = db.messageDao()
    private val smsManagerHelper = SmsManagerHelper(context)

    companion object {
        private const val TAG = "SmsRepository"
        private const val PREFS_NAME = "sms_connect_prefs"
        private const val KEY_PREFERRED_SUB_ID = "preferred_sub_id"
        private const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_CHAT_THEME_COLOR = "chat_theme_color"
        private const val KEY_IN_CHAT_SOUNDS_ENABLED = "in_chat_sounds_enabled"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Flujo de todas las conversaciones agrupadas por número/remitente.
     */
    fun getConversations(): Flow<List<ConversationItem>> {
        return messageDao.getAllMessages().map { messages ->
            // Agrupar por dirección normalizada
            val grouped = messages.groupBy { normalizePhoneNumber(it.address) }
            grouped.map { (normalizedAddr, msgList) ->
                val sorted = msgList.sortedByDescending { it.timestamp }
                val latest = sorted.first()
                val unreadCount = sorted.count { !it.isRead && !it.isSentByUser }
                val contactName = latest.contactName ?: resolveContactName(latest.address)
                val photoUri = resolveContactPhoto(latest.address)

                ConversationItem(
                    threadId = latest.threadId,
                    address = latest.address,
                    contactName = contactName,
                    lastMessage = latest.body,
                    timestamp = latest.timestamp,
                    unreadCount = unreadCount,
                    photoUri = photoUri,
                    simSlot = latest.simSlot,
                    lastMessageStatus = latest.status,
                    isLastMessageSentByUser = latest.isSentByUser
                )
            }.sortedByDescending { it.timestamp }
        }
    }

    /**
     * Flujo de mensajes de una conversación específica garantizando aislamiento estricto por número.
     */
    fun getMessagesForConversation(address: String, threadId: Long): Flow<List<MessageItem>> {
        val cleanAddr = normalizePhoneNumber(address)
        return messageDao.getAllMessages().map { allMessages ->
            allMessages.filter { msg ->
                val msgClean = normalizePhoneNumber(msg.address)
                val matchesNumber = if (cleanAddr.length >= 7 && msgClean.length >= 7) {
                    cleanAddr == msgClean ||
                    cleanAddr.endsWith(msgClean.takeLast(7)) ||
                    msgClean.endsWith(cleanAddr.takeLast(7))
                } else {
                    cleanAddr.isNotBlank() && cleanAddr == msgClean
                }
                matchesNumber || msg.address.equals(address, ignoreCase = true) || (threadId > 0 && msg.threadId == threadId)
            }.sortedBy { it.timestamp }
        }
    }

    /**
     * Obtiene las tarjetas SIM activas del dispositivo.
     */
    fun getAvailableSimCards(): List<SimInfo> {
        return smsManagerHelper.getActiveSimCards()
    }

    fun getPreferredSubscriptionId(): Int {
        return sharedPrefs.getInt(KEY_PREFERRED_SUB_ID, -1)
    }

    fun setPreferredSubscriptionId(subId: Int) {
        sharedPrefs.edit().putInt(KEY_PREFERRED_SUB_ID, subId).apply()
    }

    fun getThemeMode(): Int {
        return sharedPrefs.getInt(KEY_THEME_MODE, 0)
    }

    fun setThemeMode(mode: Int) {
        sharedPrefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getChatThemeColor(): Int {
        return sharedPrefs.getInt(KEY_CHAT_THEME_COLOR, 0)
    }

    fun setChatThemeColor(colorId: Int) {
        sharedPrefs.edit().putInt(KEY_CHAT_THEME_COLOR, colorId).apply()
    }

    fun isInChatSoundsEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_IN_CHAT_SOUNDS_ENABLED, true)
    }

    fun setInChatSoundsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_IN_CHAT_SOUNDS_ENABLED, enabled).apply()
    }

    /**
     * Sincroniza los mensajes SMS reales del ContentProvider del sistema Android hacia Room.
     */
    suspend fun syncDeviceSms() = withContext(Dispatchers.IO) {
        val hasReadSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReadSmsPermission) return@withContext

        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )

            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 500"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdCol = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeCol = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readCol = it.getColumnIndexOrThrow(Telephony.Sms.READ)

                val itemsToInsert = mutableListOf<MessageItem>()

                while (it.moveToNext()) {
                    val sysId = it.getLong(idCol)
                    val threadId = it.getLong(threadIdCol)
                    val address = it.getString(addressCol) ?: "Desconocido"
                    val body = it.getString(bodyCol) ?: ""
                    val date = it.getLong(dateCol)
                    val type = it.getInt(typeCol)
                    val isRead = it.getInt(readCol) == 1
                    val isSentByUser = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX

                    val contactName = resolveContactName(address)

                    val existing = messageDao.getMessageBySystemId(sysId)
                    if (existing == null) {
                        val duplicate = messageDao.findDuplicate(address, body, date)
                        if (duplicate != null) {
                            messageDao.updateMessage(duplicate.copy(systemSmsId = sysId))
                        } else {
                            itemsToInsert.add(
                                MessageItem(
                                    systemSmsId = sysId,
                                    threadId = threadId,
                                    address = address,
                                    contactName = contactName,
                                    body = body,
                                    timestamp = date,
                                    isSentByUser = isSentByUser,
                                    status = if (isSentByUser) MessageStatus.SENT else MessageStatus.DELIVERED,
                                    isRead = isRead
                                )
                            )
                        }
                    }
                }

                if (itemsToInsert.isNotEmpty()) {
                    messageDao.insertMessages(itemsToInsert)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando SMS del sistema: ${e.message}", e)
        }
    }

    /**
     * Envía un mensaje SMS real utilizando la infraestructura del teléfono.
     */
    suspend fun sendRealSms(
        destinationAddress: String,
        messageBody: String,
        subscriptionId: Int? = null,
        simSlot: Int = 0
    ): Result<MessageItem> = withContext(Dispatchers.IO) {
        val hasSendPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSendPermission) {
            return@withContext Result.failure(SecurityException("Permiso SEND_SMS no otorgado."))
        }

        if (destinationAddress.isBlank() || messageBody.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El destinatario o el mensaje no pueden estar vacíos."))
        }

        val contactName = resolveContactName(destinationAddress)
        val threadId = destinationAddress.hashCode().toLong()

        // 1. Guardar mensaje en Room con estado SENDING
        val message = MessageItem(
            threadId = threadId,
            address = destinationAddress,
            contactName = contactName,
            body = messageBody,
            timestamp = System.currentTimeMillis(),
            isSentByUser = true,
            status = MessageStatus.SENDING,
            simSlot = simSlot,
            subId = subscriptionId ?: -1,
            isRead = true
        )
        val generatedId = messageDao.insertMessage(message)
        val savedMessage = message.copy(id = generatedId)

        // 2. Si la app es la app de SMS predeterminada, escribir también en el Telephony Provider oficial
        writeSentSmsToSystemProvider(destinationAddress, messageBody, threadId)

        // 3. Ejecutar el envío de SMS a través del SmsManager
        val sentSuccess = smsManagerHelper.sendSms(
            destinationNumber = destinationAddress,
            messageText = messageBody,
            localMessageId = generatedId,
            subscriptionId = subscriptionId
        )

        if (sentSuccess) {
            Result.success(savedMessage)
        } else {
            messageDao.updateMessageStatus(generatedId, MessageStatus.FAILED)
            Result.failure(Exception("Fallo al despachar el SMS a la red móvil."))
        }
    }

    private fun writeSentSmsToSystemProvider(address: String, body: String, threadId: Long) {
        try {
            val isDefault = isDefaultSmsApp()
            if (isDefault) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                    put(Telephony.Sms.THREAD_ID, threadId)
                }
                context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo escribir en el proveedor de SMS del sistema: ${e.message}")
        }
    }

    suspend fun markConversationAsRead(address: String) = withContext(Dispatchers.IO) {
        messageDao.markConversationAsRead(address)
        try {
            if (isDefaultSmsApp()) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.READ, 1)
                }
                context.contentResolver.update(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    values,
                    "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.READ} = 0",
                    arrayOf(address)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error actualizando lectura en el proveedor de SMS: ${e.message}")
        }
    }

    suspend fun deleteConversation(address: String, threadId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteConversation(address, threadId)
        try {
            if (isDefaultSmsApp()) {
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.ADDRESS} = ? OR ${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(address, threadId.toString())
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error borrando del proveedor de SMS del sistema: ${e.message}")
        }
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(id)
    }

    /**
     * Carga y busca la lista de contactos del dispositivo.
     */
    suspend fun searchContacts(query: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission) return@withContext emptyList()

        val contactList = mutableListOf<ContactItem>()
        val seenNumbers = mutableSetOf<String>()

        try {
            val selection = if (query.isNotBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            } else null

            val selectionArgs = if (query.isNotBlank()) {
                arrayOf("%$query%", "%$query%")
            } else null

            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                ),
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)
                val nameCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = it.getString(idCol) ?: ""
                    val name = it.getString(nameCol) ?: "Sin Nombre"
                    val number = it.getString(numCol) ?: ""
                    val photoUri = it.getString(photoCol)

                    val cleanNumber = normalizePhoneNumber(number)
                    if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                        contactList.add(
                            ContactItem(
                                id = id,
                                name = name,
                                number = number,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando contactos: ${e.message}", e)
        }

        contactList
    }

    fun resolveContactName(address: String): String? {
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission || address.isBlank()) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun resolveContactPhoto(address: String): String? {
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission || address.isBlank()) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val photoIndex = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    if (photoIndex != -1) it.getString(photoIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isDefaultSmsApp(): Boolean {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        return defaultPackage == context.packageName
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace("[^0-9+]".toRegex(), "")
    }
}
