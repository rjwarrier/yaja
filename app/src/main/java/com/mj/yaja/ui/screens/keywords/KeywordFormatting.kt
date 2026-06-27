package com.mj.yaja.ui.screens

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val keywordIndexedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MMM • hh:mm a")

internal val keywordDetailDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MMM-yyyy")

internal val keywordTimelineMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM yyyy")

internal val keywordTimelineShortFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM")

internal val keywordTimelineYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yy")

internal fun formatKeywordIndexedAt(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(keywordIndexedAtFormatter)
