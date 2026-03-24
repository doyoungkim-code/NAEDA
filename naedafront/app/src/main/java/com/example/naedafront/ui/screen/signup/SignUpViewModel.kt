package com.example.naedafront.ui.screen.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.data.remote.request.SignUpRequest
import com.example.naedafront.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private fun logState(action: String) {
        val state = _uiState.value
        Log.d(
            TAG,
            "$action | userId=${state.userId}, username=${state.username}, residentNo=${state.residentNo}, phone=${state.phone}, institutionCode=${state.institutionCode}, passwordSet=${state.password.isNotBlank()}, pinSet=${state.pin.isNotBlank()}"
        )
    }

    fun updateUserId(userId: String) {
        _uiState.update { currentState ->
            currentState.copy(userId = userId)
        }
        logState("updateUserId")
    }

    fun updatePassword(password: String) {
        _uiState.update { currentState ->
            currentState.copy(password = password)
        }
        logState("updatePassword")
    }

    fun updateUsername(username: String) {
        _uiState.update { currentState ->
            currentState.copy(username = username)
        }
        logState("updateUsername")
    }

    fun updateResidentNo(residentNo: String) {
        _uiState.update { currentState ->
            currentState.copy(residentNo = residentNo)
        }
        logState("updateResidentNo")
    }

    fun updatePhone(phone: String) {
        _uiState.update { currentState ->
            currentState.copy(phone = phone)
        }
        logState("updatePhone")
    }

    fun updateInstitutionCode(institutionCode: String) {
        _uiState.update { currentState ->
            currentState.copy(institutionCode = institutionCode)
        }
        logState("updateInstitutionCode")
    }

    fun updatePin(pin: String) {
        _uiState.update { currentState ->
            currentState.copy(pin = pin)
        }
        logState("updatePin")
    }

    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun resetSignUpSuccess() {
        _uiState.update { currentState ->
            currentState.copy(isSignUpSuccess = false)
        }
    }

    fun submitSignUp() {
        val state = _uiState.value

        if (state.isLoading) {
            Log.d(TAG, "submitSignUp ignored | already loading")
            return
        }

        val request = SignUpRequest(
            userId = state.userId.trim(),
            password = state.password,
            username = state.username.trim(),
            residentNo = state.residentNo.replace("-", "").trim(),
            phone = state.phone.replace("-", "").trim(),
            institutionCode = state.institutionCode.trim(),
            pin = state.pin
        )

        Log.d(TAG, "submitSignUp start | request=$request")

        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                    isSignUpSuccess = false
                )
            }

            try {
                val response = authRepository.signUp(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "submitSignUp success | code=${response.code()}")

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = null,
                            isSignUpSuccess = true
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        TAG,
                        "submitSignUp failed | code=${response.code()}, body=$errorBody"
                    )

                    val message = when (response.code()) {
                        409 -> {
                            when {
                                errorBody?.contains("userId", ignoreCase = true) == true ||
                                errorBody?.contains("이메일", ignoreCase = true) == true ||
                                errorBody?.contains("아이디", ignoreCase = true) == true ->
                                    "이미 사용 중인 이메일입니다."
                                errorBody?.contains("phone", ignoreCase = true) == true ||
                                errorBody?.contains("전화", ignoreCase = true) == true ||
                                errorBody?.contains("휴대폰", ignoreCase = true) == true ->
                                    "이미 등록된 전화번호입니다."
                                else -> "이미 가입된 정보입니다."
                            }
                        }
                        400 -> "입력 정보를 확인해주세요."
                        500, 502 -> "이미 등록된 전화번호이거나 서버에 문제가 발생했습니다.\n정보를 확인 후 다시 시도해주세요."
                        else -> "회원가입에 실패했습니다. (${response.code()})"
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isSignUpSuccess = false,
                            errorMessage = message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "submitSignUp exception", e)

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isSignUpSuccess = false,
                        errorMessage = "네트워크 오류가 발생했습니다."
                    )
                }
            }
        }
    }
}