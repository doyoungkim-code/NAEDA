package com.example.naedafront.ui.screen.mypage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.AuthApi
import com.example.naedafront.data.remote.request.LogoutRequest
import com.example.naedafront.data.remote.api.UserApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageViewModel : ViewModel() {

    private val userApi: UserApi = ApiConfig.retrofit.create(UserApi::class.java)
    private val authApi: AuthApi = ApiConfig.retrofit.create(AuthApi::class.java)

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    fun loadInitialUserInfo(context: Context) {
        _uiState.update {
            it.copy(
                userNo = AuthPrefs.getUserNo(context),
                userName = AuthPrefs.getUsername(context).orEmpty(),
                userEmail = AuthPrefs.getUserId(context).orEmpty(),
                phone = AuthPrefs.getPhone(context).orEmpty(),
                faceRegistered = AuthPrefs.isFaceRegistered(context),
                secondaryAuthEnabled = AuthPrefs.isSecondaryAuthEnabled(context)
            )
        }
    }

    fun fetchMyInfo(context: Context) {
        val userNo = AuthPrefs.getUserNo(context)

        if (userNo == null) {
            _uiState.update {
                it.copy(errorMessage = "userNo가 없습니다.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                userApi.getMyInfo(userNo)
            }.onSuccess { response ->
                AuthPrefs.saveUserInfo(
                    context = context,
                    userNo = response.userNo,
                    userId = response.userId,
                    username = response.username,
                    phone = response.phone,
                    faceRegistered = response.faceRegistered,
                    secondaryAuthEnabled = response.secondaryAuthEnabled
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userNo = response.userNo,
                        userName = response.username,
                        userEmail = response.userId,
                        phone = response.phone,
                        faceRegistered = response.faceRegistered,
                        secondaryAuthEnabled = response.secondaryAuthEnabled,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "회원 정보 조회 실패"
                    )
                }
            }
        }
    }

    fun logout(
        context: Context,
        onLogoutSuccess: () -> Unit
    ) {
        val refreshToken = AuthPrefs.getRefreshToken(context)

        if (refreshToken.isNullOrBlank()) {
            AuthPrefs.clearSession(context)
            onLogoutSuccess()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLogoutLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                authApi.logout(
                    LogoutRequest(refreshToken = refreshToken)
                )
            }.onSuccess { response ->
                AuthPrefs.clearSession(context)

                _uiState.update {
                    it.copy(
                        isLogoutLoading = false
                    )
                }

                onLogoutSuccess()
            }.onFailure { throwable ->
                AuthPrefs.clearSession(context)

                _uiState.update {
                    it.copy(
                        isLogoutLoading = false,
                        errorMessage = throwable.message ?: "로그아웃 처리 중 오류가 발생했습니다."
                    )
                }

                onLogoutSuccess()
            }
        }
    }
}