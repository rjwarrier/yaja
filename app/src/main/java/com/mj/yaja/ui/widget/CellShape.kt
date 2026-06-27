package com.mj.yaja.ui.widget

/**
 * Material Design 3 shapes suitable for individual heatmap cells.
 * Each shape is drawn via [android.graphics.Canvas] / [android.graphics.Path].
 *
 * Only shapes that remain visually distinct at small cell sizes are included.
 */
enum class CellShape(val key: String, val label: String) {
    SQUARE("square", "Square"),
    ROUNDED("rounded", "Rounded"),
    CIRCLE("circle", "Circle"),
    CUT_CORNER("cut_corner", "Cut Corner"),
    DIAMOND("diamond", "Diamond"),
    HEART("heart", "Heart"),
    SUNNY("sunny", "Sunny"),
    COOKIE_4("cookie_4", "Cookie 4"),
    COOKIE_6("cookie_6", "Cookie 6");

    companion object {
        fun fromKey(key: String?): CellShape =
            entries.firstOrNull { it.key == key } ?: ROUNDED
    }
}
