package com.example.naedafront.data.remote

import android.content.Context
import com.example.naedafront.AuthPrefs
import com.example.naedafront.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    @Volatile
    private var appContext: Context? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = appContext?.let(AuthPrefs::getAccessToken)
            val request = buildRequestWithAuth(chain.request(), token)
            chain.proceed(request)
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
        if (accessToken.isNullOrBlank()) {
            return request
        }

        return request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }
}
