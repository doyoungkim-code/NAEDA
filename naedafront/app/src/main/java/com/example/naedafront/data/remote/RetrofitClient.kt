package com.example.naedafront.data.remote

import android.util.Log
import com.example.naedafront.BuildConfig
import com.example.naedafront.data.remote.api.CardApi
import com.example.naedafront.data.remote.api.NaverGeocodingApi
import com.example.naedafront.data.remote.api.OrderApi
import com.example.naedafront.data.remote.api.UserApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val NAVER_GEOCODE_BASE_URL = "https://maps.apigw.ntruss.com/"
    private const val TAG = "RetrofitClient"

    @Volatile
    private var accessToken: String? = null

    fun setAccessToken(token: String?) {
        accessToken = token
        Log.d(TAG, "setAccessToken called | token=${if (token.isNullOrBlank()) "EMPTY" else "SET"}")
    }

    private val backendClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()

                Log.d(
                    TAG,
                    "backend request | url=${originalRequest.url} | token=${if (accessToken.isNullOrBlank()) "EMPTY" else "SET"}"
                )

                accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                    requestBuilder.header("accessToken", token)
                }

                val request = requestBuilder.build()

                Log.d(
                    TAG,
                    "Authorization header = ${request.header("Authorization") ?: "NONE"}"
                )

                chain.proceed(request)
            }
            .build()
    }

    private val naverClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-ncp-apigw-api-key-id", BuildConfig.NAVER_MAPS_KEY_ID)
                    .header("x-ncp-apigw-api-key", BuildConfig.NAVER_MAPS_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val backendRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(backendClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val naverRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NAVER_GEOCODE_BASE_URL)
            .client(naverClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val userApi: UserApi by lazy {
        backendRetrofit.create(UserApi::class.java)
    }

    val orderApi: OrderApi by lazy {
        backendRetrofit.create(OrderApi::class.java)
    }

    val cardApi: CardApi by lazy {
        backendRetrofit.create(CardApi::class.java)
    }

    val naverGeocodingApi: NaverGeocodingApi by lazy {
        naverRetrofit.create(NaverGeocodingApi::class.java)
    }
}