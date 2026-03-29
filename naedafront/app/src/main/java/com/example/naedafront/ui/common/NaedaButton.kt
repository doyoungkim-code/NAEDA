package com.example.naedafront.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 내다(NAEDA) 공통 버튼
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// 사용 예시:
// NaedaButton(text = "결제하기", onClick = { })
// NaedaButton(text = "취소", onClick = { }, type = NaedaButtonType.OUTLINED)
// NaedaButton(text = "건너뛰기", onClick = { }, type = NaedaButtonType.TEXT)
// NaedaButton(text = "처리 중...", onClick = { }, enabled = false)

enum class NaedaButtonType {
    PRIMARY,    // 메인 액션 (결제, 확인, 등록)
    OUTLINED,   // 보조 액션 (취소, 이전)
    TEXT,       // 텍스트만 (건너뛰기, 링크형)
}

@Composable
fun NaedaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: NaedaButtonType = NaedaButtonType.PRIMARY,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth().height(52.dp)
    } else {
        modifier.height(52.dp)
    }

    when (type) {
        NaedaButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = buttonModifier,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        NaedaButtonType.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    },
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = buttonModifier,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        NaedaButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
