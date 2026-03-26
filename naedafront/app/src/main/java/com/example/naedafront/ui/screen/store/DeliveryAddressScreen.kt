@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.naedafront.ui.screen.store

import android.util.Log
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import com.example.naedafront.ui.theme.Background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.Background
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.response.AddressResponse
import com.example.naedafront.data.remote.response.NaverGeocodeAddress
import com.example.naedafront.data.repository.OrderRepository
import kotlinx.coroutines.launch

private val StorePrimary = Color(0xFF00695C)
private val StoreMint = Color(0xFF20D5BE)
private val ScreenBg = Background
private val DividerColor = Color(0xFFE3E8EF)
private val LabelColor = Color(0xFFB2BCCB)
private val ValueColor = Color(0xFF6B7280)
private val TitleColor = Color(0xFF111827)
private val ErrorColor = Color(0xFFD92D20)

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
    val coroutineScope = rememberCoroutineScope()
    val orderRepository = remember { OrderRepository() }

    var addressName by remember { mutableStateOf("\uC9D1") }
    var isCustomAddressName by remember { mutableStateOf(false) }
    var recipientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var postCode by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var detailAddress by remember { mutableStateOf("") }
    var deliveryRequest by remember { mutableStateOf("") }
    var isCustomRequest by remember { mutableStateOf(false) }
    var saveAsDefault by remember { mutableStateOf(true) }
    var showAddressSearchSheet by remember { mutableStateOf(false) }
    var showAddressCreatedDialog by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    var isOrdering by remember { mutableStateOf(false) }

    LaunchedEffect(userNo) {
        if (userNo != null) {
            if (recipientName.isBlank()) {
                recipientName = AuthPrefs.getUsername(context).orEmpty()
            }
            if (phone.isBlank()) {
                phone = formatPhoneNumber(AuthPrefs.getPhone(context).orEmpty())
                phoneFieldValue = TextFieldValue(
                    text = phone,
                    selection = TextRange(phone.length)
                )
            }
            viewModel.loadAddresses(userNo)
        } else {
            localErrorMessage = "로그인 사용자 정보를 찾을 수 없습니다."
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
                        text = localErrorMessage ?: "로그인 사용자 정보를 찾을 수 없습니다.",
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

            else -> {
                DeliveryAddressFormContent(
                    addresses = uiState.addresses,
                    addressName = addressName,
                    isCustomAddressName = isCustomAddressName,
                    onAddressNameChange = { addressName = it },
                    onCustomAddressNameChange = { isCustomAddressName = it },
                    recipientName = recipientName,
                    onRecipientNameChange = { recipientName = it },
                    phoneValue = phoneFieldValue,
                    onPhoneChange = {
                        val formattedValue = formatPhoneTextFieldValue(it)
                        phoneFieldValue = formattedValue
                        phone = formattedValue.text
                    },
                    postCode = postCode,
                    onPostCodeChange = { postCode = it },
                    address = address,
                    onAddressChange = { address = it },
                    detailAddress = detailAddress,
                    onDetailAddressChange = { detailAddress = it },
                    deliveryRequest = deliveryRequest,
                    isCustomRequest = isCustomRequest,
                    onDeliveryRequestChange = { deliveryRequest = it },
                    onCustomRequestChange = { isCustomRequest = it },
                    saveAsDefault = saveAsDefault,
                    onSaveAsDefaultChange = { saveAsDefault = it },
                    phoneErrorMessage = if (shouldShowPhoneFormatError(phone)) {
                        "올바른 연락처 형식이 아닙니다."
                    } else {
                        null
                    },
                    errorMessage = localErrorMessage ?: uiState.errorMessage,
                    isSubmitting = uiState.isSubmitting || isOrdering,
                    onSearchPostCodeClick = {
                        onSearchPostCodeClick()
                        showAddressSearchSheet = true
                    },
                    onSelectSavedAddress = { selected ->
                        addressName = selected.addressName
                        isCustomAddressName = selected.addressName.isNotBlank() &&
                            selected.addressName != "\uC9D1" &&
                            selected.addressName != "\uD68C\uC0AC"
                        recipientName = selected.recipient
                        phone = formatPhoneNumber(selected.phone)
                        phoneFieldValue = TextFieldValue(
                            text = phone,
                            selection = TextRange(phone.length)
                        )
                        postCode = selected.zipCode
                        address = selected.roadAddress
                        detailAddress = selected.detailAddress
                        saveAsDefault = selected.isDefault
                    },
                    onDeleteSavedAddress = { addressId ->
                        viewModel.deleteAddress(userNo, addressId)
                    },
                    onCompleteClick = {
                        val selectedItem = StoreOrderDraftStore.selectedItem
                        val isOrderFlow = selectedItem != null

                        if (false && selectedItem == null) {
                            localErrorMessage = "선택된 상품 정보가 없습니다. 상품 화면에서 다시 선택해주세요."
                            return@DeliveryAddressFormContent
                        }

                        if (userNo == null) {
                            localErrorMessage = "로그인 사용자 정보를 찾을 수 없습니다."
                            return@DeliveryAddressFormContent
                        }

                        localErrorMessage = null
                        val formattedPhone = formatPhoneNumber(phone)
                        val normalizedPhone = normalizePhoneNumber(formattedPhone)

                        if (!isValidPhoneNumber(formattedPhone)) {
                            localErrorMessage = "올바른 연락처 형식이 아닙니다."
                            return@DeliveryAddressFormContent
                        }

                        phone = formattedPhone
                        StoreOrderDraftStore.updateDeliveryRequest(deliveryRequest)

                        val existingAddress = uiState.addresses.firstOrNull { saved ->
                            saved.recipient.trim() == recipientName.trim() &&
                                    normalizePhoneNumber(saved.phone) == normalizedPhone &&
                                    saved.zipCode.trim() == postCode.trim() &&
                                    saved.roadAddress.trim() == address.trim() &&
                                    saved.detailAddress.trim() == detailAddress.trim()
                        }

                        if (existingAddress != null) {
                            if (!isOrderFlow || selectedItem == null) {
                                localErrorMessage = "이미 등록된 배송지입니다."
                                return@DeliveryAddressFormContent
                            }

                            val orderItem = selectedItem ?: return@DeliveryAddressFormContent
                            coroutineScope.launch {
                                isOrdering = true

                                Log.d(
                                    "ORDER_FLOW",
                                    "use existing address userNo=$userNo, productId=${orderItem.id}, addressId=${existingAddress.addressId}"
                                )

                                orderRepository.createOrder(
                                    userNo = userNo,
                                    productId = orderItem.id,
                                    addressId = existingAddress.addressId
                                ).onSuccess { orderResponse ->
                                        StoreOrderDraftStore.buildCompletedOrder(
                                            order = orderResponse,
                                            address = existingAddress,
                                            recipientName = recipientName,
                                            phone = formattedPhone
                                        )
                                    isOrdering = false
                                    onAddressSelected(existingAddress)
                                }.onFailure { throwable ->
                                    isOrdering = false
                                    localErrorMessage = throwable.message ?: "주문 생성에 실패했습니다."
                                    Log.e("ORDER_FLOW", "createOrder failed", throwable)
                                }
                            }
                            return@DeliveryAddressFormContent
                        }

                        viewModel.createAddress(
                            userNo = userNo,
                            addressName = addressName,
                            recipientName = recipientName,
                            phone = normalizedPhone,
                            postCode = postCode,
                            address = address,
                            detailAddress = detailAddress,
                            saveAsDefault = saveAsDefault,
                            onSuccess = { savedAddress ->
                                if (!isOrderFlow || selectedItem == null) {
                                    showAddressCreatedDialog = true
                                    return@createAddress
                                }

                                val orderItem = selectedItem ?: return@createAddress
                                coroutineScope.launch {
                                    isOrdering = true

                                    Log.d(
                                        "ORDER_FLOW",
                                        "createOrder start userNo=$userNo, productId=${orderItem.id}, addressId=${savedAddress.addressId}"
                                    )

                                    orderRepository.createOrder(
                                        userNo = userNo,
                                        productId = orderItem.id,
                                        addressId = savedAddress.addressId
                                    ).onSuccess { orderResponse ->
                                        StoreOrderDraftStore.buildCompletedOrder(
                                            order = orderResponse,
                                            address = savedAddress,
                                            recipientName = recipientName,
                                            phone = formattedPhone
                                        )
                                        isOrdering = false
                                        onAddressSelected(savedAddress)
                                    }.onFailure { throwable ->
                                        isOrdering = false
                                        localErrorMessage =
                                            throwable.message ?: "주문 생성에 실패했습니다."
                                        Log.e("ORDER_FLOW", "createOrder failed", throwable)
                                    }
                                }
                            }
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

    if (showAddressCreatedDialog) {
        AlertDialog(
            onDismissRequest = { showAddressCreatedDialog = false },
            confirmButton = {
                TextButton(onClick = { showAddressCreatedDialog = false }) {
                    Text("확인")
                }
            },
            title = {
                Text(
                    text = "배송지 등록 완료",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("배송지 정보가 등록되었습니다.")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryAddressTopBar(
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "배송지 관리",
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "back"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background
        ),
        windowInsets = WindowInsets(0)
    )
}

@Composable
private fun DeliveryAddressFormContent(
    addresses: List<AddressResponse>,
    addressName: String,
    isCustomAddressName: Boolean,
    onAddressNameChange: (String) -> Unit,
    onCustomAddressNameChange: (Boolean) -> Unit,
    recipientName: String,
    onRecipientNameChange: (String) -> Unit,
    phoneValue: TextFieldValue,
    onPhoneChange: (TextFieldValue) -> Unit,
    postCode: String,
    onPostCodeChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    detailAddress: String,
    onDetailAddressChange: (String) -> Unit,
    deliveryRequest: String,
    isCustomRequest: Boolean,
    onDeliveryRequestChange: (String) -> Unit,
    onCustomRequestChange: (Boolean) -> Unit,
    saveAsDefault: Boolean,
    onSaveAsDefaultChange: (Boolean) -> Unit,
    phoneErrorMessage: String?,
    errorMessage: String?,
    isSubmitting: Boolean,
    onSearchPostCodeClick: () -> Unit,
    onSelectSavedAddress: (AddressResponse) -> Unit,
    onDeleteSavedAddress: (Long) -> Unit,
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
            text = if (addresses.isEmpty()) "등록된 주소가 없습니다." else "배송지 정보를 입력하거나 기존 배송지를 선택하세요.",
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

        if (addresses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            SavedAddressDropdown(
                addresses = addresses,
                onSelect = onSelectSavedAddress,
                onDelete = onDeleteSavedAddress
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AddressNameSection(
            value = addressName,
            isCustomAddressName = isCustomAddressName,
            onValueChange = onAddressNameChange,
            onCustomAddressNameChange = onCustomAddressNameChange
        )

        if (false) {
        InputSection(
            label = "배송지명",
            value = addressName,
            placeholder = "예: 집, 회사",
            onValueChange = onAddressNameChange
        )
        }

        Spacer(modifier = Modifier.height(28.dp))

        InputSection(
            label = "수령인",
            value = recipientName,
            placeholder = "이름을 입력해주세요",
            onValueChange = onRecipientNameChange
        )

        Spacer(modifier = Modifier.height(28.dp))

        InputSection(
            label = "연락처",
            value = phoneValue,
            placeholder = "010-0000-0000",
            errorMessage = phoneErrorMessage,
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
            isCustomRequest = isCustomRequest,
            onValueChange = onDeliveryRequestChange,
            onCustomRequestChange = onCustomRequestChange
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
private fun AddressNameSection(
    value: String,
    isCustomAddressName: Boolean,
    onValueChange: (String) -> Unit,
    onCustomAddressNameChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNameText = when {
        isCustomAddressName -> "\uC9C1\uC811\uC785\uB825"
        value == "\uC9D1" || value == "\uD68C\uC0AC" -> value
        else -> ""
    }
    val presetNames = listOf(
        "\uC9D1",
        "\uD68C\uC0AC",
        "\uC9C1\uC811\uC785\uB825"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "\uBC30\uC1A1\uC9C0\uBA85",
            color = LabelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                OutlinedTextField(
                    value = selectedNameText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "\uBC30\uC1A1\uC9C0\uBA85\uC744 \uC120\uD0DD\uD574\uC8FC\uC138\uC694",
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
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "open",
                            tint = ValueColor
                        )
                    },
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
                        disabledTextColor = TitleColor,
                        disabledPlaceholderColor = ValueColor,
                        disabledTrailingIconColor = ValueColor,
                        cursorColor = StorePrimary
                    )
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presetNames.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                color = TitleColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            expanded = false
                            if (item == "\uC9C1\uC811\uC785\uB825") {
                                onCustomAddressNameChange(true)
                                if (value == "\uC9D1" || value == "\uD68C\uC0AC") {
                                    onValueChange("")
                                }
                            } else {
                                onCustomAddressNameChange(false)
                                onValueChange(item)
                            }
                        }
                    )
                }
            }
        }

        if (isCustomAddressName) {
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "\uBC30\uC1A1\uC9C0\uBA85\uC744 \uC9C1\uC811 \uC785\uB825\uD574\uC8FC\uC138\uC694",
                        color = ValueColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = TitleColor,
                    fontSize = 16.sp,
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
                )
            )
        }
    }
}

@Composable
private fun SavedAddressDropdown(
    addresses: List<AddressResponse>,
    onSelect: (AddressResponse) -> Unit,
    onDelete: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "등록된 배송지 선택",
            color = LabelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "배송지를 선택해주세요",
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
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "expand",
                            tint = ValueColor
                        )
                    },
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
                        disabledTextColor = TitleColor,
                        disabledPlaceholderColor = ValueColor,
                        disabledTrailingIconColor = ValueColor,
                        cursorColor = StorePrimary
                    )
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                addresses.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = buildString {
                                        if (item.isDefault) append("[기본] ")
                                        append(
                                            if (item.addressName.isBlank()) item.recipient
                                            else item.addressName
                                        )
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TitleColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${item.zipCode} ${item.roadAddress}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = ValueColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            selectedLabel = buildString {
                                if (item.isDefault) append("[기본] ")
                                append(
                                    if (item.addressName.isBlank()) item.recipient
                                    else item.addressName
                                )
                            }
                            expanded = false
                            onSelect(item)
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { onDelete(item.addressId) },
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
                    )
                }
            }
        }
    }
}

