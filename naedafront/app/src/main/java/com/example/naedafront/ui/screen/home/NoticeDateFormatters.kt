package com.example.naedafront.ui.screen.home

private val noticeDateRegex = Regex("""(\d{4})[-./](\d{1,2})[-./](\d{1,2})""")

internal fun String?.toNoticeMonthDay(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return ""

    noticeDateRegex.find(raw)?.destructured?.let { (_, month, day) ->
        return "${month.padStart(2, '0')}.${day.padStart(2, '0')}"
    }

    val digits = raw.filter(Char::isDigit)
    return if (digits.length >= 8) {
        "${digits.substring(4, 6)}.${digits.substring(6, 8)}"
    } else {
        raw
    }
}

internal fun formatNoticePeriod(startRaw: String?, endRaw: String?): String {
    val start = startRaw.toNoticeMonthDay()
    val end = endRaw.toNoticeMonthDay()

    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start ~ $end"
        start.isNotBlank() -> start
        else -> end
    }
}
