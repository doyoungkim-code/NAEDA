package com.example.naedaterminal.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.NaedaFontFamily

private val Primary = Color(0xFF00635A)
private val BgColor = Color(0xFFFCFFFF)
private val BgCard = Color(0xFFECF8F7)
private val TextPrimary = Color(0xFF0D3B35)

data class LinkedAccount(
    val accountId: String,
    val bankName: String,
    val accountNumber: String,
    val balance: Long,
    val isPrimary: Boolean
)

data class MatchedUserInfo(
    val userId: String,
    val userName: String,
    val requiresPin: Boolean,       // 사용자가 PIN 2차인증 설정했는지
    val linkedAccounts: List<LinkedAccount>
)

@Composable
fun FaceMatchUserScreen(
    userInfo: MatchedUserInfo,
    amount: Long,
    merchant: String,
    onConfirm: (selectedAccountId: String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedAccount by remember {
        mutableStateOf(
            userInfo.linkedAccounts.firstOrNull { it.isPrimary }
                ?: userInfo.linkedAccounts.firstOrNull()
        )
    }
    var showAccountList by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // 인증 성공 배지
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "얼굴 인식 성공",
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = NaedaFontFamily
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = userInfo.userName,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "본인 확인이 완료되었습니다",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily
            )

            Spacer(Modifier.height(32.dp))

            if (selectedAccount == null) {
                Text(
                    text = "연결된 계좌 정보가 없습니다.",
                    color = TextPrimary.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    fontFamily = NaedaFontFamily
                )
            }

            // 선택된 계좌 카드
            val account = selectedAccount
            if (account != null) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = account.bankName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = NaedaFontFamily
                        )
                    }
                    if (account.isPrimary) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "대표",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = NaedaFontFamily
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = maskAccountNumber(account.accountNumber),
                    color = TextPrimary.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontFamily = NaedaFontFamily
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "%,d원".format(account.balance),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = NaedaFontFamily
                )

                // 다른 계좌 버튼 (2개 이상일 때만)
                if (userInfo.linkedAccounts.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Primary.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountList = !showAccountList },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "다른 계좌로 결제",
                            color = Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = NaedaFontFamily
                        )
                        Icon(
                            imageVector = if (showAccountList)
                                Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 다른 계좌 목록
            AnimatedVisibility(
                visible = showAccountList,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userInfo.linkedAccounts
                        .filter { it.accountId != selectedAccount?.accountId }
                        .forEach { account ->
                            AccountListItem(
                                account = account,
                                onClick = {
                                    selectedAccount = account
                                    showAccountList = false
                                }
                            )
                        }
                }
            }

            Spacer(Modifier.weight(1f))

            // 결제 금액 요약
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TextPrimary.copy(alpha = 0.04f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = merchant,
                    color = TextPrimary.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontFamily = NaedaFontFamily
                )
                Text(
                    text = "%,d원".format(amount),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onConfirm(selectedAccount?.accountId ?: "") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = if (userInfo.requiresPin) "결제하기  (PIN 인증 필요)" else "결제하기",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "취소",
                    color = TextPrimary.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountListItem(account: LinkedAccount, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = account.bankName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = NaedaFontFamily
                )
                Text(
                    text = maskAccountNumber(account.accountNumber),
                    color = TextPrimary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = NaedaFontFamily
                )
            }
        }
        Text(
            text = "%,d원".format(account.balance),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = NaedaFontFamily
        )
    }
}

private fun maskAccountNumber(accountNumber: String): String {
    val parts = accountNumber.split("-")
    return if (parts.size >= 3) "${parts[0]}-****-${parts.last()}"
    else accountNumber.take(4) + "****" + accountNumber.takeLast(4)
}