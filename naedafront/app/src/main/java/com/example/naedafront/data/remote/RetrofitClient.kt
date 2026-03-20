package com.example.naedafront.data.remote

import com.example.naedafront.BuildConfig
import com.example.naedafront.data.remote.api.NaverGeocodingApi
import com.example.naedafront.data.remote.api.UserApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BACKEND_BASE_URL = "http://10.0.2.2:8080/"
    private const val NAVER_GEOCODE_BASE_URL = "https://maps.apigw.ntruss.com/"

    private val backendClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
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
            .baseUrl(BACKEND_BASE_URL)
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

    val naverGeocodingApi: NaverGeocodingApi by lazy {
        naverRetrofit.create(NaverGeocodingApi::class.java)
    }
}