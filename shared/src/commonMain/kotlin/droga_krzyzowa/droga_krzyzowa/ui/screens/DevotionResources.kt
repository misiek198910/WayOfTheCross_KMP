package droga_krzyzowa.droga_krzyzowa.ui.screens

import droga_krzyzowa.droga_krzyzowa.data.Devotion
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.devotion_part_label
import wayofthecross_kmp.shared.generated.resources.devotion_station_label
import wayofthecross_kmp.shared.generated.resources.devotion_tap_to_read
import wayofthecross_kmp.shared.generated.resources.devotion_tap_to_read_part
import wayofthecross_kmp.shared.generated.resources.home_lamentations
import wayofthecross_kmp.shared.generated.resources.home_way_of_cross
import wayofthecross_kmp.shared.generated.resources.lamentations_1
import wayofthecross_kmp.shared.generated.resources.lamentations_2
import wayofthecross_kmp.shared.generated.resources.lamentations_3
import wayofthecross_kmp.shared.generated.resources.lamentations_titles
import wayofthecross_kmp.shared.generated.resources.station_1
import wayofthecross_kmp.shared.generated.resources.station_10
import wayofthecross_kmp.shared.generated.resources.station_10_land
import wayofthecross_kmp.shared.generated.resources.station_11
import wayofthecross_kmp.shared.generated.resources.station_11_land
import wayofthecross_kmp.shared.generated.resources.station_12
import wayofthecross_kmp.shared.generated.resources.station_12_land
import wayofthecross_kmp.shared.generated.resources.station_13
import wayofthecross_kmp.shared.generated.resources.station_13_land
import wayofthecross_kmp.shared.generated.resources.station_14
import wayofthecross_kmp.shared.generated.resources.station_14_land
import wayofthecross_kmp.shared.generated.resources.station_1_land
import wayofthecross_kmp.shared.generated.resources.station_2
import wayofthecross_kmp.shared.generated.resources.station_2_land
import wayofthecross_kmp.shared.generated.resources.station_3
import wayofthecross_kmp.shared.generated.resources.station_3_land
import wayofthecross_kmp.shared.generated.resources.station_4
import wayofthecross_kmp.shared.generated.resources.station_4_land
import wayofthecross_kmp.shared.generated.resources.station_5
import wayofthecross_kmp.shared.generated.resources.station_5_land
import wayofthecross_kmp.shared.generated.resources.station_6
import wayofthecross_kmp.shared.generated.resources.station_6_land
import wayofthecross_kmp.shared.generated.resources.station_7
import wayofthecross_kmp.shared.generated.resources.station_7_land
import wayofthecross_kmp.shared.generated.resources.station_8
import wayofthecross_kmp.shared.generated.resources.station_8_land
import wayofthecross_kmp.shared.generated.resources.station_9
import wayofthecross_kmp.shared.generated.resources.station_9_land
import wayofthecross_kmp.shared.generated.resources.station_titles

private val stationImages = listOf(
    Res.drawable.station_1, Res.drawable.station_2, Res.drawable.station_3, Res.drawable.station_4, Res.drawable.station_5, Res.drawable.station_6, Res.drawable.station_7, Res.drawable.station_8, Res.drawable.station_9, Res.drawable.station_10, Res.drawable.station_11, Res.drawable.station_12, Res.drawable.station_13, Res.drawable.station_14,
)

private val stationLandscapeImages = listOf(
    Res.drawable.station_1_land, Res.drawable.station_2_land, Res.drawable.station_3_land, Res.drawable.station_4_land, Res.drawable.station_5_land, Res.drawable.station_6_land, Res.drawable.station_7_land, Res.drawable.station_8_land, Res.drawable.station_9_land, Res.drawable.station_10_land, Res.drawable.station_11_land, Res.drawable.station_12_land, Res.drawable.station_13_land, Res.drawable.station_14_land,
)

private val lamentationsImages = listOf(Res.drawable.lamentations_1, Res.drawable.lamentations_2, Res.drawable.lamentations_3)

/** Lamentations have one square picture per part, so it serves both orientations. */
fun Devotion.image(page: Int, landscape: Boolean): DrawableResource = when (this) {
    Devotion.WayOfTheCross -> (if (landscape) stationLandscapeImages else stationImages)[page]
    Devotion.Lamentations -> lamentationsImages[page]
}

val Devotion.titleRes: StringResource
    get() = when (this) {
        Devotion.WayOfTheCross -> Res.string.home_way_of_cross
        Devotion.Lamentations -> Res.string.home_lamentations
    }

val Devotion.pageTitlesRes: StringArrayResource
    get() = when (this) {
        Devotion.WayOfTheCross -> Res.array.station_titles
        Devotion.Lamentations -> Res.array.lamentations_titles
    }

/** "Station %s" or "Part %s", filled with a Roman numeral. */
val Devotion.pageLabelRes: StringResource
    get() = when (this) {
        Devotion.WayOfTheCross -> Res.string.devotion_station_label
        Devotion.Lamentations -> Res.string.devotion_part_label
    }

val Devotion.tapToReadRes: StringResource
    get() = when (this) {
        Devotion.WayOfTheCross -> Res.string.devotion_tap_to_read
        Devotion.Lamentations -> Res.string.devotion_tap_to_read_part
    }
