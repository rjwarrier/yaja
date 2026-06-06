package com.mj.yaja.ui.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

object DateLinkUtils {

    data class DateLink(val range: IntRange, val date: LocalDate)

    private val MONTH_NAMES = mapOf(
        "january" to 1,   "jan" to 1,
        "february" to 2,  "feb" to 2,
        "march" to 3,     "mar" to 3,
        "april" to 4,     "apr" to 4,
        "may" to 5,
        "june" to 6,      "jun" to 6,
        "july" to 7,      "jul" to 7,
        "august" to 8,    "aug" to 8,
        "september" to 9, "sept" to 9, "sep" to 9,
        "october" to 10,  "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12
    )

    private val DAY_NAMES = mapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
        "mon" to DayOfWeek.MONDAY,
        "tues" to DayOfWeek.TUESDAY,
        "tue" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY,
        "thurs" to DayOfWeek.THURSDAY,
        "thur" to DayOfWeek.THURSDAY,
        "thu" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY,
        "sat" to DayOfWeek.SATURDAY,
        "sun" to DayOfWeek.SUNDAY
    )

    /**
     * Returns true if the device locale uses month-first ordering (e.g. M/d/yy for US),
     * false for day-first (e.g. dd/MM/yy for UK/EU). Requires no permissions.
     */
    fun isMonthFirst(): Boolean {
        val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT, null, IsoChronology.INSTANCE, Locale.getDefault()
        )
        val mIdx = pattern.indexOfFirst { it == 'M' }
        val dIdx = pattern.indexOfFirst { it == 'd' }
        return if (mIdx >= 0 && dIdx >= 0) mIdx < dIdx else false
    }

    // Alternation strings sorted longest-first so greedy matching works correctly
    private val monthAlt by lazy {
        MONTH_NAMES.keys.sortedByDescending { it.length }.joinToString("|")
    }
    private val dayAlt by lazy {
        DAY_NAMES.keys.sortedByDescending { it.length }.joinToString("|")
    }

    // ── Pre-compiled regexes ───────────────────────────────────────────────────

    // ── Numeric ───────────────────────────────────────────────────────────────

    /** ISO full date: 2024-03-28 or 2024/03/28 */
    private val reIsoFull by lazy {
        Regex("""\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b""")
    }

    /** Numeric full, 4-digit year: 28/03/2024, 28.03.2024, 28-03-2024 (locale-aware) */
    private val reNumericFull by lazy {
        Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})\b""")
    }

    /** Numeric full, 2-digit year: 28-03-24, 28/03/24, 24-3-24 (locale-aware) */
    private val reNumericFullShortYear by lazy {
        Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2})\b""")
    }

    /** Numeric short, slash only (no year): 28/03 or 03/28 (locale-aware) */
    private val reNumericShort by lazy {
        Regex("""\b(\d{1,2})/(\d{1,2})\b""")
    }

    // ── Written (plain numbers + month name) ─────────────────────────────────

    /** Written full, day-first: "28 March 2024" */
    private val reWrittenFullDF by lazy {
        Regex("""\b(\d{1,2})\s+($monthAlt),?\s+(\d{4})\b""", RegexOption.IGNORE_CASE)
    }

    /** Written full, month-first: "March 28, 2024" or "March 28 2024" */
    private val reWrittenFullMF by lazy {
        Regex("""\b($monthAlt)\s+(\d{1,2}),?\s+(\d{4})\b""", RegexOption.IGNORE_CASE)
    }

    /** Written short, day-first (no year): "28 March" */
    private val reWrittenShortDF by lazy {
        Regex("""\b(\d{1,2})\s+($monthAlt)\b""", RegexOption.IGNORE_CASE)
    }

    /** Written short, month-first (no year): "March 28" */
    private val reWrittenShortMF by lazy {
        Regex("""\b($monthAlt)\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    }

    // ── Ordinal (1st, 2nd, 3rd, 4th …) ──────────────────────────────────────

    /**
     * Ordinal day-first with optional year:
     * "28th March", "28th March 2024", "28th March 24", "1st January, 2025"
     */
    private val reOrdinalDF by lazy {
        Regex("""\b(\d{1,2})(?:st|nd|rd|th)\s+($monthAlt)(?:[,.]?\s+(\d{4}|\d{2}))?\b""",
            RegexOption.IGNORE_CASE)
    }

    /**
     * Ordinal month-first with optional year:
     * "March 28th", "March 28th, 2024", "March 28th 24"
     */
    private val reOrdinalMF by lazy {
        Regex("""\b($monthAlt)\s+(\d{1,2})(?:st|nd|rd|th)(?:[,.]?\s+(\d{4}|\d{2}))?\b""",
            RegexOption.IGNORE_CASE)
    }

    /**
     * "Nth of Month" and "the Nth of Month" with optional year:
     * "28th of March", "the 5th of October", "the 1st of Jan 2025", "3rd of July 24"
     */
    private val reNthOfMonth by lazy {
        Regex("""\b(?:the\s+)?(\d{1,2})(?:st|nd|rd|th)\s+of\s+($monthAlt)(?:[,.]?\s+(\d{4}|\d{2}))?\b""",
            RegexOption.IGNORE_CASE)
    }

    // ── Relative ─────────────────────────────────────────────────────────────

    private val reYesterday by lazy {
        Regex("""\b(?:yesterday|yday)\b""", RegexOption.IGNORE_CASE)
    }
    private val reToday by lazy {
        Regex("""\b(?:today|2day|tday)\b""", RegexOption.IGNORE_CASE)
    }
    private val reTomorrow by lazy {
        Regex("""\b(?:tomorrow|tmrw|tmr|2mrw|2morrow|2moro|tmmrw|tomoro|tomrw)\b""",
            RegexOption.IGNORE_CASE)
    }

    /** "day before yesterday" → -2 days */
    private val reDayBeforeYesterday by lazy {
        Regex("""\bday\s+before\s+yesterday\b""", RegexOption.IGNORE_CASE)
    }

    /** "day after tomorrow" → +2 days */
    private val reDayAfterTomorrow by lazy {
        Regex("""\bday\s+after\s+tomorrow\b""", RegexOption.IGNORE_CASE)
    }

    /** "3 days ago", "2 weeks ago" → subtract N days/weeks */
    private val reNUnitsAgo by lazy {
        Regex("""\b(\d{1,3})\s+(days?|weeks?)\s+ago\b""", RegexOption.IGNORE_CASE)
    }

    /** "in 3 days", "in 2 weeks" → add N days/weeks */
    private val reInNUnits by lazy {
        Regex("""\bin\s+(\d{1,3})\s+(days?|weeks?)\b""", RegexOption.IGNORE_CASE)
    }

    /** "last week" → -7 days, "next week" → +7 days */
    private val reLastNextWeek by lazy {
        Regex("""\b(last|next)\s+week\b""", RegexOption.IGNORE_CASE)
    }

    /** Optional prefix (last/this/next) followed by a day name */
    private val reDayName by lazy {
        Regex("""\b(?:(last|this|next)\s+)?($dayAlt)\b""", RegexOption.IGNORE_CASE)
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Detect all date references in [text], resolving them relative to [entryDate].
     * Returns a list of [DateLink] objects sorted by range start position.
     * Dates that resolve to [entryDate] itself are excluded (no self-links).
     */
    /**
     * Resolve the effective month-first flag from a [DateOrderPreference].
     * AUTO falls back to the device locale.
     */
    fun resolveMonthFirst(pref: com.mj.yaja.data.DateOrderPreference): Boolean = when (pref) {
        com.mj.yaja.data.DateOrderPreference.DMY  -> false
        com.mj.yaja.data.DateOrderPreference.MDY  -> true
        com.mj.yaja.data.DateOrderPreference.AUTO -> isMonthFirst()
    }

    fun detectDateLinks(
        text: String,
        entryDate: LocalDate,
        monthFirst: Boolean = isMonthFirst(),
        customKeywords: List<com.mj.yaja.data.DateKeywordEntry> = emptyList()
    ): List<DateLink> {
        val results = mutableListOf<DateLink>()
        val used = mutableListOf<IntRange>()

        fun tryAdd(range: IntRange, date: LocalDate?) {
            if (date == null) return
            if (date == entryDate) return        // No link to the current entry's own date
            if (used.any { it.overlaps(range) }) return
            results.add(DateLink(range, date))
            used.add(range)
        }

        // ── 1. ISO full: 2024-03-28 / 2024/03/28 ──────────────────────────────
        for (m in reIsoFull.findAll(text)) {
            val (y, mo, d) = m.destructured
            tryAdd(m.range, safeDate(y.toInt(), mo.toInt(), d.toInt()))
        }

        // ── 2. Written full with 4-digit year ─────────────────────────────────
        // day-first: "28 March 2024"
        for (m in reWrittenFullDF.findAll(text)) {
            val (d, mon, y) = m.destructured
            val mo = MONTH_NAMES[mon.lowercase()] ?: continue
            tryAdd(m.range, safeDate(y.toInt(), mo, d.toInt()))
        }
        // month-first: "March 28, 2024"
        for (m in reWrittenFullMF.findAll(text)) {
            val (mon, d, y) = m.destructured
            val mo = MONTH_NAMES[mon.lowercase()] ?: continue
            tryAdd(m.range, safeDate(y.toInt(), mo, d.toInt()))
        }

        // ── 3. Numeric full with 4-digit year (locale-aware, with fallback) ─────
        for (m in reNumericFull.findAll(text)) {
            val (a, b, y) = m.destructured
            tryAdd(m.range, resolveNumericDate(a.toInt(), b.toInt(), y.toInt(), monthFirst))
        }

        // ── 4. Numeric full with 2-digit year (locale-aware, with fallback) ─────
        // e.g. 28-03-24, 24-3-24, 28/03/24
        for (m in reNumericFullShortYear.findAll(text)) {
            val (a, b, yy) = m.destructured
            tryAdd(m.range, resolveNumericDate(a.toInt(), b.toInt(), expandTwoDigitYear(yy.toInt()), monthFirst))
        }

        // ── 5. Ordinal written dates ───────────────────────────────────────────
        // "28th of March [year]" / "the 28th of March [year]" — run before plain ordinals
        for (m in reNthOfMonth.findAll(text)) {
            val mo = MONTH_NAMES[m.groupValues[2].lowercase()] ?: continue
            tryAdd(m.range, resolveYear(m.groupValues[3], mo, m.groupValues[1].toInt(), entryDate))
        }
        // "28th March [year]"
        for (m in reOrdinalDF.findAll(text)) {
            val mo = MONTH_NAMES[m.groupValues[2].lowercase()] ?: continue
            tryAdd(m.range, resolveYear(m.groupValues[3], mo, m.groupValues[1].toInt(), entryDate))
        }
        // "March 28th [year]"
        for (m in reOrdinalMF.findAll(text)) {
            val mo = MONTH_NAMES[m.groupValues[1].lowercase()] ?: continue
            tryAdd(m.range, resolveYear(m.groupValues[3], mo, m.groupValues[2].toInt(), entryDate))
        }

        // ── 6. Written short without year ─────────────────────────────────────
        // day-first: "28 March"
        for (m in reWrittenShortDF.findAll(text)) {
            val (d, mon) = m.destructured
            val mo = MONTH_NAMES[mon.lowercase()] ?: continue
            tryAdd(m.range, nearestMonthDay(mo, d.toInt(), entryDate))
        }
        // month-first: "March 28"
        for (m in reWrittenShortMF.findAll(text)) {
            val (mon, d) = m.destructured
            val mo = MONTH_NAMES[mon.lowercase()] ?: continue
            tryAdd(m.range, nearestMonthDay(mo, d.toInt(), entryDate))
        }

        // ── 7. Numeric short without year, slash only (locale-aware, with fallback)
        for (m in reNumericShort.findAll(text)) {
            val (a, b) = m.destructured
            tryAdd(m.range, resolveNumericMonthDay(a.toInt(), b.toInt(), monthFirst, entryDate))
        }

        // ── 8. Relative references ─────────────────────────────────────────────
        // Specific phrases first, then general
        for (m in reDayBeforeYesterday.findAll(text)) tryAdd(m.range, entryDate.minusDays(2))
        for (m in reDayAfterTomorrow.findAll(text))   tryAdd(m.range, entryDate.plusDays(2))
        for (m in reYesterday.findAll(text))          tryAdd(m.range, entryDate.minusDays(1))
        for (m in reToday.findAll(text))              tryAdd(m.range, entryDate)   // excluded as self
        for (m in reTomorrow.findAll(text))           tryAdd(m.range, entryDate.plusDays(1))

        // "3 days ago" / "2 weeks ago"
        for (m in reNUnitsAgo.findAll(text)) {
            val n = m.groupValues[1].toLongOrNull() ?: continue
            val days = if (m.groupValues[2].startsWith("week", ignoreCase = true)) n * 7 else n
            tryAdd(m.range, entryDate.minusDays(days))
        }
        // "in 3 days" / "in 2 weeks"
        for (m in reInNUnits.findAll(text)) {
            val n = m.groupValues[1].toLongOrNull() ?: continue
            val days = if (m.groupValues[2].startsWith("week", ignoreCase = true)) n * 7 else n
            tryAdd(m.range, entryDate.plusDays(days))
        }
        // "last week" / "next week"
        for (m in reLastNextWeek.findAll(text)) {
            val date = if (m.groupValues[1].equals("last", ignoreCase = true))
                entryDate.minusDays(7) else entryDate.plusDays(7)
            tryAdd(m.range, date)
        }

        // ── 9. Day names ───────────────────────────────────────────────────────
        for (m in reDayName.findAll(text)) {
            val modifier = m.groupValues[1].lowercase()
            val dayName  = m.groupValues[2].lowercase()
            val dow = DAY_NAMES[dayName] ?: continue
            val resolved = when (modifier) {
                "last" -> mostRecentBefore(dow, entryDate)
                "next" -> nextAfter(dow, entryDate)
                "this" -> nearestDay(dow, entryDate)
                else   -> mostRecent(dow, entryDate)   // no modifier → most recent ≤ entryDate
            }
            tryAdd(m.range, resolved)
        }

        // ── 10. Custom language keywords ───────────────────────────────────────
        for (entry in customKeywords) {
            if (entry.keyword.isBlank()) continue
            // Use Unicode-aware boundaries: not preceded/followed by a Unicode letter.
            // This works for both Latin and non-Latin scripts (e.g. Malayalam, Arabic).
            val re = try {
                Regex("""(?<!\p{L})${Regex.escape(entry.keyword)}(?!\p{L})""")
            } catch (_: Exception) { continue }
            for (m in re.findAll(text)) {
                tryAdd(m.range, resolveKeywordMeaning(entry.meaning, entryDate))
            }
        }

        return results.sortedBy { it.range.first }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Maps a user-supplied meaning string to a resolved [LocalDate], or null if unknown. */
    private fun resolveKeywordMeaning(meaning: String, entryDate: LocalDate): LocalDate? =
        when (meaning.trim().lowercase()) {
            "yesterday"            -> entryDate.minusDays(1)
            "today"                -> entryDate          // will be filtered as self-link
            "tomorrow"             -> entryDate.plusDays(1)
            "day before yesterday" -> entryDate.minusDays(2)
            "day after tomorrow"   -> entryDate.plusDays(2)
            "last week"            -> entryDate.minusDays(7)
            "next week"            -> entryDate.plusDays(7)
            "monday"               -> mostRecent(DayOfWeek.MONDAY,    entryDate)
            "tuesday"              -> mostRecent(DayOfWeek.TUESDAY,   entryDate)
            "wednesday"            -> mostRecent(DayOfWeek.WEDNESDAY, entryDate)
            "thursday"             -> mostRecent(DayOfWeek.THURSDAY,  entryDate)
            "friday"               -> mostRecent(DayOfWeek.FRIDAY,    entryDate)
            "saturday"             -> mostRecent(DayOfWeek.SATURDAY,  entryDate)
            "sunday"               -> mostRecent(DayOfWeek.SUNDAY,    entryDate)
            else                   -> null
        }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
        try { LocalDate.of(year, month, day) } catch (_: Exception) { null }

    /**
     * Parse a numeric date where [a] and [b] are the two ambiguous components (day and month,
     * order unknown). Tries the locale-preferred ordering first; if that produces an invalid
     * date (e.g. month > 12), automatically falls back to the opposite ordering.
     *
     * This means "28-03-26" works correctly even on a month-first device: preferred gives
     * month=28 (invalid) → fallback gives month=3, day=28 (valid). ✓
     */
    private fun resolveNumericDate(a: Int, b: Int, year: Int, monthFirst: Boolean): LocalDate? {
        val (mo1, d1) = if (monthFirst) a to b else b to a
        safeDate(year, mo1, d1)?.let { return it }
        // Preferred ordering was invalid — try the other way
        val (mo2, d2) = if (monthFirst) b to a else a to b
        return safeDate(year, mo2, d2)
    }

    /**
     * Same as [resolveNumericDate] but resolves to the nearest year instead of a fixed year.
     */
    private fun resolveNumericMonthDay(a: Int, b: Int, monthFirst: Boolean, anchor: LocalDate): LocalDate? {
        val (mo1, d1) = if (monthFirst) a to b else b to a
        nearestMonthDay(mo1, d1, anchor)?.let { return it }
        val (mo2, d2) = if (monthFirst) b to a else a to b
        return nearestMonthDay(mo2, d2, anchor)
    }

    /**
     * Expand a 2-digit year using a rolling 50-year window anchored to today.
     * e.g. in 2026: "24" → 2024, "99" → 1999, "76" → 1976, "27" → 2027
     */
    private fun expandTwoDigitYear(yy: Int): Int {
        val currentYear = LocalDate.now().year
        val century = (currentYear / 100) * 100
        val full = century + yy
        return if (full > currentYear + 50) full - 100 else full
    }

    /**
     * Resolve a year string (may be empty, 2-digit, or 4-digit) against [entryDate].
     * Empty → nearest year for [month]/[day]; 2-digit → expand; 4-digit → use directly.
     */
    private fun resolveYear(yearStr: String, month: Int, day: Int, entryDate: LocalDate): LocalDate? {
        val y = yearStr.trim()
        return when (y.length) {
            4    -> safeDate(y.toInt(), month, day)
            2    -> safeDate(expandTwoDigitYear(y.toInt()), month, day)
            else -> nearestMonthDay(month, day, entryDate)
        }
    }

    /**
     * Returns the date with [month]/[day] closest to [anchor] (same year or ±1 year),
     * excluding [anchor] itself.
     */
    private fun nearestMonthDay(month: Int, day: Int, anchor: LocalDate): LocalDate? {
        val same = safeDate(anchor.year, month, day) ?: return null
        val prev = safeDate(anchor.year - 1, month, day)
        val next = safeDate(anchor.year + 1, month, day)
        val candidates = listOfNotNull(prev, same, next).filter { it != anchor }
        return candidates.minByOrNull { kotlin.math.abs(it.toEpochDay() - anchor.toEpochDay()) }
    }

    /** Most recent occurrence of [dow] on or before [anchor]. */
    private fun mostRecent(dow: DayOfWeek, anchor: LocalDate): LocalDate {
        var d = anchor
        while (d.dayOfWeek != dow) d = d.minusDays(1)
        return d
    }

    /** Most recent occurrence of [dow] strictly before [anchor]. */
    private fun mostRecentBefore(dow: DayOfWeek, anchor: LocalDate): LocalDate {
        var d = anchor.minusDays(1)
        while (d.dayOfWeek != dow) d = d.minusDays(1)
        return d
    }

    /** Next occurrence of [dow] strictly after [anchor]. */
    private fun nextAfter(dow: DayOfWeek, anchor: LocalDate): LocalDate {
        var d = anchor.plusDays(1)
        while (d.dayOfWeek != dow) d = d.plusDays(1)
        return d
    }

    /** Nearest occurrence of [dow] (most recent or next, whichever is closer to [anchor]). */
    private fun nearestDay(dow: DayOfWeek, anchor: LocalDate): LocalDate {
        val prev = mostRecent(dow, anchor)
        val next = nextAfter(dow, anchor)
        return if (anchor.toEpochDay() - prev.toEpochDay() <=
                   next.toEpochDay() - anchor.toEpochDay()) prev else next
    }

    private fun IntRange.overlaps(other: IntRange) = first <= other.last && other.first <= last
}
