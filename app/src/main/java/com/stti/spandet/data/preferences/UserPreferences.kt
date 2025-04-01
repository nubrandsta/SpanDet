package com.stti.spandet.data.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveLogin(username: String, token: String) {
        prefs.edit().apply {
            putString("username", username)
            putString("token", token)
            apply()
        }
    }

    fun saveUsername(username: String) {
        prefs.edit().apply {
            putString("username", username)
            apply()
        }
    }

    fun getUsername(): String? = prefs.getString("username", null)

    fun getToken(): String? = prefs.getString("token", null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
