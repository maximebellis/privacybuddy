package be.kuleuven.privacybuddy.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object DateTimeUtils {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun formatDateLabel(date: Date): String {
        val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        return when {
            localDate.isEqual(LocalDate.now()) -> "Today"
            localDate.isEqual(LocalDate.now().minusDays(1)) -> "Yesterday"
            else -> localDate.format(dateFormatter)
        }
    }

    fun formatTimestamp(timestamp: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val date = inputFormat.parse(timestamp) ?: return "Invalid Time"
            outputFormat.format(date)
        } catch (e: Exception) {
            "Invalid Time"
        }
    }
}