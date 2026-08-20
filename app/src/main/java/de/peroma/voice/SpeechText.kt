package de.peroma.voice

/**
 * Turns the HTML that Pleroma returns into something a speech engine can
 * read without stumbling over markup, bare URLs or emoji shortcodes.
 */
object SpeechText {

    private val BLOCK_END = Regex("(?i)</(p|div|blockquote|li|h[1-6])>")
    private val LINE_BREAK = Regex("(?i)<br\\s*/?>")
    private val LIST_ITEM = Regex("(?i)<li[^>]*>")
    private val TAG = Regex("<[^>]+>")
    private val URL = Regex("(?i)\\bhttps?://\\S+")
    private val CUSTOM_EMOJI = Regex(":[a-zA-Z0-9_+-]{2,}:")
    private val MENTION = Regex("@([A-Za-z0-9_.-]+)(@[A-Za-z0-9_.-]+)?")
    private val HASHTAG = Regex("#([\\p{L}0-9_]+)")
    private val WHITESPACE = Regex("[ \\t\\x0B\\f\\r]+")
    private val BLANK_LINES = Regex("\\n{2,}")

    fun fromHtml(html: String?, strings: Strings): String {
        if (html.isNullOrBlank()) return ""

        var text = html
        text = LINE_BREAK.replace(text, "\n")
        text = LIST_ITEM.replace(text, "\n")
        text = BLOCK_END.replace(text, "\n")
        text = TAG.replace(text, "")
        text = unescapeEntities(text, strings)

        // Bare links are unlistenable; announce them instead of spelling them out.
        text = URL.replace(text) { match ->
            val host = match.value
                .substringAfter("://")
                .substringBefore('/')
                .removePrefix("www.")
            if (host.isNotBlank()) strings.linkTo(host) else strings.bareLink
        }

        text = CUSTOM_EMOJI.replace(text, "")
        text = MENTION.replace(text) { match -> match.groupValues[1] }
        text = HASHTAG.replace(text) { match -> strings.hashtag(match.groupValues[1]) }

        text = WHITESPACE.replace(text, " ")
        text = BLANK_LINES.replace(text, "\n")

        return text.lines().joinToString(" ") { it.trim() }.trim()
    }

    /** Display names often carry decorative emoji and shortcodes. */
    fun cleanupName(name: String, strings: Strings): String {
        var cleaned = unescapeEntities(name, strings)
        cleaned = CUSTOM_EMOJI.replace(cleaned, "")
        cleaned = WHITESPACE.replace(cleaned, " ")
        return cleaned.trim()
    }

    private fun unescapeEntities(input: String, strings: Strings): String {
        var text = input
        text = text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&hellip;", "…")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
        // Numeric entities such as &#8217;
        text = Regex("&#(\\d+);").replace(text) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else ""
        }
        // Ampersand last so it cannot re-create an entity.
        return text.replace("&amp;", strings.ampersand)
    }
}
