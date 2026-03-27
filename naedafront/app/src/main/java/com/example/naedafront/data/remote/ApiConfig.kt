package com.example.naedafront.data.remote

import android.content.Context
import com.example.naedafront.AuthPrefs
import com.example.naedafront.BuildConfig
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiConfig {
    private data class RefreshTokenRequestBody(
        val refreshToken: String
    )

    @Volatile
    private var appContext: Context? = null

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val refreshLock = Any()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun newBaseClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)

    private val refreshClient = newBaseClientBuilder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val client = newBaseClientBuilder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val token = appContext?.let(AuthPrefs::getAccessToken)
            val request = buildRequestWithAuth(originalRequest, token)
            val response = chain.proceed(request)
            retryWithRefreshedTokenIfNeeded(
                chain = chain,
                originalRequest = originalRequest,
                attemptedAccessToken = token,
                response = response
            )
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun buildRequestWithAuth(request: Request, accessToken: String?): Request {
        if (accessToken.isNullOrBlank() || !shouldAttachAccessToken(request)) {
            return request
        }

        return request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }

    private fun retryWithRefreshedTokenIfNeeded(
        chain: Interceptor.Chain,
        originalRequest: Request,
        attemptedAccessToken: String?,
        response: okhttp3.Response
    ): okhttp3.Response {
        if (!shouldAttemptTokenRefresh(originalRequest, response.code) || attemptedAccessToken.isNullOrBlank()) {
            return response
        }

        val context = appContext ?: return response
        val refreshToken = AuthPrefs.getRefreshToken(context)
        if (refreshToken.isNullOrBlank()) {
            return response
        }

        synchronized(refreshLock) {
            val latestAccessToken = AuthPrefs.getAccessToken(context)
            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != attemptedAccessToken) {
                response.close()
                return chain.proceed(buildRequestWithAuth(originalRequest, latestAccessToken))
            }

            val refreshedSession = refreshSession(refreshToken)
            if (refreshedSession == null) {
                AuthPrefs.clearSession(context)
                return response
            }

            response.close()
            return chain.proceed(buildRequestWithAuth(originalRequest, refreshedSession.accessToken))
        }
    }

    private fun refreshSession(refreshToken: String): LoginResponseDto? {
        val context = appContext ?: return null
        val requestBody = gson.toJson(
            RefreshTokenRequestBody(refreshToken = refreshToken)
        ).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_BASE_URL}api/auth/refresh")
            .post(requestBody)
            .build()

        return try {
            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }

                val rawBody = response.body?.string().orEmpty()
                if (rawBody.isBlank()) {
                    return null
                }

                val refreshed = gson.fromJson(rawBody, LoginResponseDto::class.java) ?: return null
                AuthPrefs.saveLoginSession(
                    context = context,
                    userNo = refreshed.userNo,
                    userId = refreshed.userId,
                    username = refreshed.username,
                    userKey = refreshed.userKey,
                    accessToken = refreshed.accessToken,
                    refreshToken = refreshed.refreshToken,
                    faceRegistered = refreshed.faceRegistered,
                    secondaryAuthEnabled = refreshed.secondaryAuthEnabled
                )
                refreshed
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun shouldAttachAccessToken(request: Request): Boolean {
        val path = request.url.encodedPath
        return path != "/api/auth/login" &&
            path != "/api/auth/signup" &&
            path != "/api/auth/refresh" &&
            path != "/api/auth/check-email" &&
            path != "/api/auth/check-phone"
    }

    private fun shouldAttemptTokenRefresh(request: Request, responseCode: Int): Boolean {
        val path = request.url.encodedPath
        val isAuthEndpoint = path == "/api/auth/login" ||
            path == "/api/auth/signup" ||
            path == "/api/auth/refresh" ||
            path == "/api/auth/check-email" ||
            path == "/api/auth/check-phone"
        return !isAuthEndpoint && (responseCode == 401 || responseCode == 403)
    }
}

