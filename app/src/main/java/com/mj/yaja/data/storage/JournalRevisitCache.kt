package com.mj.yaja.data.storage

import java.time.LocalDate

data class JournalRevisitCache(
    val revisitDates: Map<LocalDate, LocalDate>,
    val revisitNotes: Map<LocalDate, String>
)
