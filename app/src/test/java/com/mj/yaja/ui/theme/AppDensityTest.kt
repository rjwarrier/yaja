package com.mj.yaja.ui.theme

import androidx.compose.ui.unit.Density
import com.mj.yaja.data.UiScalePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDensityTest {

    private val system = Density(density = 2.75f, fontScale = 1.0f)

    /** What an sp value ultimately resolves to in pixels. */
    private fun Density.spPixelScale(): Float = density * fontScale

    @Test
    fun `neutral scales leave the system density untouched`() {
        val result = resolveAppDensity(system, fontScale = 1f, uiScale = 1f)

        assertEquals(system.density, result.density, 0.0001f)
        assertEquals(system.fontScale, result.fontScale, 0.0001f)
    }

    @Test
    fun `ui scale grows every dp`() {
        val result = resolveAppDensity(system, fontScale = 1f, uiScale = 1.16f)

        assertEquals(system.density * 1.16f, result.density, 0.0001f)
    }

    @Test
    fun `ui scale leaves text where the font slider put it`() {
        val base = resolveAppDensity(system, fontScale = 1.16f, uiScale = 1f)

        for (step in UiScalePreference.entries) {
            val scaled = resolveAppDensity(system, fontScale = 1.16f, uiScale = step.scale)
            assertEquals(
                "text must not move when only UI size changes (${step.name})",
                base.spPixelScale(),
                scaled.spPixelScale(),
                0.0001f
            )
        }
    }

    @Test
    fun `font scale still drives text independently of ui scale`() {
        val small = resolveAppDensity(system, fontScale = 0.8f, uiScale = 1.16f)
        val large = resolveAppDensity(system, fontScale = 1.28f, uiScale = 1.16f)

        assertTrue(large.spPixelScale() > small.spPixelScale())
        assertEquals(
            "layout must not move when only font size changes",
            small.density,
            large.density,
            0.0001f
        )
    }

    @Test
    fun `a zero ui scale cannot collapse the density`() {
        val result = resolveAppDensity(system, fontScale = 1f, uiScale = 0f)

        assertEquals(system.density, result.density, 0.0001f)
        assertTrue(result.density > 0f)
        assertTrue(result.fontScale.isFinite())
    }

    @Test
    fun `negative and non-finite scales fall back to neutral`() {
        for (bad in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val result = resolveAppDensity(system, fontScale = bad, uiScale = bad)

            assertTrue("density stays usable for $bad", result.density > 0f)
            assertTrue("fontScale stays usable for $bad", result.fontScale > 0f)
            assertTrue(result.density.isFinite() && result.fontScale.isFinite())
        }
    }

    @Test
    fun `ui scale is clamped to sane bounds`() {
        val huge = resolveAppDensity(system, fontScale = 1f, uiScale = 50f)
        val tiny = resolveAppDensity(system, fontScale = 1f, uiScale = 0.01f)

        assertEquals(system.density * MAX_UI_SCALE, huge.density, 0.0001f)
        assertEquals(system.density * MIN_UI_SCALE, tiny.density, 0.0001f)
    }

    @Test
    fun `every shipped step stays inside the clamp`() {
        for (step in UiScalePreference.entries) {
            assertTrue(
                "${step.name} (${step.scale}) must not be clamped",
                step.scale in MIN_UI_SCALE..MAX_UI_SCALE
            )
        }
    }

    @Test
    fun `normal is exactly neutral so existing installs do not shift`() {
        assertEquals(1.0f, UiScalePreference.NORMAL.scale, 0.0001f)
    }

    @Test
    fun `journal text follows the data font scale, not the ui size`() {
        // The UI-size compensation must cancel out of the data-font path too,
        // otherwise entry text would drift every time UI size moved.
        val reference =
            resolveDataFontDensity(
                current = resolveAppDensity(system, fontScale = 1.16f, uiScale = 1f),
                uiFontScale = 1.16f,
                dataFontScale = 0.8f
            )

        for (step in UiScalePreference.entries) {
            val scaled =
                resolveDataFontDensity(
                    current = resolveAppDensity(system, fontScale = 1.16f, uiScale = step.scale),
                    uiFontScale = 1.16f,
                    dataFontScale = 0.8f
                )
            assertEquals(
                "entry text must not move with UI size (${step.name})",
                reference.spPixelScale(),
                scaled.spPixelScale(),
                0.0001f
            )
        }
    }

    @Test
    fun `data font density keeps the ui size scaling`() {
        val appDensity = resolveAppDensity(system, fontScale = 1f, uiScale = 1.16f)
        val dataDensity = resolveDataFontDensity(appDensity, uiFontScale = 1f, dataFontScale = 1.28f)

        assertEquals(
            "layout density must pass through untouched",
            appDensity.density,
            dataDensity.density,
            0.0001f
        )
    }

    @Test
    fun `a zero ui font scale cannot blow up journal text`() {
        val result = resolveDataFontDensity(system, uiFontScale = 0f, dataFontScale = 1f)

        assertTrue(result.fontScale.isFinite())
        assertTrue(result.fontScale > 0f)
    }
}
