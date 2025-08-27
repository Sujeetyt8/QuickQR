package com.example.quickqr.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferenceManager(context: Context, private val name: String) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "${context.packageName}_prefs",
        Context.MODE_PRIVATE
    )
    
    // String preference
    fun string(
        key: String,
        defaultValue: String = ""
    ): ReadWriteProperty<Any, String> = StringPreference(key, defaultValue)
    
    // Int preference
    fun int(
        key: String,
        defaultValue: Int = 0
    ): ReadWriteProperty<Any, Int> = IntPreference(key, defaultValue)
    
    // Long preference
    fun long(
        key: String,
        defaultValue: Long = 0L
    ): ReadWriteProperty<Any, Long> = LongPreference(key, defaultValue)
    
    // Float preference
    fun float(
        key: String,
        defaultValue: Float = 0f
    ): ReadWriteProperty<Any, Float> = FloatPreference(key, defaultValue)
    
    // Boolean preference
    fun boolean(
        key: String,
        defaultValue: Boolean = false
    ): ReadWriteProperty<Any, Boolean> = BooleanPreference(key, defaultValue)
    
    // String set preference
    fun stringSet(
        key: String,
        defaultValue: Set<String> = emptySet()
    ): ReadWriteProperty<Any, Set<String>> = StringSetPreference(key, defaultValue)
    
    // Clear all preferences
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    // Remove a specific preference
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
    
    // Check if a key exists
    fun contains(key: String): Boolean = prefs.contains(key)
    
    // Get all keys
    val all: Map<String, *>
        get() = prefs.all
    
    // Base preference class
    private abstract class BasePreference<T>(
        private val key: String,
        private val defaultValue: T
    ) : ReadWriteProperty<Any, T> {
        
        protected abstract fun SharedPreferences.getPreference(key: String, defaultValue: T): T
        protected abstract fun SharedPreferences.Editor.putPreference(key: String, value: T)
        
        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return prefs.getPreference(key, defaultValue)
        }
        
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            prefs.edit { putPreference(key, value) }
        }
        
        companion object {
            lateinit var prefs: SharedPreferences
        }
    }
    
    // String preference implementation
    private inner class StringPreference(
        key: String,
        defaultValue: String
    ) : BasePreference<String>(key, defaultValue) {
        override fun SharedPreferences.getPreference(key: String, defaultValue: String): String =
            getString(key, defaultValue) ?: defaultValue
            
        override fun SharedPreferences.Editor.putPreference(key: String, value: String) {
            putString(key, value)
        }
    }
    
    // Int preference implementation
    private inner class IntPreference(
        key: String,
        defaultValue: Int
    ) : BasePreference<Int>(key, defaultValue) {
        override fun SharedPreferences.getPreference(key: String, defaultValue: Int): Int =
            getInt(key, defaultValue)
            
        override fun SharedPreferences.Editor.putPreference(key: String, value: Int) {
            putInt(key, value)
        }
    }
    
    // Long preference implementation
    private inner class LongPreference(
        key: String,
        defaultValue: Long
    ) : BasePreference<Long>(key, defaultValue) {
        override fun SharedPreferences.getPreference(key: String, defaultValue: Long): Long =
            getLong(key, defaultValue)
            
        override fun SharedPreferences.Editor.putPreference(key: String, value: Long) {
            putLong(key, value)
        }
    }
    
    // Float preference implementation
    private inner class FloatPreference(
        key: String,
        defaultValue: Float
    ) : BasePreference<Float>(key, defaultValue) {
        override fun SharedPreferences.getPreference(key: String, defaultValue: Float): Float =
            getFloat(key, defaultValue)
            
        override fun SharedPreferences.Editor.putPreference(key: String, value: Float) {
            putFloat(key, value)
        }
    }
    
    // Boolean preference implementation
    private inner class BooleanPreference(
        key: String,
        defaultValue: Boolean
    ) : BasePreference<Boolean>(key, defaultValue) {
        override fun SharedPreferences.getPreference(key: String, defaultValue: Boolean): Boolean =
            getBoolean(key, defaultValue)
            
        override fun SharedPreferences.Editor.putPreference(key: String, value: Boolean) {
            putBoolean(key, value)
        }
    }
    
    // String set preference implementation
    private inner class StringSetPreference(
        key: String,
        defaultValue: Set<String>
    ) : BasePreference<Set<String>>(key, defaultValue) {
        override fun SharedPreferences.getPreference(key: String, defaultValue: Set<String>): Set<String> =
            getStringSet(key, defaultValue) ?: defaultValue
            
        override fun SharedPreferences.Editor.putPreference(key: String, value: Set<String>) {
            putStringSet(key, value)
        }
    }
    
    init {
        // Set the prefs instance for all preference delegates
        BasePreference.prefs = prefs
    }
    
    companion object {
        // Singleton instance
        @Volatile private var instance: PreferenceManager? = null
        
        fun getInstance(context: Context, name: String = "default"): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context, name).also { instance = it }
            }
        }
    }
}
