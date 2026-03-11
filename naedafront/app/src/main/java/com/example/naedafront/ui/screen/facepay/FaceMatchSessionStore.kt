package com.example.naedafront.ui.screen.facepay

import com.example.naedafront.data.remote.AccountResponse
import com.example.naedafront.data.remote.SearchResponse

data class FaceMatchCandidateResult(
    val userId: String,
    val userNo: Long?,
    val pose: String,
    val similarity: Float,
    val accounts: List<AccountResponse>
)

data class FaceMatchResultSnapshot(
    val searchResponse: SearchResponse,
    val topCandidate: FaceMatchCandidateResult?,
    val matchedCandidates: List<FaceMatchCandidateResult>,
    val ambiguousCandidates: List<FaceMatchCandidateResult>,
    val belowThresholdCandidates: List<FaceMatchCandidateResult>
)

object FaceMatchSessionStore {
    var latestResult: FaceMatchResultSnapshot? = null
}
