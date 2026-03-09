package com.example.naedafront

import android.content.Context

object AuthPrefs {
    private const val PREFS_NAME = "naeda_auth"
    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_FACE_REGISTERED = "is_face_registered"

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, value)
            .apply()
    }

    fun isFaceRegistered(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FACE_REGISTERED, false)

    fun setFaceRegistered(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FACE_REGISTERED, value)
            .apply()
    }
}
