package com.example.quickqr.util

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    
    private const val DATE_FORMAT = "dd MMM yyyy"
    private const val TIME_FORMAT = "HH:mm"
    private const val DATE_TIME_FORMAT = "dd MMM yyyy, HH:mm"
    private const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    
    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    private val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault())
    private val isoFormat = SimpleDateFormat(ISO_FORMAT, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    fun formatToISO(timestamp: Long): String {
        return isoFormat.format(Date(timestamp))
    }
    
    fun parseISO(isoString: String): Long {
        return isoFormat.parse(isoString)?.time ?: 0L
    }
    
    fun getRelativeTimeSpan(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
            diff < 604800000 -> "${diff / 86400000}d ago" // Less than 1 week
            else -> formatDate(timestamp) // More than 1 week
        }
    }
    
    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.apply { timeInMillis = System.currentTimeMillis() }
        val date = calendar.apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }
    
    fun isYesterday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val yesterday = calendar.apply { 
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val date = calendar.apply { timeInMillis = timestamp }
        
        return yesterday.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
               yesterday.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }
    
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    fun getStartOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getEndOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek + 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    fun getStartOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getEndOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
