package com.example.naedafront.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 내다(NAEDA) 공통 다이얼로그
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// 사용 예시:
// NaedaDialog(
//     title = "결제 확인",
//     message = "30,000원을 결제하시겠습니까?",
//     confirmText = "결제",
//     onConfirm = { processPayment() },
//     onDismiss = { showDialog = false },
// )
//
// 단일 버튼 (알림용):
// NaedaDialog(
//     title = "결제 완료",
//     message = "결제가 성공적으로 완료되었습니다.",
//     confirmText = "확인",
//     onConfirm = { showDialog = false },
//     onDismiss = { showDialog = false },
//     showCancelButton = false,
// )

@Composable
fun NaedaDialog(
    title: String,
    message: String,
    confirmText: String = "확인",
    cancelText: String = "취소",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showCancelButton: Boolean = true,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 제목
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 메시지
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (showCancelButton) {
                        NaedaButton(
                            text = cancelText,
                            onClick = onDismiss,
                            type = NaedaButtonType.TEXT,
                            fullWidth = false,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    NaedaButton(
                        text = confirmText,
                        onClick = onConfirm,
                        fullWidth = false,
                    )
                }
            }
        }
    }
}
