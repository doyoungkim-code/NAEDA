package com.example.naedafront.data.repository

import android.util.Log
import com.example.naedafront.data.remote.RetrofitClient
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
    val transactedAt: String,
    val approvalNumber: String?,
    val cardNo: String?,
    val installment: String?
)

object CardRepository {

    private const val TAG = "CardRepository"

    private val api: CardApi by lazy {
        RetrofitClient.cardApi
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
        period: String,
        transactionId: String? = null
    ): Result<List<CardTransactionItemData>> {
        return runCatching {
            val query = buildTransactionQuery(period)

            Log.d(
                TAG,
                "getCardTransactions start | userNo=$userNo | cardId=$cardId | query=$query"
            )

            val response = api.getCardTransactions(
                cardId = cardId,
                userNo = userNo,
                request = query
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

            val items = response.body().orEmpty().map { it.toItemData() }

            transactionId?.takeIf { it.isNotBlank() }?.let { targetId ->
                items.filter { it.transactionId == targetId }
            } ?: items
        }
    }

    private fun buildTransactionQuery(period: String): Map<String, String> {
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

        return mapOf(
            "startDate" to format.format(start.time),
            "endDate" to format.format(end.time)
        )
    }

    private fun CardTransactionResponse.toItemData(): CardTransactionItemData {
        val resolvedCategory = when {
            !categoryName.isNullOrBlank() -> categoryName
            !aiCategory.isNullOrBlank() -> aiCategory
            else -> "기타"
        }

        val transacted = buildString {
            if (!transactionDate.isNullOrBlank()) append(transactionDate.trim())
            if (!transactionTime.isNullOrBlank()) {
                if (isNotBlank()) append(" ")
                append(transactionTime.trim())
            }
        }

        val canceled = when (cardStatus?.uppercase()) {
            "CANCELED", "CANCELLED", "CANCEL", "승인취소", "취소" -> true
            else -> false
        }

        return CardTransactionItemData(
            transactionId = transactionUniqueNo.ifBlank { logId.toString() },
            merchantName = merchantName.orEmpty().ifBlank { "가맹점 정보 없음" },
            category = resolvedCategory,
            amount = amount,
            isCanceled = canceled,
            transactedAt = transacted,
            approvalNumber = null,
            cardNo = null,
            installment = null
        )
    }
}