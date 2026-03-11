package com.example.naedafront.data.remote

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

data class LoginRequest(
    val userId: String,
    val password: String
)

data class LoginResponse(
    val userNo: Long?,
    val userId: String?,
    val username: String?,
    val userKey: String?,
    val accessToken: String?,
    val refreshToken: String?
)

data class AccountResponse(
    val accountId: Long?,
    val bankCode: String?,
    val bankName: String?,
    val accountNo: String?,
    val accountName: String?,
    val accountBalance: Long?,
    val currency: String?
)

data class CandidateDto(
    val userId: String?,
    val userNo: Long?,
    val pose: String?,
    val similarity: Float?
)

data class SearchResponse(
    val matched: Boolean,
    val status: String?,
    val nextAction: String?,
    val bestUserId: String?,
    val matchedUserNo: Long?,
    val similarity: Float,
    val matchThreshold: Float,
    val ambiguousThreshold: Float,
    val qualityScore: Float,
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val blocked: Boolean,
    val rbaReason: String?,
    val candidates: List<CandidateDto> = emptyList()
)

data class ApiErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val detail: String? = null
)

class ApiRequestException(
    val errorCode: String?,
    val statusCode: Int,
    override val message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

interface FaceMatchApi {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @Multipart
    @POST("api/v1/face/search")
    suspend fun searchFace(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part,
        @Part("topK") topK: RequestBody,
        @Part("amount") amount: RequestBody
    ): SearchResponse

    @GET("api/accounts")
    suspend fun getAccounts(
        @Header("Authorization") authorization: String,
        @Query("userNo") userNo: Long
    ): List<AccountResponse>
}

object FaceMatchRepository {
    private val api = ApiConfig.retrofit.create(FaceMatchApi::class.java)
    private val textType = "text/plain".toMediaType()
    private val gson = Gson()

    suspend fun login(userId: String, password: String): LoginResponse {
        return api.login(LoginRequest(userId = userId, password = password))
    }

    suspend fun searchFace(
        token: String,
        imageBytes: ByteArray,
        topK: Int = 5,
        amount: Long = 0L
    ): SearchResponse {
        return runCatching {
            api.searchFace(
                authorization = bearer(token),
                image = imagePart(imageBytes),
                topK = topK.toString().toRequestBody(textType),
                amount = amount.toString().toRequestBody(textType)
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "얼굴 검색 요청에 실패했습니다.")
        }
    }

    suspend fun getAccounts(token: String, userNo: Long): List<AccountResponse> {
        return runCatching {
            api.getAccounts(
                authorization = bearer(token),
                userNo = userNo
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "계좌 정보를 불러오지 못했습니다.")
        }
    }

    private fun bearer(token: String): String = "Bearer $token"

    private fun imagePart(imageBytes: ByteArray): MultipartBody.Part {
        val body = imageBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("image", "frame.jpg", body)
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
