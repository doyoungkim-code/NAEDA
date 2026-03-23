package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.FestivalApiResponse
import com.example.naedafront.data.remote.NoticeApi
import com.example.naedafront.data.remote.NoticeApiResponse

object NoticeRepository {
    private val api = ApiConfig.retrofit.create(NoticeApi::class.java)

    suspend fun getAllNotices(): Result<List<NoticeApiResponse>> = runCatching {
        api.getAllNotices()
    }

    suspend fun getNotice(noticeId: Long): Result<NoticeApiResponse> = runCatching {
        api.getNotice(noticeId)
    }

    suspend fun getAllFestivals(): Result<List<FestivalApiResponse>> = runCatching {
        api.getAllFestivals()
    }

    suspend fun getFestival(festivalId: Long): Result<FestivalApiResponse> = runCatching {
        api.getFestival(festivalId)
    }
}
