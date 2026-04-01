package com.example.naedafront.data.remote

import android.util.Log
import com.example.naedafront.data.remote.api.NaverGeocodingApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NaverMapApiConfig {

    private const val BASE_URL = "https://maps.apigw.ntruss.com/"


    private const val CLIENT_ID = "kzwtezgf2l"
    private const val CLIENT_SECRET = "ocyVTPHZdfzoIlgOJTXuxuwgvVe7gOKrHDJYZJVZ"

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("x-ncp-apigw-api-key-id", CLIENT_ID)
            .addHeader("x-ncp-apigw-api-key", CLIENT_SECRET)
            .addHeader("Accept", "application/json")
            .build()

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("NaverGeocodeApi", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val geocodingApi: NaverGeocodingApi by lazy {
        retrofit.create(NaverGeocodingApi::class.java)
    }
}