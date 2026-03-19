package com.mj.yaja.ui.viewmodel

import android.content.Context
import android.content.Intent
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
    val allowFutureEntries = settingsRepository.allowFutureEntries
    val swipeToDeleteEnabled = settingsRepository.swipeToDeleteEnabled
    val swipeDeleteDirection = settingsRepository.swipeDeleteDirection
    val swipeToSyncEnabled = settingsRepository.swipeToSyncEnabled
    val widgetCornerRadius = settingsRepository.widgetCornerRadius
    val showWidgetLabel = settingsRepository.showWidgetLabel
    val hasActiveWidgets = settingsRepository.hasActiveWidgets
    val showBottomBar = settingsRepository.showBottomBar
    val customShortcodes = settingsRepository.customShortcodes
    val isPreviewLimitEnabled = settingsRepository.isPreviewLimitEnabled
    val previewLimitLength = settingsRepository.previewLimitLength

    private var lookbackJob: kotlinx.coroutines.Job? = null
    private var highlightsJob: kotlinx.coroutines.Job? = null
    private var entriesJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null

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
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setPin(plain: String) = settingsRepository.setPin(plain)
    fun clearPin() = settingsRepository.clearPin()
    fun checkPin(plain: String) = settingsRepository.checkPin(plain)
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
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadEntries(date)
        updateLookback(date)
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

    fun addEntry(entry: String) {
        val currentDate = _uiState.value.selectedDate

        // Add timestamp if not already present
        var finalEntry = entry
        if (!finalEntry.startsWith("<!--time:")) {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val timeString = LocalTime.now().format(timeFormatter)

            val timestamp =
                    if (currentDate.isAfter(LocalDate.now())) {
                        val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                        val dateString = LocalDate.now().format(dateFormatter)
                        "<!--time:$timeString, added on $dateString-->"
                    } else {
                        "<!--time:$timeString-->"
                    }
            finalEntry = "$timestamp\n$finalEntry"
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileManager.addEntryForDate(currentDate, finalEntry) }
            loadEntries(currentDate)
            // Incrementally add the date to the calendar set instead of full re-scan
            addDateToCalendar(currentDate)
        }
    }

    fun startEditing(entry: String, index: Int) {
        _uiState.update { it.copy(editingEntry = entry, editingIndex = index) }
    }

    fun clearEditing() {
        _uiState.update { it.copy(editingEntry = null, editingIndex = -1) }
    }

    fun updateEntry(newEntry: String) {
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

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileManager.updateEntryForDate(currentDate, index, finalNewEntry)
            }
            clearEditing()
            loadEntries(currentDate)
        }
    }

    fun deleteEntry(index: Int) {
        val currentDate = _uiState.value.selectedDate
        val entries = _uiState.value.entries
        val entry = entries.getOrNull(index) ?: return

        // Remember position so UNDO can restore to the same index
        _lastDeleted.value = Triple(currentDate, entry, index)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileManager.deleteEntryForDate(currentDate, index) }
            loadEntries(currentDate)
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
            loadEntries(_uiState.value.selectedDate)
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
                                        val date = LocalDate.parse(dateStr)
                                        if (fileManager.getEntriesForDate(date).isNotEmpty()) date else null
                                    }
                                }
                                .awaitAll()
                                .filterNotNull()
                                .sortedDescending()
                    }
                    _uiState.update { it.copy(favoritedHighlights = highlights) }
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