@Composable
private fun InputSection(
    label: String,
    value: TextFieldValue,
    placeholder: String,
    errorMessage: String? = null,
    onValueChange: (TextFieldValue) -> Unit
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
            isError = errorMessage != null,
            placeholder = {
                Text(
                    text = placeholder,
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
                errorIndicatorColor = ErrorColor,
                disabledIndicatorColor = DividerColor,
                focusedTextColor = TitleColor,
                unfocusedTextColor = TitleColor,
                focusedPlaceholderColor = ValueColor,
                unfocusedPlaceholderColor = ValueColor,
                cursorColor = StorePrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = ErrorColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
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
    readOnly: Boolean = false,
    errorMessage: String? = null,
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
            isError = errorMessage != null,
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
                errorIndicatorColor = ErrorColor,
                disabledIndicatorColor = DividerColor,
                focusedTextColor = TitleColor,
                unfocusedTextColor = TitleColor,
                focusedPlaceholderColor = ValueColor,
                unfocusedPlaceholderColor = ValueColor,
                cursorColor = StorePrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = ErrorColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
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
    isCustomRequest: Boolean,
    onValueChange: (String) -> Unit,
    onCustomRequestChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedRequestText = if (isCustomRequest) "직접 입력" else value

    val presetRequests = listOf(
        "문 앞에 놔주세요",
        "경비실에 맡겨주세요",
        "직접 입력"
    )

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

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                OutlinedTextField(
                    value = selectedRequestText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "배송 요청사항을 선택해주세요",
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
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "open",
                            tint = ValueColor
                        )
                    },
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
                        disabledTextColor = TitleColor,
                        disabledPlaceholderColor = ValueColor,
                        disabledTrailingIconColor = ValueColor,
                        cursorColor = StorePrimary
                    )
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presetRequests.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                color = TitleColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            expanded = false
                            if (item == "직접 입력") {
                                onCustomRequestChange(true)
                                onValueChange("")
                            } else {
                                onCustomRequestChange(false)
                                onValueChange(item)
                            }
                        }
                    )
                }
            }
        }

        if (isCustomRequest) {
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "배송 요청사항을 직접 입력해주세요",
                        color = ValueColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                singleLine = false,
                minLines = 3,
                textStyle = TextStyle(
                    color = TitleColor,
                    fontSize = 16.sp,
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
                )
            )
        }
    }
}

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
                            text = uiState.errorMessage ?: "주소 검색에 실패했습니다.",
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
                        items(uiState.results) { item ->
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

private fun normalizePhoneNumber(value: String): String {
    return value.filter { it.isDigit() }.take(11)
}

private fun formatPhoneNumber(value: String): String {
    val digits = normalizePhoneNumber(value)
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.take(3)}-${digits.drop(3)}"
        else -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.drop(7)}"
    }
}

