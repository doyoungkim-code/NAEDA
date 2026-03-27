package com.example.naedafront.ui.screen.asset

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.CardProductResponse
import com.example.naedafront.data.remote.CardRegisterRequest
import com.example.naedafront.data.remote.AssetAccountResponse
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// 전체 화면 버전 (네비게이션 라우트용)
// ─────────────────────────────────────────────

@Composable
fun RegisterAssetScreen(
    initialTab: Int = 0,
    onDismiss: () -> Unit = {},
    onRegisterComplete: () -> Unit = {}
) {
    val title = if (initialTab == 0) "계좌 안내" else "카드 등록"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        if (initialTab == 0) {
            AccountInfoContent()
        } else {
            CardRegisterForm(onRegisterComplete = onRegisterComplete)
        }
    }
}

// ─────────────────────────────────────────────
// 메인 Dialog (인라인 팝업용으로 유지)
// ─────────────────────────────────────────────

@Composable
fun RegisterAssetDialog(
    initialTab: Int = 0,   // 0: 계좌, 1: 카드
    onDismiss: () -> Unit = {},
    onRegisterComplete: () -> Unit = {}
) {
    val title = if (initialTab == 0) "계좌 안내" else "카드 등록"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column {
                // 헤더
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                if (initialTab == 0) {
                    AccountInfoContent()
                } else {
                    CardRegisterForm(onRegisterComplete = onRegisterComplete)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 계좌 안내 (등록 API 없음)
// ─────────────────────────────────────────────

@Composable
private fun AccountInfoContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InfoBox(text = "계좌는 SSAFY 금융 API를 통해 자동으로 연동됩니다. 회원가입 시 등록된 계좌 정보가 자동으로 표시됩니다.")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "새 계좌를 추가하려면 SSAFY 금융 시스템에서\n계좌를 개설한 후 앱을 새로고침 해주세요.",
            style = NaedaTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

// ─────────────────────────────────────────────
// 카드 등록 폼 (백엔드 API 연동)
// ─────────────────────────────────────────────

@Composable
private fun CardRegisterForm(onRegisterComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userNo = AuthPrefs.getUserNo(context) ?: return

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var cardProducts by remember { mutableStateOf<List<CardProductResponse>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AssetAccountResponse>>(emptyList()) }
    var loadTick by remember { mutableIntStateOf(0) }

    var selectedProduct by remember { mutableStateOf<CardProductResponse?>(null) }
    var selectedAccount by remember { mutableStateOf<AssetAccountResponse?>(null) }
    var selectedWithdrawalDate by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    var showProductPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isFormValid = selectedProduct != null
            && selectedAccount != null
            && selectedWithdrawalDate.isNotEmpty()

    // 카드 상품 목록 + 계좌 목록 로드
    LaunchedEffect(loadTick) {
        isLoading = true
        loadError = null
        try {
            val productsResult = AssetRepository.getCardProducts(userNo)
            val accountsResult = AssetRepository.getWalletAssets(userNo)
            cardProducts = productsResult
            accounts = accountsResult.accounts
        } catch (e: Exception) {
            loadError = e.message ?: "데이터를 불러오지 못했습니다."
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (loadError != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = loadError!!,
                style = NaedaTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { loadTick++ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = Color.White)
            ) {
                Text("다시 시도")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "카드 상품을 선택하고\n결제 정보를 입력해주세요",
            style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 카드 상품 선택
        FieldLabel(text = "카드 상품")
        Spacer(modifier = Modifier.height(8.dp))
        InlineDropdown(
            value = selectedProduct?.let { "${it.cardIssuerName} ${it.cardName}" } ?: "",
            placeholder = "카드를 선택해주세요",
            expanded = showProductPicker,
            onExpandChange = { showProductPicker = it },
            items = cardProducts.map { "${it.cardIssuerName ?: ""} ${it.cardName ?: ""}" },
            onSelect = { index ->
                selectedProduct = cardProducts[index]
                showProductPicker = false
            }
        )

        if (selectedProduct != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "유형: ${selectedProduct!!.cardTypeName ?: if (selectedProduct!!.cardTypeCode == "1") "신용카드" else "체크카드"}",
                style = NaedaTypography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 출금 계좌 선택
        FieldLabel(text = "출금 계좌")
        Spacer(modifier = Modifier.height(8.dp))
        InlineDropdown(
            value = selectedAccount?.let { "${it.bankName} ${it.accountNo}" } ?: "",
            placeholder = "출금 계좌를 선택해주세요",
            expanded = showAccountPicker,
            onExpandChange = { showAccountPicker = it },
            items = accounts.map { "${it.bankName ?: ""} ${it.accountNo ?: ""}" },
            onSelect = { index ->
                selectedAccount = accounts[index]
                showAccountPicker = false
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 결제일 선택
        FieldLabel(text = "결제일")
        Spacer(modifier = Modifier.height(8.dp))
        InlineDropdown(
            value = if (selectedWithdrawalDate.isNotEmpty()) "매월 ${selectedWithdrawalDate.trimStart('0')}일" else "",
            placeholder = "결제일을 선택해주세요",
            expanded = showDatePicker,
            onExpandChange = { showDatePicker = it },
            items = (1..7).map { "매월 ${it}일" },
            onSelect = { index ->
                selectedWithdrawalDate = String.format("%02d", index + 1)
                showDatePicker = false
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        InfoBox(text = "카드 등록 시 선택한 출금 계좌에서 결제 금액이 자동으로 출금됩니다.")

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // 등록 버튼
        Button(
            onClick = {
                if (!isFormValid || isRegistering) return@Button
                isRegistering = true
                scope.launch {
                    try {
                        AssetRepository.registerCard(
                            userNo = userNo,
                            request = CardRegisterRequest(
                                cardUniqueNo = selectedProduct!!.cardUniqueNo!!,
                                withdrawalAccountNo = selectedAccount!!.accountNo!!,
                                withdrawalDate = selectedWithdrawalDate,
                                cardTypeCode = selectedProduct!!.cardTypeCode ?: "2"
                            )
                        )
                        Toast.makeText(context, "카드가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                        onRegisterComplete()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "카드 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    } finally {
                        isRegistering = false
                    }
                }
            },
            enabled = isFormValid && !isRegistering,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.outline,
                contentColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            if (isRegistering) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "등록하기",
                    style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

}

// ─────────────────────────────────────────────
// 공통 컴포넌트
// ─────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun InlineDropdown(
    value: String,
    placeholder: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    items: List<String>,
    onSelect: (Int) -> Unit
) {
    val isEmpty = value.isEmpty()
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, if (expanded) MaterialTheme.colorScheme.primary else if (isEmpty) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .clickable { onExpandChange(!expanded) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isEmpty) placeholder else value,
                style = NaedaTypography.bodyMedium,
                color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = NaedaTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = { onSelect(index) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBox(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "ⓘ",
            style = NaedaTypography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = NaedaTypography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 18.sp
        )
    }
}

