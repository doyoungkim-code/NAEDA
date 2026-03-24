package com.example.naedafront.data.remote

import com.example.naedafront.data.remote.api.PaymentApi
import com.example.naedafront.data.remote.response.PaymentDetailResponse
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class AssetAccountResponse(
    val accountId: Long?,
    val bankCode: String?,
    val bankName: String?,
    val accountNo: String?,
    val accountName: String?,
    val accountBalance: Long?,
    val currency: String?
)

data class AssetCardResponse(
    val cardId: Long?,
    val cardNo: String?,
    val cardUniqueNo: String?,
    val cardIssuerCode: String?,
    val cardIssuerName: String?,
    val cardName: String?,
    val cardExpiryDate: String?,
    val isActive: Boolean?,
    val accountId: Long?,
    val cardType: String?,
    val creditLimit: Long?,
    val billingDate: Int?
)

data class AssetTransactionResponse(
    val logId: Long?,
    val accountId: Long?,
    val transactionType: String?,
    val amount: Long?,
    val balanceAfter: Long?,
    val counterpart: String?,
    val memo: String?,
    val category: String?,
    val aiCategory: String?,
    val ssafyTransactionId: String?,
    val transacted: String?
)

data class AssetPayMethodResponse(
    val paymentMethodId: Long?,
    val userNo: Long?,
    val methodType: String?,
    val accountId: Long?,
    val debitCardId: Long?,
    val creditCardId: Long?,
    val isDefault: Boolean?,
    val isFacePay: Boolean?,
    val isActive: Boolean?
)

data class CardProductResponse(
    val cardUniqueNo: String?,
    val cardIssuerCode: String?,
    val cardIssuerName: String?,
    val cardName: String?,
    val cardTypeCode: String?,
    val cardTypeName: String?,
    val baselinePerformance: String?,
    val maxBenefitLimit: String?,
    val cardDescription: String?
)

data class CardRegisterRequest(
    val cardUniqueNo: String,
    val withdrawalAccountNo: String,
    val withdrawalDate: String,
    val cardTypeCode: String
)

data class CardRegisterResponse(
    val cardId: Long?,
    val cardNo: String?,
    val cardUniqueNo: String?,
    val cardIssuerCode: String?,
    val cardIssuerName: String?,
    val cardName: String?,
    val cardExpiryDate: String?,
    val cardType: String?,
    val withdrawalAccountNo: String?,
    val withdrawalDate: String?,
    val paymentMethodId: Long?
)

data class WalletAssetsResponse(
    val accounts: List<AssetAccountResponse>,
    val cards: List<AssetCardResponse>,
    val payMethods: List<AssetPayMethodResponse>
)

data class PaymentResponse(
    val paymentId: Long?,
    val userNo: Long?,
    val storeId: Long?,
    val storeName: String?,
    val categoryName: String?,
    val paymentMethodId: Long?,
    val amount: Long?,
    val status: String?,
    val authMethod: String?,
    val authLevel: String?,
    val facePay: Boolean?,
    val faceDistance: Double?,
    val livenessPass: Boolean?,
    val pinVerified: Boolean?,
    val fdsScore: Int?,
    val fdsAction: String?,
    val earnedPoints: Long?,
    val ssafyTransactionId: String?,
    val failureReason: String?,
    val createdAt: String?
)
data class CurrentMonthSpendingAnalysisResponse(
    val periodStart: String?,
    val periodEnd: String?,
    val totalSpending: Long?,
    val transactionCount: Int?,
    val topCategory: String?,
    val topAmount: Long?,
    val categoryBreakdown: Map<String, Long>,
    val insights: List<String>
)


interface AssetApi {
    @GET("api/accounts")
    suspend fun getAccounts(
        @Query("userNo") userNo: Long
    ): List<AssetAccountResponse>

    @GET("api/accounts/{accountNo}")
    suspend fun getAccount(
        @Path("accountNo") accountNo: String,
        @Query("userNo") userNo: Long
    ): AssetAccountResponse

    @GET("api/cards")
    suspend fun getCards(
        @Query("userNo") userNo: Long
    ): List<AssetCardResponse>

    @GET("api/cards/products")
    suspend fun getCardProducts(
        @Query("userNo") userNo: Long
    ): List<CardProductResponse>

    @POST("api/cards")
    suspend fun registerCard(
        @Query("userNo") userNo: Long,
        @Body request: CardRegisterRequest
    ): CardRegisterResponse

    @DELETE("api/cards/{cardId}")
    suspend fun deleteCard(
        @Path("cardId") cardId: Long,
        @Query("userNo") userNo: Long,
        @Query("cardType") cardType: String
    )

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("userNo") userNo: Long,
        @Query("accountId") accountId: Long,
        @Query("size") size: Int = 500
    ): List<AssetTransactionResponse>

    @GET("api/pay-methods")
    suspend fun getPayMethods(
        @Query("userNo") userNo: Long
    ): List<AssetPayMethodResponse>

    @PATCH("api/pay-methods/{id}/default")
    suspend fun setDefaultPayMethod(
        @Path("id") paymentMethodId: Long,
        @Query("userNo") userNo: Long
    ): AssetPayMethodResponse

    @PATCH("api/pay-methods/{id}/face-pay")
    suspend fun setFacePayMethod(
        @Path("id") paymentMethodId: Long,
        @Query("userNo") userNo: Long,
        @Query("enabled") enabled: Boolean = true
    ): AssetPayMethodResponse
}

