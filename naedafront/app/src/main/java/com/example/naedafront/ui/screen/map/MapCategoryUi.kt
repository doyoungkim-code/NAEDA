package com.example.naedafront.ui.screen.map

internal fun toMapCategoryLabel(categoryName: String?): String? {
    return categoryName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace("까페", "카페")
}

internal fun toUsableMapImageUrl(imageUrl: String?): String? {
    val normalized = imageUrl
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("null", ignoreCase = true) }
        ?: return null

    if (normalized.startsWith("/images/store/default-")) {
        return null
    }

    return normalized
}
