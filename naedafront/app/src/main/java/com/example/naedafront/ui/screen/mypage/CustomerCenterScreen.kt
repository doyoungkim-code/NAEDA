package com.example.naedafront.ui.screen.mypage

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.NaedaFontFamily
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant

private data class FaqItem(
    val question: String,
    val answer: String
)

private val faqList = listOf(
    FaqItem(
        question = "페이스페이는 어떻게 등록하나요?",
        answer = "홈 화면 하단의 스캔 탭에서 '페이스페이 등록'을 누르면 얼굴 촬영 → 신분증 인증 → 결제수단 선택 → PIN 설정 순서로 등록할 수 있습니다."
    ),
    FaqItem(
        question = "PIN 번호를 잊어버렸어요.",
        answer = "마이페이지 > PIN 번호 변경에서 'PIN 번호를 잊으셨나요?'를 누르면 로그인 비밀번호를 다시 입력한 뒤 새로운 PIN을 설정할 수 있습니다."
    ),
    FaqItem(
        question = "결제 한도는 어떻게 변경하나요?",
        answer = "페이스페이 등록 과정에서 결제 한도를 설정할 수 있으며, 이후 설정 화면에서 변경할 수 있습니다."
    ),
    FaqItem(
        question = "계좌 연결은 어떻게 하나요?",
        answer = "자산 탭에서 '계좌 추가' 버튼을 누르면 은행을 선택하고 계좌 정보를 입력하여 연결할 수 있습니다."
    ),
    FaqItem(
        question = "얼굴 인식이 잘 안 돼요.",
        answer = "밝은 곳에서 정면을 바라보며 다시 시도해 주세요. 마스크, 모자, 선글라스 등을 착용하면 인식률이 떨어질 수 있습니다."
    ),
    FaqItem(
        question = "포인트는 어떻게 적립되나요?",
        answer = "페이스페이로 결제하면 결제 금액의 일정 비율이 자동으로 적립됩니다. 적립된 포인트는 스토어에서 상품 구매 시 사용할 수 있습니다."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCenterScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "고객센터",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Background),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── 전화 문의 카드 ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:010-5191-8793"))
                            context.startActivity(intent)
                        }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE8F7F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "전화",
                            tint = Mint500,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "전화 문의",
                            fontFamily = NaedaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = OnBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "010-5191-8793",
                            fontFamily = NaedaFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Mint500
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "평일 09:00 ~ 18:00 (점심 12:00 ~ 13:00)",
                            fontFamily = NaedaFontFamily,
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── FAQ 섹션 ──
            Text(
                text = "자주 묻는 질문",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OnBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    faqList.forEachIndexed { index, faq ->
                        FaqRow(faq = faq)
                        if (index != faqList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = Color(0xFFF1F3F5),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FaqRow(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Q.",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Mint500
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = faq.question,
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = OnBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "접기" else "펼치기",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Text(
                text = faq.answer,
                fontFamily = NaedaFontFamily,
                fontSize = 14.sp,
                color = OnSurfaceVariant,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 12.dp, start = 22.dp)
            )
        }
    }
}
