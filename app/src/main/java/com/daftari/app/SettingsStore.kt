package com.daftari.app

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("daftari_settings", Context.MODE_PRIVATE)
    var apiKey: String
        get() = prefs.getString("openai_key", "").orEmpty()
        set(value) = prefs.edit().putString("openai_key", value.trim()).apply()
    var model: String
        get() = prefs.getString("model", "gemini-3.6-flash").orEmpty()
        set(value) = prefs.edit().putString("model", value.trim()).apply()
}


