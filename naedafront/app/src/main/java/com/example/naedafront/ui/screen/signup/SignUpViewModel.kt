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
                    errorMessage = null
                )
            }

            try {
                val response = authRepository.signUp(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "submitSignUp success | code=${response.code()}")

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isSignUpSuccess = true
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        TAG,
                        "submitSignUp failed | code=${response.code()}, body=$errorBody"
                    )

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = "회원가입에 실패했습니다. (${response.code()})"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "submitSignUp exception", e)

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = "네트워크 오류가 발생했습니다."
                    )
                }
            }
        }
    }
}