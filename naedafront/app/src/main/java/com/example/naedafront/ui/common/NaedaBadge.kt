package com.example.naedafront.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.naedafront.ui.theme.Error
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Success
import com.example.naedafront.ui.theme.Warning

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 내다(NAEDA) 뱃지 / 태그
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// 사용 예시:
// NaedaBadge(text = "결제 완료", type = BadgeType.SUCCESS)
// NaedaBadge(text = "처리 중", type = BadgeType.INFO)
// NaedaBadge(text = "이상 거래 감지", type = BadgeType.WARNING)
// NaedaBadge(text = "결제 실패", type = BadgeType.ERROR)
// NaedaBadge(text = "+500P", type = BadgeType.POINT)

enum class BadgeType {
    SUCCESS,   // 결제 완료, 이체 성공
    WARNING,   // FDS 경고
    ERROR,     // 결제 실패, 차단
    INFO,      // 처리 중, 일반 정보
    POINT,     // 포인트 적립/사용
}

@Composable
fun NaedaBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, contentColor) = when (type) {
        BadgeType.SUCCESS -> Success.copy(alpha = 0.12f) to Success
        BadgeType.WARNING -> Warning.copy(alpha = 0.15f) to Warning.copy(red = 0.7f, green = 0.55f, blue = 0f)
        BadgeType.ERROR -> Error.copy(alpha = 0.12f) to Error
        BadgeType.INFO -> Mint500.copy(alpha = 0.12f) to Mint500
        BadgeType.POINT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
