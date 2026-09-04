package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatMessageTime(timestamp: Long): String {
        val messageDate = Date(timestamp)
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = messageDate }

        return if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
        } else if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1
        ) {
            "Ayer " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
        } else if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(messageDate)
        } else {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate)
        }
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val messageDate = Date(timestamp)
        val nowCal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = messageDate }

        return when {
            diff < 60 * 1000 -> "Ahora"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min"
            nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
            }
            nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Ayer"
            nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("d MMM", Locale.getDefault()).format(messageDate)
            }
            else -> {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate)
            }
        }
    }

    fun formatHeaderDate(timestamp: Long): String {
        val messageDate = Date(timestamp)
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = messageDate }

        return if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            "Hoy"
        } else if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1
        ) {
            "Ayer"
        } else {
            SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(messageDate).replaceFirstChar { it.uppercase() }
        }
    }
}
