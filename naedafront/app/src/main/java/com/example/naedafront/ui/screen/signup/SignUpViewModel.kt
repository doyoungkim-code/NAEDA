package com.example.naedafront.ui.screen.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignUpViewModel : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private fun logState(action: String) {
        val state = _uiState.value
        Log.d(
            TAG,
            "$action | userId=${state.userId}, password=${state.password}, username=${state.username}, residentNo=${state.residentNo}, phone=${state.phone}, institutionCode=${state.institutionCode}, pin=${state.pin}"
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
}