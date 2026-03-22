package com.example.naedafront.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.Background

private val StorePrimary = Color(0xFF00695C)
private val StoreMint = Color(0xFF20D5BE)
private val ScreenBg = Background
private val DividerColor = Color(0xFFE3E8EF)
private val LabelColor = Color(0xFFB2BCCB)
private val ValueColor = Color(0xFF6B7280)
private val TitleColor = Color(0xFF111827)

@Composable
fun DeliveryAddressScreen(
    onBackClick: () -> Unit = {},
    onSearchPostCodeClick: () -> Unit = {},
    onRequestClick: () -> Unit = {},
    onSaveAndPayClick: (
        recipientName: String,
        phone: String,
        postCode: String,
        address: String,
        detailAddress: String,
        deliveryRequest: String,
        saveAsDefault: Boolean
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var recipientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var postCode by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var detailAddress by remember { mutableStateOf("") }
    var deliveryRequest by remember { mutableStateOf("") }
    var saveAsDefault by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        DeliveryAddressTopBar(
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            InputSection(
                label = "수령인",
                value = recipientName,
                placeholder = "이름을 입력해주세요",
                onValueChange = { recipientName = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            InputSection(
                label = "연락처",
                value = phone,
                placeholder = "010-0000-0000",
                keyboardType = KeyboardType.Phone,
                onValueChange = { phone = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            PostCodeSection(
                postCode = postCode,
                onPostCodeChange = { postCode = it },
                onSearchClick = onSearchPostCodeClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            InputSection(
                label = "주소",
                value = address,
                placeholder = "기본 주소",
                onValueChange = { address = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            InputSection(
                label = "상세 주소",
                value = detailAddress,
                placeholder = "상세 주소를 입력해주세요 (동, 호수 등)",
                onValueChange = { detailAddress = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            DeliveryRequestSection(
                value = deliveryRequest,
                onClick = onRequestClick
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "기본 배송지로 저장",
                    color = ValueColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                Switch(
                    checked = saveAsDefault,
                    onCheckedChange = { saveAsDefault = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StoreMint,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD7DEE8),
                        uncheckedBorderColor = Color.Transparent,
                        checkedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Button(
                onClick = {
                    onSaveAndPayClick(
                        recipientName,
                        phone,
                        postCode,
                        address,
                        detailAddress,
                        deliveryRequest,
                        saveAsDefault
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StorePrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "결제",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DeliveryAddressTopBar(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "back",
                    tint = TitleColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "배송지 입력",
                color = TitleColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun InputSection(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = LabelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = ValueColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TitleColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ScreenBg,
                unfocusedContainerColor = ScreenBg,
                disabledContainerColor = ScreenBg,
                errorContainerColor = ScreenBg,
                focusedIndicatorColor = DividerColor,
                unfocusedIndicatorColor = DividerColor,
                disabledIndicatorColor = DividerColor,
                focusedTextColor = TitleColor,
                unfocusedTextColor = TitleColor,
                focusedPlaceholderColor = ValueColor,
                unfocusedPlaceholderColor = ValueColor,
                cursorColor = StorePrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

@Composable
private fun PostCodeSection(
    postCode: String,
    onPostCodeChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "우편번호",
            color = LabelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = postCode,
                onValueChange = onPostCodeChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "우편번호",
                        color = ValueColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TitleColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ScreenBg,
                    unfocusedContainerColor = ScreenBg,
                    disabledContainerColor = ScreenBg,
                    errorContainerColor = ScreenBg,
                    focusedIndicatorColor = DividerColor,
                    unfocusedIndicatorColor = DividerColor,
                    disabledIndicatorColor = DividerColor,
                    focusedTextColor = TitleColor,
                    unfocusedTextColor = TitleColor,
                    focusedPlaceholderColor = ValueColor,
                    unfocusedPlaceholderColor = ValueColor,
                    cursorColor = StorePrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .width(94.dp)
                    .height(40.dp)
                    .border(
                        width = 1.dp,
                        color = StoreMint,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "주소 검색",
                    color = StoreMint,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DeliveryRequestSection(
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "배송 요청사항",
            color = LabelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isBlank()) "배송 요청사항을 선택해주세요" else value,
                color = if (value.isBlank()) TitleColor else ValueColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "open",
                tint = Color(0xFF7A869A),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            thickness = 1.dp,
            color = DividerColor
        )
    }
}