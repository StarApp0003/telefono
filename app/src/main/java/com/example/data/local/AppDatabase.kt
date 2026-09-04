package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.MessageItem
import com.example.data.model.MessageStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MessageStatus = try {
        MessageStatus.valueOf(value)
    } catch (e: Exception) {
        MessageStatus.SENT
    }
}

@Database(entities = [MessageItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_connect_db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
