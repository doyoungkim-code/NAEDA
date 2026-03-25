// File: app/src/main/java/com/example/naedafront/data/repository/CardRepository.kt
package com.example.naedafront.data.repository

import android.util.Log
import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.CardApi
import com.example.naedafront.data.remote.response.CardResponse
import com.example.naedafront.data.remote.response.CardTransactionResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CardTransactionItemData(
    val transactionId: String,
    val merchantName: String,
    val category: String,
    val amount: Long,
    val isCanceled: Boolean,
    val transactedAt: String
)

data class CardTransactionQuery(
    val startDate: String,
    val endDate: String
)

object CardRepository {

    private const val TAG = "CardRepository"

    private val api by lazy {
        ApiConfig.retrofit.create(CardApi::class.java)
    }

    suspend fun getCards(userNo: Long): Result<List<CardResponse>> {
        return runCatching {
            Log.d(TAG, "getCards start | userNo=$userNo")

            val response = api.getCards(userNo)
            val errorBody = response.errorBody()?.string()

            Log.d(
                TAG,
                "getCards response | code=${response.code()} | body=${response.body()} | error=$errorBody"
            )

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "카드 목록 조회 실패(code=${response.code()}) ${errorBody ?: ""}".trim()
                )
            }

            response.body().orEmpty()
        }
    }

    suspend fun getCardTransactions(
        userNo: Long,
        cardId: Long,
        period: String
    ): Result<List<CardTransactionItemData>> {
        return runCatching {
            val query = buildTransactionQuery(period)

            Log.d(
                TAG,
                "getCardTransactions start | userNo=$userNo | cardId=$cardId | startDate=${query.startDate} | endDate=${query.endDate}"
            )

            val response = api.getCardTransactions(
                cardId = cardId,
                userNo = userNo,
                startDate = query.startDate,
                endDate = query.endDate
            )
            val errorBody = response.errorBody()?.string()

            Log.d(
                TAG,
                "getCardTransactions response | code=${response.code()} | body=${response.body()} | error=$errorBody"
            )

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "카드 거래내역 조회 실패(code=${response.code()}) ${errorBody ?: ""}".trim()
                )
            }

            response.body().orEmpty().map { it.toItemData() }
        }
    }

    private fun buildTransactionQuery(period: String): CardTransactionQuery {
        val format = SimpleDateFormat("yyyyMMdd", Locale.KOREA)

        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val start = Calendar.getInstance().apply {
            when (period) {
                "전체" -> add(Calendar.YEAR, -10)
                "1주일" -> add(Calendar.DAY_OF_MONTH, -7)
                "1개월" -> add(Calendar.MONTH, -1)
                "3개월" -> add(Calendar.MONTH, -3)
                "6개월" -> add(Calendar.MONTH, -6)
                else -> add(Calendar.MONTH, -1)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return CardTransactionQuery(
            startDate = format.format(start.time),
            endDate = format.format(end.time)
        )
    }

    private fun CardTransactionResponse.toItemData(): CardTransactionItemData {
        val resolvedCategory = when {
            !categoryName.isNullOrBlank() -> categoryName
            !aiCategory.isNullOrBlank() -> aiCategory
            else -> "기타"
        }

        val date = transactionDate?.trim().orEmpty()
        val time = transactionTime?.trim().orEmpty()
        val transacted = listOf(date, time)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val canceled = when (cardStatus?.trim()?.uppercase()) {
            "CANCELED", "CANCELLED", "CANCEL", "승인취소", "취소" -> true
            else -> false
        }

        return CardTransactionItemData(
            transactionId = transactionUniqueNo.ifBlank { logId.toString() },
            merchantName = merchantName.orEmpty().ifBlank { "가맹점 정보 없음" },
            category = resolvedCategory,
            amount = amount,
            isCanceled = canceled,
            transactedAt = transacted
        )
    }
}
