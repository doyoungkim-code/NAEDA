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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.response.AddressResponse
import com.example.naedafront.data.remote.response.NaverGeocodeAddress

private val StorePrimary = Color(0xFF00695C)
private val StoreMint = Color(0xFF20D5BE)
private val ScreenBg = Color(0xFFF7F8FA)
private val DividerColor = Color(0xFFE3E8EF)
private val LabelColor = Color(0xFFB2BCCB)
private val ValueColor = Color(0xFF6B7280)
private val TitleColor = Color(0xFF111827)
private val ErrorColor = Color(0xFFD92D20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryAddressScreen(
    onBackClick: () -> Unit = {},
    onSearchPostCodeClick: () -> Unit = {},
    onRequestClick: () -> Unit = {},
    onAddressSelected: (AddressResponse) -> Unit = {},
    viewModel: DeliveryAddressViewModel = viewModel(
        factory = DeliveryAddressViewModel.factory()
    ),
    addressSearchViewModel: AddressSearchViewModel = viewModel(
        factory = AddressSearchViewModel.factory()
    )
) {
    val context = LocalContext.current
    val userNo = AuthPrefs.getUserNo(context)
    val uiState by viewModel.uiState.collectAsState()
    val searchUiState by addressSearchViewModel.uiState.collectAsState()

    var recipientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var postCode by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var detailAddress by remember { mutableStateOf("") }
    var deliveryRequest by remember { mutableStateOf("") }
    var saveAsDefault by remember { mutableStateOf(true) }
    var showAddressSearchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userNo) {
        if (userNo != null) {
            viewModel.loadAddresses(userNo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        DeliveryAddressTopBar(
            onBackClick = onBackClick
        )

        when {
            userNo == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "로그인 사용자 정보를 찾을 수 없습니다.",
                        color = ErrorColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StorePrimary)
                }
            }

            uiState.addresses.isNotEmpty() -> {
                AddressListContent(
                    addresses = uiState.addresses,
                    errorMessage = uiState.errorMessage,
                    onAddressClick = { addressId: Long ->
                        viewModel.getAddressDetail(
                            userNo = userNo,
                            addressId = addressId,
                            onSuccess = onAddressSelected
                        )
                    },
                    onDeleteClick = { addressId: Long ->
                        viewModel.deleteAddress(userNo, addressId)
                    }
                )
            }

            else -> {
                DeliveryAddressFormContent(
                    recipientName = recipientName,
                    onRecipientNameChange = { recipientName = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    postCode = postCode,
                    onPostCodeChange = { postCode = it },
                    address = address,
                    onAddressChange = { address = it },
                    detailAddress = detailAddress,
                    onDetailAddressChange = { detailAddress = it },
                    deliveryRequest = deliveryRequest,
                    onRequestClick = onRequestClick,
                    saveAsDefault = saveAsDefault,
                    onSaveAsDefaultChange = { saveAsDefault = it },
                    errorMessage = uiState.errorMessage,
                    isSubmitting = uiState.isSubmitting,
                    onSearchPostCodeClick = {
                        onSearchPostCodeClick()
                        showAddressSearchSheet = true
                    },
                    onCompleteClick = {
                        viewModel.createAddress(
                            userNo = userNo,
                            recipientName = recipientName,
                            phone = phone,
                            postCode = postCode,
                            address = address,
                            detailAddress = detailAddress,
                            saveAsDefault = saveAsDefault,
                            onSuccess = onAddressSelected
                        )
                    }
                )
            }
        }
    }

    if (showAddressSearchSheet) {
        AddressSearchBottomSheet(
            uiState = searchUiState,
            onDismiss = {
                showAddressSearchSheet = false
                addressSearchViewModel.clear()
            },
            onQueryChange = { query ->
                addressSearchViewModel.updateQuery(query)
            },
            onSearch = {
                addressSearchViewModel.search()
            },
            onSelect = { selected ->
                postCode = selected.postalCode()
                address = selected.roadAddress.ifBlank { selected.jibunAddress }
                showAddressSearchSheet = false
                addressSearchViewModel.clear()
            }
        )
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
private fun AddressListContent(
    addresses: List<AddressResponse>,
    errorMessage: String?,
    onAddressClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "등록된 배송지",
            color = TitleColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        errorMessage?.let { message ->
            Text(
                text = message,
                color = ErrorColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        addresses.forEach { item ->
            AddressListItem(
                item = item,
                onClick = { onAddressClick(item.addressId) },
                onDeleteClick = { onDeleteClick(item.addressId) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun AddressListItem(
    item: AddressResponse,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (item.isDefault) StoreMint else DividerColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (item.addressName.isBlank()) "배송지" else item.addressName,
                    color = TitleColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                if (item.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = StoreMint.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "기본",
                            color = StorePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDeleteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "delete",
                        tint = ValueColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.recipient,
                color = TitleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.phone,
                color = ValueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildString {
                    append("(${item.zipCode}) ")
                    append(item.roadAddress)
                    if (item.detailAddress.isNotBlank()) {
                        append(" ")
                        append(item.detailAddress)
                    }
                },
                color = ValueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun DeliveryAddressFormContent(
    recipientName: String,
    onRecipientNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    postCode: String,
    onPostCodeChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    detailAddress: String,
    onDetailAddressChange: (String) -> Unit,
    deliveryRequest: String,
    onRequestClick: () -> Unit,
    saveAsDefault: Boolean,
    onSaveAsDefaultChange: (Boolean) -> Unit,
    errorMessage: String?,
    isSubmitting: Boolean,
    onSearchPostCodeClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "등록된 주소가 없습니다.",
            color = TitleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "배송에 사용할 주소를 입력해주세요.",
            color = ValueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        InputSection(
            label = "수령인",
            value = recipientName,
            placeholder = "이름을 입력해주세요",
            onValueChange = onRecipientNameChange
        )

        Spacer(modifier = Modifier.height(28.dp))

        InputSection(
            label = "연락처",
            value = phone,
            placeholder = "010-0000-0000",
            keyboardType = KeyboardType.Phone,
            onValueChange = onPhoneChange
        )

        Spacer(modifier = Modifier.height(28.dp))

        PostCodeSection(
            postCode = postCode,
            onPostCodeChange = onPostCodeChange,
            onSearchClick = onSearchPostCodeClick
        )

        Spacer(modifier = Modifier.height(28.dp))

        InputSection(
            label = "주소",
            value = address,
            placeholder = "주소 검색으로 선택해주세요",
            readOnly = true,
            onValueChange = onAddressChange
        )

        Spacer(modifier = Modifier.height(28.dp))

        InputSection(
            label = "상세 주소",
            value = detailAddress,
            placeholder = "상세 주소를 입력해주세요 (동, 호수 등)",
            onValueChange = onDetailAddressChange
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
                onCheckedChange = onSaveAsDefaultChange,
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

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let { message ->
            Text(
                text = message,
                color = ErrorColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Button(
            onClick = onCompleteClick,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StorePrimary,
                contentColor = Color.White
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "완료",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun InputSection(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
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
            readOnly = readOnly,
            textStyle = TextStyle(
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
                readOnly = true,
                placeholder = {
                    Text(
                        text = "우편번호",
                        color = ValueColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSearchBottomSheet(
    uiState: AddressSearchUiState,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelect: (NaverGeocodeAddress) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "주소 검색",
                color = TitleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "도로명 주소를 입력해주세요",
                        color = ValueColor
                    )
                },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onSearch,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "search",
                            tint = StorePrimary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ScreenBg,
                    unfocusedContainerColor = ScreenBg,
                    disabledContainerColor = ScreenBg,
                    errorContainerColor = ScreenBg,
                    focusedIndicatorColor = DividerColor,
                    unfocusedIndicatorColor = DividerColor,
                    cursorColor = StorePrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StorePrimary)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = ErrorColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                uiState.results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "주소를 검색해주세요.",
                            color = ValueColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.results) { item: NaverGeocodeAddress ->
                            AddressSearchResultItem(
                                item = item,
                                onClick = { onSelect(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressSearchResultItem(
    item: NaverGeocodeAddress,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DividerColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        if (item.postalCode().isNotBlank()) {
            Text(
                text = item.postalCode(),
                color = StorePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
            text = item.roadAddress.ifBlank { item.jibunAddress },
            color = TitleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (item.jibunAddress.isNotBlank() && item.jibunAddress != item.roadAddress) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.jibunAddress,
                color = ValueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}