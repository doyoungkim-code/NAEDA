package com.example.naedafront.ui.screen.store

import com.example.naedafront.data.remote.response.AddressResponse
import com.example.naedafront.data.remote.response.OrderResponse

object StoreOrderDraftStore {
    var selectedItem: StoreItem? = null
    var deliveryRequest: String = ""
    var completedOrder: OrderCompleteUiModel? = null

    fun updateSelectedItem(item: StoreItem) {
        selectedItem = item
    }

    fun updateDeliveryRequest(request: String) {
        deliveryRequest = request
    }

    fun clearCompletedOrder() {
        completedOrder = null
    }

    fun buildCompletedOrder(
        order: OrderResponse,
        address: AddressResponse,
        recipientName: String,
        phone: String
    ): OrderCompleteUiModel {
        val model = OrderCompleteUiModel(
            productName = order.productName,
            imageUrl = selectedItem?.imageUrl.orEmpty(),
            thumbnailLabel = selectedItem?.thumbnailLabel.orEmpty(),
            orderNumber = order.orderId.toString(),
            recipientName = recipientName,
            phone = phone,
            zipCode = address.zipCode,
            address = address.roadAddress,
            detailAddress = address.detailAddress,
            deliveryRequest = deliveryRequest,
            totalPaymentAmount = order.pointPrice.toInt()
        )
        completedOrder = model
        return model
    }
}
