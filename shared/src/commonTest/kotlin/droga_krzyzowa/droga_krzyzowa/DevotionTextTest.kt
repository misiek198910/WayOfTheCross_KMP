package droga_krzyzowa.droga_krzyzowa

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import droga_krzyzowa.droga_krzyzowa.data.AppLanguage
import droga_krzyzowa.droga_krzyzowa.data.Devotion
import droga_krzyzowa.droga_krzyzowa.data.parseDevotionText
import droga_krzyzowa.droga_krzyzowa.data.romanNumeral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevotionTextTest {
    @Test
    fun collapsesSourceWhitespaceAndKeepsBreaks() {
        val text = parseDevotionText("\n    <b>K. Kłaniamy</b><br/>\n    Wyrok   wydany\n przez Piłata.<br/><br/>\n<i>Któryś</i>\n")
        assertEquals("K. Kłaniamy\nWyrok wydany przez Piłata.\n\nKtóryś", text.text)
    }

    @Test
    fun stylesBoldItalicAndHeadings() {
        val text = parseDevotionText("<big><b>HYMN</b></big><br/>a <b>b</b> <i>c</i>")
        assertEquals("HYMN\na b c", text.text)
        val bold = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }.map { text.text.substring(it.start, it.end) }
        assertEquals(listOf("HYMN", "b"), bold)
        val italic = text.spanStyles.single { it.item.fontStyle == FontStyle.Italic }
        assertEquals("c", text.text.substring(italic.start, italic.end))
    }

    @Test
    fun toleratesUnbalancedTags() {
        assertEquals("a b", parseDevotionText("</b>a <i>b").text)
    }

    @Test
    fun pathsAndNumerals() {
        assertEquals("files/station_14_pl.html", Devotion.WayOfTheCross.textPath(13, AppLanguage.Polish))
        assertEquals("files/lamentations_1_en.html", Devotion.Lamentations.textPath(0, AppLanguage.English))
        assertEquals("XIV", romanNumeral(14))
        assertTrue(Devotion.entries.all { it.pageCount > 0 })
    }
}
