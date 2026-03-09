package com.example.naedafront

import android.content.Context

object AuthPrefs {
    private const val PREFS_NAME = "naeda_auth"
    private const val KEY_LOGGED_IN = "is_logged_in"

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, value)
            .apply()
    }
}
