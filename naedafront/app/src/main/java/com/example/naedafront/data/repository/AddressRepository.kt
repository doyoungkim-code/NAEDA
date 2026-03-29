package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.AddressApi
import com.example.naedafront.data.remote.request.CreateAddressRequest
import com.example.naedafront.data.remote.response.AddressResponse

class AddressRepository {

    private val addressApi: AddressApi =
        ApiConfig.retrofit.create(AddressApi::class.java)

    suspend fun getAddresses(
        userNo: Long
    ): Result<List<AddressResponse>> {
        return try {
            val response = addressApi.getAddresses(userNo)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "배송지 목록 조회 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "배송지 목록 조회 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "배송지 목록 조회 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }

    suspend fun getAddressDetail(
        userNo: Long,
        addressId: Long
    ): Result<AddressResponse> {
        return try {
            val response = addressApi.getAddressDetail(
                userNo = userNo,
                addressId = addressId
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(
                        IllegalStateException("배송지 상세 응답 바디가 비어 있습니다.")
                    )

                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "배송지 상세 조회 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "배송지 상세 조회 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "배송지 상세 조회 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }

    suspend fun deleteAddress(
        userNo: Long,
        addressId: Long
    ): Result<Unit> {
        return try {
            val response = addressApi.deleteAddress(
                userNo = userNo,
                addressId = addressId
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "배송지 삭제 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "배송지 삭제 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "배송지 삭제 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }

    suspend fun createAddress(
        userNo: Long,
        request: CreateAddressRequest
    ): Result<AddressResponse> {
        return try {
            val response = addressApi.createAddress(
                userNo = userNo,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(
                        IllegalStateException("배송지 생성 응답 바디가 비어 있습니다.")
                    )

                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "배송지 생성 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "배송지 생성 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "배송지 생성 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }

    suspend fun setDefaultAddress(
        userNo: Long,
        addressId: Long
    ): Result<AddressResponse> {
        return try {
            val response = addressApi.setDefaultAddress(
                userNo = userNo,
                addressId = addressId
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(
                        IllegalStateException("기본 배송지 설정 응답 바디가 비어 있습니다.")
                    )

                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "기본 배송지 설정 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "기본 배송지 설정 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "기본 배송지 설정 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }
}