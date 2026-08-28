package com.mj.yaja.ui.app

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationIndicatorTest {

    /** Widths the panel actually produces: screen width divided by 2..6 destinations. */
    private fun slotWidths(screenWidth: Int) = (2..6).map { screenWidth.dp / it }

    @Test
    fun `an indicator never spills out of its slot`() {
        for (screenWidth in listOf(320, 345, 411, 448, 600)) {
            for (slot in slotWidths(screenWidth)) {
                for (showLabels in listOf(true, false)) {
                    val width = resolveIndicatorWidth(slot, showLabels)
                    assertTrue(
                        "indicator ${width} must fit slot $slot " +
                            "(screen ${screenWidth}dp, labels=$showLabels)",
                        width <= slot
                    )
                }
            }
        }
    }

    @Test
    fun `six destinations on a large display size no longer overlap`() {
        // ~345dp wide at the largest display size; the old floor forced 72dp into a 57dp slot.
        val slot = 345.dp / 6

        val width = resolveIndicatorWidth(slot, showLabels = true)

        assertTrue("must not exceed its slot", width <= slot)
        assertTrue("must leave a visible gap", width < slot)
    }

    @Test
    fun `roomy slots still get the comfortable minimum`() {
        val slot = 448.dp / 3

        assertEquals(slot - 18.dp, resolveIndicatorWidth(slot, showLabels = true))
        assertTrue(resolveIndicatorWidth(slot, showLabels = true) >= 72.dp)
    }

    @Test
    fun `a narrow slot falls back to the slot rather than the minimum`() {
        val slot = 40.dp

        val width = resolveIndicatorWidth(slot, showLabels = true)

        assertTrue("minimum must give way", width < 72.dp)
        assertEquals(slot - 4.dp, width)
    }

    @Test
    fun `a degenerate slot cannot produce a negative width`() {
        for (slot in listOf(0.dp, 2.dp, 4.dp)) {
            val width = resolveIndicatorWidth(slot, showLabels = true)
            assertTrue("width $width must not be negative for slot $slot", width >= 0.dp)
        }
    }

    @Test
    fun `label-less panels use their own smaller minimum`() {
        val roomy = 448.dp / 3

        assertTrue(resolveIndicatorWidth(roomy, showLabels = false) >= 56.dp)
        assertTrue(
            "label-less indicators sit tighter in their slot",
            resolveIndicatorWidth(roomy, showLabels = false) <
                resolveIndicatorWidth(roomy, showLabels = true)
        )
    }
}
