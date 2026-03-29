// File: app/src/main/java/com/example/naedafront/data/remote/api/CardApi.kt
package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.CardResponse
import com.example.naedafront.data.remote.response.CardTransactionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CardApi {

    @GET("/api/cards")
    suspend fun getCards(
        @Query("userNo") userNo: Long
    ): Response<List<CardResponse>>

    @GET("/api/cards/{cardId}/transactions")
    suspend fun getCardTransactions(
        @Path("cardId") cardId: Long,
        @Query("userNo") userNo: Long,
        @Query("cardType") cardType: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<CardTransactionResponse>>
}