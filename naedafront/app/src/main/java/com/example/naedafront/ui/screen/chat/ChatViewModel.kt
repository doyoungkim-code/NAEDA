package com.example.naedafront.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.data.remote.AiApiConfig
import com.example.naedafront.data.remote.api.ChatMessageDto
import com.example.naedafront.data.remote.api.ChatRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String,
    val content: String,
    val isLoading: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(userNo: Long, message: String) {
        if (message.isBlank()) return

        val userMessage = ChatMessage(role = "user", content = message)
        val loadingMessage = ChatMessage(role = "assistant", content = "", isLoading = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + loadingMessage,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val history = _uiState.value.messages
                    .filter { !it.isLoading }
                    .dropLast(1) // 방금 추가한 user 메시지 제외 (history용)
                    .takeLast(20)
                    .map { ChatMessageDto(role = it.role, content = it.content) }

                val response = AiApiConfig.chatApi.chat(
                    ChatRequestDto(
                        user_no = userNo,
                        message = message,
                        history = history
                    )
                )

                _uiState.update { state ->
                    val updatedMessages = state.messages.dropLast(1) +
                        ChatMessage(role = "assistant", content = response.reply)
                    state.copy(messages = updatedMessages, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    val updatedMessages = state.messages.dropLast(1) +
                        ChatMessage(role = "assistant", content = "죄송합니다, 잠시 후 다시 시도해주세요.")
                    state.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}
