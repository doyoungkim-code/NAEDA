package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.CardResponse
import com.example.naedafront.data.remote.response.CardTransactionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface CardApi {

    @GET("/api/cards")
    suspend fun getCards(
        @Query("userNo") userNo: Long
    ): Response<List<CardResponse>>

    @GET("/api/cards/{cardId}/transactions")
    suspend fun getCardTransactions(
        @Path("cardId") cardId: Long,
        @Query("userNo") userNo: Long,
        @QueryMap(encoded = true) request: Map<String, String>
    ): Response<List<CardTransactionResponse>>
}