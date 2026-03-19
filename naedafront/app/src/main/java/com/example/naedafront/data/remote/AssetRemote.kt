package com.example.naedafront.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import retrofit2.http.GET
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

data class WalletAssetsResponse(
    val accounts: List<AssetAccountResponse>,
    val cards: List<AssetCardResponse>
)

interface AssetApi {
    @GET("api/accounts")
    suspend fun getAccounts(
        @Query("userNo") userNo: Long
    ): List<AssetAccountResponse>

    @GET("api/cards")
    suspend fun getCards(
        @Query("userNo") userNo: Long
    ): List<AssetCardResponse>
}

object AssetRepository {
    private val api = ApiConfig.retrofit.create(AssetApi::class.java)
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

        WalletAssetsResponse(
            accounts = accountsDeferred.await(),
            cards = cardsDeferred.await()
        )
    }

    private fun toReadableException(throwable: Throwable, fallback: String): Throwable {
        if (throwable !is HttpException) {
            return throwable
        }

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
