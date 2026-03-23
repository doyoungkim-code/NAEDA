package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.AddressApi
import com.example.naedafront.data.remote.request.CreateAddressRequest
import com.example.naedafront.data.remote.response.AddressResponse

class AddressRepository {

    private val addressApi: AddressApi =
        ApiConfig.retrofit.create(AddressApi::class.java)

    suspend fun getAddresses(userNo: Long): Result<List<AddressResponse>> {
        return try {
            val response = addressApi.getAddresses(userNo)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("주소 목록 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAddressDetail(userNo: Long, addressId: Long): Result<AddressResponse> {
        return try {
            val response = addressApi.getAddressDetail(userNo, addressId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("주소 상세 응답이 비어있습니다."))
                }
            } else {
                Result.failure(Exception("주소 단건 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(userNo: Long, addressId: Long): Result<Unit> {
        return try {
            val response = addressApi.deleteAddress(userNo, addressId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("주소 삭제 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAddress(
        userNo: Long,
        request: CreateAddressRequest
    ): Result<AddressResponse> {
        return try {
            val response = addressApi.createAddress(userNo, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("주소 생성 응답이 비어있습니다."))
                }
            } else {
                Result.failure(Exception("주소 생성 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDefaultAddress(userNo: Long, addressId: Long): Result<AddressResponse> {
        return try {
            val response = addressApi.setDefaultAddress(userNo, addressId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("기본 배송지 설정 응답이 비어있습니다."))
                }
            } else {
                Result.failure(Exception("기본 배송지 설정 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}