private fun formatPhoneTextFieldValue(value: TextFieldValue): TextFieldValue {
    val digitsBeforeCursor = value.text
        .take(value.selection.start.coerceAtMost(value.text.length))
        .count { it.isDigit() }
        .coerceAtMost(11)
    val formatted = formatPhoneNumber(value.text)
    val cursor = phoneCursorFromDigitCount(formatted, digitsBeforeCursor)
    return TextFieldValue(
        text = formatted,
        selection = TextRange(cursor)
    )
}

private fun phoneCursorFromDigitCount(formatted: String, digitCount: Int): Int {
    if (digitCount <= 0) return 0

    var seenDigits = 0
    formatted.forEachIndexed { index, char ->
        if (char.isDigit()) {
            seenDigits++
            if (seenDigits == digitCount) {
                return index + 1
            }
        }
    }

    return formatted.length
}

private fun isValidPhoneNumber(value: String): Boolean {
    return normalizePhoneNumber(value).matches(Regex("^01[0-9]\\d{8}$"))
}

private fun shouldShowPhoneFormatError(value: String): Boolean {
    val digits = normalizePhoneNumber(value)
    return when {
        digits.isEmpty() -> false
        digits.length >= 2 && digits.take(2) != "01" -> true
        digits.length == 11 && !isValidPhoneNumber(value) -> true
        else -> false
    }
}
