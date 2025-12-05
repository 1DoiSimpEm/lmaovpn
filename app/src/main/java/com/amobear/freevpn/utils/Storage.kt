package com.amobear.freevpn.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

object Storage {
    private var preferences: SharedPreferences? = null
    
    private val exclusionStrategy = object : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes): Boolean {
            return f.name == "connectIntent"
        }
        
        override fun shouldSkipClass(clazz: Class<*>): Boolean {
            return false
        }
    }
    
    private val gson: Gson = GsonBuilder()
        .enableComplexMapKeySerialization()
        .setExclusionStrategies(exclusionStrategy)
        .create()

    fun init(context: Context) {
        preferences = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
    }

    fun <T> save(data: T?, clazz: Class<T>) {
        val prefs = preferences ?: return
        if (data != null) {
            prefs.edit().putString(clazz.name, gson.toJson(data)).apply()
        } else {
            prefs.edit().remove(clazz.name).apply()
        }
    }

    fun <K, V : K> load(keyClass: Class<K>, objClass: Class<V>): V? {
        val prefs = preferences ?: return null
        val key = keyClass.name
        if (!prefs.contains(key)) {
            return null
        }

        return try {
            val json = prefs.getString(key, null)
            gson.fromJson(json, objClass)
        } catch (e: Exception) {
            android.util.Log.e("Storage", "Error loading ${keyClass.name}: ${e.message}")
            null
        }
    }

    fun <T> delete(clazz: Class<T>) {
        preferences?.edit()?.remove(clazz.name)?.apply()
    }
    
    fun <T> save(data: T?, key: String) {
        val prefs = preferences ?: return
        if (data != null) {
            prefs.edit().putString(key, gson.toJson(data)).apply()
        } else {
            prefs.edit().remove(key).apply()
        }
    }
    
    fun <T> load(clazz: Class<T>, key: String): T? {
        val prefs = preferences ?: return null
        if (!prefs.contains(key)) {
            return null
        }
        return try {
            val json = prefs.getString(key, null)
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            android.util.Log.e("Storage", "Error loading $key: ${e.message}")
            null
        }
    }
    
    fun delete(key: String) {
        preferences?.edit()?.remove(key)?.apply()
    }

    fun saveString(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    fun getString(key: String, defValue: String?): String? {
        return preferences?.getString(key, defValue)
    }
}

