package com.example.naedafront.ui.screen.asset

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.Outline
import com.example.naedafront.ui.theme.OutlineVariant
import com.example.naedafront.ui.theme.Surface
import com.example.naedafront.ui.theme.SurfaceVariant

// ─────────────────────────────────────────────
// 은행 / 카드사 목록
// ─────────────────────────────────────────────

private val bankList = listOf(
    "KB국민은행", "신한은행", "하나은행", "우리은행",
    "NH농협은행", "카카오뱅크", "토스뱅크", "IBK기업은행",
    "SC제일은행", "케이뱅크", "씨티은행", "부산은행"
)

private val cardList = listOf(
    "신한카드", "삼성카드", "현대카드", "KB국민카드",
    "롯데카드", "하나카드", "우리카드", "NH농협카드",
    "BC카드", "씨티카드"
)

// ─────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────

@Composable
fun RegisterAssetDialog(
    initialTab: Int = 0,   // 0: 계좌, 1: 카드
    onDismiss: () -> Unit = {},
    onRegisterComplete: () -> Unit = {}
) {
    val isAccount = initialTab == 0
    val title = if (isAccount) "계좌 추가" else "카드 추가"

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
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "닫기",
                            tint = OnSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = OutlineVariant, thickness = 0.5.dp)

                // 폼
                if (isAccount) {
                    AccountRegisterForm(onRegisterComplete = onRegisterComplete)
                } else {
                    CardRegisterForm(onRegisterComplete = onRegisterComplete)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 탭 바
// ─────────────────────────────────────────────

@Composable
private fun RegisterTabRow(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Surface(color = Surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = NaedaTypography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) Mint900 else OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) Mint900 else Color.Transparent)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 계좌 등록 폼
// ─────────────────────────────────────────────

