package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.request.CreateAddressRequest
import com.example.naedafront.data.remote.response.AddressResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AddressApi {

    @GET("/api/addresses/{userNo}")
    suspend fun getAddresses(
        @Path("userNo") userNo: Long
    ): Response<List<AddressResponse>>

    @GET("/api/addresses/{userNo}/{addressId}")
    suspend fun getAddressDetail(
        @Path("userNo") userNo: Long,
        @Path("addressId") addressId: Long
    ): Response<AddressResponse>

    @DELETE("/api/addresses/{userNo}/{addressId}")
    suspend fun deleteAddress(
        @Path("userNo") userNo: Long,
        @Path("addressId") addressId: Long
    ): Response<Unit>

    @POST("/api/addresses/{userNo}")
    suspend fun createAddress(
        @Path("userNo") userNo: Long,
        @Body request: CreateAddressRequest
    ): Response<AddressResponse>

    @PATCH("/api/addresses/{userNo}/{addressId}/default")
    suspend fun setDefaultAddress(
        @Path("userNo") userNo: Long,
        @Path("addressId") addressId: Long
    ): Response<AddressResponse>
}