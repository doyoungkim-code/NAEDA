package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.NaverGeocodeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverGeocodingApi {

    @GET("map-geocode/v2/geocode")
    suspend fun searchAddress(
        @Query("query") query: String,
        @Query("coordinate") coordinate: String? = null,
        @Query("filter") filter: String? = null,
        @Query("language") language: String = "kor",
        @Query("page") page: Int = 1,
        @Query("count") count: Int = 20
    ): Response<NaverGeocodeResponse>
}