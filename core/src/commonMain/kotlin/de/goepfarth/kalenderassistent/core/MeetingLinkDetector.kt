package de.goepfarth.kalenderassistent.core

enum class MeetingProvider(val displayName: String) {
    TEAMS("Teams"),
    ZOOM("Zoom"),
    GOOGLE_MEET("Google Meet"),
    WEBEX("Webex"),
}

data class MeetingLink(val provider: MeetingProvider, val url: String)

/**
 * Findet Links zu Online-Meetings in Ort oder Beschreibung eines Termins.
 * Microsoft-„Safe Links" werden entpackt, damit auch umgeschriebene Teams-Links erkannt werden.
 */
object MeetingLinkDetector {

    private val urlPattern = Regex("""https?://[^\s<>"'()\[\]]+""", RegexOption.IGNORE_CASE)

    private val safeLinkPattern = Regex(
        """^https://[a-z0-9.-]+\.safelinks\.protection\.outlook\.com/.*[?&]url=([^&]+)""",
        RegexOption.IGNORE_CASE,
    )

    private val providerPatterns = listOf(
        MeetingProvider.TEAMS to """^https://teams\.(microsoft|live)\.com/(l/meetup-join|l/meeting|meet)/""",
        MeetingProvider.ZOOM to """^https://([a-z0-9-]+\.)?zoom\.(us|com)/(j|my|w|s)/""",
        MeetingProvider.GOOGLE_MEET to """^https://meet\.google\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}""",
        MeetingProvider.WEBEX to """^https://[a-z0-9-]+\.webex\.com/([a-z0-9-]+/)?(meet|join|j\.php|wbxmjs)""",
    ).map { (provider, pattern) -> provider to Regex(pattern, RegexOption.IGNORE_CASE) }

    /** Durchsucht die Texte in der angegebenen Reihenfolge und liefert den ersten Meeting-Link. */
    fun find(vararg texts: String?): MeetingLink? {
        for (text in texts) {
            if (text.isNullOrEmpty()) continue
            for (match in urlPattern.findAll(text)) {
                val url = unwrapSafeLink(match.value.trimEnd('.', ',', ';', ':', '!', '?'))
                val provider = providerPatterns.firstOrNull { (_, pattern) -> pattern.containsMatchIn(url) }?.first
                if (provider != null) return MeetingLink(provider, url)
            }
        }
        return null
    }

    private fun unwrapSafeLink(url: String): String {
        val encoded = safeLinkPattern.find(url)?.groupValues?.get(1) ?: return url
        return percentDecode(encoded)
    }
}

/** Dekodiert %XX-Sequenzen als UTF-8. Ungültige Sequenzen bleiben unverändert. */
internal fun percentDecode(value: String): String {
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '%' && i + 2 < value.length) {
            val high = hexDigit(value[i + 1])
            val low = hexDigit(value[i + 2])
            if (high >= 0 && low >= 0) {
                bytes.add(((high shl 4) or low).toByte())
                i += 3
                continue
            }
        }
        val charCount = if (c.isHighSurrogate() && i + 1 < value.length && value[i + 1].isLowSurrogate()) 2 else 1
        value.substring(i, i + charCount).encodeToByteArray().forEach(bytes::add)
        i += charCount
    }
    return bytes.toByteArray().decodeToString()
}

private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> -1
}
