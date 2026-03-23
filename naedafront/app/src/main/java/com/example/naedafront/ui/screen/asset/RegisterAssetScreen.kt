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
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.Outline
import com.example.naedafront.ui.theme.OutlineVariant
import com.example.naedafront.ui.theme.Surface
import com.example.naedafront.ui.theme.SurfaceVariant
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// 메인 Dialog
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
            color = Surface,
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
                        color = OnBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = OnSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = OutlineVariant, thickness = 0.5.dp)

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
            color = OnSurfaceVariant,
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
            CircularProgressIndicator(color = Mint900)
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
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { loadTick++ },
                colors = ButtonDefaults.buttonColors(containerColor = Mint900)
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
            color = OnBackground,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 카드 상품 선택
        FieldLabel(text = "카드 상품")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownSelector(
            value = selectedProduct?.let { "${it.cardIssuerName} ${it.cardName}" } ?: "",
            placeholder = "카드를 선택해주세요",
            onClick = { showProductPicker = true }
        )

        if (selectedProduct != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "유형: ${selectedProduct!!.cardTypeName ?: if (selectedProduct!!.cardTypeCode == "1") "신용카드" else "체크카드"}",
                style = NaedaTypography.labelSmall,
                color = Mint900
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 출금 계좌 선택
        FieldLabel(text = "출금 계좌")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownSelector(
            value = selectedAccount?.let { "${it.bankName} ${it.accountNo}" } ?: "",
            placeholder = "출금 계좌를 선택해주세요",
            onClick = { showAccountPicker = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 결제일 선택
        FieldLabel(text = "결제일")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownSelector(
            value = if (selectedWithdrawalDate.isNotEmpty()) "매월 ${selectedWithdrawalDate.trimStart('0')}일" else "",
            placeholder = "결제일을 선택해주세요",
            onClick = { showDatePicker = true }
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
                containerColor = Mint900,
                disabledContainerColor = Outline,
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

    // 카드 상품 선택 다이얼로그
    if (showProductPicker) {
        ListPickerDialog(
            title = "카드 상품 선택",
            items = cardProducts.map { "${it.cardIssuerName ?: ""} ${it.cardName ?: ""}" },
            onSelect = { index ->
                selectedProduct = cardProducts[index]
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false }
        )
    }

    // 출금 계좌 선택 다이얼로그
    if (showAccountPicker) {
        ListPickerDialog(
            title = "출금 계좌 선택",
            items = accounts.map { "${it.bankName ?: ""} ${it.accountNo ?: ""}" },
            onSelect = { index ->
                selectedAccount = accounts[index]
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    // 결제일 선택 다이얼로그
    if (showDatePicker) {
        ListPickerDialog(
            title = "결제일 선택",
            items = listOf("01", "05", "10", "15", "20", "25").map { "매월 ${it.trimStart('0')}일" },
            onSelect = { index ->
                selectedWithdrawalDate = listOf("01", "05", "10", "15", "20", "25")[index]
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
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
        color = OnBackground
    )
}

@Composable
private fun DropdownSelector(
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    val isEmpty = value.isEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .border(1.dp, if (isEmpty) OutlineVariant else Mint900, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isEmpty) placeholder else value,
            style = NaedaTypography.bodyMedium,
            color = if (isEmpty) OnSurfaceVariant else OnBackground,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = OnSurfaceVariant
        )
    }
}

@Composable
private fun InfoBox(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Mint50)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "ⓘ",
            style = NaedaTypography.labelSmall,
            color = Mint900
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = NaedaTypography.labelSmall,
            color = Mint900,
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────
// 목록 선택 다이얼로그
// ─────────────────────────────────────────────

@Composable
private fun ListPickerDialog(
    title: String,
    items: List<String>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = title,
                    style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = item,
                                style = NaedaTypography.bodyMedium,
                                color = OnBackground
                            )
                        }
                        if (index < items.lastIndex) {
                            HorizontalDivider(
                                color = OutlineVariant,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "취소",
                        style = NaedaTypography.labelLarge,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}
