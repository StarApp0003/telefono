package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MessageItem
import com.example.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageItem>>

    @Query("SELECT * FROM messages WHERE address = :address OR (threadId = :threadId AND :threadId > 0) ORDER BY timestamp ASC")
    fun getMessagesForConversation(address: String, threadId: Long): Flow<List<MessageItem>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageItem?

    @Query("SELECT * FROM messages WHERE systemSmsId = :systemSmsId LIMIT 1")
    suspend fun getMessageBySystemId(systemSmsId: Long): MessageItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageItem>)

    @Update
    suspend fun updateMessage(message: MessageItem)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: MessageStatus)

    @Query("UPDATE messages SET isRead = 1 WHERE address = :address")
    suspend fun markConversationAsRead(address: String)

    @Query("DELETE FROM messages WHERE address = :address OR threadId = :threadId")
    suspend fun deleteConversation(address: String, threadId: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
}
