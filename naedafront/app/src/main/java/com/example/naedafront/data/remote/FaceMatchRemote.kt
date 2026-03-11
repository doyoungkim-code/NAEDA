package com.example.naedafront.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun login(userId: String, password: String): LoginResponse {
        return api.login(LoginRequest(userId = userId, password = password))
    }

    suspend fun searchFace(
        token: String,
        imageBytes: ByteArray,
        topK: Int = 5,
        amount: Long = 0L
    ): SearchResponse {
        return api.searchFace(
            authorization = bearer(token),
            image = imagePart(imageBytes),
            topK = topK.toString().toRequestBody(textType),
            amount = amount.toString().toRequestBody(textType)
        )
    }

    suspend fun getAccounts(token: String, userNo: Long): List<AccountResponse> {
        return api.getAccounts(
            authorization = bearer(token),
            userNo = userNo
        )
    }

    private fun bearer(token: String): String = "Bearer $token"

    private fun imagePart(imageBytes: ByteArray): MultipartBody.Part {
        val body = imageBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("image", "frame.jpg", body)
    }
}
