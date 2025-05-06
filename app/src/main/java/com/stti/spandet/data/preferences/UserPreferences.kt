package com.stti.spandet.data.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveLogin(username: String, token: String, fullname: String, group: String) {
        prefs.edit().apply {
            putString("username", username)
            putString("full_name", fullname)
            putString("group", group)
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

    fun getFullName(): String? = prefs.getString("full_name", null)

    fun getGroup(): String? = prefs.getString("group", null)

    fun getToken(): String? = prefs.getString("token", null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
