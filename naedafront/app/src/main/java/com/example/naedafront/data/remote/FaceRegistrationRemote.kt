package com.example.naedafront.data.remote

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

data class EnrollResponseDto(
    val success: Boolean,
    val userId: String?,
    val pose: String?,
    val savedAt: String?
)

data class HeadPoseCheckResponseDto(
    val expectedDirection: String? = null,
    val detectedDirection: String? = null,
    val matched: Boolean = false,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val confidence: Float = 0f
)

data class ResidentIdExtractResponseDto(
    val documentType: String? = null,
    val documentMatched: Boolean = false,
    val name: String? = null,
    val residentFront6: String? = null,
    val residentBackFirst1: String? = null,
    val provider: String? = null,
    val confidence: Double = 0.0,
    val documentConfidence: Double = 0.0,
    val nameConfidence: Double = 0.0,
    val residentNumberConfidence: Double = 0.0,
    val extractionStatus: String? = null,
    val warnings: List<String> = emptyList()
)

data class ResidentIdConfirmRequestBody(
    val name: String,
    val residentFront6: String,
    val residentBackFirst1: String
)

data class ResidentIdVerifyResponseDto(
    val verified: Boolean,
    val nameMatched: Boolean,
    val residentNoMatched: Boolean,
    val nextAction: String? = null
)

data class UpdatePinRequestBody(
    val currentPin: String? = null,
    val newPin: String
)

data class VerifyPinRequestBody(
    val pin: String
)

data class VerifyPasswordRequestBody(
    val password: String
)

data class ResetPinWithPasswordRequestBody(
    val password: String,
    val newPin: String
)

data class PinUpdateResponseDto(
    val pinSet: Boolean,
    val message: String? = null
)

data class PinVerifyResponseDto(
    val verified: Boolean,
    val message: String? = null
)

data class PasswordVerifyResponseDto(
    val verified: Boolean,
    val message: String? = null
)

data class FacePaySettingsRequestBody(
    val enableSecondaryAuth: Boolean,
    val currentPin: String? = null,
    val paymentMethodId: Long? = null,
    val dailyLimit: Long? = null,
    val monthlyLimit: Long? = null,
    val singleTransactionLimit: Long? = null
)

data class FacePaySettingsResponseDto(
    val faceRegistered: Boolean = false,
    val secondaryAuthEnabled: Boolean = false,
    val message: String? = null
)

data class PayLimitRequestBody(
    val dailyLimit: Long,
    val monthlyLimit: Long,
    val singleTransactionLimit: Long
)

data class PayLimitResponseDto(
    val userNo: Long,
    val dailyLimit: Long,
    val monthlyLimit: Long,
    val singleTransactionLimit: Long
)

private interface FaceRegistrationApiService {
    @Multipart
    @POST("api/v1/face/enroll")
    suspend fun enrollFace(
        @Part("pose") pose: RequestBody,
        @Part image: MultipartBody.Part
    ): EnrollResponseDto

    @Multipart
    @POST("api/v1/face/liveness/headpose/check")
    suspend fun checkHeadPose(
        @Part("expectedDirection") expectedDirection: RequestBody,
        @Part image: MultipartBody.Part
    ): HeadPoseCheckResponseDto

    @Multipart
    @POST("api/v1/identity/id-card/extract")
    suspend fun extractResidentId(
        @Part image: MultipartBody.Part
    ): ResidentIdExtractResponseDto

    @POST("api/v1/identity/id-card/confirm")
    suspend fun confirmResidentId(
        @Body request: ResidentIdConfirmRequestBody
    ): ResidentIdVerifyResponseDto

    @PUT("api/users/me/pin")
    suspend fun updatePin(
        @Body request: UpdatePinRequestBody
    ): PinUpdateResponseDto

    @POST("api/users/me/pin/verify")
    suspend fun verifyPin(
        @Body request: VerifyPinRequestBody
    ): PinVerifyResponseDto

    @POST("api/users/me/pin/password-verify")
    suspend fun verifyPasswordForPinReset(
        @Body request: VerifyPasswordRequestBody
    ): PasswordVerifyResponseDto

    @GET("api/users/me/face-pay-settings")
    suspend fun getFacePaySettings(): FacePaySettingsResponseDto

    @PUT("api/users/me/pin/reset-with-password")
    suspend fun resetPinWithPassword(
        @Body request: ResetPinWithPasswordRequestBody
    ): PinUpdateResponseDto

    @PUT("api/users/me/face-pay-settings")
    suspend fun updateFacePaySettings(
        @Body request: FacePaySettingsRequestBody
    ): FacePaySettingsResponseDto

    @GET("api/pay/limit")
    suspend fun getPayLimit(
        @Query("userNo") userNo: Long
    ): PayLimitResponseDto

    @PUT("api/pay/limit")
    suspend fun updatePayLimit(
        @Query("userNo") userNo: Long,
        @Body request: PayLimitRequestBody
    ): PayLimitResponseDto
}

object FaceRegistrationRepository {
    private val service = ApiConfig.retrofit.create(FaceRegistrationApiService::class.java)
    private val textType = "text/plain".toMediaType()
    private val gson = Gson()

    suspend fun enrollFace(pose: String, imageBytes: ByteArray): EnrollResponseDto {
        return runCatching {
            service.enrollFace(
                pose = pose.toRequestBody(textType),
                image = imagePart(imageBytes, "face.jpg")
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "얼굴 등록에 실패했습니다.")
        }
    }

