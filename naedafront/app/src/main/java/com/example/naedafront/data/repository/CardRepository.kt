package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.RetrofitClient
import com.example.naedafront.data.remote.api.CardApi
import com.example.naedafront.data.remote.response.CardResponse
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CardTransactionResponse(
    val transactionId: String,
    val merchantName: String,
    val category: String,
    val amount: Long,
    val isCanceled: Boolean,
    val transactedAt: String,
    val approvalNumber: String?,
    val cardNo: String?,
    val installment: String?,
    val rawJson: JsonObject
)

object CardRepository {

    // 네 RetrofitClient 구조가 다르면 이 줄만 바꾸면 됨.
    private val api: CardApi by lazy {
        RetrofitClient.cardApi
    }

    suspend fun getCards(userNo: Long): Result<List<CardResponse>> {
        return runCatching {
            val response = api.getCards(userNo)
            if (!response.isSuccessful) {
                throw IllegalStateException("카드 목록 조회 실패: ${response.code()}")
            }
            response.body().orEmpty()
        }
    }

    suspend fun getCardTransactions(
        userNo: Long,
        cardId: Long,
        period: String,
        transactionId: String? = null
    ): Result<List<CardTransactionResponse>> {
        return runCatching {
            val query = buildTransactionQuery(period, transactionId)
            val response = api.getCardTransactions(
                cardId = cardId,
                userNo = userNo,
                request = query
            )

            if (!response.isSuccessful) {
                throw IllegalStateException("카드 거래내역 조회 실패: ${response.code()}")
            }

            response.body().orEmpty().map { it.toCardTransactionResponse() }
        }
    }

    private fun buildTransactionQuery(
        period: String,
        transactionId: String? = null
    ): Map<String, String> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val end = Calendar.getInstance()
        val start = Calendar.getInstance()

        when (period) {
            "1주일" -> start.add(Calendar.DAY_OF_MONTH, -7)
            "1개월" -> start.add(Calendar.MONTH, -1)
            "3개월" -> start.add(Calendar.MONTH, -3)
            "6개월" -> start.add(Calendar.MONTH, -6)
            else -> start.add(Calendar.MONTH, -1)
        }

        return buildMap {
            put("fromDate", format.format(start.time))
            put("toDate", format.format(end.time))
            transactionId?.takeIf { it.isNotBlank() }?.let {
                put("transactionId", it)
            }
        }
    }

    private fun JsonObject.toCardTransactionResponse(): CardTransactionResponse {
        val txId = stringOf(
            "transactionId", "id", "paymentId", "approvalId", "historyId"
        ) ?: System.currentTimeMillis().toString()

        val merchant = stringOf(
            "merchantName", "storeName", "shopName", "franchiseName", "placeName", "description"
        ) ?: "가맹점 정보 없음"

        val category = stringOf(
            "category", "merchantCategory", "storeCategory", "type"
        ) ?: "기타"

        val amount = longOf(
            "amount", "paymentAmount", "approvedAmount", "transactionAmount", "useAmount"
        ) ?: 0L

        val isCanceled = booleanOf(
            "isCanceled", "canceled", "cancelled", "isCancel", "cancelYn"
        ) ?: run {
            val status = stringOf("status", "transactionStatus", "paymentStatus")?.uppercase()
            status in listOf("CANCELED", "CANCELLED", "CANCEL", "VOID")
        }

        val transactedAt = stringOf(
            "transactedAt", "transactionAt", "approvedAt", "createdAt", "usedAt", "paymentAt"
        ) ?: ""

        val approvalNumber = stringOf(
            "approvalNumber", "approveNumber", "approvalNo", "authCode"
        )

        val cardNo = stringOf(
            "cardNo", "maskedCardNo"
        )

        val installment = stringOf(
            "installment", "installmentMonths", "monthlyInstallment"
        )

        return CardTransactionResponse(
            transactionId = txId,
            merchantName = merchant,
            category = category,
            amount = amount,
            isCanceled = isCanceled,
            transactedAt = transactedAt,
            approvalNumber = approvalNumber,
            cardNo = cardNo,
            installment = installment,
            rawJson = this
        )
    }

    private fun JsonObject.stringOf(vararg keys: String): String? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) {
                return value.asString
            }
        }
        return null
    }

    private fun JsonObject.longOf(vararg keys: String): Long? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) {
                runCatching { return value.asLong }
                runCatching { return value.asString.replace(",", "").toLong() }
            }
        }
        return null
    }

    private fun JsonObject.booleanOf(vararg keys: String): Boolean? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) {
                runCatching { return value.asBoolean }
                val text = runCatching { value.asString }.getOrNull()?.uppercase()
                if (text == "Y" || text == "TRUE") return true
                if (text == "N" || text == "FALSE") return false
            }
        }
        return null
    }
}