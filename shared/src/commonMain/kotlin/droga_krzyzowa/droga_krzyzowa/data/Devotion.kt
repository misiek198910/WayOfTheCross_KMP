package droga_krzyzowa.droga_krzyzowa.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import org.jetbrains.compose.resources.ExperimentalResourceApi
import wayofthecross_kmp.shared.generated.resources.Res

enum class Devotion(val key: String, val pageCount: Int, private val filePrefix: String) {
    WayOfTheCross("way_of_the_cross", 14, "station"),
    Lamentations("lamentations", 3, "lamentations");

    fun textPath(page: Int, language: AppLanguage): String = "files/${filePrefix}_${page + 1}_${language.code}.html"
}

private val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV")

fun romanNumeral(number: Int): String = romanNumerals.getOrElse(number - 1) { number.toString() }

@OptIn(ExperimentalResourceApi::class)
suspend fun loadDevotionText(devotion: Devotion, page: Int, language: AppLanguage, headingColor: Color): AnnotatedString =
    parseDevotionText(Res.readBytes(devotion.textPath(page, language)).decodeToString(), headingColor)

private val tagRegex = Regex("""<(/?)(b|i|big|br)\s*/?>""", RegexOption.IGNORE_CASE)
private val whitespaceRegex = Regex("""\s+""")

/**
 * Turns the small HTML subset of the texts (b, i, big, br) into styled text. Source line breaks are only
 * formatting, like in HTML: whitespace collapses and only br starts a new line.
 */
fun parseDevotionText(raw: String, headingColor: Color = Color.Unspecified): AnnotatedString {
    val result = buildAnnotatedString {
        var atLineStart = true
        var pendingSpace = false
        var openStyles = 0

        fun appendText(text: String) {
            for (word in text.split(whitespaceRegex)) {
                if (word.isEmpty()) {
                    pendingSpace = true
                    continue
                }
                if (pendingSpace && !atLineStart) append(' ')
                append(word)
                atLineStart = false
                pendingSpace = true
            }
            if (text.isNotEmpty() && !text.last().isWhitespace()) pendingSpace = false
        }

        var cursor = 0
        for (match in tagRegex.findAll(raw)) {
            appendText(raw.substring(cursor, match.range.first))
            val closing = match.groupValues[1] == "/"
            val tag = match.groupValues[2].lowercase()
            when {
                tag == "br" -> {
                    append('\n')
                    atLineStart = true
                    pendingSpace = false
                }
                closing -> if (openStyles > 0) {
                    pop()
                    openStyles--
                }
                else -> {
                    // The space before a styled word belongs to the plain text, e.g. not underlined with a heading.
                    if (pendingSpace && !atLineStart) {
                        append(' ')
                        pendingSpace = false
                    }
                    pushStyle(
                        when (tag) {
                            "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                            "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                            // Section headings of the Lamentations, styled like the subheadings in SlaveOfMary.
                            else -> SpanStyle(fontSize = 0.9.em, color = headingColor, letterSpacing = 0.08.em)
                        }
                    )
                    openStyles++
                }
            }
            cursor = match.range.last + 1
        }
        appendText(raw.substring(cursor))
        repeat(openStyles) { pop() }
    }
    val end = result.text.trimEnd().length
    return if (end == result.length) result else result.subSequence(0, end)
}
