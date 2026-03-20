package com.mj.yaja.ui.viewmodel

import android.content.Context
import android.content.Intent
import java.io.File
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mj.yaja.data.FontScalePreference
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SearchResult
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.SwipeDirection
import com.mj.yaja.data.ThemePreference
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class JournalUiState(
        val selectedDate: LocalDate = LocalDate.now(),
        val entries: List<String> = emptyList(),
        val datesWithEntries: Set<LocalDate> = emptySet(),
        val monthlyStats: List<Pair<YearMonth, Int>> = emptyList(),
        val yearlyStats: List<Pair<Int, Float>> = emptyList(),
        val isLoading: Boolean = false,
        val editingEntry: String? = null,
        val searchQuery: String = "",
        val searchResults: List<SearchResult> = emptyList(),
        val lookbackEntries: Map<Int, List<String>> = emptyMap(),
        val favoritedHighlights: List<LocalDate> = emptyList(),
        val editingIndex: Int = -1,
        val showCacheAnomalyDialog: Boolean = false
)

class JournalViewModel(
        private val fileManager: MarkdownFileManager,
        private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Dedicated single-thread executor for ML Kit callbacks — keeps all callbacks off the main
    // thread so language detection never touches the main dispatcher
    private val langDetectExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    // Soft-delete staging: holds (date, rawEntry, originalIndex) so we can restore at the same
    // position
    private val _lastDeleted = MutableStateFlow<Triple<LocalDate, String, Int>?>(null)
    val lastDeleted: StateFlow<Triple<LocalDate, String, Int>?> = _lastDeleted.asStateFlow()

    private val _syncProgress = MutableStateFlow<Float?>(null)
    val syncProgress: StateFlow<Float?> = _syncProgress.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents

    val themePreference = settingsRepository.themePreference
    val appFontFamily = settingsRepository.appFontFamily
    val storageUri = settingsRepository.storageUri
    val showTimestamps = settingsRepository.showTimestamps
    val fontScalePreference = settingsRepository.fontScalePreference
    val lastBackupTimestamp = settingsRepository.lastBackupTimestamp
    val firstDayOfWeek = settingsRepository.firstDayOfWeek
    val favoritedDates = settingsRepository.favoritedDates
    val isPinEnabled = settingsRepository.isPinEnabled
    val isBiometricEnabled = settingsRepository.isBiometricEnabled
    val allowFutureEntries = settingsRepository.allowFutureEntries
    val swipeToDeleteEnabled = settingsRepository.swipeToDeleteEnabled
    val swipeDeleteDirection = settingsRepository.swipeDeleteDirection
    val swipeToSyncEnabled = settingsRepository.swipeToSyncEnabled
    val widgetCornerRadius = settingsRepository.widgetCornerRadius
    val showStatistics = settingsRepository.showStatistics
    val enableDragAndDrop = settingsRepository.enableDragAndDrop
    val showWidgetLabel = settingsRepository.showWidgetLabel
    val hasActiveWidgets = settingsRepository.hasActiveWidgets
    val showBottomBar = settingsRepository.showBottomBar
    val customShortcodes = settingsRepository.customShortcodes
    val isPreviewLimitEnabled = settingsRepository.isPreviewLimitEnabled
    val previewLimitLength = settingsRepository.previewLimitLength
    val statisticsSectionOrder = settingsRepository.statisticsSectionOrder

    private val _monthlyStats = MutableStateFlow<com.mj.yaja.ui.screens.MonthlyStatsData?>(null)
    val monthlyStats: StateFlow<com.mj.yaja.ui.screens.MonthlyStatsData?> = _monthlyStats.asStateFlow()

    private val _allTimeStats = MutableStateFlow<com.mj.yaja.ui.screens.AllTimeStatsData?>(null)
    val allTimeStats: StateFlow<com.mj.yaja.ui.screens.AllTimeStatsData?> = _allTimeStats.asStateFlow()

    private val _heatmapData = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val heatmapData: StateFlow<Map<LocalDate, Int>> = _heatmapData.asStateFlow()

    private var lookbackJob: kotlinx.coroutines.Job? = null
    private var highlightsJob: kotlinx.coroutines.Job? = null
    private var entriesJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    
    fun clearCrashLog() {
        try {
            val file = File(fileManager.getContext().cacheDir, "crash_log.txt")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // Ignore errors while clearing logs
        }
    }

    init {
        // Refresh active widget status first (fast, synchronous)
        settingsRepository.refreshActiveWidgetsStatus()

        val today = LocalDate.now()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                // Initial load: populate cache and check if it's the first time
                val dates = fileManager.getAllJournalDatesWithData()

                // Cache Integrity Check
                val currentCount = fileManager.getTotalEntryCount()
                val lastCount = settingsRepository.lastKnownEntryCount.value

                // Alert if a significant drop in entry count is detected
                if (lastCount > 0 && (currentCount == 0 || currentCount < (lastCount * 0.8))) {
                    _uiState.update { it.copy(showCacheAnomalyDialog = true) }
                } else {
                    settingsRepository.setLastKnownEntryCount(currentCount)
                }

                if (dates.isNotEmpty()) {
                    _toastEvents.emit("Data cache synced!")
                }
            }
            loadEntries(today)
            refreshCalendarDates()
            updateLookback(today)
            updateFavoritedHighlights()
            updateMonthlyStats()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setPin(plain: String) = settingsRepository.setPin(plain)
    fun clearPin() = settingsRepository.clearPin()
    fun checkPin(plain: String) = settingsRepository.checkPin(plain)
    fun enableBiometric() = settingsRepository.enableBiometric()
    fun disableBiometric() = settingsRepository.disableBiometric()
    fun setAllowFutureEntries(allow: Boolean) = settingsRepository.setAllowFutureEntries(allow)
    fun setSwipeToDeleteEnabled(enabled: Boolean) =
            settingsRepository.setSwipeToDeleteEnabled(enabled)
    fun setSwipeDeleteDirection(direction: SwipeDirection) =
            settingsRepository.setSwipeDeleteDirection(direction)
    fun setSwipeToSyncEnabled(enabled: Boolean) = settingsRepository.setSwipeToSyncEnabled(enabled)
    fun setWidgetCornerRadius(radius: Int) = settingsRepository.setWidgetCornerRadius(radius)
    fun setShowWidgetLabel(show: Boolean) = settingsRepository.setShowWidgetLabel(show)
    fun setShowBottomBar(show: Boolean) = settingsRepository.setShowBottomBar(show)
    fun setPreviewLimitEnabled(enabled: Boolean) =
            settingsRepository.setPreviewLimitEnabled(enabled)
    fun setPreviewLimitLength(length: Int) = settingsRepository.setPreviewLimitLength(length)
    fun refreshWidgetStatus() = settingsRepository.refreshActiveWidgetsStatus()

    fun dismissCacheAnomalyDialog() {
        _uiState.update { it.copy(showCacheAnomalyDialog = false) }
        // Save the new lower count so we don't nag again
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setLastKnownEntryCount(fileManager.getTotalEntryCount())
        }
    }

    fun acceptCacheAnomalyRefresh() {
        _uiState.update { it.copy(showCacheAnomalyDialog = false) }
        refreshCache()
    }

    /** Reload data when app returns to foreground to pick up changes made by widgets. */
    fun onAppResume() {
        val today = LocalDate.now()
        loadEntries(uiState.value.selectedDate)
        refreshCalendarDates()
        updateLookback(uiState.value.selectedDate)
        updateFavoritedHighlights()
        updateMonthlyStats()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadEntries(date)
        updateLookback(date)
        updateMonthlyStats()
    }

    private fun loadEntries(date: LocalDate) {
        entriesJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        entriesJob =
                viewModelScope.launch {
                    val entries = withContext(Dispatchers.IO) { fileManager.getEntriesForDate(date) }
                    _uiState.update { it.copy(entries = entries, isLoading = false) }
                }
    }

    suspend fun addEntry(entry: String) {
        val currentDate = _uiState.value.selectedDate

        // Add timestamp if not already present
        var finalEntry = entry
        if (!finalEntry.startsWith("<!--time:")) {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val timeString = LocalTime.now().format(timeFormatter)
            val today = LocalDate.now()

            val timestamp =
                    if (currentDate != today) {
                        // For past or future dates, add the date when the entry was added
                        val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                        val dateString = today.format(dateFormatter)
                        "<!--time:$timeString, added on $dateString-->"
                    } else {
                        // For today's entries, just add the time
                        "<!--time:$timeString-->"
                    }
            finalEntry = "$timestamp\n$finalEntry"
        }

        withContext(Dispatchers.IO) { fileManager.addEntryForDate(currentDate, finalEntry) }
        // Load fresh entries directly from disk for immediate UI update
        val freshEntries = withContext(Dispatchers.IO) { fileManager.getEntriesForDateFromDisk(currentDate) }
        _uiState.update { it.copy(entries = freshEntries) }
        // Incrementally add the date to the calendar set instead of full re-scan
        addDateToCalendar(currentDate)
    }

    fun startEditing(entry: String, index: Int) {
        _uiState.update { it.copy(editingEntry = entry, editingIndex = index) }
    }

    fun clearEditing() {
        _uiState.update { it.copy(editingEntry = null, editingIndex = -1) }
    }

    suspend fun updateEntry(newEntry: String) {
        val currentDate = _uiState.value.selectedDate
        val oldEntry = _uiState.value.editingEntry ?: return
        val index = _uiState.value.editingIndex
        if (index == -1) return

        var finalNewEntry = newEntry

        // Preserve original timestamp if it existed.
        // Regex handles both standard <!--time:HH:mm--> and future <!--time:HH:mm, added on
        // dd-MMM-yyyy-->
        val timeRegex = Regex("^<!--time:(\\d{2}:\\d{2})(?:, added on (.*?))?-->\\n?")
        val match = timeRegex.find(oldEntry)
        if (match != null && !finalNewEntry.startsWith("<!--time:")) {
            // Re-attach the timestamp
            finalNewEntry = "${match.value}$finalNewEntry"
        }

        withContext(Dispatchers.IO) {
            fileManager.updateEntryForDate(currentDate, index, finalNewEntry)
        }
        clearEditing()
        // Load fresh entries directly from disk for immediate UI update
        val freshEntries = withContext(Dispatchers.IO) { fileManager.getEntriesForDateFromDisk(currentDate) }
        _uiState.update { it.copy(entries = freshEntries) }
    }

    fun deleteEntry(index: Int) {
        val currentDate = _uiState.value.selectedDate
        val entries = _uiState.value.entries
        val entry = entries.getOrNull(index) ?: return

        // Remember position so UNDO can restore to the same index
        _lastDeleted.value = Triple(currentDate, entry, index)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileManager.deleteEntryForDate(currentDate, index) }
            // Load fresh entries directly from disk for immediate UI update
            val freshEntries = withContext(Dispatchers.IO) { fileManager.getEntriesForDateFromDisk(currentDate) }
            _uiState.update { it.copy(entries = freshEntries) }
            // Only remove from calendar set if there are no entries left for that date
            if (entries.size == 1) removeDateFromCalendar(currentDate)
        }
    }

    /** Restore the most recently deleted entry at its original position. */
    fun undoDelete() {
        val (date, entry, index) = _lastDeleted.value ?: return
        _lastDeleted.value = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileManager.insertEntryAtPosition(date, entry, index) }
            // Load fresh entries directly from disk for immediate UI update
            val freshEntries = withContext(Dispatchers.IO) { fileManager.getEntriesForDateFromDisk(_uiState.value.selectedDate) }
            _uiState.update { it.copy(entries = freshEntries) }
            addDateToCalendar(date) // Re-add in case it was the last entry and was removed
        }
    }

    /** Called when the UNDO window expires without user action. */
    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

    fun refreshCalendarDates() {
        viewModelScope.launch {
            val dates = withContext(Dispatchers.IO) { fileManager.getAllJournalDatesWithData() }
            val stats = calculateMonthlyStats(dates)
            val yStats = calculateYearlyStats(dates)
            _uiState.update {
                it.copy(datesWithEntries = dates, monthlyStats = stats, yearlyStats = yStats)
            }
        }
    }

    /** Returns a random past date that has entries, or null if fewer than 50 past entries exist. */
    fun surpriseMe(): LocalDate? {
        val candidates = _uiState.value.datesWithEntries.filter { it.isBefore(LocalDate.now()) }
        return if (candidates.size < 50) null else candidates.random()
    }

    /** Incrementally add a date to the calendar set without a full re-scan. */
    private fun addDateToCalendar(date: LocalDate) {
        val updated = _uiState.value.datesWithEntries + date
        _uiState.update {
            it.copy(
                    datesWithEntries = updated,
                    monthlyStats = calculateMonthlyStats(updated),
                    yearlyStats = calculateYearlyStats(updated)
            )
        }
    }

    /** Incrementally remove a date from the calendar set without a full re-scan. */
    private fun removeDateFromCalendar(date: LocalDate) {
        val updated = _uiState.value.datesWithEntries - date
        _uiState.update {
            it.copy(
                    datesWithEntries = updated,
                    monthlyStats = calculateMonthlyStats(updated),
                    yearlyStats = calculateYearlyStats(updated)
            )
        }
    }

    private fun calculateMonthlyStats(dates: Set<LocalDate>): List<Pair<YearMonth, Int>> {
        val currentMonth = YearMonth.now()
        val stats = mutableListOf<Pair<YearMonth, Int>>()

        for (i in 11 downTo 0) {
            val month = currentMonth.minusMonths(i.toLong())
            val count = dates.count { YearMonth.from(it) == month }
            stats.add(month to count)
        }
        return stats
    }

    private fun calculateYearlyStats(dates: Set<LocalDate>): List<Pair<Int, Float>> {
        if (dates.isEmpty()) return emptyList()

        val years = dates.map { it.year }.distinct().sorted()
        return years.map { year ->
            val count = dates.count { it.year == year }.toFloat()
            year to count
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob =
                viewModelScope.launch {
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList()) }
                        return@launch
                    }
                    delay(300) // debounce — cancelled if user types another character
                    val results = withContext(Dispatchers.IO) { fileManager.searchEntries(query) }
                    _uiState.update { it.copy(searchResults = results) }
                }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            val shortcodes = settingsRepository.customShortcodes.value
            val uri = withContext(Dispatchers.IO) { fileManager.createBackupZip(shortcodes) }
            if (uri != null) {
                settingsRepository.setLastBackupTimestamp(System.currentTimeMillis())
                val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                context.startActivity(Intent.createChooser(intent, "Share Backup"))
            }
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        settingsRepository.setThemePreference(preference)
    }

    fun setAppFontFamily(fontFamily: com.mj.yaja.data.AppFontFamily) {
        settingsRepository.setAppFontFamily(fontFamily)
    }

    fun setFontScalePreference(preference: FontScalePreference) {
        settingsRepository.setFontScalePreference(preference)
    }

    fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
        settingsRepository.setFirstDayOfWeek(dayOfWeek)
    }

    fun setShowTimestamps(show: Boolean) {
        settingsRepository.setShowTimestamps(show)
    }

    fun setShowStatistics(show: Boolean) {
        settingsRepository.setShowStatistics(show)
    }

    fun setEnableDragAndDrop(enable: Boolean) {
        settingsRepository.setEnableDragAndDrop(enable)
    }

    fun setStatisticsSectionOrder(order: List<String>) {
        settingsRepository.setStatisticsSectionOrder(order)
    }

    fun setCustomShortcode(code: String, value: String) {
        val current = settingsRepository.customShortcodes.value.toMutableMap()
        current[code] = value
        settingsRepository.setCustomShortcodes(current)
    }

    fun removeCustomShortcode(code: String) {
        val current = settingsRepository.customShortcodes.value.toMutableMap()
        current.remove(code)
        settingsRepository.setCustomShortcodes(current)
    }

    fun reorderEntries(reorderedEntries: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDate = _uiState.value.selectedDate
            fileManager.setEntriesForDate(currentDate, reorderedEntries)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(entries = reorderedEntries) }
            }
        }
    }

    fun importCustomShortcodes(newShortcodes: Map<String, String>) {
        val current = settingsRepository.customShortcodes.value
        // Retain existing shortcodes in case of conflicts
        val merged = current.toMutableMap()
        newShortcodes.forEach { (code, value) ->
            if (!merged.containsKey(code)) {
                merged[code] = value
            }
        }
        settingsRepository.setCustomShortcodes(merged)
    }

    fun toggleFavorite(date: LocalDate) {
        settingsRepository.toggleFavorite(date)
        updateFavoritedHighlights()
    }

    private fun updateLookback(date: LocalDate) {
        lookbackJob?.cancel()
        lookbackJob =
                viewModelScope.launch {
                    // Launch all IO reads in parallel for faster lookback loading
                    val results = coroutineScope {
                        (1..10)
                                .map { i ->
                                    async(Dispatchers.IO) {
                                        val pastDate = date.minusYears(i.toLong())
                                        val entries = fileManager.getEntriesForDate(pastDate)
                                        if (entries.isNotEmpty()) i to entries else null
                                    }
                                }
                                .awaitAll()
                                .filterNotNull()
                    }
                    val lookbackMap = results.toMap().toSortedMap()
                    _uiState.update { it.copy(lookbackEntries = lookbackMap) }
                }
    }

    private fun updateFavoritedHighlights() {
        highlightsJob?.cancel()
        highlightsJob =
                viewModelScope.launch {
                    val favorites = settingsRepository.favoritedDates.value
                    // Parallelize all IO reads for each favorite date
                    val highlights = coroutineScope {
                        favorites
                                .map { dateStr ->
                                    async(Dispatchers.IO) {
                                        try {
                                            val date = LocalDate.parse(dateStr)
                                            if (fileManager.getEntriesForDate(date).isNotEmpty()) date else null
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                                .awaitAll()
                                .filterNotNull()
                                .sortedDescending()
                    }
                    _uiState.update { it.copy(favoritedHighlights = highlights) }
                }
    }

    private fun updateMonthlyStats() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val currentMonth = YearMonth.now()
                val monthStart = currentMonth.atDay(1)
                val monthEnd = currentMonth.atEndOfMonth()

                var wordCount = 0
                var entriesCount = 0
                val dayEntryCounts = mutableMapOf<DayOfWeek, Int>()
                val dateCounts = mutableMapOf<LocalDate, Int>()

                // Count entries and words for each day of the current month
                var date = monthStart
                while (date <= monthEnd) {
                    val entries = fileManager.getEntriesForDate(date)
                    if (entries.isNotEmpty()) {
                        entriesCount += entries.size
                        entries.forEach { entry ->
                            // Count words by splitting on whitespace
                            wordCount += entry.split(Regex("\\s+")).count { it.isNotBlank() }
                        }
                        val dayOfWeek = date.dayOfWeek
                        dayEntryCounts[dayOfWeek] = (dayEntryCounts[dayOfWeek] ?: 0) + entries.size
                        dateCounts[date] = entries.size
                    }
                    date = date.plusDays(1)
                }

                // Find most active day
                val mostActiveDay = dayEntryCounts.maxByOrNull { it.value }?.key

                // Calculate longest streak (consecutive days with entries)
                var longestStreak = 0
                var currentStreak = 0
                date = monthStart
                while (date <= monthEnd) {
                    if (dateCounts.containsKey(date)) {
                        currentStreak++
                        longestStreak = maxOf(longestStreak, currentStreak)
                    } else {
                        currentStreak = 0
                    }
                    date = date.plusDays(1)
                }

                com.mj.yaja.ui.screens.MonthlyStatsData(
                    entriesCount = entriesCount,
                    wordCount = wordCount,
                    mostActiveDay = mostActiveDay?.name,
                    longestStreak = longestStreak
                )
            }
            _monthlyStats.value = stats
        }
    }

    fun calculateStatsByPeriod(
        period: com.mj.yaja.ui.screens.StatisticsPeriod,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        viewModelScope.launch {
            // Create language identifier here (on the coroutine's Main dispatcher context)
            // so ML Kit can safely initialise — never inside withContext(Dispatchers.IO)
            val langIdentifier = LanguageIdentification.getClient(
                LanguageIdentificationOptions.Builder()
                    .setConfidenceThreshold(0.5f)
                    .build()
            )

            // Phase 1 — all file-IO work; collect entry texts for later language detection
            data class IoResult(
                val stats: com.mj.yaja.ui.screens.AllTimeStatsData,
                val textsToDetect: List<String>
            )

            val ioResult = withContext(Dispatchers.IO) {
                val now = LocalDate.now()
                val (rangeStart, rangeEnd) = when (period) {
                    com.mj.yaja.ui.screens.StatisticsPeriod.ALL_TIME -> {
                        Pair(null, null)
                    }
                    com.mj.yaja.ui.screens.StatisticsPeriod.CURRENT_YEAR -> {
                        Pair(LocalDate.of(now.year, 1, 1), LocalDate.of(now.year, 12, 31))
                    }
                    com.mj.yaja.ui.screens.StatisticsPeriod.PREVIOUS_YEAR -> {
                        Pair(LocalDate.of(now.year - 1, 1, 1), LocalDate.of(now.year - 1, 12, 31))
                    }
                    com.mj.yaja.ui.screens.StatisticsPeriod.CURRENT_MONTH -> {
                        Pair(LocalDate.of(now.year, now.monthValue, 1), LocalDate.of(now.year, now.monthValue, now.dayOfMonth))
                    }
                    com.mj.yaja.ui.screens.StatisticsPeriod.PREVIOUS_MONTH -> {
                        val prevMonth = now.minusMonths(1)
                        val lastDay = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
                        Pair(LocalDate.of(prevMonth.year, prevMonth.monthValue, 1), lastDay)
                    }
                    com.mj.yaja.ui.screens.StatisticsPeriod.CUSTOM -> {
                        Pair(startDate, endDate)
                    }
                }

                val allDates = _uiState.value.datesWithEntries.sorted()
                    .filter { date ->
                        (rangeStart == null || date >= rangeStart) && (rangeEnd == null || date <= rangeEnd)
                    }

                if (allDates.isEmpty()) {
                    return@withContext IoResult(
                        stats = com.mj.yaja.ui.screens.AllTimeStatsData(
                            totalEntries = 0,
                            totalWords = 0,
                            averageWordsPerEntry = 0f,
                            currentStreak = 0,
                            longestStreakAllTime = 0,
                            mostActiveDay = null,
                            totalDaysWithEntries = 0,
                            writingConsistencyScore = 0f,
                            monthlyEntryTrend = emptyList(),
                            entriesByLength = com.mj.yaja.ui.screens.DayDistribution(0, 0, 0, 0),
                            totalHighlightedDays = 0,
                            bestMonthLabel = null,
                            bestMonthCount = 0,
                            averageDaysPerWeek = 0f,
                            writingTimeDistribution = com.mj.yaja.ui.screens.TimeDistribution(0, 0, 0, 0),
                            languageDistribution = emptyMap()
                        ),
                        textsToDetect = emptyList()
                    )
                }

                var totalEntries = 0
                var totalWords = 0
                val dayEntryCounts = mutableMapOf<DayOfWeek, Int>()
                val monthEntryCounts = mutableMapOf<String, Int>()
                var shortCount = 0
                var mediumCount = 0
                var longCount = 0
                var intenseCount = 0
                // Time-of-day distribution
                var morningCount = 0    // 05:00–11:59
                var afternoonCount = 0  // 12:00–16:59
                var eveningCount = 0    // 17:00–20:59
                var nightCount = 0      // 21:00–04:59
                val timeRegex = Regex("""<!--time:(\d{2}):\d{2}""")
                val timestampRegex = Regex("<!--time:[^>]+-->\\n?")

                // Collect texts for language detection (done after IO, using proper suspension)
                val textsToDetect = mutableListOf<String>()

                // Highlighted days that fall within this period
                val favoritedInPeriod = _uiState.value.favoritedHighlights
                    .count { date -> allDates.contains(date) }

                // Iterate through all dates and collect statistics
                for (date in allDates) {
                    val entries = fileManager.getEntriesForDate(date)
                    if (entries.isNotEmpty()) {
                        totalEntries += entries.size
                        val dayOfWeek = date.dayOfWeek
                        dayEntryCounts[dayOfWeek] = (dayEntryCounts[dayOfWeek] ?: 0) + entries.size

                        val monthKey = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
                        monthEntryCounts[monthKey] = (monthEntryCounts[monthKey] ?: 0) + entries.size

                        // Sum all entry word counts for this day first, then classify the day
                        var dayTotalWords = 0
                        entries.forEach { entry ->
                            val wordCount = entry.split(Regex("\\s+")).count { it.isNotBlank() }
                            totalWords += wordCount
                            dayTotalWords += wordCount

                            // Parse timestamp to build time-of-day distribution
                            val hourStr = timeRegex.find(entry)?.groupValues?.getOrNull(1)
                            if (hourStr != null) {
                                val hour = hourStr.toIntOrNull() ?: -1
                                when (hour) {
                                    in 5..11  -> morningCount++
                                    in 12..16 -> afternoonCount++
                                    in 17..20 -> eveningCount++
                                    in 0..4, in 21..23 -> nightCount++
                                }
                            }

                            // Collect clean text for language detection (done in Phase 2)
                            val cleanText = entry.replace(timestampRegex, "").trim()
                            if (cleanText.length >= 20) {
                                textsToDetect.add(cleanText)
                            }
                        }

                        // Categorize the whole day by its total word count
                        when {
                            dayTotalWords < 50   -> shortCount++
                            dayTotalWords < 200  -> mediumCount++
                            dayTotalWords < 500  -> longCount++
                            else                 -> intenseCount++
                        }
                    }
                }

                // Find most active day
                val mostActiveDay = dayEntryCounts.maxByOrNull { it.value }?.key?.name

                // Calculate average words per entry
                val averageWordsPerEntry = if (totalEntries > 0) {
                    totalWords.toFloat() / totalEntries
                } else {
                    0f
                }

                // Calculate current streak (from today backwards)
                var currentStreak = 0
                var tempDate = LocalDate.now()
                while (allDates.contains(tempDate)) {
                    currentStreak++
                    tempDate = tempDate.minusDays(1)
                }

                // Calculate longest streak all-time
                var longestStreakAllTime = 0
                var currentStreakTemp = 0
                for (i in allDates.indices) {
                    val date = allDates[i]
                    val previousDate = if (i > 0) allDates[i - 1] else null
                    val isConsecutive = previousDate?.plusDays(1) == date || previousDate == null

                    if (isConsecutive) {
                        currentStreakTemp++
                    } else {
                        longestStreakAllTime = maxOf(longestStreakAllTime, currentStreakTemp)
                        currentStreakTemp = 1
                    }
                }
                longestStreakAllTime = maxOf(longestStreakAllTime, currentStreakTemp)

                // Calculate writing consistency score (based on regularity)
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(
                    allDates.first(),
                    allDates.last()
                ).toInt() + 1
                val consistencyScore = if (daysDiff > 0) {
                    (allDates.size.toFloat() / daysDiff) * 100
                } else {
                    0f
                }

                // Average writing days per week
                val totalWeeks = (daysDiff / 7.0).coerceAtLeast(1.0)
                val averageDaysPerWeek = (allDates.size.toFloat() / totalWeeks).toFloat()

                // Best month by entry count
                val bestMonthEntry = monthEntryCounts.maxByOrNull { it.value }
                val bestMonthLabel: String? = bestMonthEntry?.key?.let { key ->
                    try {
                        val parts = key.split("-")
                        val ym = YearMonth.of(parts[0].toInt(), parts[1].toInt())
                        ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
                    } catch (e: Exception) { null }
                }
                val bestMonthCount = bestMonthEntry?.value ?: 0

                // Build monthly entry trend (last 12 months)
                val monthlyTrend = mutableListOf<Pair<String, Int>>()
                val currentMonth = YearMonth.now()
                repeat(12) { i ->
                    val month = currentMonth.minusMonths(i.toLong())
                    val monthKey = "${month.year}-${month.monthValue.toString().padStart(2, '0')}"
                    monthlyTrend.add(monthKey to (monthEntryCounts[monthKey] ?: 0))
                }
                monthlyTrend.reverse()

                IoResult(
                    stats = com.mj.yaja.ui.screens.AllTimeStatsData(
                        totalEntries = totalEntries,
                        totalWords = totalWords,
                        averageWordsPerEntry = averageWordsPerEntry,
                        currentStreak = currentStreak,
                        longestStreakAllTime = longestStreakAllTime,
                        mostActiveDay = mostActiveDay,
                        totalDaysWithEntries = allDates.size,
                        writingConsistencyScore = consistencyScore,
                        monthlyEntryTrend = monthlyTrend,
                        entriesByLength = com.mj.yaja.ui.screens.DayDistribution(shortCount, mediumCount, longCount, intenseCount),
                        totalHighlightedDays = favoritedInPeriod,
                        bestMonthLabel = bestMonthLabel,
                        bestMonthCount = bestMonthCount,
                        averageDaysPerWeek = averageDaysPerWeek,
                        writingTimeDistribution = com.mj.yaja.ui.screens.TimeDistribution(
                            morning = morningCount,
                            afternoon = afternoonCount,
                            evening = eveningCount,
                            night = nightCount
                        ),
                        languageDistribution = emptyMap() // filled in Phase 2
                    ),
                    textsToDetect = textsToDetect
                )
            }

            // Show stats immediately so the screen is responsive while language detection runs
            _allTimeStats.value = ioResult.stats

            // Phase 2 — language detection on IO dispatcher, callbacks on dedicated executor
            // (neither the main thread nor the IO thread pool are blocked)
            if (ioResult.textsToDetect.isNotEmpty()) {
                // Sample at most 200 entries evenly — accurate distribution, avoids overwhelming ML Kit
                val sampled = ioResult.textsToDetect.let { texts ->
                    if (texts.size <= 200) texts
                    else (0 until 200).map { i ->
                        texts[(i.toDouble() / 200.0 * texts.size).toInt()]
                    }
                }

                val langCounts = withContext(Dispatchers.IO) {
                    val counts = mutableMapOf<String, Int>()
                    for (text in sampled) {
                        val code = try {
                            detectLanguage(langIdentifier, text)
                        } catch (e: Exception) {
                            "und"
                        }
                        counts[code] = (counts[code] ?: 0) + 1
                    }
                    counts
                }
                langIdentifier.close()

                val languageDistribution = langCounts
                    .entries
                    .sortedWith(compareBy({ it.key == "und" }, { -it.value }))
                    .associate { it.key to it.value }

                _allTimeStats.value = ioResult.stats.copy(languageDistribution = languageDistribution)
            } else {
                langIdentifier.close()
            }
        }
    }

    /**
     * Bridges ML Kit's Task into a coroutine suspension.
     * All callbacks run on [langDetectExecutor] — never on the main thread — so the main
     * dispatcher is never touched during language detection.
     */
    private suspend fun detectLanguage(identifier: LanguageIdentifier, text: String): String =
        suspendCancellableCoroutine { cont ->
            try {
                identifier.identifyLanguage(text)
                    .addOnSuccessListener(langDetectExecutor) { code ->
                        if (cont.isActive) cont.resume(code ?: "und")
                    }
                    .addOnFailureListener(langDetectExecutor) {
                        if (cont.isActive) cont.resume("und")
                    }
                    .addOnCanceledListener(langDetectExecutor) {
                        if (cont.isActive) cont.resume("und")
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume("und")
            }
        }

    fun updateHeatmapData() {
        viewModelScope.launch {
            val heatmap = withContext(Dispatchers.IO) {
                val map = mutableMapOf<LocalDate, Int>()
                _uiState.value.datesWithEntries.forEach { date ->
                    val entries = fileManager.getEntriesForDate(date)
                    val totalWords = entries.sumOf { entry ->
                        entry.split(Regex("\\s+")).count { it.isNotBlank() }
                    }
                    map[date] = totalWords
                }
                map
            }
            _heatmapData.value = heatmap
        }
    }

    fun setStorageUri(uriString: String?) {
        val oldUri = settingsRepository.storageUri.value

        viewModelScope.launch(Dispatchers.IO) {
            // Invalidate cache before migration so it reads fresh from source
            fileManager.invalidateCache()
            // Perform migration
            fileManager.migrateEntries(oldUri, uriString)

            // Save new preference — MutableStateFlow.value= and SharedPreferences.apply() are
            // both thread-safe, so no need to switch to Main here.
            settingsRepository.setStorageUri(uriString)
            // Refresh entries when storage location changes
            loadEntries(_uiState.value.selectedDate)
            refreshCalendarDates()
        }
    }

    fun refreshCache() {
        viewModelScope.launch {
            _syncProgress.value = 0f
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                fileManager.forceRefresh { current, total ->
                    if (total > 0) {
                        _syncProgress.value = current.toFloat() / total.toFloat()
                    }
                }
            }
            // Refresh all visible data
            val today = _uiState.value.selectedDate
            loadEntries(today)
            refreshCalendarDates()
            updateLookback(today)
            updateFavoritedHighlights()
            updateMonthlyStats()
            _uiState.update { it.copy(isLoading = false) }
            _syncProgress.value = null
            _toastEvents.emit("Data cache synced!")
        }
    }

    class Factory(
            private val fileManager: MarkdownFileManager,
            private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return JournalViewModel(fileManager, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