interface PayApi {
    @GET("api/pay")
    suspend fun getPayments(
        @Header("X-User-No") userNo: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<PaymentResponse>

    @GET("api/pay/analysis/current-month")
    suspend fun getCurrentMonthSpendingAnalysis(
        @Header("X-User-No") userNo: Long
    ): CurrentMonthSpendingAnalysisResponse
}

object AssetRepository {
    private val api = ApiConfig.retrofit.create(AssetApi::class.java)
    private val payApi = ApiConfig.retrofit.create(PayApi::class.java)
    private val paymentDetailApi = ApiConfig.retrofit.create(PaymentApi::class.java)
    private val gson = Gson()

    suspend fun getWalletAssets(userNo: Long): WalletAssetsResponse = coroutineScope {
        val accountsDeferred = async {
            runCatching { api.getAccounts(userNo) }
                .getOrElse { throwable ->
                    throw toReadableException(throwable, "계좌 정보를 불러오지 못했습니다.")
                }
        }
        val cardsDeferred = async {
            runCatching { api.getCards(userNo) }
                .getOrElse { throwable ->
                    throw toReadableException(throwable, "카드 정보를 불러오지 못했습니다.")
                }
        }
        val payMethodsDeferred = async {
            runCatching { api.getPayMethods(userNo) }
                .getOrElse { throwable ->
                    throw toReadableException(throwable, "결제수단 정보를 불러오지 못했습니다.")
                }
        }

        WalletAssetsResponse(
            accounts = accountsDeferred.await(),
            cards = cardsDeferred.await(),
            payMethods = payMethodsDeferred.await()
        )
    }

    suspend fun getAccount(userNo: Long, accountNo: String): AssetAccountResponse {
        return runCatching {
            api.getAccount(accountNo = accountNo, userNo = userNo)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "계좌 상세 정보를 불러오지 못했습니다.")
        }
    }

    suspend fun getTransactions(userNo: Long, accountId: Long, size: Int = 500): List<AssetTransactionResponse> {
        return runCatching {
            api.getTransactions(
                userNo = userNo,
                accountId = accountId,
                size = size
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "거래내역을 불러오지 못했습니다.")
        }
    }

    suspend fun setDefaultPaymentMethod(userNo: Long, paymentMethodId: Long): AssetPayMethodResponse {
        return runCatching {
            api.setDefaultPayMethod(
                paymentMethodId = paymentMethodId,
                userNo = userNo
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "대표 결제수단을 변경하지 못했습니다.")
        }
    }

    suspend fun setFacePayPaymentMethod(
        userNo: Long,
        paymentMethodId: Long,
        enabled: Boolean = true
    ): AssetPayMethodResponse {
        return runCatching {
            api.setFacePayMethod(
                paymentMethodId = paymentMethodId,
                userNo = userNo,
                enabled = enabled
            )
        }.getOrElse { throwable ->
            throw toReadableException(
                throwable,
                if (enabled) "페이스페이 사용 설정에 실패했습니다." else "페이스페이 사용 해제에 실패했습니다."
            )
        }
    }

    suspend fun getCardProducts(userNo: Long): List<CardProductResponse> {
        return runCatching {
            api.getCardProducts(userNo)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "카드 상품 목록을 불러오지 못했습니다.")
        }
    }

    suspend fun registerCard(userNo: Long, request: CardRegisterRequest): CardRegisterResponse {
        return runCatching {
            api.registerCard(userNo, request)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "카드 등록에 실패했습니다.")
        }
    }

    suspend fun getPayments(
        userNo: Long,
        from: String? = null,
        to: String? = null
    ): Result<List<PaymentResponse>> = runCatching {
        payApi.getPayments(userNo = userNo, from = from, to = to)
    }

    suspend fun getCurrentMonthSpendingAnalysis(
        userNo: Long
    ): Result<CurrentMonthSpendingAnalysisResponse> = runCatching {
        payApi.getCurrentMonthSpendingAnalysis(userNo = userNo)
    }

    suspend fun getPaymentDetail(
        userNo: Long,
        paymentId: Long
    ): Result<PaymentDetailResponse> = runCatching {
        val response = paymentDetailApi.getPaymentDetail(
            userNo = userNo,
            id = paymentId
        )

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("결제 상세 조회 실패: ${response.code()}")
        }

        response.body()!!
    }

    private fun toReadableException(throwable: Throwable, fallback: String): Throwable {
        if (throwable !is HttpException) return throwable

        val errorBody = throwable.response()?.errorBody()?.string().orEmpty()
        val parsed = runCatching { gson.fromJson(errorBody, ApiErrorResponse::class.java) }.getOrNull()
        val message = buildString {
            append(parsed?.message?.takeUnless { it.isBlank() } ?: fallback)
            parsed?.code?.takeUnless { it.isBlank() }?.let { append(" [$it]") }
            parsed?.detail?.takeUnless { it.isBlank() }?.let { append(" - $it") }
            append(" (HTTP ${throwable.code()})")
        }

        return ApiRequestException(
            errorCode = parsed?.code,
            statusCode = throwable.code(),
            message = message,
            cause = throwable
        )
    }
}


