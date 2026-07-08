package com.mj.yaja.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTimeTextParserTest {

    @Test
    fun stripMentionedEventTimeFromText_removesMatchingTimeFromTitleAndDescription() {
        val text = "11:30 : Asif Nikkah\n\nAt 11.30 Hrs Karalam"

        val cleaned = stripMentionedEventTimeFromText(text, "11.30 Hrs")

        assertEquals("Asif Nikkah\n\nAt Karalam", cleaned)
    }

    @Test
    fun stripMentionedEventTimeFromText_keepsOtherTimes() {
        val text = "10:00 : Advanced IIT\n\nPrep at 09:00"

        val cleaned = stripMentionedEventTimeFromText(text, "10.00 Hrs")

        assertEquals("Advanced IIT\n\nPrep at 09:00", cleaned)
    }
}
