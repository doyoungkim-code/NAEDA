package com.example.naedafront.ui.screen.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.repository.PointRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class PointHistoryItemUi(
    val id: Long,
    val title: String,
    val dateText: String,
    val detailText: String,
    val pointText: String,
    val positive: Boolean,
    val iconText: String,
    val year: Int,
    val month: Int
)

data class PointHistoryUiState(
    val isLoading: Boolean = false,
    val pointBalance: Long = 0L,
    val totalEarned: Long = 0L,
    val totalUsed: Long = 0L,
    val selectedYear: Int = 2026,
    val selectedMonth: Int? = 3,
    val items: List<PointHistoryItemUi> = emptyList(),
    val errorMessage: String? = null
)

class PointHistoryViewModel : ViewModel() {

    private val pointRepository = PointRepository()

    private val _uiState = MutableStateFlow(PointHistoryUiState())
    val uiState: StateFlow<PointHistoryUiState> = _uiState.asStateFlow()

    fun load(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: 0L

        if (userNo <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "사용자 정보가 없습니다."
                )
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

            val walletResult = pointRepository.getPointWallet(userNo)
            val historyResult = pointRepository.getPointHistories(userNo, 100)

            var nextBalance = 0L
            var nextEarned = 0L
            var nextUsed = 0L
            var nextItems: List<PointHistoryItemUi> = emptyList()
            var nextError: String? = null

            walletResult
                .onSuccess { wallet ->
                    nextBalance = wallet.balance
                    nextEarned = wallet.totalEarned
                    nextUsed = wallet.totalUsed
                }
                .onFailure { throwable ->
                    nextError = throwable.message ?: "포인트 지갑을 불러오지 못했습니다."
                }

            historyResult
                .onSuccess { histories ->
                    nextItems = histories.map { history ->
                        val dateTime = runCatching {
                            LocalDateTime.parse(history.created)
                        }.getOrNull()

                        val year = dateTime?.year ?: 2026
                        val month = dateTime?.monthValue ?: 3

                        val formattedDate = if (dateTime != null) {
                            "${month}월 ${dateTime.dayOfMonth}일 " +
                                    "%02d:%02d".format(dateTime.hour, dateTime.minute)
                        } else {
                            history.created
                        }

                        PointHistoryItemUi(
                            id = history.historyId,
                            title = history.description.ifBlank { if (history.type == "EARN") "포인트 적립" else "포인트 사용" },
                            dateText = formattedDate,
                            detailText = "잔액 %,d P".format(history.balanceAfter),
                            pointText = if (history.type == "EARN") {
                                "+%,d P".format(history.amount)
                            } else {
                                "-%,d P".format(history.amount)
                            },
                            positive = history.type == "EARN",
                            iconText = history.description.take(1).ifBlank { if (history.type == "EARN") "적" else "사" },
                            year = year,
                            month = month
                        )
                    }
                }
                .onFailure { throwable ->
                    if (nextError == null) {
                        nextError = throwable.message ?: "포인트 이력을 불러오지 못했습니다."
                    }
                }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    pointBalance = nextBalance,
                    totalEarned = nextEarned,
                    totalUsed = nextUsed,
                    items = nextItems,
                    errorMessage = nextError
                )
            }
        }
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }

    fun setMonth(month: Int?) {
        _uiState.update { it.copy(selectedMonth = month) }
    }
}