@Composable
private fun AccountRegisterForm(onRegisterComplete: () -> Unit) {
    var selectedBank by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountAlias by remember { mutableStateOf("") }
    var showBankPicker by remember { mutableStateOf(false) }

    val isFormValid = selectedBank.isNotEmpty()
            && accountNumber.isNotEmpty()
            && accountAlias.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "등록할 정보를 입력해주세요",
            style = NaedaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "계좌 정보를 정확히 입력해 주세요.",
            style = NaedaTypography.bodyMedium,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 금융사 선택
        AssetFieldLabel(text = "금융사")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownSelector(
            value = selectedBank,
            placeholder = "선택해주세요",
            onClick = { showBankPicker = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 계좌 번호
        AssetFieldLabel(text = "계좌 번호")
        Spacer(modifier = Modifier.height(8.dp))
        AssetTextField(
            value = accountNumber,
            onValueChange = { raw ->
                // 숫자만 허용, 자동 하이픈 포맷: 123-456-7890
                val digits = raw.filter { it.isDigit() }.take(14)
                accountNumber = formatAccountNumber(digits)
            },
            placeholder = "123-456-7890",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 자산 별칭
        AssetFieldLabel(text = "자산 별칭")
        Spacer(modifier = Modifier.height(8.dp))
        AssetTextField(
            value = accountAlias,
            onValueChange = { if (it.length <= 20) accountAlias = it },
            placeholder = "예: 생활비 통장"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 안내 박스
        AssetInfoBox(text = "입력하신 정보는 자산 관리를 위해서만 사용되며, 안전하게 암호화되어 관리됩니다. 정보가 명확하지 않을 경우 서비스 이용이 제한될 수 있습니다.")

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // 등록 버튼
        RegisterButton(
            enabled = isFormValid,
            onClick = onRegisterComplete
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 은행 선택 다이얼로그
    if (showBankPicker) {
        InstitutionPickerDialog(
            title = "금융사 선택",
            items = bankList,
            onSelect = { bank ->
                selectedBank = bank
                showBankPicker = false
            },
            onDismiss = { showBankPicker = false }
        )
    }
}

// ─────────────────────────────────────────────
// 카드 등록 폼
// ─────────────────────────────────────────────

@Composable
private fun CardRegisterForm(onRegisterComplete: () -> Unit) {
    var selectedCard by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardProductName by remember { mutableStateOf("") }  // card_name
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvc by remember { mutableStateOf("") }
    var cardAlias by remember { mutableStateOf("") }
    var showCardPicker by remember { mutableStateOf(false) }

    val isFormValid = selectedCard.isNotEmpty()
            && cardNumber.length >= 19
            && cardProductName.isNotEmpty()
            && cardExpiry.length == 5
            && cardCvc.length == 3
            && cardAlias.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "등록할 정보를 입력해주세요",
            style = NaedaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "카드 정보를 정확히 입력해 주세요.",
            style = NaedaTypography.bodyMedium,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 카드사 선택
        AssetFieldLabel(text = "카드사")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownSelector(
            value = selectedCard,
            placeholder = "선택해주세요",
            onClick = { showCardPicker = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 카드 번호
        AssetFieldLabel(text = "카드 번호")
        Spacer(modifier = Modifier.height(8.dp))
        AssetTextField(
            value = cardNumber,
            onValueChange = { raw ->
                // 숫자만 허용, 자동 하이픈 포맷: 1234-5678-9012-3456
                val digits = raw.filter { it.isDigit() }.take(16)
                cardNumber = formatCardNumber(digits)
            },
            placeholder = "1234-5678-9012-3456",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 카드 상품명
        AssetFieldLabel(text = "카드 상품명")
        Spacer(modifier = Modifier.height(8.dp))
        AssetTextField(
            value = cardProductName,
            onValueChange = { if (it.length <= 50) cardProductName = it },
            placeholder = "예: 삼성 taptap O카드"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 만료일 + CVC (한 줄에 나란히)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AssetFieldLabel(text = "만료일")
                Spacer(modifier = Modifier.height(8.dp))
                AssetTextField(
                    value = cardExpiry,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        cardExpiry = formatExpiry(digits)
                    },
                    placeholder = "MM / YY",
                    keyboardType = KeyboardType.Number
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AssetFieldLabel(text = "CVC")
                Spacer(modifier = Modifier.height(8.dp))
                AssetTextField(
                    value = cardCvc,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) cardCvc = it },
                    placeholder = "000",
                    keyboardType = KeyboardType.Number
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 카드 별칭
        AssetFieldLabel(text = "카드 별칭")
        Spacer(modifier = Modifier.height(8.dp))
        AssetTextField(
            value = cardAlias,
            onValueChange = { if (it.length <= 20) cardAlias = it },
            placeholder = "예: 주거래 카드"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 안내 박스
        AssetInfoBox(text = "입력하신 정보는 자산 관리를 위해서만 사용되며, 안전하게 암호화되어 관리됩니다. 정보가 명확하지 않을 경우 서비스 이용이 제한될 수 있습니다.")

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // 등록 버튼
        RegisterButton(
            enabled = isFormValid,
            onClick = onRegisterComplete
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 카드사 선택 다이얼로그
    if (showCardPicker) {
        InstitutionPickerDialog(
            title = "카드사 선택",
            items = cardList,
            onSelect = { card ->
                selectedCard = card
                showCardPicker = false
            },
            onDismiss = { showCardPicker = false }
        )
    }
}

// ─────────────────────────────────────────────
// 공통 컴포넌트
// ─────────────────────────────────────────────

@Composable
private fun AssetFieldLabel(text: String) {
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
            color = if (isEmpty) OnSurfaceVariant else OnBackground
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = OnSurfaceVariant
        )
    }
}

@Composable
private fun AssetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val isEmpty = value.isEmpty()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = NaedaTypography.bodyMedium,
                color = OnSurfaceVariant
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = if (isEmpty) OnSurfaceVariant else Mint900,
                modifier = Modifier.size(18.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Mint900,
            unfocusedBorderColor = OutlineVariant,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            cursorColor = Mint900
        )
    )
}

@Composable
private fun AssetInfoBox(text: String) {
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

@Composable
private fun RegisterButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
        Text(
            text = "등록하기",
            style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// ─────────────────────────────────────────────
// 금융사 / 카드사 선택 다이얼로그
// ─────────────────────────────────────────────

@Composable
private fun InstitutionPickerDialog(
    title: String,
    items: List<String>,
    onSelect: (String) -> Unit,
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

                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = item,
                            style = NaedaTypography.bodyMedium,
                            color = OnBackground
                        )
                    }
                    HorizontalDivider(
                        color = OutlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
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

// ─────────────────────────────────────────────
// 포맷 유틸
// ─────────────────────────────────────────────

/** 만료일 포맷: MM/YY */
private fun formatExpiry(digits: String): String {
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2) append('/')
            append(c)
        }
    }
}

/** 계좌번호 포맷: 123-456-7890 */
private fun formatAccountNumber(digits: String): String {
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 3 || i == 6) append('-')
            append(c)
        }
    }
}

/** 카드번호 포맷: 1234-5678-9012-3456 */
private fun formatCardNumber(digits: String): String {
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 4 || i == 8 || i == 12) append('-')
            append(c)
        }
    }
}