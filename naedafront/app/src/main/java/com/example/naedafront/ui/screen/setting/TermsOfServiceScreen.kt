package com.example.naedafront.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.Background

@Composable
fun TermsOfServiceScreen(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF5E6776),
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "이용약관",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202632)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "이 약관은 서비스 이용 방법과 포인트 환불 불가 정책, 페이스페이 책임 소재 등을 명시합니다.",
                fontSize = 13.sp,
                color = Color(0xFF9AA3AF)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 제1조
                ArticleCard(title = "제1조 (목적)") {
                    ArticleBody(
                        "본 약관은 'NAEDA'(이하 \"회사\")가 제공하는 금융 서비스 및 관련 부가 서비스(이하 \"서비스\")의 이용 조건 및 절차, 회사와 회원 간의 권리와 의무 및 책임 사항을 규정함을 목적으로 합니다."
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제2조
                ArticleCard(title = "제2조 (용어의 정의)") {
                    DefinitionItem("페이스페이(Face Pay)", "회원의 생체 정보(얼굴)를 활용하여 본인 인증 및 결제를 진행하는 서비스입니다.")
                    Spacer(modifier = Modifier.height(10.dp))
                    DefinitionItem("포인트", "앱 내 스토어에서 상품을 구매할 때 사용하는 가상 결제 수단입니다.")
                    Spacer(modifier = Modifier.height(10.dp))
                    DefinitionItem("가맹점", "회사와 계약을 체결하여 페이스페이 결제 수단을 제공하는 구미 지역 내 매장입니다.")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제3조
                ArticleCard(title = "제3조 (페이스페이 이용 및 보안)") {
                    BulletItem("회원은 얼굴 등록 시 본인의 정면 및 다각도 사진을 촬영해야 하며, 회사는 이를 벡터값으로 암호화하여 관리합니다.")
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem("회사는 'Passive Liveness' 기술 및 자이로 센서를 활용해 부정 등록을 방지합니다.")
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem("결제 승인: 유사도가 일정 기준 이하일 경우 PIN 번호 등 추가 인증을 요구할 수 있습니다.")
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem("5만 원 이상 결제 시에는 얼굴 인증 외에 추가 서명 절차가 필수적으로 요구됩니다.")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제4조
                ArticleCard(title = "제4조 (포인트 및 스토어 이용)") {
                    BulletItem("회원은 활동 및 구매를 통해 포인트를 적립하고 사용할 수 있습니다.")
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem("환불 정책: 포인트 스토어에서 구매한 상품은 디지털 쿠폰 또는 배송 상품의 특성상 구매 확정 후 취소 및 환불이 불가능합니다. 구매 전 노출되는 안내 메시지를 확인한 것으로 간주합니다.")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제5조
                ArticleCard(title = "제5조 (서비스의 제한)") {
                    BulletItem("회사는 구미 지역 특화 서비스를 제공하며, 위치 정보 오류나 크롤링된 매장 정보의 실시간 변경에 대해서는 보장하지 않습니다.")
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem("얼굴 인식 실패가 15초 이상 지속될 경우 보안을 위해 서비스 이용이 자동 종료될 수 있습니다.")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ArticleCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF202632)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ArticleBody(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = Color(0xFF5E6776)
    )
}

@Composable
private fun DefinitionItem(term: String, description: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF202632))) {
                append(term)
            }
            append("\n$description")
        },
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = Color(0xFF5E6776)
    )
}

@Composable
private fun BulletItem(text: String) {
    Row {
        Text(
            text = "\u2022",
            fontSize = 14.sp,
            color = Color(0xFF5E6776)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = Color(0xFF5E6776)
        )
    }
}
