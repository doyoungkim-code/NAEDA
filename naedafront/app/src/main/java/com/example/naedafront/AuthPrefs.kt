package com.example.naedafront

import android.content.Context

object AuthPrefs {
    private const val PREFS_NAME = "naeda_auth"

    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_FACE_REGISTERED = "is_face_registered"
    private const val KEY_SECONDARY_AUTH_ENABLED = "is_secondary_auth_enabled"

    private const val KEY_USER_NO = "user_no"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_PHONE = "phone"
    private const val KEY_USER_KEY = "user_key"

    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)

    fun hasSession(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        return isLoggedIn && !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
    }

    fun setLoggedIn(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, value)
            .apply()
    }

    fun saveLoginSession(
        context: Context,
        userNo: Long,
        userId: String,
        username: String,
        userKey: String,
        accessToken: String,
        refreshToken: String,
        faceRegistered: Boolean,
        secondaryAuthEnabled: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putLong(KEY_USER_NO, userNo)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_KEY, userKey)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putBoolean(KEY_FACE_REGISTERED, faceRegistered)
            .putBoolean(KEY_SECONDARY_AUTH_ENABLED, secondaryAuthEnabled)
            .apply()
    }

    /**
     * /api/users/me 조회 결과로 회원 기본정보를 갱신할 때 사용
     * 토큰/로그인 상태/유저키는 건드리지 않음
     */
    fun saveUserInfo(
        context: Context,
        userNo: Long,
        userId: String,
        username: String,
        phone: String,
        faceRegistered: Boolean,
        secondaryAuthEnabled: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_NO, userNo)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PHONE, phone)
            .putBoolean(KEY_FACE_REGISTERED, faceRegistered)
            .putBoolean(KEY_SECONDARY_AUTH_ENABLED, secondaryAuthEnabled)
            .apply()
    }

    fun getUserNo(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_USER_NO)) prefs.getLong(KEY_USER_NO, -1L) else null
    }

    fun getUserId(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null)

    fun getUsername(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, null)

    fun getPhone(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE, null)

    fun getUserKey(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_KEY, null)

    fun getAccessToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH_TOKEN, null)

    fun clearSession(context: Context) {
        val isFaceRegistered = isFaceRegistered(context)
        val isSecondaryAuthEnabled = isSecondaryAuthEnabled(context)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean(KEY_FACE_REGISTERED, isFaceRegistered)
            .putBoolean(KEY_SECONDARY_AUTH_ENABLED, isSecondaryAuthEnabled)
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

    fun isSecondaryAuthEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SECONDARY_AUTH_ENABLED, false)

    fun saveFacePaySettings(
        context: Context,
        faceRegistered: Boolean,
        secondaryAuthEnabled: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FACE_REGISTERED, faceRegistered)
            .putBoolean(KEY_SECONDARY_AUTH_ENABLED, secondaryAuthEnabled)
            .apply()
    }

}