    suspend fun extractResidentId(imageBytes: ByteArray): ResidentIdExtractResponseDto {
        val requestImagePart = imagePart(imageBytes, "id-card.jpg")
        val initialResult = runCatching {
            service.extractResidentId(image = requestImagePart)
        }

        val initialFailure = initialResult.exceptionOrNull()
        if (initialFailure != null) {
            val readable = toReadableException(initialFailure, "신분증 OCR 추출에 실패했습니다.")
            if (isRecoverableOcrServiceError(readable)) {
                delay(350L)
                return runCatching {
                    service.extractResidentId(image = requestImagePart)
                }.getOrElse { retryThrowable ->
                    throw toReadableException(retryThrowable, "신분증 OCR 추출에 실패했습니다.")
                }
            }
            throw readable
        }

        return initialResult.getOrThrow()
    }

    suspend fun checkHeadPose(expectedDirection: String, imageBytes: ByteArray): HeadPoseCheckResponseDto {
        return runCatching {
            service.checkHeadPose(
                expectedDirection = expectedDirection.toRequestBody(textType),
                image = imagePart(imageBytes, "face.jpg")
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "얼굴 방향 검증에 실패했습니다.")
        }
    }

    suspend fun confirmResidentId(
        name: String,
        residentFront6: String,
        residentBackFirst1: String
    ): ResidentIdVerifyResponseDto {
        return runCatching {
            service.confirmResidentId(
                ResidentIdConfirmRequestBody(
                    name = name.trim(),
                    residentFront6 = residentFront6.trim(),
                    residentBackFirst1 = residentBackFirst1.trim()
                )
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "신분증 확인에 실패했습니다.")
        }
    }

    suspend fun updatePin(currentPin: String?, newPin: String): PinUpdateResponseDto {
        val body = UpdatePinRequestBody(
            currentPin = currentPin?.trim()?.takeUnless { it.isBlank() },
            newPin = newPin.trim()
        )
        android.util.Log.d("PinChange", "updatePin request: currentPin=${body.currentPin}, newPin=${body.newPin}")
        return runCatching {
            service.updatePin(body)
        }.getOrElse { throwable ->
            android.util.Log.e("PinChange", "updatePin failed", throwable)
            throw toReadableException(throwable, "PIN 설정에 실패했습니다.")
        }
    }

    suspend fun verifyPin(pin: String): PinVerifyResponseDto {
        val body = VerifyPinRequestBody(pin = pin.trim())
        return runCatching {
            service.verifyPin(body)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "현재 PIN 확인에 실패했습니다.")
        }
    }

    suspend fun verifyPasswordForPinReset(password: String): PasswordVerifyResponseDto {
        val body = VerifyPasswordRequestBody(password = password)
        return runCatching {
            service.verifyPasswordForPinReset(body)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "비밀번호 확인에 실패했습니다.")
        }
    }

    suspend fun resetPinWithPassword(password: String, newPin: String): PinUpdateResponseDto {
        val body = ResetPinWithPasswordRequestBody(
            password = password,
            newPin = newPin.trim()
        )
        return runCatching {
            service.resetPinWithPassword(body)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "PIN 재설정에 실패했습니다.")
        }
    }

    suspend fun getFacePaySettings(): FacePaySettingsResponseDto {
        return runCatching {
            service.getFacePaySettings()
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "페이스페이 설정 조회에 실패했습니다.")
        }
    }

    suspend fun updateFacePaySettings(
        enableSecondaryAuth: Boolean,
        currentPin: String?,
        paymentMethodId: Long?,
        dailyLimit: Long?,
        monthlyLimit: Long?,
        singleTransactionLimit: Long?
    ): FacePaySettingsResponseDto {
        return runCatching {
            service.updateFacePaySettings(
                FacePaySettingsRequestBody(
                    enableSecondaryAuth = enableSecondaryAuth,
                    currentPin = currentPin?.trim()?.takeUnless { it.isBlank() },
                    paymentMethodId = paymentMethodId,
                    dailyLimit = dailyLimit,
                    monthlyLimit = monthlyLimit,
                    singleTransactionLimit = singleTransactionLimit
                )
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "페이스페이 설정 저장에 실패했습니다.")
        }
    }

    suspend fun getPayLimit(userNo: Long): PayLimitResponseDto {
        return runCatching {
            service.getPayLimit(userNo)
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "결제 한도 조회에 실패했습니다.")
        }
    }

    suspend fun updatePayLimit(
        userNo: Long,
        dailyLimit: Long,
        monthlyLimit: Long,
        singleTransactionLimit: Long
    ): PayLimitResponseDto {
        return runCatching {
            service.updatePayLimit(
                userNo = userNo,
                request = PayLimitRequestBody(
                    dailyLimit = dailyLimit,
                    monthlyLimit = monthlyLimit,
                    singleTransactionLimit = singleTransactionLimit
                )
            )
        }.getOrElse { throwable ->
            throw toReadableException(throwable, "결제 한도 저장에 실패했습니다.")
        }
    }

    private fun imagePart(imageBytes: ByteArray, fileName: String): MultipartBody.Part {
        val body = imageBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("image", fileName, body)
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

    private fun isRecoverableOcrServiceError(throwable: Throwable): Boolean {
        if (throwable !is ApiRequestException) {
            return false
        }
        val code = throwable.errorCode?.trim()?.uppercase()
        return throwable.statusCode in setOf(502, 503, 504) ||
                code == "OCR_UNAVAILABLE" ||
                code == "OCR_TIMEOUT" ||
                code == "AI_TIMEOUT" ||
                code == "AI_UNAVAILABLE"
    }
}
