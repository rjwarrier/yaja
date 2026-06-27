package com.mj.yaja.data

import java.time.LocalDate

data class HomeScreenSnapshot(
    val selectedDate: LocalDate,
    val entries: List<String>,
    val dayLabel: String
)
