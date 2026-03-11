package com.example.naedafront.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.BuildConfig
import com.example.naedafront.data.remote.BackendConnectionStatus
import com.example.naedafront.data.remote.BackendStatusRepository
import com.example.naedafront.ui.theme.Mint900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    var connectionStatus by remember {
        mutableStateOf(
            BackendConnectionStatus(
                isConnected = false,
                message = "배포 서버 연결 확인 중..."
            )
        )
    }

    LaunchedEffect(Unit) {
        connectionStatus = withContext(Dispatchers.IO) {
            BackendStatusRepository.fetchStatus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryContainer.copy(alpha = 0.4f),
                        surface
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "내다를 시작하려면\n본인인증을 해주세요",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "신속하고 안전한 금융 서비스를 시작합니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                NaedaLogoPlaceholder()
            }

            Spacer(modifier = Modifier.weight(1f))

            ConnectionStatusCard(connectionStatus = connectionStatus)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Mint900
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Text(
                    text = "시작하기",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    text = "이미 계정이 있으신가요?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "로그인",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(connectionStatus: BackendConnectionStatus) {
    val borderColor = if (connectionStatus.isConnected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    }
    val containerColor = if (connectionStatus.isConnected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (connectionStatus.isConnected) "배포 서버 연결됨" else "배포 서버 확인 필요",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = BuildConfig.BACKEND_BASE_URL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = connectionStatus.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NaedaLogoPlaceholder() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier.size(100.dp)) {
        val cornerRadius = 20.dp.toPx()

        drawRoundRect(
            color = primaryColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )

        drawCircle(
            color = onSurface,
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.35f,
                y = size.height * 0.4f
            )
        )

        drawCircle(
            color = onSurface,
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.65f,
                y = size.height * 0.4f
            )
        )

        drawArc(
            color = onSurface,
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.25f,
                y = size.height * 0.35f
            ),
            size = androidx.compose.ui.geometry.Size(
                width = size.width * 0.5f,
                height = size.height * 0.35f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen()
    }
}
