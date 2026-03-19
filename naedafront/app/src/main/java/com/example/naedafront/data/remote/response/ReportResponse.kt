package com.example.naedafront.data.remote.response

data class ReportResponse(
    val reportId: Long,
    val periodType: String,
    val periodStart: String,
    val periodEnd: String,
    val categoryBreakdown: Map<String, Long>,
    val totalSpending: Long,
    val localSpending: Long,
    val localRatio: Double,
    val localGrade: String,
    val insights: List<String>,
    val generated: String
)