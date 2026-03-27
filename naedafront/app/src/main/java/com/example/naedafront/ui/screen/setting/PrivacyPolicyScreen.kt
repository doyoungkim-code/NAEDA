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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "개인정보 처리방침",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "생체 정보(얼굴 벡터)와 주민등록번호 처리에 대한 법적 고지가 핵심입니다.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 1. 수집하는 개인정보 항목 및 목적
                SectionCard(title = "1. 수집하는 개인정보 항목 및 목적") {
                    Text(
                        text = "회사는 다음의 목적을 위해 최소한의 개인정보를 수집합니다.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 테이블
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column {
                            TableRow(
                                col1 = "구분",
                                col2 = "수집 항목",
                                col3 = "수집 목적",
                                isHeader = true
                            )
                            TableDivider()
                            TableRow(
                                col1 = "회원가입",
                                col2 = "이름, 주민번호(앞6+뒤1), 이메일, 휴대폰, 비밀번호, PIN",
                                col3 = "본인 확인, 서비스 가입 의사 확인"
                            )
                            TableDivider()
                            TableRow(
                                col1 = "페이스페이",
                                col2 = "얼굴 이미지(7장) 기반 벡터값, 주민등록증 사진(뒷자리 마스킹)",
                                col3 = "생체 인증 결제, 본인 인증 및 위변조 방지"
                            )
                            TableDivider()
                            TableRow(
                                col1 = "계좌 연동",
                                col2 = "계좌번호, 은행명",
                                col3 = "계좌 개설 및 입출금 서비스 제공"
                            )
                            TableDivider()
                            TableRow(
                                col1 = "포인트 쇼핑",
                                col2 = "수령인 이름, 배송지 주소, 연락처",
                                col3 = "상품 배송 및 서비스 이행"
                            )
                            TableDivider()
                            TableRow(
                                col1 = "기기 정보",
                                col2 = "자이로 센서 데이터, 단말기 식별 번호",
                                col3 = "얼굴 등록 시 기기 수직 상태 확인, 보안"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 생체정보의 보호 및 파기
                SectionCard(title = "2. 생체정보의 보호 및 파기") {
                    BulletItem(
                        title = "암호화",
                        body = "수집된 얼굴 이미지는 즉시 벡터 데이터로 변환되어 암호화 저장되며, 원본 사진은 인증 완료 후 즉시 파기하거나 별도의 분리된 저장소에 안전하게 보관합니다."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BulletItem(
                        title = "처리 제한",
                        body = "생체 정보는 본인 인증 및 결제 목적으로만 사용되며 외부에 유출되지 않도록 기술적/관리적 보호 조치를 수행합니다."
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 개인정보의 제3자 제공
                SectionCard(title = "3. 개인정보의 제3자 제공") {
                    Text(
                        text = "서비스 제공을 위해 아래와 같이 정보를 제공합니다.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BulletItem(
                        title = "금융기관",
                        body = "계좌 개설 및 이체를 위한 정보 전달"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem(
                        title = "네이버(Naver Map API)",
                        body = "지도 서비스 이용 시 위치 정보 활용"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem(
                        title = "결제 가맹점",
                        body = "페이스페이 결제 시 결제 승인 결과값 전달"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 개인정보의 보유 및 이용 기간
                SectionCard(title = "4. 개인정보의 보유 및 이용 기간") {
                    BulletItem(
                        title = null,
                        body = "회원 탈퇴 시까지 보유를 원칙으로 합니다."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletItem(
                        title = null,
                        body = "단, 관련 법령(전자상거래법 등)에 따라 결제 기록은 5년, 소비 분석 데이터는 이용 목적 달성 시까지 보유합니다."
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BulletItem(title: String?, body: String) {
    Row {
        Text(
            text = "\u2022",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (title != null) {
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TableRow(
    col1: String,
    col2: String,
    col3: String,
    isHeader: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHeader) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = col1,
            fontSize = 12.sp,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = col2,
            fontSize = 12.sp,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = col3,
            fontSize = 12.sp,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.weight(0.8f)
        )
    }
}

@Composable
private fun TableDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
