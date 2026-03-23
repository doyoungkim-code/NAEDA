package com.example.naedafront.data.remote.response

data class NaverGeocodeResponse(
    val status: String = "",
    val meta: NaverGeocodeMeta = NaverGeocodeMeta(),
    val addresses: List<NaverGeocodeAddress> = emptyList(),
    val errorMessage: String = ""
)

data class NaverGeocodeMeta(
    val totalCount: Int = 0,
    val page: Int = 1,
    val count: Int = 0
)

data class NaverGeocodeAddress(
    val roadAddress: String = "",
    val jibunAddress: String = "",
    val englishAddress: String = "",
    val x: String = "",
    val y: String = "",
    val distance: Double = 0.0,
    val addressElements: List<NaverAddressElement> = emptyList()
) {
    fun postalCode(): String {
        return addressElements
            .firstOrNull { element ->
                element.types.any { it == "POSTAL_CODE" }
            }
            ?.longName
            .orEmpty()
    }
}

data class NaverAddressElement(
    val types: List<String> = emptyList(),
    val longName: String = "",
    val shortName: String = "",
    val code: String = ""
)