package com.example.naedafront.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatRequestDto(
    val user_no: Long,
    val message: String,
    val history: List<ChatMessageDto>
)

data class ChatResponseDto(
    val reply: String,
    val referenced_stores: List<Int>,
    val referenced_festivals: List<Int>
)

interface ChatApi {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequestDto): ChatResponseDto
}
