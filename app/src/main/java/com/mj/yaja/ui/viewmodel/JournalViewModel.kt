package com.mj.yaja.ui.viewmodel

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mj.yaja.data.DailyJournalMetrics
import com.mj.yaja.data.EventItem
import com.mj.yaja.data.EventIndexRepository
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.applyEntryKindMetadata
import com.mj.yaja.data.FabPlacement
import com.mj.yaja.data.FontScalePreference
import com.mj.yaja.data.UiScalePreference
import com.mj.yaja.data.HomeScreenSnapshot
import com.mj.yaja.data.BackgroundTintLevel
import com.mj.yaja.data.CalendarDensityPreference
import com.mj.yaja.data.ColorSource
import com.mj.yaja.data.CustomPalette
import com.mj.yaja.data.DateOrderPreference
import com.mj.yaja.data.PersonalAccentStyle
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.data.KeywordMatchCache
import com.mj.yaja.data.KeywordRepository
import com.mj.yaja.data.KeywordStats
import com.mj.yaja.data.KeywordType
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.data.SearchResult
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.ThemeColorIntensity
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.data.TodoIndexRepository
import com.mj.yaja.data.TodoItem
import com.mj.yaja.data.TodoParser
import com.mj.yaja.data.countWordsIgnoringChecklistMarkers
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.AppLanguage
import com.mj.yaja.data.AppLogRepository
import com.mj.yaja.data.DueRevisitItem
import com.mj.yaja.data.RevisitMarker
import com.mj.yaja.data.keywords.KeywordCsvCodec
import com.mj.yaja.domain.entries.DeletedEntryBatch
import com.mj.yaja.domain.entries.EntryCoordinator
import com.mj.yaja.domain.keywords.KeywordCoordinator
import com.mj.yaja.util.PerformanceTrace
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        val showCacheAnomalyDialog: Boolean = false,
        val activeKeywordFilter: KeywordDefinition? = null,
        val keywordFilteredEntries: List<Pair<LocalDate, List<KeywordMatch>>>? = null
)

sealed interface ExternalOpenRequest {
    data class Date(val date: LocalDate) : ExternalOpenRequest
    data class Entry(val date: LocalDate, val entryIndex: Int) : ExternalOpenRequest
}

class JournalViewModel(
        internal val fileManager: MarkdownFileManager,
        private val settingsRepository: SettingsRepository,
        private val keywordRepository: KeywordRepository,
        private val keywordMatchCache: KeywordMatchCache
) : ViewModel() {
    private val todoIndexRepository = TodoIndexRepository.getInstance(fileManager.getContext())
    private val eventIndexRepository = EventIndexRepository.getInstance(fileManager.getContext())
    private val appLogRepository =
            AppLogRepository.getInstance(fileManager.getContext(), settingsRepository)

    data class VersionHistorySnapshotUi(
        val id: String,
        val createdAt: Long,
        val summary: String,
        val content: String
    )

    data class RestoreSummary(
        val newDays: Int,
        val mergedDays: Int,
        val skippedJournalEntries: Int,
        val shortcodesAdded: Int,
        val shortcodesSkipped: Int,
        val dateKeywordsAdded: Int,
        val dateKeywordsSkipped: Int,
        val peoplePlacesAdded: Int,
        val peoplePlacesSkipped: Int,
        val recurringTasksAdded: Int = 0,
        val recurringTasksSkipped: Int = 0
    )

    private data class StatisticsBuildResult(
        val stats: com.mj.yaja.ui.screens.AllTimeStatsData,
        val contributions: LinkedHashMap<LocalDate, DayStatisticsAnalysis>,
        val rangeStart: LocalDate?,
        val rangeEnd: LocalDate?
    )

    companion object {
        private const val TAG = "JournalViewModel"
        private const val LARGE_JOURNAL_DATE_THRESHOLD = 1000
        private const val STATS_FRESHNESS_MS = 10 * 60 * 1000L
        private const val HEATMAP_FRESHNESS_MS = 10 * 60 * 1000L
        private const val MONTHLY_STATS_FRESHNESS_MS = 5 * 60 * 1000L
        private const val RESUME_REFRESH_MIN_INTERVAL_MS = 15 * 1000L
        private const val RESUME_KEYWORD_REINDEX_INTERVAL_MS = 10 * 60 * 1000L
        private const val RESUME_FORCE_DATE_REFRESH_INTERVAL_MS = 2 * 60 * 1000L
        private const val BACKGROUND_FULL_REFRESH_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000L
    }

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    // Soft-delete staging keeps enough context to restore an entry in place.
    private val _lastDeleted = MutableStateFlow<DeletedEntryBatch?>(null)
    val lastDeleted: StateFlow<DeletedEntryBatch?> = _lastDeleted.asStateFlow()

    private val _syncProgress = MutableStateFlow<Float?>(null)
    val syncProgress: StateFlow<Float?> = _syncProgress.asStateFlow()
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()
    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events: StateFlow<List<EventItem>> = _events.asStateFlow()
    private val _todoRefreshInProgress = MutableStateFlow(false)
    val todoRefreshInProgress: StateFlow<Boolean> = _todoRefreshInProgress.asStateFlow()
    private val _appLogText = MutableStateFlow("")
    val appLogText: StateFlow<String> = _appLogText.asStateFlow()
    private val _backgroundWorkLabel = MutableStateFlow<String?>(null)
    val backgroundWorkLabel: StateFlow<String?> = _backgroundWorkLabel.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents
    private val _externalOpenRequests = MutableSharedFlow<ExternalOpenRequest>()
    val externalOpenRequests: SharedFlow<ExternalOpenRequest> = _externalOpenRequests
    private var lastBackgroundToastMessage: String? = null
    private var lastBackgroundToastAt: Long = 0L

    sealed class ImportState {
        object Idle : ImportState()
        data class Running(
            val current: Int = 0,
            val total: Int = 0
        ) : ImportState() {
            val progress: Float get() = if (total > 0) current.toFloat() / total else 0f
        }
        data class Success(
            val newDays: Int,
            val mergedDays: Int,
            val skippedEntries: Int
        ) : ImportState()
        data class Error(val message: String) : ImportState()
    }
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()
    private val _restoreSummary = MutableStateFlow<RestoreSummary?>(null)
    val restoreSummary: StateFlow<RestoreSummary?> = _restoreSummary.asStateFlow()
    private val _storageMigrationInProgress = MutableStateFlow(false)
    val storageMigrationInProgress: StateFlow<Boolean> = _storageMigrationInProgress.asStateFlow()
    private val _versionHistorySnapshots = MutableStateFlow<List<VersionHistorySnapshotUi>>(emptyList())
    val versionHistorySnapshots: StateFlow<List<VersionHistorySnapshotUi>> = _versionHistorySnapshots.asStateFlow()
    private val _versionHistoryRestoreInProgress = MutableStateFlow(false)
    val versionHistoryRestoreInProgress: StateFlow<Boolean> = _versionHistoryRestoreInProgress.asStateFlow()

    private var currentDayOneImporter: com.mj.yaja.data.DayOneImporter? = null
    private var currentJournalisticImporter: com.mj.yaja.data.JournalisticImporter? = null
    private var currentMarkdownFolderImporter: com.mj.yaja.data.MarkdownFolderImporter? = null

    private val settingsFeature = SettingsFeatureController(
        settingsRepository = settingsRepository,
        keywordRepository = keywordRepository,
        importState = importState,
        restoreSummary = restoreSummary,
        storageMigrationInProgress = storageMigrationInProgress,
        scope = viewModelScope
    )

    val themePreference = settingsFeature.themePreference
    val colorSource = settingsFeature.colorSource
    val customPalette = settingsFeature.customPalette
    val themeColorIntensity = settingsFeature.themeColorIntensity
    val backgroundTintLevel = settingsFeature.backgroundTintLevel
    val personalThemeSlots = settingsFeature.personalThemeSlots
    val activePersonalThemeSlotId = settingsFeature.activePersonalThemeSlotId
    val appFontFamily = settingsFeature.appFontFamily
    val monoFontWeight = settingsFeature.monoFontWeight
    val customFontPath = settingsFeature.customFontPath
    val customFontName = settingsFeature.customFontName
    val entryStyle = settingsFeature.entryStyle
    val storageUri = settingsFeature.storageUri
    val hasCompletedOnboarding = settingsFeature.hasCompletedOnboarding
    val shouldShowOnboarding = settingsFeature.shouldShowOnboarding
    val showOnboardingNextLaunch = settingsFeature.showOnboardingNextLaunch
    val showTimestamps = settingsFeature.showTimestamps
    val showDayHeaderStats = settingsFeature.showDayHeaderStats
    val renderCheckboxesAsText = settingsFeature.renderCheckboxesAsText
    val uiScalePreference = settingsFeature.uiScalePreference
    val fontScalePreference = settingsFeature.fontScalePreference
    val dataFontScalePreference = settingsFeature.dataFontScalePreference
    val followUiFontScale = settingsFeature.followUiFontScale
    val appLanguage = settingsFeature.appLanguage
    val animationPreference = settingsFeature.animationPreference
    val lastBackupTimestamp = settingsFeature.lastBackupTimestamp
    val backupReminderDays = settingsFeature.backupReminderDays
    val appLogRetentionDays = settingsFeature.appLogRetentionDays
    val firstDayOfWeek = settingsFeature.firstDayOfWeek
    val dateOrderPreference = settingsFeature.dateOrderPreference
    val customDateKeywords = settingsFeature.customDateKeywords
    val isPinEnabled = settingsFeature.isPinEnabled
    val isBiometricEnabled = settingsFeature.isBiometricEnabled
    val autoLockTimeoutMinutes = settingsFeature.autoLockTimeoutMinutes
    val hideTextModeEnabled = settingsFeature.hideTextModeEnabled
    val privacySecuritySettingsUiState = settingsFeature.privacySecuritySettingsUiState
    val carryForwardTodosEnabled = settingsFeature.carryForwardTodosEnabled

    private val _shouldLock = MutableStateFlow(false)
    val shouldLock: StateFlow<Boolean> = _shouldLock.asStateFlow()

    /** Called from MainActivity.onStart with the timestamp of last user activity. */
    fun checkAutoLockTimeout(lastActivityTime: Long) {
        if (!isPinEnabled.value) return
        val timeoutMillis = autoLockTimeoutMinutes.value * 60_000L
        if (System.currentTimeMillis() - lastActivityTime >= timeoutMillis) {
            _shouldLock.value = true
        }
    }

    /** Called after the composable has navigated to PIN lock so the flag is not re-triggered. */
    fun onLockHandled() {
        _shouldLock.value = false
    }

    val allowFutureEntries = settingsFeature.allowFutureEntries
    val allowTaskerAccess = settingsFeature.allowTaskerAccess
    val allowTaskerEvents = settingsFeature.allowTaskerEvents
    val includeEntryTextInTaskerEvents = settingsFeature.includeEntryTextInTaskerEvents
    val swipeToNavigateDatesEnabled = settingsFeature.swipeToNavigateDatesEnabled
    val swipeToSyncEnabled = settingsFeature.swipeToSyncEnabled
    val largeJournalSafeMode = settingsFeature.largeJournalSafeMode
    val versionHistoryEnabled = settingsFeature.versionHistoryEnabled
    val versionHistoryMaxVersions = settingsFeature.versionHistoryMaxVersions
    val versionHistoryRetentionDays = settingsFeature.versionHistoryRetentionDays
    val dataRecoverySettingsUiState = settingsFeature.dataRecoverySettingsUiState
    val lastBackgroundFullRefreshAt = settingsFeature.lastBackgroundFullRefreshAt
    val showStatistics = settingsFeature.showStatistics
    val showLookbackInNavBar = settingsFeature.showLookbackInNavBar
    val showKeywordsInNavBar = settingsFeature.showKeywordsInNavBar
    val showTodosInNavBar = settingsFeature.showTodosInNavBar
    val showCompletedTodos = settingsFeature.showCompletedTodos
    val showStatisticsInNavBar = settingsFeature.showStatisticsInNavBar
    val enableDragAndDrop = settingsFeature.enableDragAndDrop
    val entryDeleteSelectionEnabled = settingsFeature.entryDeleteSelectionEnabled
    val hasActiveWidgets = settingsFeature.hasActiveWidgets
    val showBottomBar = settingsFeature.showBottomBar
    val navigationChromeMode = settingsFeature.navigationChromeMode
    val showBottomPanelLabels = settingsFeature.showBottomPanelLabels
    val fabPlacement = settingsFeature.fabPlacement
    val calendarDensityPreference = settingsFeature.calendarDensityPreference
    val adaptiveBottomNav = settingsFeature.adaptiveBottomNav
    val navigationGesturesSettingsUiState = settingsFeature.navigationGesturesSettingsUiState
    val customShortcodes = settingsFeature.customShortcodes
    val recentTemplateIds = settingsFeature.recentTemplateIds
    val favoriteTemplateIds = settingsFeature.favoriteTemplateIds
    val templateUsageCounts = settingsFeature.templateUsageCounts
    val templateFollowUpCounts = settingsFeature.templateFollowUpCounts
    val entryReviewEnabled = settingsFeature.entryReviewEnabled
    val keywordHighlightingEnabled = settingsFeature.keywordHighlightingEnabled
    val isPreviewLimitEnabled = settingsFeature.isPreviewLimitEnabled
    val previewLimitLength = settingsFeature.previewLimitLength
    val statisticsSectionOrder = settingsFeature.statisticsSectionOrder
    val visibleStatisticsSections = settingsFeature.visibleStatisticsSections
    val useMLKitDetection = settingsFeature.useMLKitDetection
    val keywords = settingsFeature.keywords
    val fuzzyThreshold = settingsFeature.fuzzyThreshold
    val journalExperienceSettingsUiState = settingsFeature.journalExperienceSettingsUiState
    val rootSettingsUiState = settingsFeature.rootSettingsUiState
    val appearanceSettingsUiState = settingsFeature.appearanceSettingsUiState
    private val keywordCoordinator = KeywordCoordinator(
        fileManager = fileManager,
        keywordRepository = keywordRepository,
        keywordMatchCache = keywordMatchCache,
        scope = viewModelScope,
        getUiState = { _uiState.value },
        updateUiState = { transform -> _uiState.update(transform) },
        emitToast = { message -> _toastEvents.emit(message) }
    )
    val keywordIndexingIds: StateFlow<Set<String>> = keywordCoordinator.keywordIndexingIds
    val keywordMatchCounts: StateFlow<Map<String, Int>> = keywordCoordinator.keywordMatchCounts
    val keywordMatchState = keywordCoordinator.keywordMatchState
    val keywordLastIndexedAt = keywordCoordinator.keywordLastIndexedAt
    val keywordRebuildProgress = keywordCoordinator.keywordRebuildProgress
    val keywordEstimatedRemainingMillis = keywordCoordinator.keywordEstimatedRemainingMillis

    fun markTemplateUsed(templateId: String) {
        settingsFeature.markTemplateUsed(templateId)
    }

    fun toggleFavoriteTemplate(templateId: String) {
        settingsFeature.toggleFavoriteTemplate(templateId)
    }

    fun incrementTemplateUsage(templateId: String) {
        settingsFeature.incrementTemplateUsage(templateId)
    }

    fun incrementTemplateFollowUp(templateId: String) {
        settingsFeature.incrementTemplateFollowUp(templateId)
    }

    suspend fun buildReviewSummary(
        period: com.mj.yaja.ui.screens.ReviewPeriodType,
        anchorDate: LocalDate = LocalDate.now()
    ): com.mj.yaja.ui.screens.ReviewSummaryData = withContext(Dispatchers.IO) {
        buildReviewSummaryData(
            period = period,
            anchorDate = anchorDate,
            firstDayOfWeek = settingsRepository.firstDayOfWeek.value,
            allJournalDates = fileManager.getAllJournalDatesLightweight().toList(),
            metricsSnapshotProvider = { dates -> prepareDailyMetricsSnapshot(fileManager, dates) },
            entrySnapshotProvider = { dates -> prepareEntriesSnapshot(fileManager, dates) },
            allLabels = fileManager.getAllStarredLabels(),
            starredDates = fileManager.getStarredDates(),
            keywordsById = keywordRepository.keywords.value.associateBy { it.id },
            matchesForDateProvider = keywordMatchCache::getMatchesForDate,
            detectScript = ::detectDominantScriptSnapshot
        )
    }

    // Dedicated executor for ML Kit callbacks â€” keeps them off the main thread
    private val langDetectExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private val _monthlyStats = MutableStateFlow<com.mj.yaja.ui.screens.MonthlyStatsData?>(null)
    val monthlyStats: StateFlow<com.mj.yaja.ui.screens.MonthlyStatsData?> = _monthlyStats.asStateFlow()

    private val _allTimeStats = MutableStateFlow<com.mj.yaja.ui.screens.AllTimeStatsData?>(null)
    val allTimeStats: StateFlow<com.mj.yaja.ui.screens.AllTimeStatsData?> = _allTimeStats.asStateFlow()

    private val _heatmapData = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val heatmapData: StateFlow<Map<LocalDate, Int>> = _heatmapData.asStateFlow()

    private val _statisticsProgress = MutableStateFlow<Float?>(null)
    val statisticsProgress: StateFlow<Float?> = _statisticsProgress.asStateFlow()

    private val _statisticsSettling = MutableStateFlow(false)
    val statisticsSettling: StateFlow<Boolean> = _statisticsSettling.asStateFlow()

    private val _statisticsComparison =
        MutableStateFlow<com.mj.yaja.ui.screens.StatisticsComparisonData?>(null)
    val statisticsComparison:
        StateFlow<com.mj.yaja.ui.screens.StatisticsComparisonData?> =
            _statisticsComparison.asStateFlow()

    private var lookbackJob: kotlinx.coroutines.Job? = null
    private var highlightsJob: kotlinx.coroutines.Job? = null
    private var entriesJob: kotlinx.coroutines.Job? = null
    private var loadingDate: LocalDate? = null
    private var calendarDatesJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private var deferredStartupJob: kotlinx.coroutines.Job? = null
    private var statisticsJob: kotlinx.coroutines.Job? = null
    private var heatmapJob: kotlinx.coroutines.Job? = null
    private var monthlyStatsJob: kotlinx.coroutines.Job? = null
    private var importJob: kotlinx.coroutines.Job? = null
    private var storageMigrationJob: kotlinx.coroutines.Job? = null
    private var backgroundMaintenanceJob: kotlinx.coroutines.Job? = null
    private var todoRefreshJob: kotlinx.coroutines.Job? = null
    private var cacheRefreshJob: kotlinx.coroutines.Job? = null
    private val storageMigrationMutex = Mutex()
    private var latestEntriesRequestId: Long = 0L
    private var latestTodoRefreshRequestId: Long = 0L
    private var latestCacheRefreshRequestId: Long = 0L
    private var latestStatisticsRequestId: Long = 0L
    private val lookbackSnapshotCache =
        object : LinkedHashMap<LocalDate, Map<Int, List<String>>>(24, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<LocalDate, Map<Int, List<String>>>?
            ): Boolean = size > 24
        }
    private var lastPersistedHomeSnapshot: HomeScreenSnapshot? = null
    private var lastStatisticsRequestKey: String? = null
    private var lastStatisticsCompletedAt: Long = 0L
    private var statisticsRangeStart: LocalDate? = null
    private var statisticsRangeEnd: LocalDate? = null
    private var statisticsContributionCache = linkedMapOf<LocalDate, DayStatisticsAnalysis>()
    private val statisticsIncrementalMutex = Mutex()
    private var lastHeatmapRefreshAt: Long = 0L
    private var lastMonthlyStatsRefreshAt: Long = 0L
    private var lastMonthlyStatsMonth: YearMonth? = null
    private var lastResumeRefreshAt: Long = 0L
    private var lastForcedDateRefreshAt: Long = 0L
    private val _currentDayLabel = MutableStateFlow("")
    val currentDayLabel: StateFlow<String> = _currentDayLabel.asStateFlow()
    private val _starredDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val starredDates: StateFlow<Set<LocalDate>> = _starredDates.asStateFlow()
    private val _favoritedDates = MutableStateFlow<Set<String>>(emptySet())
    val favoritedDates: StateFlow<Set<String>> = _favoritedDates.asStateFlow()
    private val todoToggleMutex = Mutex()

    private val _starredLabels = MutableStateFlow<Map<LocalDate, String>>(emptyMap())
    val starredLabels: StateFlow<Map<LocalDate, String>> = _starredLabels.asStateFlow()
    private val _revisitMarkers = MutableStateFlow<List<RevisitMarker>>(emptyList())
    val revisitMarkers: StateFlow<List<RevisitMarker>> = _revisitMarkers.asStateFlow()
    private val _revisitTargetDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val revisitTargetDates: StateFlow<Set<LocalDate>> = _revisitTargetDates.asStateFlow()
    private val _currentRevisitDate = MutableStateFlow<LocalDate?>(null)
    val currentRevisitDate: StateFlow<LocalDate?> = _currentRevisitDate.asStateFlow()
    private val _currentRevisitNote = MutableStateFlow("")
    val currentRevisitNote: StateFlow<String> = _currentRevisitNote.asStateFlow()
    private val _dueRevisits = MutableStateFlow<List<DueRevisitItem>>(emptyList())
    val dueRevisits: StateFlow<List<DueRevisitItem>> = _dueRevisits.asStateFlow()
    private val entryCoordinator = EntryCoordinator(
        fileManager = fileManager,
        settingsRepository = settingsRepository,
        keywordCoordinator = keywordCoordinator,
        scope = viewModelScope,
        uiStateFlow = _uiState,
        lastDeletedFlow = _lastDeleted
    )

      private fun logPerf(phase: String, elapsedMs: Long) {
          PerformanceTrace.log(TAG, phase, elapsedMs)
      }

    private suspend fun emitBackgroundToast(message: String) {
        val now = System.currentTimeMillis()
        if (message == lastBackgroundToastMessage && now - lastBackgroundToastAt < 2_000L) return
        lastBackgroundToastMessage = message
        lastBackgroundToastAt = now
        _toastEvents.emit(message)
    }

    fun clearCrashLog() {
        try {
            val file = File(fileManager.getContext().cacheDir, "crash_log.txt")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // Ignore errors while clearing logs
        }
    }

    fun loadAppLog() {
        viewModelScope.launch(Dispatchers.IO) {
            _appLogText.value = appLogRepository.readLog()
        }
    }

    fun clearAppLog() {
        viewModelScope.launch(Dispatchers.IO) {
            appLogRepository.clearLog()
            appLogRepository.logInfo("App log cleared")
            _appLogText.value = appLogRepository.readLog()
        }
    }

    fun setAppLogRetentionDays(days: Int) {
        settingsRepository.setAppLogRetentionDays(days)
        viewModelScope.launch(Dispatchers.IO) {
            appLogRepository.pruneLogBlocking()
            appLogRepository.logInfo(
                event = "App log retention changed",
                details = "days=${settingsRepository.appLogRetentionDays.value}"
            )
            _appLogText.value = appLogRepository.readLog()
        }
    }

    init {
        appLogRepository.logInfo("App ViewModel initialized")
        // Refresh active widget status first (fast, synchronous)
        settingsRepository.refreshActiveWidgetsStatus()
        keywordMatchCache.loadFromDisk()

        viewModelScope.launch {
            todoIndexRepository.entries.collect { entries ->
                _todos.value = todoIndexRepository.toTodoItems(entries)
            }
        }
        viewModelScope.launch {
            eventIndexRepository.entries.collect { entries ->
                _events.value = entries
            }
        }

        val savedHomeSnapshot = settingsRepository.getHomeScreenSnapshot()
        lastPersistedHomeSnapshot = savedHomeSnapshot
        val today = savedHomeSnapshot?.selectedDate ?: LocalDate.now()
        viewModelScope.launch {
            try {
                val startupStartedAt = System.currentTimeMillis()
                _uiState.update { it.copy(isLoading = true) }
                var bootstrapSnapshot: StartupBootstrapSnapshot? = null
                timedPhaseWorkflow("startup.bootstrap", ::logPerf) {
                    withContext(Dispatchers.IO) {
                        val bootstrap = loadStartupBootstrapSnapshot(
                            fileManager = fileManager,
                            savedHomeSnapshot = savedHomeSnapshot,
                            today = today,
                            lastKnownEntryCount = settingsRepository.lastKnownEntryCount.value,
                            largeJournalThreshold = LARGE_JOURNAL_DATE_THRESHOLD,
                            logTag = TAG,
                            logPerf = ::logPerf
                        )
                        bootstrapSnapshot = bootstrap
                    }
                }
                val bootstrap = bootstrapSnapshot ?: error("Startup bootstrap did not produce a snapshot")
                applyStartupBootstrapSnapshot(
                    bootstrap = bootstrap,
                    startupDate = today,
                    uiState = _uiState,
                    currentDayLabel = _currentDayLabel,
                    persistHomeScreenSnapshot = { selectedDate, entries, dayLabel ->
                        persistHomeSnapshotIfChanged(
                            selectedDate = selectedDate,
                            entries = entries,
                            dayLabel = dayLabel,
                            lastPersistedSnapshot = lastPersistedHomeSnapshot,
                            persistSnapshot = settingsRepository::setHomeScreenSnapshot,
                            updateLastPersistedSnapshot = { snapshot ->
                                lastPersistedHomeSnapshot = snapshot
                            }
                        )
                    },
                    calculateMonthlyStats = ::calculateMonthlyEntryStats,
                    calculateYearlyStats = ::calculateYearlyEntryStats,
                    refreshSelectedDateOnStartup = { startupDate ->
                        refreshSelectedDateOnStartup(
                            date = startupDate,
                            loadEntries = { d, r -> loadEntries(d, reason = r) }
                        )
                    },
                    publishCachedTodos = { ensureTodosLoaded() },
                    onCacheAnomalyDetected = {
                        _uiState.update { it.copy(showCacheAnomalyDialog = true) }
                    },
                    onEntryCountConfirmed = { count ->
                        settingsRepository.setLastKnownEntryCount(count)
                    },
                    onFallbackImmediateLoad = {
                        Log.d(TAG, "startup.fallingBackToImmediateLoad")
                    },
                    logTag = TAG
                )
                logPerf("startup.total", System.currentTimeMillis() - startupStartedAt)
                queuePostLaunchRefreshWork(
                    date = today,
                    dateCount = bootstrap.initialDateCount
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Startup bootstrap failed; falling back to selected date load", e)
                _currentDayLabel.value = savedHomeSnapshot?.dayLabel.orEmpty()
                _uiState.update {
                    it.copy(
                        selectedDate = today,
                        entries = savedHomeSnapshot?.entries.orEmpty(),
                        isLoading = false
                    )
                }
                loadEntries(today, showLoading = false, reason = "startup_bootstrap_fallback")
            }
        }
    }

    fun setPin(plain: String) = settingsFeature.setPin(plain)
    fun clearPin() = settingsFeature.clearPin()
    fun checkPin(plain: String) = settingsFeature.checkPin(plain)
    fun enableBiometric() = settingsFeature.enableBiometric()
    fun disableBiometric() = settingsFeature.disableBiometric()
    fun setAutoLockTimeout(minutes: Int) = settingsFeature.setAutoLockTimeout(minutes)
    fun setAllowFutureEntries(allow: Boolean) = settingsFeature.setAllowFutureEntries(allow)
    fun setHideTextModeEnabled(enabled: Boolean) =
        settingsFeature.setHideTextModeEnabled(enabled)
    fun setCarryForwardTodosEnabled(enabled: Boolean) =
        settingsFeature.setCarryForwardTodosEnabled(enabled)
    fun setAllowTaskerAccess(allow: Boolean) = settingsFeature.setAllowTaskerAccess(allow)
    fun setAllowTaskerEvents(allow: Boolean) = settingsFeature.setAllowTaskerEvents(allow)
    fun setIncludeEntryTextInTaskerEvents(include: Boolean) =
        settingsFeature.setIncludeEntryTextInTaskerEvents(include)
    fun setSwipeToNavigateDatesEnabled(enabled: Boolean) =
            settingsFeature.setSwipeToNavigateDatesEnabled(enabled)
    fun setSwipeToSyncEnabled(enabled: Boolean) = settingsFeature.setSwipeToSyncEnabled(enabled)
    fun setLargeJournalSafeMode(enabled: Boolean) =
            settingsFeature.setLargeJournalSafeMode(enabled)
    fun setVersionHistoryEnabled(enabled: Boolean) =
            settingsFeature.setVersionHistoryEnabled(enabled)
    fun setVersionHistoryMaxVersions(count: Int) =
            settingsFeature.setVersionHistoryMaxVersions(count)
    fun setVersionHistoryRetentionDays(days: Int) =
        settingsFeature.setVersionHistoryRetentionDays(days)

    fun setBackupReminderDays(days: Int) = settingsFeature.setBackupReminderDays(days)

    fun loadVersionHistorySnapshots(date: LocalDate = _uiState.value.selectedDate) {
        viewModelScope.launch(Dispatchers.IO) {
            _versionHistorySnapshots.value =
                mapVersionHistorySnapshotsUi(fileManager.getVersionHistorySnapshots(date))
        }
    }

    fun restoreVersionHistorySnapshot(snapshotId: String, date: LocalDate = _uiState.value.selectedDate) {
        if (_versionHistoryRestoreInProgress.value) return
        viewModelScope.launch {
            _versionHistoryRestoreInProgress.value = true
            try {
                clearLookbackSnapshotCache(lookbackSnapshotCache)
                withContext(Dispatchers.IO) {
                    restoreVersionHistorySnapshotAndRefresh(
                        fileManager = fileManager,
                        date = date,
                        snapshotId = snapshotId,
                        reloadEntries = { restoreDate ->
                            withContext(Dispatchers.Main) {
                                loadEntries(restoreDate, showLoading = false)
                            }
                        },
                        refreshCalendarDates = { refreshCalendarDates(forceRefresh = true) },
                        refreshStarredLabels = {
                            refreshJournalMetaWorkflow(
                                scope = viewModelScope,
                                fileManager = fileManager,
                                selectedDate = _uiState.value.selectedDate,
                                starredDates = _starredDates,
                                favoritedDates = _favoritedDates,
                                starredLabels = _starredLabels,
                                revisitMarkers = _revisitMarkers,
                                revisitTargetDates = _revisitTargetDates,
                                currentRevisitDate = _currentRevisitDate,
                                currentRevisitNote = _currentRevisitNote,
                                dueRevisits = _dueRevisits
                            )
                        },
                        refreshRevisitState = { selectedDate ->
                            refreshRevisitStateWorkflow(
                                fileManager = fileManager,
                                selectedDate = selectedDate,
                                revisitMarkers = _revisitMarkers,
                                revisitTargetDates = _revisitTargetDates,
                                currentRevisitDate = _currentRevisitDate,
                                currentRevisitNote = _currentRevisitNote,
                                dueRevisits = _dueRevisits
                            )
                        },
                        refreshFavoritedHighlights = {
                            highlightsJob = refreshFavoritedHighlightsWorkflow(
                                scope = viewModelScope,
                                currentJob = highlightsJob,
                                starredDates = _starredDates.value,
                                uiState = _uiState
                            )
                        },
                        refreshTodos = { forceRebuild -> refreshTodos(forceRebuild = forceRebuild) },
                        updateHeatmap = { updateHeatmapData(force = true) },
                        ensureMonthlyStatsLoaded = {
                            monthlyStatsJob = refreshMonthlyStatsWorkflow(
                                force = true,
                                currentStats = _monthlyStats.value,
                                lastMonthlyStatsMonth = lastMonthlyStatsMonth,
                                lastMonthlyStatsRefreshAt = lastMonthlyStatsRefreshAt,
                                freshnessMs = MONTHLY_STATS_FRESHNESS_MS,
                                fileManager = fileManager,
                                monthlyStatsState = _monthlyStats,
                                scope = viewModelScope,
                                currentJob = monthlyStatsJob,
                                onStatsLoaded = { refreshedMonth ->
                                    lastMonthlyStatsMonth = refreshedMonth
                                    lastMonthlyStatsRefreshAt = System.currentTimeMillis()
                                },
                                logPerf = ::logPerf
                            )
                        },
                        reloadVersionHistorySnapshots = ::loadVersionHistorySnapshots,
                        emitToast = { message -> _toastEvents.emit(message) }
                    )
                }
            } finally {
                _versionHistoryRestoreInProgress.value = false
            }
        }
    }
    fun setShowBottomBar(show: Boolean) = settingsFeature.setShowBottomBar(show)
    fun setNavigationChromeMode(mode: NavigationChromeMode) =
            settingsFeature.setNavigationChromeMode(mode)
    fun setShowBottomPanelLabels(show: Boolean) =
            settingsFeature.setShowBottomPanelLabels(show)
    fun setFabPlacement(placement: com.mj.yaja.data.FabPlacement) =
            settingsFeature.setFabPlacement(placement)
    fun setCalendarDensityPreference(preference: CalendarDensityPreference) =
            settingsFeature.setCalendarDensityPreference(preference)
    fun setAdaptiveBottomNav(enabled: Boolean) =
            settingsFeature.setAdaptiveBottomNav(enabled)
    fun setPreviewLimitEnabled(enabled: Boolean) =
            settingsFeature.setPreviewLimitEnabled(enabled)
    fun setPreviewLimitLength(length: Int) = settingsFeature.setPreviewLimitLength(length)
    fun refreshWidgetStatus() = settingsFeature.refreshWidgetStatus()

    // Keyword actions

    fun addKeyword(
        name: String,
        type: KeywordType,
        relation: String = "",
        aliases: List<String> = emptyList(),
        enabled: Boolean = true
    ) {
        appLogRepository.logInfo(
            event = "Keyword added",
            details = "type=$type aliases=${aliases.size} relationSet=${relation.isNotBlank()} enabled=$enabled"
        )
        keywordCoordinator.addKeyword(name, type, relation, aliases, enabled)
    }

    fun updateKeyword(keyword: KeywordDefinition) {
        appLogRepository.logInfo(
            event = "Keyword updated",
            details = "type=${keyword.type} aliases=${keyword.aliases.size} enabled=${keyword.isEnabled}"
        )
        keywordCoordinator.updateKeyword(keyword)
    }

    fun deleteKeyword(keywordId: String) {
        appLogRepository.logInfo("Keyword deleted")
        keywordCoordinator.deleteKeyword(keywordId)
    }

    fun setKeywordEnabled(keywordId: String, enabled: Boolean) {
        appLogRepository.logInfo(
            event = "Keyword enabled changed",
            details = "enabled=$enabled"
        )
        keywordCoordinator.setKeywordEnabled(keywordId, enabled)
    }

    fun setKeywordFuzzyThreshold(threshold: Float) =
        keywordCoordinator.setKeywordFuzzyThreshold(threshold)

    fun reindexAllKeywords() {
        appLogRepository.logInfo("Keyword reindex requested")
        keywordCoordinator.reindexAllKeywords()
    }

    fun importKeywords(importedKeywords: List<KeywordDefinition>) {
        appLogRepository.logInfo(
            event = "Keywords import requested",
            details = "count=${importedKeywords.size}"
        )
        keywordCoordinator.importKeywords(importedKeywords)
    }

    fun rebuildKeywordIndex(
        immediate: Boolean = false,
        trackedKeywordIds: Set<String> = emptySet()
    ) {
        appLogRepository.logInfo(
            event = "Keyword index rebuild requested",
            details = "immediate=$immediate tracked=${trackedKeywordIds.size}"
        )
        keywordCoordinator.rebuildKeywordIndex(immediate, trackedKeywordIds)
    }

    fun filterByKeyword(keywordId: String?) = keywordCoordinator.filterByKeyword(keywordId)

    fun filterByKeywordType(type: KeywordType?) = keywordCoordinator.filterByKeywordType(type)

    fun clearKeywordFilter() = keywordCoordinator.clearKeywordFilter()

    fun getKeywordById(keywordId: String): KeywordDefinition? =
        keywordCoordinator.getKeywordById(keywordId)

    fun getKeywordStats(keywordId: String): KeywordStats? =
        keywordCoordinator.getKeywordStats(keywordId)

    fun getMatchesForKeyword(keywordId: String): List<KeywordMatch> =
        keywordCoordinator.getMatchesForKeyword(keywordId)

    fun getKeywordMatchCounts(): Map<String, Int> =
        keywordCoordinator.getKeywordMatchCounts()

    fun getTopKeywords(type: KeywordType? = null, limit: Int = 5): List<Pair<KeywordDefinition, Int>> =
        keywordCoordinator.getTopKeywords(type, limit)

    // App and journal settings

    fun dismissCacheAnomalyDialog() {
        dismissCacheAnomalyWorkflow(
            uiState = _uiState,
            scope = viewModelScope,
            persistLastKnownEntryCount = {
                settingsRepository.setLastKnownEntryCount(fileManager.getTotalEntryCount())
            }
        )
    }

    fun acceptCacheAnomalyRefresh() {
        acceptCacheAnomalyRefreshWorkflow(
            uiState = _uiState,
            refreshCache = ::refreshCache
        )
    }

    /** Reload data when app returns to foreground to pick up changes made by widgets. */
    fun onAppResume() {
        val now = System.currentTimeMillis()
        val dateCount = _uiState.value.datesWithEntries.size
        backgroundMaintenanceJob?.cancel()

        runResumeRefreshWorkflow(
            now = now,
            selectedDate = uiState.value.selectedDate,
            dateCount = dateCount,
            lastResumeRefreshAt = lastResumeRefreshAt,
            lastForcedDateRefreshAt = lastForcedDateRefreshAt,
            lastKeywordIndexAt = keywordLastIndexedAt.value,
            largeJournalDateThreshold = LARGE_JOURNAL_DATE_THRESHOLD,
            largeJournalSafeModeEnabled = settingsRepository.largeJournalSafeMode.value,
            resumeRefreshMinIntervalMs = RESUME_REFRESH_MIN_INTERVAL_MS,
            resumeKeywordReindexIntervalMs = RESUME_KEYWORD_REINDEX_INTERVAL_MS,
            resumeForceDateRefreshIntervalMs = RESUME_FORCE_DATE_REFRESH_INTERVAL_MS,
            updateLastResumeRefreshAt = { timestamp -> lastResumeRefreshAt = timestamp },
            markForcedDateRefresh = { timestamp -> lastForcedDateRefreshAt = timestamp },
            refreshSelectedDateOnResume = { date, shouldForceDateRefresh, markForcedDateRefresh ->
                refreshSelectedDateOnResume(
                    date = date,
                    shouldForceDateRefresh = shouldForceDateRefresh,
                    loadEntries = { d, show, r -> loadEntries(d, showLoading = show, reason = r) },
                    refreshCalendarDates = ::refreshCalendarDates,
                    markForcedDateRefresh = markForcedDateRefresh,
                    refreshStarredLabels = {
                        refreshJournalMetaWorkflow(
                            scope = viewModelScope,
                            fileManager = fileManager,
                            selectedDate = _uiState.value.selectedDate,
                            starredDates = _starredDates,
                            favoritedDates = _favoritedDates,
                            starredLabels = _starredLabels,
                            revisitMarkers = _revisitMarkers,
                            revisitTargetDates = _revisitTargetDates,
                            currentRevisitDate = _currentRevisitDate,
                            currentRevisitNote = _currentRevisitNote,
                            dueRevisits = _dueRevisits
                        )
                    }
                )
            }
        )
    }

    fun onAppBackgrounded() {
        backgroundMaintenanceJob?.cancel()
        backgroundMaintenanceJob = null
    }

      fun onExternalEntryAdded(date: LocalDate) {
          viewModelScope.launch {
              updateLoadedStatisticsForChangedDate(date)
              if (shouldReloadCurrentTodayAfterExternalEntry(date, _uiState.value.selectedDate)) {
                  loadEntries(date, showLoading = false)
              }
          }
      }

    fun selectDate(date: LocalDate, source: String = "date_select") {
        navigateToSelectedDate(
            date = date,
            uiState = _uiState,
            loadEntries = { d, r -> loadEntries(d, reason = r) },
            reason = source
        )
    }

    fun openExternalDateOrEntry(date: LocalDate, entryIndex: Int? = null) {
        selectDate(date)
        viewModelScope.launch {
            if (entryIndex != null && entryIndex >= 0) {
                val entry = withContext(Dispatchers.IO) {
                    fileManager.getEntriesForDate(date).getOrNull(entryIndex)
                }
                if (entry != null) {
                    startEditing(entry, entryIndex)
                    _externalOpenRequests.emit(ExternalOpenRequest.Entry(date, entryIndex))
                    return@launch
                }
            }
            _externalOpenRequests.emit(ExternalOpenRequest.Date(date))
        }
    }

    private suspend fun carryForwardOpenTodosIfNeeded(date: LocalDate) {
        if (date != LocalDate.now() || !settingsRepository.carryForwardTodosEnabled.value) return

        val previousDate = date.minusDays(1)
        val openTodosFromYesterday =
            todoIndexRepository.getEntries(showCompleted = false)
                .filter { it.date == previousDate && !it.isChecked }
                .map { TodoCarryForwardPlanner.stripCarryForwardMarker(it.displayText) }
                .filter { it.isNotBlank() }
                .distinctBy(TodoCarryForwardPlanner::normalizeTodoText)

        if (openTodosFromYesterday.isEmpty()) return

        val yesterdaysEntries = fileManager.getEntriesForDateFromDisk(previousDate)
        val todaysEntries = fileManager.getEntriesForDateFromDisk(date)
        val missingTodos =
            TodoCarryForwardPlanner.missingTodosForToday(
                openTodosFromPreviousDay = openTodosFromYesterday,
                todaysEntries = todaysEntries
            )

        if (missingTodos.isEmpty()) return

        val updatedYesterdaysEntries =
            TodoCarryForwardPlanner.removeMovedTodosFromEntries(
                entries = yesterdaysEntries,
                movedTodos = missingTodos
            )

        val carryForwardBlock =
            TodoCarryForwardPlanner.buildCarryForwardBlock(
                todaysEntries = todaysEntries,
                missingTodos = missingTodos,
                sourceDate = previousDate
            )

        val updatedEntries = todaysEntries.toMutableList()
        updatedEntries += carryForwardBlock

        fileManager.setEntriesForDate(previousDate, updatedYesterdaysEntries)
        fileManager.setEntriesForDate(date, updatedEntries)
    }

    private fun loadEntries(date: LocalDate, showLoading: Boolean = true, reason: String = "unknown") {
        val isForcedRefresh = reason == "selected_date_refresh" || reason == "pull_to_refresh"
        if (!isForcedRefresh && entriesJob?.isActive == true && loadingDate == date) {
            return
        }

        entriesJob?.cancel()
        loadingDate = date
        val requestId = ++latestEntriesRequestId

        // Memory-cache only on the caller (main) thread — a cache miss must not trigger a
        // synchronous SAF/disk read here. The coroutine below loads from disk on IO.
        val cachedEntries = fileManager.getCachedEntriesForDate(date)

        _uiState.update {
            it.copy(
                isLoading = if (showLoading) true else it.isLoading,
                entries = cachedEntries ?: if (showLoading) emptyList() else it.entries
            )
        }
        entriesJob = launchSelectedDateLoad(
            scope = viewModelScope,
            fileManager = fileManager,
            date = date,
            beforeLoad = { carryForwardOpenTodosIfNeeded(date) },
            isRequestStillCurrent = {
                requestId == latestEntriesRequestId && _uiState.value.selectedDate == date
            },
            uiState = _uiState,
            currentDayLabel = _currentDayLabel,
            currentRevisitDate = _currentRevisitDate,
            currentRevisitNote = _currentRevisitNote,
            dueRevisits = _dueRevisits,
            persistHomeScreenSnapshot = { selectedDate, entries, dayLabel ->
                persistHomeSnapshotIfChanged(
                    selectedDate = selectedDate,
                    entries = entries,
                    dayLabel = dayLabel,
                    lastPersistedSnapshot = lastPersistedHomeSnapshot,
                    persistSnapshot = settingsRepository::setHomeScreenSnapshot,
                    updateLastPersistedSnapshot = { snapshot ->
                        lastPersistedHomeSnapshot = snapshot
                    }
                )
            },
            logPerf = ::logPerf,
            onLoadApplied = { _, _ ->
                if (requestId == latestEntriesRequestId) {
                    loadingDate = null
                }
            },
            onStaleResultDiscarded = { _, _ ->
                if (requestId == latestEntriesRequestId) {
                    loadingDate = null
                }
            },
            onLoadFailed = { error, _ ->
                if (requestId == latestEntriesRequestId) {
                    loadingDate = null
                }
                Log.w(TAG, "Failed to load entries for $date", error)
            }
        )
    }

      suspend fun addEntry(entry: String, customTime: String? = null) {
            entryCoordinator.addEntry(entry, customTime)
            val date = _uiState.value.selectedDate
            appLogRepository.logInfo(
                event = "Entry added",
                details = "date=$date chars=${entry.length} customTime=${customTime != null}"
            )
            updateLoadedStatisticsForChangedDate(date)
            val entriesSnapshot = _uiState.value.entries
            val dayLabelSnapshot = _currentDayLabel.value
            viewModelScope.launch {
              invalidateLookbackSnapshotCache(lookbackSnapshotCache, date)
              highlightsJob = refreshFavoritedHighlightsWorkflow(
                  scope = viewModelScope,
                  currentJob = highlightsJob,
                  starredDates = _starredDates.value,
                  uiState = _uiState
              )
              emitTaskerEntrySavedEvent(
                  context = fileManager.getContext(),
                  date = date,
                  entries = entriesSnapshot,
                  sourceEntry = entry,
                  dayLabel = dayLabelSnapshot,
                  customTime = customTime,
                  isEdit = false
              )
          }
      }

    fun startEditing(entry: String, index: Int) = entryCoordinator.startEditing(entry, index)

    fun clearEditing() = entryCoordinator.clearEditing()

      suspend fun updateEntry(newEntry: String, customTime: String? = null) {
            val editedIndex = _uiState.value.editingIndex
            entryCoordinator.updateEntry(newEntry, customTime)
            val date = _uiState.value.selectedDate
            appLogRepository.logInfo(
                event = "Entry updated",
                details = "date=$date index=$editedIndex chars=${newEntry.length} customTime=${customTime != null}"
            )
            updateLoadedStatisticsForChangedDate(date)
            val entriesSnapshot = _uiState.value.entries
            val dayLabelSnapshot = _currentDayLabel.value
            viewModelScope.launch {
              invalidateLookbackSnapshotCache(lookbackSnapshotCache, date)
              highlightsJob = refreshFavoritedHighlightsWorkflow(
                  scope = viewModelScope,
                  currentJob = highlightsJob,
                  starredDates = _starredDates.value,
                  uiState = _uiState
              )
              emitTaskerEntrySavedEvent(
                  context = fileManager.getContext(),
                  date = date,
                  entries = entriesSnapshot,
                  sourceEntry = newEntry,
                  dayLabel = dayLabelSnapshot,
                  customTime = customTime,
                  isEdit = true,
                  entryIndexHint = editedIndex
              )
          }
      }

      fun deleteEntry(index: Int) {
            viewModelScope.launch {
                  val deletedEntry = _uiState.value.entries.getOrNull(index)
                  val date = _uiState.value.selectedDate
                  val deleted =
                      runCatching { entryCoordinator.deleteEntry(index) }
                          .onFailure { error ->
                              Log.e(TAG, "Entry delete failed for $date index=$index", error)
                          }
                          .getOrDefault(false)
                  if (!deleted) {
                      appLogRepository.logWarning(
                          event = "Entry delete failed safely",
                          details = "date=$date index=$index"
                      )
                      loadEntries(date, showLoading = false)
                      _toastEvents.emit("Delete failed. Entry was kept safely.")
                      return@launch
                  }
                  appLogRepository.logInfo(
                      event = "Entry deleted",
                      details = "date=$date index=$index hadEntry=${deletedEntry != null}"
                   )
                   updateLoadedStatisticsForChangedDate(date)
                   val dayLabelSnapshot = _currentDayLabel.value
                   viewModelScope.launch {
                     invalidateLookbackSnapshotCache(lookbackSnapshotCache, date)
                   highlightsJob = refreshFavoritedHighlightsWorkflow(
                      scope = viewModelScope,
                      currentJob = highlightsJob,
                      starredDates = _starredDates.value,
                      uiState = _uiState
                  )
                   deletedEntry?.let {
                       emitTaskerEntryDeletedEvent(
                           context = fileManager.getContext(),
                           date = date,
                           sourceEntry = it,
                           dayLabel = dayLabelSnapshot
                       )
                   }
              }
          }
      }

      fun deleteEntries(indices: Set<Int>) {
            if (indices.isEmpty()) return
            viewModelScope.launch {
                  val date = _uiState.value.selectedDate
                  val deleted =
                      runCatching { entryCoordinator.deleteEntries(indices) }
                          .onFailure { error ->
                              Log.e(TAG, "Batch entry delete failed for $date indices=$indices", error)
                          }
                          .getOrDefault(false)
                  if (!deleted) {
                      appLogRepository.logWarning(
                          event = "Batch entry delete failed safely",
                          details = "date=$date count=${indices.size}"
                      )
                      loadEntries(date, showLoading = false)
                      _toastEvents.emit("Delete failed. Entries were kept safely.")
                      return@launch
                  }
                  appLogRepository.logInfo(
                      event = "Entries pending delete",
                      details = "date=$date count=${indices.size}"
                  )
            }
      }

      suspend fun deleteEntryAndWait(index: Int) {
              val deletedEntry = _uiState.value.entries.getOrNull(index)
              val date = _uiState.value.selectedDate
              val deleted =
                  runCatching { entryCoordinator.deleteEntry(index) }
                      .onFailure { error ->
                          Log.e(TAG, "Entry delete failed for $date index=$index", error)
                      }
                      .getOrDefault(false)
              if (!deleted) {
                  appLogRepository.logWarning(
                      event = "Entry delete failed safely",
                      details = "date=$date index=$index"
                  )
                  loadEntries(date, showLoading = false)
                  _toastEvents.emit("Delete failed. Entry was kept safely.")
                  return
              }
              appLogRepository.logInfo(
                  event = "Entry deleted",
                  details = "date=$date index=$index hadEntry=${deletedEntry != null}"
               )
               updateLoadedStatisticsForChangedDate(date)
               val dayLabelSnapshot = _currentDayLabel.value
               viewModelScope.launch {
                 invalidateLookbackSnapshotCache(lookbackSnapshotCache, date)
               highlightsJob = refreshFavoritedHighlightsWorkflow(
                  scope = viewModelScope,
                  currentJob = highlightsJob,
                  starredDates = _starredDates.value,
                  uiState = _uiState
              )
               deletedEntry?.let {
                   emitTaskerEntryDeletedEvent(
                       context = fileManager.getContext(),
                       date = date,
                       sourceEntry = it,
                       dayLabel = dayLabelSnapshot
                   )
               }
          }
      }

    /** Restore the most recently deleted entry at its original position. */
    fun undoDelete() {
        viewModelScope.launch {
              val cancelledPendingDeleteCount = entryCoordinator.undoDelete()
              if (cancelledPendingDeleteCount > 0) {
                  _toastEvents.emit(
                      if (cancelledPendingDeleteCount == 1) {
                          "No Entry Deleted"
                      } else {
                          "No Entries Deleted"
                      }
                  )
                  return@launch
              }
              updateLoadedStatisticsForChangedDate(_uiState.value.selectedDate)
              invalidateLookbackSnapshotCache(lookbackSnapshotCache, _uiState.value.selectedDate)
            highlightsJob = refreshFavoritedHighlightsWorkflow(
                scope = viewModelScope,
                currentJob = highlightsJob,
                starredDates = _starredDates.value,
                uiState = _uiState
            )
        }
    }

    /** Called when the UNDO window expires without user action. */
    fun clearLastDeleted() = entryCoordinator.clearLastDeleted()

    fun finalizeDeleteCountdown() {
        viewModelScope.launch {
            val commitResult =
                runCatching { entryCoordinator.commitPendingDelete() }
                    .onFailure { error ->
                        Log.e(TAG, "Pending entry delete failed", error)
                        _toastEvents.emit(
                            if (error.message == "Bulk Deletion Failed") {
                                "Bulk Deletion Failed"
                            } else {
                                "Delete failed. Entries were kept safely."
                            }
                        )
                    }
                    .getOrNull()
            if (commitResult == null) {
                entryCoordinator.clearLastDeleted()
                return@launch
            }
            appLogRepository.logInfo(
                event = "Pending entries deleted",
                details = "date=${commitResult.date} count=${commitResult.deletedCount}"
            )
            _toastEvents.emit(
                if (commitResult.deletedCount == 1) {
                    "Entry Deleted"
                } else {
                    "Entries Deleted"
                }
            )
            updateLoadedStatisticsForChangedDate(commitResult.date)
            invalidateLookbackSnapshotCache(lookbackSnapshotCache, commitResult.date)
            highlightsJob = refreshFavoritedHighlightsWorkflow(
                scope = viewModelScope,
                currentJob = highlightsJob,
                starredDates = _starredDates.value,
                uiState = _uiState
            )
        }
    }

    fun refreshCalendarDates(forceRefresh: Boolean = false) {
        calendarDatesJob?.cancel()
        calendarDatesJob = viewModelScope.launch {
            refreshCalendarDatesNow(forceRefresh = forceRefresh)
        }
    }

    private fun queuePostLaunchRefreshWork(date: LocalDate, dateCount: Int) {
        val safeMode =
            dateCount >= LARGE_JOURNAL_DATE_THRESHOLD && settingsRepository.largeJournalSafeMode.value
        deferredStartupJob = launchStartupRefreshQueueWorkflow(
            currentJob = deferredStartupJob,
            scope = viewModelScope,
            tasks = listOf(
                StartupRefreshTask(
                    name = "journalMeta",
                    label = "Loading labels and follow-ups",
                    priority = StartupRefreshPriority.VISIBLE_STATE,
                    timeoutMs = if (safeMode) 12_000L else 8_000L,
                    block = ::refreshJournalMetaNow
                ),
                StartupRefreshTask(
                    name = "calendarDates",
                    label = "Refreshing journal dates",
                    priority = StartupRefreshPriority.VISIBLE_STATE,
                    timeoutMs = if (safeMode) 14_000L else 10_000L,
                    block = { refreshCalendarDatesNow(forceRefresh = false) }
                ),
                StartupRefreshTask(
                    name = "lookback",
                    label = "Preparing lookback",
                    priority = StartupRefreshPriority.SECONDARY_STATE,
                    timeoutMs = if (safeMode) 16_000L else 12_000L,
                    block = { refreshLookbackNow(date) }
                ),
                StartupRefreshTask(
                    name = "highlights",
                    label = "Preparing highlights",
                    priority = StartupRefreshPriority.SECONDARY_STATE,
                    timeoutMs = 5_000L,
                    block = { refreshHighlightsNow() }
                ),
                StartupRefreshTask(
                    name = "monthlyStats",
                    label = "Preparing monthly stats",
                    priority = StartupRefreshPriority.BACKGROUND_STATE,
                    timeoutMs = if (safeMode) 16_000L else 12_000L,
                    block = { refreshMonthlyStatsNow(force = false) }
                )
            ),
            initialDelayMs = if (safeMode) 1_200L else 450L,
            betweenTaskDelayMs = if (safeMode) 220L else 100L,
            backgroundWorkLabel = _backgroundWorkLabel,
            logPerf = ::logPerf,
            logWarning = { event, details ->
                appLogRepository.logWarning(event = event, details = details)
            }
        )
    }

    private suspend fun refreshJournalMetaNow() {
        val snapshot = withContext(Dispatchers.IO) {
            loadJournalMetaRefreshSnapshot(
                fileManager = fileManager,
                selectedDate = _uiState.value.selectedDate
            )
        }
        applyJournalMetaRefreshSnapshot(
            snapshot = snapshot,
            starredDates = _starredDates,
            favoritedDates = _favoritedDates,
            starredLabels = _starredLabels,
            revisitMarkers = _revisitMarkers,
            revisitTargetDates = _revisitTargetDates,
            currentRevisitDate = _currentRevisitDate,
            currentRevisitNote = _currentRevisitNote,
            dueRevisits = _dueRevisits
        )
    }

    private suspend fun refreshCalendarDatesNow(forceRefresh: Boolean) {
        val startedAt = System.currentTimeMillis()
        val dates = withContext(Dispatchers.IO) {
            fileManager.getAllJournalDatesLightweight(forceRefresh = forceRefresh)
        }
        val stats = calculateMonthlyEntryStats(dates)
        val yStats = calculateYearlyEntryStats(dates)
        _uiState.update {
            it.copy(datesWithEntries = dates, monthlyStats = stats, yearlyStats = yStats)
        }
        logPerf(
            if (forceRefresh) "refreshCalendarDates.force" else "refreshCalendarDates.cached",
            System.currentTimeMillis() - startedAt
        )
    }

    private suspend fun refreshLookbackNow(date: LocalDate) {
        val knownDates = _uiState.value.datesWithEntries
        val availableDates =
            if (knownDates.isNotEmpty()) {
                knownDates
            } else {
                withContext(Dispatchers.IO) {
                    fileManager.getAllJournalDatesLightweight(forceRefresh = false)
                }
            }
        val lookbackMap = buildLookbackSnapshot(
            date = date,
            availableDates = availableDates,
            entriesForDateProvider = fileManager::getEntriesForDate
        )
        synchronized(lookbackSnapshotCache) {
            lookbackSnapshotCache[date] = lookbackMap
        }
        _uiState.update { it.copy(lookbackEntries = lookbackMap) }
    }

    private fun refreshHighlightsNow() {
        _uiState.update { it.copy(favoritedHighlights = buildFavoritedHighlights(_starredDates.value)) }
    }

    private suspend fun refreshMonthlyStatsNow(force: Boolean) {
        val now = System.currentTimeMillis()
        val currentMonth = YearMonth.now()
        if (!shouldRefreshMonthlyStats(
                force = force,
                currentStats = _monthlyStats.value,
                lastMonthlyStatsMonth = lastMonthlyStatsMonth,
                currentMonth = currentMonth,
                now = now,
                lastMonthlyStatsRefreshAt = lastMonthlyStatsRefreshAt,
                freshnessMs = MONTHLY_STATS_FRESHNESS_MS
            )
        ) {
            return
        }
        _monthlyStats.value = buildMonthlyStatsSnapshot(
            fileManager = fileManager,
            currentMonth = currentMonth
        )
        lastMonthlyStatsMonth = currentMonth
        lastMonthlyStatsRefreshAt = System.currentTimeMillis()
    }

    /** Returns a random past date that has entries, or null if fewer than 50 past entries exist. */
    fun surpriseMe(): LocalDate? {
        return pickSurpriseJournalDate(_uiState.value.datesWithEntries)
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
                    delay(300) // debounce â€” cancelled if user types another character
                    val results = withContext(Dispatchers.IO) { fileManager.searchEntries(query) }
                    _uiState.update { it.copy(searchResults = results) }
                }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    suspend fun getTimelineMetrics(
        dates: List<LocalDate>
    ): Map<LocalDate, DailyJournalMetrics> = withContext(Dispatchers.IO) {
        if (dates.isEmpty()) emptyMap() else LinkedHashMap(fileManager.getDailyMetricsSnapshotForDates(dates))
    }

    suspend fun getTimelinePreview(date: LocalDate): String? = withContext(Dispatchers.IO) {
        fileManager.getEntriesForDate(date).firstOrNull()
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            _toastEvents.emit("Preparing backup...")
            _backgroundWorkLabel.value = "Preparing backup"
            _syncProgress.value = 0.02f
            try {
                val shortcodes = settingsRepository.customShortcodes.value
                val dateKeywords = settingsRepository.customDateKeywords.value
                val keywordList = keywordRepository.keywords.value
                val recurringTaskList = recurringTaskRepository.exportAll()
                val result = withContext(Dispatchers.IO) {
                    fileManager.createBackupZip(shortcodes, dateKeywords, keywordList, recurringTaskList) { current, total ->
                        val scanProgress = current.toFloat() / total.coerceAtLeast(1).toFloat()
                        _backgroundWorkLabel.value = "Backing up data"
                        _syncProgress.value = 0.05f + (scanProgress * 0.82f)
                    }
                }
                _backgroundWorkLabel.value = "Preparing share"
                _syncProgress.value = 0.95f
                if (result?.uri != null) {
                    settingsRepository.setLastBackupTimestamp(System.currentTimeMillis())
                    _toastEvents.emit("Backup ready (${formatBackupSize(result.sizeBytes)}).")
                    val sendIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, result.uri)
                            putExtra(Intent.EXTRA_TITLE, "Yaja backup")
                            putExtra(Intent.EXTRA_SUBJECT, "Yaja backup")
                            clipData = ClipData.newUri(
                                context.contentResolver,
                                "Yaja backup",
                                result.uri
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    grantBackupReadPermission(context, result.uri, sendIntent)
                    val chooser = Intent.createChooser(sendIntent, "Share Backup").apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    _syncProgress.value = 1f
                    delay(250)
                    context.startActivity(chooser)
                } else if (result != null) {
                    _toastEvents.emit("Backup created (${formatBackupSize(result.sizeBytes)}), but share menu couldn't open.")
                } else {
                    _toastEvents.emit("Backup couldn't be created.")
                }
            } catch (e: Exception) {
                _toastEvents.emit(e.message ?: "Backup couldn't be created.")
            } finally {
                _syncProgress.value = null
                _backgroundWorkLabel.value = null
            }
        }
    }

    private fun grantBackupReadPermission(context: Context, uri: Uri, sendIntent: Intent) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.packageManager
            .queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .forEach { resolved ->
                context.grantUriPermission(resolved.activityInfo.packageName, uri, flags)
            }
    }

    private fun formatBackupSize(sizeBytes: Long): String {
        val kb = sizeBytes / 1024.0
        return if (kb < 1024.0) {
            String.format(java.util.Locale.US, "%.1f KB", kb)
        } else {
            String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
        }
    }

    fun exportObsidianVault(treeUri: Uri, context: Context) {
        viewModelScope.launch {
            _toastEvents.emit("Preparing Obsidian export...")
            _backgroundWorkLabel.value = "Preparing export"
            _syncProgress.value = 0.02f
            try {
                val exporter = com.mj.yaja.data.ObsidianExporter(
                    context.applicationContext,
                    fileManager,
                    keywordRepository,
                    keywordMatchCache
                )
                val dates = withContext(Dispatchers.IO) { fileManager.getAllJournalDatesLightweight() }
                val result = withContext(Dispatchers.IO) {
                    exporter.exportToTreeUri(treeUri, dates) { current, total ->
                        val progress = current.toFloat() / total.coerceAtLeast(1).toFloat()
                        _backgroundWorkLabel.value = "Exporting to vault"
                        _syncProgress.value = 0.05f + (progress * 0.9f)
                    }
                }
                _syncProgress.value = 1f
                _toastEvents.emit(
                    if (result.cancelled) {
                        "Export cancelled after ${result.daysExported} day(s)."
                    } else {
                        "Exported ${result.daysExported} day(s) to Obsidian vault."
                    }
                )
            } catch (e: Exception) {
                _toastEvents.emit(e.message ?: "Obsidian export couldn't be created.")
            } finally {
                _syncProgress.value = null
                _backgroundWorkLabel.value = null
            }
        }
    }

    fun restoreBackupZip(uri: android.net.Uri, context: android.content.Context) {
        if (!canStartImport(_importState.value)) return
        importJob?.cancel()
        importJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val bundle = fileManager.readBackupZip(uri)
                    ?: throw IllegalStateException("Couldn't read this backup ZIP")
                runRestoreBackupWorkflow(
                    bundle = bundle,
                    processRestoreBundle = { restoreBundle, publishProgress ->
                        processRestoreBundle(
                            bundle = restoreBundle,
                            getEntriesForDate = fileManager::getEntriesForDate,
                            setEntriesForDate = fileManager::setEntriesForDate,
                            getDayLabel = fileManager::getDayLabel,
                            setDayLabel = fileManager::setDayLabel,
                            getRevisitDate = fileManager::getRevisitDate,
                            setRevisit = fileManager::setRevisit,
                            isDateStarred = fileManager::isDateStarred,
                            setStarred = fileManager::setStarred,
                            mergeShortcodes = { newShortcodes ->
                                mergeImportedShortcodesCountingAdded(
                                    current = settingsRepository.customShortcodes.value,
                                    incoming = newShortcodes,
                                    persist = settingsRepository::setCustomShortcodes
                                )
                            },
                            mergeDateKeywords = { importedEntries ->
                                mergeImportedDateKeywordsCountingAdded(
                                    current = settingsRepository.customDateKeywords.value,
                                    incoming = importedEntries,
                                    persist = settingsRepository::setCustomDateKeywords
                                )
                            },
                            importKeywordsIgnoringDuplicates = { importedKeywords ->
                                mergeImportedKeywordsIgnoringDuplicates(
                                    existing = keywordRepository.keywords.value,
                                    incoming = importedKeywords,
                                    importKeywords = ::importKeywords
                                )
                            },
                            importRecurringTasksIgnoringDuplicates = { importedTasks ->
                                recurringTaskRepository.importIgnoringExistingIds(importedTasks)
                            },
                            publishProgress = publishProgress
                        )
                    },
                    publishRunningState = { current, total ->
                        _importState.value = ImportState.Running(current, total)
                    },
                    clearLookbackCache = { clearLookbackSnapshotCache(lookbackSnapshotCache) },
                    reloadSelectedDate = { loadEntries(_uiState.value.selectedDate) },
                    refreshCalendarDates = { refreshCalendarDates(forceRefresh = true) },
                    refreshStarredLabels = {
                        refreshJournalMetaWorkflow(
                            scope = viewModelScope,
                            fileManager = fileManager,
                            selectedDate = _uiState.value.selectedDate,
                            starredDates = _starredDates,
                            favoritedDates = _favoritedDates,
                            starredLabels = _starredLabels,
                            revisitMarkers = _revisitMarkers,
                            revisitTargetDates = _revisitTargetDates,
                            currentRevisitDate = _currentRevisitDate,
                            currentRevisitNote = _currentRevisitNote,
                            dueRevisits = _dueRevisits
                        )
                    },
                    rebuildTodoIndex = {
                        viewModelScope.launch(Dispatchers.Default) {
                            rebuildTodoIndexWorkflow(
                                fileManager = fileManager,
                                todoIndexRepository = todoIndexRepository,
                                eventIndexRepository = eventIndexRepository,
                                emitBackgroundToast = ::emitBackgroundToast,
                                publishCurrentTodos = { publishCurrentTodoAndEventIndexes() }
                            )
                        }
                    },
                    startIncrementalWarmup = { fileManager.startIncrementalWarmup(latestFirst = true) },
                    markBackgroundRefreshComplete = {
                        settingsRepository.setLastBackgroundFullRefreshAt(System.currentTimeMillis())
                    },
                    importState = _importState,
                    restoreSummary = _restoreSummary,
                    toastEvents = _toastEvents
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Failed to restore backup ZIP")
            } finally {
                if (_importState.value is ImportState.Running) {
                    _importState.value = ImportState.Idle
                }
            }
        }
    }

    fun setThemePreference(preference: ThemePreference) = settingsFeature.setThemePreference(preference)

    fun setColorSource(source: ColorSource) = settingsFeature.setColorSource(source)

    fun setCustomPalette(palette: CustomPalette) = settingsFeature.setCustomPalette(palette)

    fun setThemeColorIntensity(intensity: ThemeColorIntensity) =
        settingsFeature.setThemeColorIntensity(intensity)

    fun setBackgroundTintLevel(level: BackgroundTintLevel) =
        settingsFeature.setBackgroundTintLevel(level)

    fun setActivePersonalThemeSlotId(slotId: Int) =
        settingsFeature.setActivePersonalThemeSlotId(slotId)

    fun renamePersonalThemeSlot(slotId: Int, name: String) =
        settingsFeature.renamePersonalThemeSlot(slotId, name)

    fun setPersonalThemeHue(slotId: Int, hue: Float) =
        settingsFeature.setPersonalThemeHue(slotId, hue)

    fun setPersonalThemeSaturation(slotId: Int, saturation: Float) =
        settingsFeature.setPersonalThemeSaturation(slotId, saturation)

    fun setPersonalThemeBrightness(slotId: Int, brightness: Float) =
        settingsFeature.setPersonalThemeBrightness(slotId, brightness)

    fun setPersonalThemeAccentStyle(slotId: Int, style: PersonalAccentStyle) =
        settingsFeature.setPersonalThemeAccentStyle(slotId, style)

    fun setAppFontFamily(fontFamily: com.mj.yaja.data.AppFontFamily) =
        settingsFeature.setAppFontFamily(fontFamily)

    fun setMonoFontWeight(weight: Int) = settingsFeature.setMonoFontWeight(weight)

    /** Copies a user-picked font file (TTF/OTF) into app storage and activates it. */
    fun setCustomFontFromUri(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val previousPath = settingsRepository.customFontPath.value
            val fontsDir = java.io.File(context.filesDir, "fonts").apply { mkdirs() }
            // Unique name per upload so the Compose font cache never serves a stale file.
            val target = java.io.File(fontsDir, "custom_font_${System.currentTimeMillis()}.ttf")
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open the selected file")
                android.graphics.Typeface.Builder(target).build()
                    ?: error("Not a valid font file")
                val displayName = context.contentResolver
                    .query(uri, null, null, null, null)
                    ?.use { cursor ->
                        val index =
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    } ?: target.name
                settingsRepository.setCustomFont(target.absolutePath, displayName)
                settingsRepository.setAppFontFamily(com.mj.yaja.data.AppFontFamily.CUSTOM)
                previousPath?.let { java.io.File(it).delete() }
            }
            if (result.isFailure) {
                target.delete()
                _toastEvents.emit("Couldn't load that file. Pick a valid .ttf or .otf font.")
            }
        }
    }

    fun clearCustomFont() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.customFontPath.value?.let { java.io.File(it).delete() }
            settingsRepository.clearCustomFont()
            if (settingsRepository.appFontFamily.value == com.mj.yaja.data.AppFontFamily.CUSTOM) {
                settingsRepository.setAppFontFamily(com.mj.yaja.data.AppFontFamily.MONO)
            }
        }
    }

    fun setEntryStyle(style: com.mj.yaja.data.EntryStyle) =
        settingsFeature.setEntryStyle(style)

    fun setUiScalePreference(preference: UiScalePreference) =
        settingsFeature.setUiScalePreference(preference)

    fun setFontScalePreference(preference: FontScalePreference) =
        settingsFeature.setFontScalePreference(preference)

    fun setAppLanguage(language: AppLanguage) =
        settingsFeature.setAppLanguage(language)

    fun setAnimationPreference(preference: AnimationPreference) =
        settingsFeature.setAnimationPreference(preference)

    fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) = settingsFeature.setFirstDayOfWeek(dayOfWeek)

    fun setDateOrderPreference(pref: com.mj.yaja.data.DateOrderPreference) =
        settingsFeature.setDateOrderPreference(pref)

    fun setCustomDateKeywords(entries: List<com.mj.yaja.data.DateKeywordEntry>) =
        settingsFeature.setCustomDateKeywords(entries)

    fun setShowTimestamps(show: Boolean) = settingsFeature.setShowTimestamps(show)
    fun setShowDayHeaderStats(show: Boolean) = settingsFeature.setShowDayHeaderStats(show)
    fun setRenderCheckboxesAsText(renderAsText: Boolean) =
        settingsFeature.setRenderCheckboxesAsText(renderAsText)

    fun setShowStatistics(show: Boolean) = settingsFeature.setShowStatistics(show)

    fun setShowLookbackInNavBar(show: Boolean) = settingsFeature.setShowLookbackInNavBar(show)

    fun setShowKeywordsInNavBar(show: Boolean) = settingsFeature.setShowKeywordsInNavBar(show)

    fun setShowTodosInNavBar(show: Boolean) = settingsFeature.setShowTodosInNavBar(show)

    fun setShowCompletedTodos(show: Boolean) = settingsFeature.setShowCompletedTodos(show)

    fun setShowStatisticsInNavBar(show: Boolean) = settingsFeature.setShowStatisticsInNavBar(show)

    fun setEnableDragAndDrop(enable: Boolean) = settingsFeature.setEnableDragAndDrop(enable)
    fun setEntryDeleteSelectionEnabled(enable: Boolean) =
            settingsFeature.setEntryDeleteSelectionEnabled(enable)

    fun setEntryReviewEnabled(enabled: Boolean) = settingsFeature.setEntryReviewEnabled(enabled)

    fun setKeywordHighlightingEnabled(enabled: Boolean) =
        settingsFeature.setKeywordHighlightingEnabled(enabled)

    fun importDayOneFile(uri: android.net.Uri, context: android.content.Context) {
        if (!canStartImport(_importState.value)) return
        importJob?.cancel()
        importJob = launchDayOneEntryImport(
            scope = viewModelScope,
            importState = _importState,
            context = context,
            uri = uri,
            fileManager = fileManager,
            onImporterChanged = { currentDayOneImporter = it },
            onImportSuccess = {
                runEntryImportSuccessRefresh(
                    clearLookbackCache = { clearLookbackSnapshotCache(lookbackSnapshotCache) },
                    forceFileRefresh = { fileManager.forceRefresh { _, _ -> } },
                    markBackgroundRefreshComplete = settingsRepository::setLastBackgroundFullRefreshAt
                )
            }
        )
    }

    fun importJournalisticFile(uri: android.net.Uri, context: android.content.Context) {
        if (!canStartImport(_importState.value)) return
        importJob?.cancel()
        importJob = launchJournalisticEntryImport(
            scope = viewModelScope,
            importState = _importState,
            context = context,
            uri = uri,
            fileManager = fileManager,
            onImporterChanged = { currentJournalisticImporter = it },
            onImportSuccess = {
                runEntryImportSuccessRefresh(
                    clearLookbackCache = { clearLookbackSnapshotCache(lookbackSnapshotCache) },
                    forceFileRefresh = { fileManager.forceRefresh { _, _ -> } },
                    markBackgroundRefreshComplete = settingsRepository::setLastBackgroundFullRefreshAt
                )
            }
        )
    }

    fun importMarkdownFolder(treeUri: android.net.Uri, context: android.content.Context) {
        if (!canStartImport(_importState.value)) return
        importJob?.cancel()
        importJob = launchMarkdownFolderEntryImport(
            scope = viewModelScope,
            importState = _importState,
            context = context,
            treeUri = treeUri,
            fileManager = fileManager,
            onImporterChanged = { currentMarkdownFolderImporter = it },
            onImportSuccess = {
                runEntryImportSuccessRefresh(
                    clearLookbackCache = { clearLookbackSnapshotCache(lookbackSnapshotCache) },
                    forceFileRefresh = { fileManager.forceRefresh { _, _ -> } },
                    markBackgroundRefreshComplete = settingsRepository::setLastBackgroundFullRefreshAt
                )
            }
        )
    }

    fun importKeywordsFromBackupZip(uri: android.net.Uri, context: android.content.Context) {
        if (!canStartImport(_importState.value)) return
        importJob?.cancel()
        importJob = launchKeywordBackupImport(
            scope = viewModelScope,
            importState = _importState,
            context = context,
            uri = uri,
            onImportKeywords = ::importKeywords
        )
    }

    fun cancelImport() {
        cancelActiveImport(
            importJob = importJob,
            dayOneImporter = currentDayOneImporter,
            journalisticImporter = currentJournalisticImporter,
            markdownFolderImporter = currentMarkdownFolderImporter
        )
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun dismissRestoreSummary() {
        _restoreSummary.value = null
    }

    /** Get the label for a starred date. */
    fun getStarredLabel(date: java.time.LocalDate): String {
        return fileManager.getStarredLabel(date)
    }

    /** Set a date as starred with an optional label (max 30 characters). */
    fun setStarredWithLabel(date: java.time.LocalDate, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val result = setStarredWithLabelMutation(
                    fileManager = fileManager,
                    settingsRepository = settingsRepository,
                    date = date,
                    label = label,
                    selectedDate = _uiState.value.selectedDate
                )
                result.selectedDayLabel?.let { updatedLabel ->
                    _currentDayLabel.value = updatedLabel
                    persistHomeSnapshotIfChanged(
                        selectedDate = date,
                        entries = _uiState.value.entries,
                        dayLabel = updatedLabel,
                        lastPersistedSnapshot = lastPersistedHomeSnapshot,
                        persistSnapshot = settingsRepository::setHomeScreenSnapshot,
                        updateLastPersistedSnapshot = { snapshot ->
                            lastPersistedHomeSnapshot = snapshot
                        }
                    )
                }
                val starredState = result.starredState ?: loadStarredStateSnapshot(fileManager)
                _starredDates.value = starredState.starredDates
                _favoritedDates.value = starredState.favoritedDates
                _starredLabels.value = starredState.starredLabels
                highlightsJob = refreshFavoritedHighlightsWorkflow(
                    scope = viewModelScope,
                    currentJob = highlightsJob,
                    starredDates = _starredDates.value,
                    uiState = _uiState
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to set starred label", e)
            }
        }
    }

    /** Un-star a date. */
    fun unStarDate(date: java.time.LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val result = unsetStarredDateMutation(
                    fileManager = fileManager,
                    settingsRepository = settingsRepository,
                    date = date,
                    selectedDate = _uiState.value.selectedDate
                )
                result.selectedDayLabel?.let { updatedLabel ->
                    _currentDayLabel.value = updatedLabel
                    persistHomeSnapshotIfChanged(
                        selectedDate = date,
                        entries = _uiState.value.entries,
                        dayLabel = updatedLabel,
                        lastPersistedSnapshot = lastPersistedHomeSnapshot,
                        persistSnapshot = settingsRepository::setHomeScreenSnapshot,
                        updateLastPersistedSnapshot = { snapshot ->
                            lastPersistedHomeSnapshot = snapshot
                        }
                    )
                }
                val starredState = result.starredState ?: loadStarredStateSnapshot(fileManager)
                _starredDates.value = starredState.starredDates
                _favoritedDates.value = starredState.favoritedDates
                _starredLabels.value = starredState.starredLabels
                highlightsJob = refreshFavoritedHighlightsWorkflow(
                    scope = viewModelScope,
                    currentJob = highlightsJob,
                    starredDates = _starredDates.value,
                    uiState = _uiState
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to unstar date", e)
            }
        }
    }

    /** Get the day label for any date (not just starred ones). */
    fun getDayLabel(date: java.time.LocalDate): String {
        return fileManager.getDayLabel(date)
    }

    /** Set or clear the day label for any date (max 30 chars). Updates all flows immediately. */
    fun setDayLabel(date: java.time.LocalDate, label: String) {
          viewModelScope.launch(Dispatchers.IO) {
              val result = applyDayLabelMutation(
                  fileManager = fileManager,
                  date = date,
                  label = label,
                  selectedDate = _uiState.value.selectedDate
              )
              result.selectedDayLabel?.let { updatedLabel ->
                  _currentDayLabel.value = updatedLabel
                  persistHomeSnapshotIfChanged(
                      selectedDate = date,
                      entries = _uiState.value.entries,
                      dayLabel = updatedLabel,
                      lastPersistedSnapshot = lastPersistedHomeSnapshot,
                      persistSnapshot = settingsRepository::setHomeScreenSnapshot,
                      updateLastPersistedSnapshot = { snapshot ->
                          lastPersistedHomeSnapshot = snapshot
                      }
                  )
              }
              _starredLabels.value = result.starredLabels ?: fileManager.getAllStarredLabels()
              highlightsJob = refreshFavoritedHighlightsWorkflow(
                  scope = viewModelScope,
                  currentJob = highlightsJob,
                  starredDates = _starredDates.value,
                  uiState = _uiState
              )
          }
      }

    fun setRevisit(date: LocalDate, revisitOn: LocalDate?, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = fileManager.setRevisit(date, revisitOn, note)
            if (saved) {
                refreshRevisitStateWorkflow(
                    fileManager = fileManager,
                    selectedDate = _uiState.value.selectedDate,
                    revisitMarkers = _revisitMarkers,
                    revisitTargetDates = _revisitTargetDates,
                    currentRevisitDate = _currentRevisitDate,
                    currentRevisitNote = _currentRevisitNote,
                    dueRevisits = _dueRevisits
                )
                if (date == _uiState.value.selectedDate) {
                    _currentRevisitDate.value = revisitOn
                    _currentRevisitNote.value = note.take(80)
                }
            } else {
                _toastEvents.emit("Follow-up was not saved. Yaja kept the file unchanged.")
            }
        }
    }

    fun setStatisticsSectionOrder(order: List<String>) {
        settingsFeature.setStatisticsSectionOrder(order)
    }

    fun setVisibleStatisticsSections(sectionNames: Set<String>) {
        settingsFeature.setVisibleStatisticsSections(sectionNames)
    }

    fun setUseMLKitDetection(enabled: Boolean) {
        settingsFeature.setUseMLKitDetection(enabled)
        calculateStatsByPeriod(com.mj.yaja.ui.screens.StatisticsPeriod.ALL_TIME)
    }

    fun setCustomShortcode(code: String, value: String) {
        val current = customShortcodes.value.toMutableMap()
        current[code] = value
        settingsFeature.setCustomShortcodes(current)
    }

    fun removeCustomShortcode(code: String) {
        val current = customShortcodes.value.toMutableMap()
        current.remove(code)
        settingsFeature.setCustomShortcodes(current)
    }

    fun reorderEntries(reorderedEntries: List<String>) {
        viewModelScope.launch {
            entryCoordinator.reorderEntries(reorderedEntries)
            appLogRepository.logInfo(
                event = "Entries reordered",
                details = "date=${_uiState.value.selectedDate} count=${reorderedEntries.size}"
            )
            updateLoadedStatisticsForChangedDate(_uiState.value.selectedDate)
        }
    }

    fun refreshTodos(forceRebuild: Boolean = false) {
        appLogRepository.logInfo(
            event = "Todo refresh requested",
            details = "forceRebuild=$forceRebuild"
        )
        if (todoRefreshJob?.isActive == true) {
            if (!forceRebuild) return
            todoRefreshJob?.cancel()
        }
        val requestId = ++latestTodoRefreshRequestId
        todoRefreshJob = viewModelScope.launch(Dispatchers.Default) {
            _todoRefreshInProgress.value = true
            try {
                refreshTodosWorkflow(
                    fileManager = fileManager,
                    todoIndexRepository = todoIndexRepository,
                    eventIndexRepository = eventIndexRepository,
                    forceRebuild = forceRebuild,
                    emitBackgroundToast = ::emitBackgroundToast,
                    publishCurrentTodos = { publishCurrentTodoAndEventIndexes() }
                )
            } finally {
                if (requestId == latestTodoRefreshRequestId) {
                    _todoRefreshInProgress.value = false
                }
            }
        }
    }

    fun ensureTodosLoaded() {
        publishCurrentTodoAndEventIndexes()
        if (!eventIndexRepository.isBuilt()) {
            refreshTodos(forceRebuild = false)
        }
    }

    fun toggleTodo(item: TodoItem) {
        optimisticallyToggleTodo(item)
        viewModelScope.launch(Dispatchers.IO) {
            todoToggleMutex.withLock {
                toggleTodoWorkflow(
                    fileManager = fileManager,
                    item = item,
                    onToggleFailed = {
                        emitBackgroundToast("Todo changed, refreshing...")
                        refreshTodos(forceRebuild = true)
                      },
                      onSelectedDateChanged = { changedItem ->
                          if (_uiState.value.selectedDate == changedItem.date) {
                              loadEntries(changedItem.date, showLoading = false)
                          }
                      },
                      publishCurrentTodos = { _todos.value = sortedTodoItems(todoIndexRepository) }
                  )
                  appLogRepository.logInfo(
                      event = "Todo toggled",
                      details = "date=${item.date} checkedFrom=${item.isChecked}"
                  )
                  updateLoadedStatisticsForChangedDate(item.date)
              }
          }
      }

    fun addTodoForDate(date: LocalDate, text: String) {
        val todoText =
            text.trim()
                .removePrefix("[ ]")
                .removePrefix("[x]")
                .removePrefix("[X]")
                .trim()
                .takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val timeString = LocalTime.now().format(timeFormatter)
                val finalEntry = "<!--time:$timeString-->\n[ ] $todoText"
                val result = fileManager.tryAddEntryForDate(date, finalEntry)
                if (!result.success) error("Todo save failed")

                publishCurrentTodoAndEventIndexes()
                if (_uiState.value.selectedDate == date) {
                    loadEntries(date, showLoading = false)
                }
                updateLoadedStatisticsForChangedDate(date)
                appLogRepository.logInfo(
                    event = "Todo added",
                    details = "date=$date chars=${todoText.length}"
                )
            }.onFailure {
                emitBackgroundToast("Couldn't save todo.")
            }
        }
    }

    fun addQuickEntryForDate(date: LocalDate, text: String, kind: EntryKind) {
        val payloadText =
            text.trim()
                .removePrefix("[ ]")
                .removePrefix("[x]")
                .removePrefix("[X]")
                .trim()
                .takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val timeString = LocalTime.now().format(timeFormatter)
                val baseEntry = when (kind) {
                    EntryKind.EVENT -> "<!--time:$timeString-->\n$payloadText"
                    EntryKind.NORMAL -> "<!--time:$timeString-->\n[ ] $payloadText"
                }
                val finalEntry =
                    if (kind == EntryKind.EVENT) {
                        applyEntryKindMetadata(baseEntry, EntryKind.EVENT)
                    } else {
                        baseEntry
                    }
                val result = fileManager.tryAddEntryForDate(date, finalEntry)
                if (!result.success) error("Quick entry save failed")

                publishCurrentTodoAndEventIndexes()
                if (_uiState.value.selectedDate == date) {
                    loadEntries(date, showLoading = false)
                }
                updateLoadedStatisticsForChangedDate(date)
                appLogRepository.logInfo(
                    event = if (kind == EntryKind.EVENT) "Event added" else "Todo added",
                    details = "date=$date chars=${payloadText.length}"
                )
            }.onFailure {
                emitBackgroundToast(
                    if (kind == EntryKind.EVENT) "Couldn't save event." else "Couldn't save todo."
                )
            }
        }
    }

    private fun optimisticallyToggleTodo(item: TodoItem) {
        _todos.value = _todos.value.map { current ->
            if (current.matchesTodoIdentity(item)) {
                current.copy(isChecked = !current.isChecked)
            } else {
                current
            }
        }
    }

    private fun TodoItem.matchesTodoIdentity(other: TodoItem): Boolean =
        date == other.date &&
            entryIndex == other.entryIndex &&
            lineIndexInEntry == other.lineIndexInEntry &&
        lineHash == other.lineHash

    private fun publishCurrentTodoAndEventIndexes() {
        _todos.value = sortedTodoItems(todoIndexRepository)
        _events.value = eventIndexRepository.getEntries()
    }

    fun importCustomShortcodes(newShortcodes: Map<String, String>) {
        mergeImportedShortcodes(
            current = customShortcodes.value,
            incoming = newShortcodes,
            persist = settingsFeature::setCustomShortcodes
        )
    }

    fun toggleFavorite(date: LocalDate) {
        settingsRepository.toggleFavorite(date)
        highlightsJob = refreshFavoritedHighlightsWorkflow(
            scope = viewModelScope,
            currentJob = highlightsJob,
            starredDates = _starredDates.value,
            uiState = _uiState
        )
    }

    fun ensureLookbackLoaded(force: Boolean = false) {
        ensureLookbackLoadedWorkflow(
            date = _uiState.value.selectedDate,
            force = force,
            lookbackSnapshotCache = lookbackSnapshotCache,
            refreshLookbackForDate = { date ->
                lookbackJob = launchLookbackRefreshWorkflow(
                    scope = viewModelScope,
                    currentJob = lookbackJob,
                    date = date,
                    uiState = _uiState,
                    lookbackSnapshotCache = lookbackSnapshotCache,
                    fileManager = fileManager
                )
            },
            refreshFavoritedHighlights = {
                highlightsJob = refreshFavoritedHighlightsWorkflow(
                    scope = viewModelScope,
                    currentJob = highlightsJob,
                    starredDates = _starredDates.value,
                    uiState = _uiState
                )
            }
        )
    }

    fun ensureStatisticsLoaded(
        period: com.mj.yaja.ui.screens.StatisticsPeriod,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        force: Boolean = false
    ) {
        val loadDecision = buildStatisticsLoadDecision(
            period = period,
            startDate = startDate,
            endDate = endDate,
            useMLKit = settingsRepository.useMLKitDetection.value,
            force = force,
            hasStats = _allTimeStats.value != null,
            isSettling = _statisticsSettling.value,
            lastRequestKey = lastStatisticsRequestKey,
            now = System.currentTimeMillis(),
            lastCompletedAt = lastStatisticsCompletedAt,
            freshnessMs = STATS_FRESHNESS_MS
        )
        if (!loadDecision.shouldLoad) {
            return
        }
        calculateStatsByPeriod(period, startDate, endDate)
    }

    fun ensureStatisticsComparisonLoaded(force: Boolean = false) {
        if (!shouldLoadStatisticsComparison(force, _statisticsComparison.value)) return

        viewModelScope.launch {
            val comparisonDates = withContext(Dispatchers.IO) {
                fileManager.getAllJournalDatesLightweight(forceRefresh = force)
            }
            val comparison = buildStatisticsComparisonSnapshot(
                fileManager = fileManager,
                knownDates = comparisonDates,
                comparisonWindowBuilder = ::buildStatisticsComparisonWindowSnapshot,
                keywordDeltaBuilder = { type, currentStart, currentEnd, previousStart, previousEnd ->
                    buildKeywordDeltaSnapshot(
                        type = type,
                        allKeywords = keywordRepository.keywords.value,
                        keywordMatchesProvider = keywordMatchCache::getMatchesForKeyword,
                        currentStart = currentStart,
                        currentEnd = currentEnd,
                        previousStart = previousStart,
                        previousEnd = previousEnd
                    )
                }
            )

            _statisticsComparison.value = comparison
        }
    }

    fun ensureHeatmapDataLoaded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!shouldLoadHeatmap(
                force = force,
                hasHeatmap = _heatmapData.value.isNotEmpty(),
                now = now,
                lastRefreshAt = lastHeatmapRefreshAt,
                freshnessMs = HEATMAP_FRESHNESS_MS,
                heatmapJobActive = heatmapJob?.isActive == true
            )
        ) {
            return
        }
        updateHeatmapData(force = force)
    }

    fun refreshStatistics(
        period: com.mj.yaja.ui.screens.StatisticsPeriod,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        includeComparison: Boolean = true,
        includeHeatmap: Boolean = true
    ) {
        calculateStatsByPeriod(
            period = period,
            startDate = startDate,
            endDate = endDate,
            showToasts = true,
            forceDateRescan = true
        )
        if (includeComparison) ensureStatisticsComparisonLoaded(force = true)
        if (includeHeatmap) ensureHeatmapDataLoaded(force = true)
    }

      fun calculateStatsByPeriod(
          period: com.mj.yaja.ui.screens.StatisticsPeriod,
          startDate: LocalDate? = null,
          endDate: LocalDate? = null,
          showToasts: Boolean = false,
          forceDateRescan: Boolean = false
    ) {
        val requestKey = buildStatisticsRequestKey(
            period = period,
            startDate = startDate,
            endDate = endDate,
              useMLKit = settingsRepository.useMLKitDetection.value
          )
          val requestId = ++latestStatisticsRequestId
          statisticsJob?.cancel()
          lastStatisticsRequestKey = requestKey
          _statisticsSettling.value = true
          _statisticsProgress.value = 0f
          if (_allTimeStats.value == null) {
              _allTimeStats.value = emptyAllTimeStatsSnapshot()
          }
        statisticsJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
            if (showToasts) {
                _toastEvents.emit("Populating Stats")
            }
            val useMLKit = settingsRepository.useMLKitDetection.value
            val textsForMLKit = if (useMLKit) mutableListOf<String>() else null

            val result = withContext(Dispatchers.IO) {
                val range =
                    resolveStatisticsDateRange(
                        period = period,
                        startDate = startDate,
                        endDate = endDate
                    )
                val rangeStart = range.start
                val rangeEnd = range.end

                // Re-listing the whole storage tree over SAF is expensive; only do it on an
                // explicit user refresh. Background freshness comes from warmup/resume refresh.
                val knownDates = fileManager.getAllJournalDatesLightweight(forceRefresh = forceDateRescan)
                val allDates = filterDatesForStatisticsRange(
                    knownDates = knownDates,
                    range = range
                )

                if (allDates.isEmpty()) {
                    return@withContext StatisticsBuildResult(
                        stats = emptyAllTimeStatsSnapshot(),
                        contributions = linkedMapOf(),
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd
                    )
                }

                val entrySnapshot = linkedMapOf<LocalDate, List<String>>()
                val metricsSnapshot = linkedMapOf<LocalDate, DailyJournalMetrics>()
                allDates.forEachIndexed { index, date ->
                    val entries = fileManager.getEntriesForDate(date)
                    if (entries.isNotEmpty()) {
                        entrySnapshot[date] = entries
                        metricsSnapshot[date] = DailyJournalMetrics(
                            entryCount = entries.size,
                            wordCount = countWordsIgnoringChecklistMarkers(entries)
                        )
                    }
                    if (index == 0 || (index + 1) % 40 == 0 || index == allDates.lastIndex) {
                        val loadProgress = ((index + 1).toFloat() / allDates.size.toFloat()) * 0.30f
                        withContext(Dispatchers.Main) {
                            _statisticsProgress.value = loadProgress.coerceIn(0.02f, 0.30f)
                        }
                    }
                }
                val populatedDates = metricsSnapshot.keys.toList()
                if (populatedDates.isEmpty()) {
                    return@withContext StatisticsBuildResult(
                        stats = emptyAllTimeStatsSnapshot(),
                        contributions = linkedMapOf(),
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd
                    )
                }

                var totalEntries = 0
                var totalWords = 0
                val dayEntryCounts = mutableMapOf<DayOfWeek, Int>()
                val monthEntryCounts = mutableMapOf<String, Int>()
                val processedDates = mutableListOf<LocalDate>()
                val processedDateSet = linkedSetOf<LocalDate>()
                var shortCount = 0
                var mediumCount = 0
                var longCount = 0
                var intenseCount = 0
                // Time-of-day distribution
                var morningCount = 0    // 05:00â€“11:59
                var afternoonCount = 0  // 12:00â€“16:59
                var eveningCount = 0    // 17:00â€“20:59
                var nightCount = 0      // 21:00â€“04:59
                val timeRegex = Regex("""<!--time:(\d{2}):\d{2}""")
                val metadataRegex = Regex("<!--.*?-->\\n?")

                val scriptCounts = mutableMapOf<String, Int>()
                val contributionSnapshot = linkedMapOf<LocalDate, DayStatisticsAnalysis>()

                // Highlighted days that fall within this period
                val allDateSet = populatedDates.toSet()
                val favoritedInPeriod = _uiState.value.favoritedHighlights
                    .count { date -> allDateSet.contains(date) }

                // Iterate through all dates and collect statistics
                for ((index, date) in populatedDates.withIndex()) {
                    val metrics = metricsSnapshot[date] ?: continue
                    val entries = entrySnapshot[date].orEmpty()
                    if (entries.isNotEmpty() && metrics.entryCount > 0) {
                        val dayAnalysis = analyzeDayForStatistics(
                            date = date,
                            metrics = metrics,
                            entries = entries,
                            useMLKit = useMLKit,
                            timeRegex = timeRegex,
                            metadataRegex = metadataRegex
                        )
                        contributionSnapshot[date] = dayAnalysis
                        processedDates += date
                        processedDateSet += date
                        totalEntries += dayAnalysis.totalEntriesDelta
                        totalWords += dayAnalysis.totalWordsDelta
                        dayAnalysis.dayOfWeek?.let { dayOfWeek ->
                            dayEntryCounts[dayOfWeek] =
                                (dayEntryCounts[dayOfWeek] ?: 0) + dayAnalysis.totalEntriesDelta
                        }
                        dayAnalysis.monthKey?.let { monthKey ->
                            monthEntryCounts[monthKey] =
                                (monthEntryCounts[monthKey] ?: 0) + dayAnalysis.totalEntriesDelta
                        }
                        shortCount += dayAnalysis.shortCountDelta
                        mediumCount += dayAnalysis.mediumCountDelta
                        longCount += dayAnalysis.longCountDelta
                        intenseCount += dayAnalysis.intenseCountDelta
                        morningCount += dayAnalysis.morningCountDelta
                        afternoonCount += dayAnalysis.afternoonCountDelta
                        eveningCount += dayAnalysis.eveningCountDelta
                        nightCount += dayAnalysis.nightCountDelta
                        dayAnalysis.mlKitTexts.forEach { text ->
                            textsForMLKit?.add(text)
                        }
                        dayAnalysis.detectedScripts.forEach { script ->
                            scriptCounts[script] = (scriptCounts[script] ?: 0) + 1
                        }
                    }

                    val processedCount = index + 1
                    val shouldPublishPartial = shouldPublishPartialStatisticsSnapshot(
                        processedCount = processedCount,
                        totalCount = populatedDates.size
                    )

                    if (shouldPublishPartial) {
                        val partialLanguageDistribution =
                            if (!useMLKit) {
                                buildLanguageDistributionSnapshot(
                                    languageCounts = scriptCounts,
                                    totalEntries = totalEntries
                                )
                            } else {
                                emptyMap()
                            }

                        val snapshot = buildAllTimeStatsSnapshot(
                            statsDates = processedDates.toList(),
                            statsDateSet = processedDateSet,
                            totalEntries = totalEntries,
                            totalWords = totalWords,
                            dayEntryCounts = dayEntryCounts,
                            monthEntryCounts = monthEntryCounts,
                            shortCount = shortCount,
                            mediumCount = mediumCount,
                            longCount = longCount,
                            intenseCount = intenseCount,
                            morningCount = morningCount,
                            afternoonCount = afternoonCount,
                            eveningCount = eveningCount,
                            nightCount = nightCount,
                            favoritedInPeriod = favoritedInPeriod,
                            languageDistribution = partialLanguageDistribution,
                            templateInsightsProvider = { totalEntriesSnapshot ->
                                buildTemplateInsightsSnapshot(
                                    totalEntries = totalEntriesSnapshot,
                                    usageCounts = settingsRepository.templateUsageCounts.value,
                                    followUpCounts = settingsRepository.templateFollowUpCounts.value
                                )
                            }
                        )

                        withContext(Dispatchers.Main) {
                            if (requestId == latestStatisticsRequestId) {
                                _allTimeStats.value = snapshot
                                val analysisProgress = buildStatisticsProgressSnapshot(
                                    processedCount = processedCount,
                                    totalCount = populatedDates.size,
                                    useMLKit = useMLKit
                                )
                                _statisticsProgress.value = 0.30f + (analysisProgress * 0.60f)
                            }
                        }
                    }
                }

                // Finalise language/script distribution (Unicode mode only; ML Kit fills later)
                val languageDistribution =
                    if (!useMLKit) {
                        buildLanguageDistributionSnapshot(
                            languageCounts = scriptCounts,
                            totalEntries = totalEntries
                        )
                    } else {
                        emptyMap()
                    }

                StatisticsBuildResult(
                    stats = buildAllTimeStatsSnapshot(
                        statsDates = populatedDates,
                        statsDateSet = populatedDates.toSet(),
                        totalEntries = totalEntries,
                        totalWords = totalWords,
                        dayEntryCounts = dayEntryCounts,
                        monthEntryCounts = monthEntryCounts,
                        shortCount = shortCount,
                        mediumCount = mediumCount,
                        longCount = longCount,
                        intenseCount = intenseCount,
                        morningCount = morningCount,
                        afternoonCount = afternoonCount,
                        eveningCount = eveningCount,
                        nightCount = nightCount,
                        favoritedInPeriod = favoritedInPeriod,
                        languageDistribution = languageDistribution,
                        templateInsightsProvider = { totalEntriesSnapshot ->
                            buildTemplateInsightsSnapshot(
                                totalEntries = totalEntriesSnapshot,
                                usageCounts = settingsRepository.templateUsageCounts.value,
                                followUpCounts = settingsRepository.templateFollowUpCounts.value
                            )
                        }
                    ),
                    contributions = contributionSnapshot,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd
                )
            }

            // Show stats immediately (language section fills after ML Kit if enabled)
            if (requestId != latestStatisticsRequestId) {
                return@launch
            }
            val stats = result.stats
            statisticsContributionCache = result.contributions
            statisticsRangeStart = result.rangeStart
            statisticsRangeEnd = result.rangeEnd
            _allTimeStats.value = stats
            lastStatisticsRequestKey = requestKey
            lastStatisticsCompletedAt = System.currentTimeMillis()
            _statisticsProgress.value =
                if (useMLKit && textsForMLKit != null && textsForMLKit.isNotEmpty()) 0.9f else 1f

            // ML Kit phase â€” runs after IO, uses proper suspension, never blocks main thread
            if (useMLKit && textsForMLKit != null && textsForMLKit.isNotEmpty()) {
                try {
                    val mlDistribution =
                        buildMlKitLanguageDistributionSnapshot(
                            texts = textsForMLKit,
                            totalEntries = stats.totalEntries,
                            executor = langDetectExecutor
                        )

                    if (requestId == latestStatisticsRequestId) {
                        _allTimeStats.value = stats.copy(languageDistribution = mlDistribution)
                        lastStatisticsRequestKey = requestKey
                        lastStatisticsCompletedAt = System.currentTimeMillis()
                        _statisticsProgress.value = 1f
                    }
                } catch (e: Exception) {
                    Log.w("JournalViewModel", "ML Kit failed, falling back to Unicode", e)
                    val fallback =
                        buildFallbackLanguageDistributionSnapshot(
                            texts = textsForMLKit,
                            totalEntries = stats.totalEntries
                        )
                    if (requestId == latestStatisticsRequestId) {
                        _allTimeStats.value = stats.copy(languageDistribution = fallback)
                        lastStatisticsRequestKey = requestKey
                        lastStatisticsCompletedAt = System.currentTimeMillis()
                        _statisticsProgress.value = 1f
                    }
                }
            }
            if (requestId == latestStatisticsRequestId) {
                _statisticsSettling.value = false
                _statisticsProgress.value = null
            }
            if (showToasts && requestId == latestStatisticsRequestId) {
                _toastEvents.emit("Stats Computed")
            }
            logPerf("statistics.total", System.currentTimeMillis() - startedAt)
            } catch (e: CancellationException) {
                if (requestId == latestStatisticsRequestId) {
                    _statisticsSettling.value = false
                    _statisticsProgress.value = null
                }
                throw e
            } catch (e: Exception) {
                Log.e("JournalViewModel", "Statistics calculation failed", e)
                if (requestId == latestStatisticsRequestId) {
                    if (_allTimeStats.value == null) {
                        _allTimeStats.value = emptyAllTimeStatsSnapshot()
                    }
                    _statisticsSettling.value = false
                    _statisticsProgress.value = null
                    _toastEvents.emit("Statistics couldn't fully load. Showing a safe fallback.")
                }
            }
        }
    }

    private fun updateLoadedStatisticsForChangedDate(date: LocalDate) {
        if (_allTimeStats.value == null || _statisticsSettling.value) return
        if (!isDateInsideLoadedStatisticsRange(date)) return
        if (statisticsContributionCache.isEmpty() && _allTimeStats.value?.totalEntries != 0) {
            lastStatisticsCompletedAt = 0L
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            statisticsIncrementalMutex.withLock {
                if (_statisticsSettling.value || !isDateInsideLoadedStatisticsRange(date)) return@withLock
                val useMLKit = settingsRepository.useMLKitDetection.value
                val entries = withContext(Dispatchers.IO) {
                    fileManager.getEntriesForDate(date)
                }
                val metrics =
                    if (entries.isEmpty()) {
                        null
                    } else {
                        DailyJournalMetrics(
                            entryCount = entries.size,
                            wordCount = countWordsIgnoringChecklistMarkers(entries)
                        )
                    }
                val updatedContributions = LinkedHashMap(statisticsContributionCache)
                if (metrics == null) {
                    updatedContributions.remove(date)
                } else {
                    updatedContributions[date] = analyzeDayForStatistics(
                        date = date,
                        metrics = metrics,
                        entries = entries,
                        useMLKit = useMLKit,
                        timeRegex = Regex("""<!--time:(\d{2}):\d{2}"""),
                        metadataRegex = Regex("<!--.*?-->\\n?")
                    )
                }

                statisticsContributionCache = updatedContributions
                val currentLanguageDistribution = _allTimeStats.value?.languageDistribution.orEmpty()
                _allTimeStats.value = buildStatsFromContributionCache(
                    contributions = updatedContributions,
                    preserveLanguageDistribution = currentLanguageDistribution,
                    useMLKit = useMLKit
                )
                updateLoadedHeatmapForChangedDate(date, metrics?.wordCount)
                lastStatisticsCompletedAt = System.currentTimeMillis()
            }
        }
    }

    private fun isDateInsideLoadedStatisticsRange(date: LocalDate): Boolean =
        (statisticsRangeStart == null || !date.isBefore(statisticsRangeStart)) &&
            (statisticsRangeEnd == null || !date.isAfter(statisticsRangeEnd))

    private fun updateLoadedHeatmapForChangedDate(date: LocalDate, wordCount: Int?) {
        if (_heatmapData.value.isEmpty()) return
        val updatedHeatmap = _heatmapData.value.toMutableMap()
        if (wordCount == null) {
            updatedHeatmap.remove(date)
        } else {
            updatedHeatmap[date] = wordCount
        }
        _heatmapData.value = updatedHeatmap
        lastHeatmapRefreshAt = System.currentTimeMillis()
    }

    private fun buildStatsFromContributionCache(
        contributions: Map<LocalDate, DayStatisticsAnalysis>,
        preserveLanguageDistribution: Map<String, Int>,
        useMLKit: Boolean
    ): com.mj.yaja.ui.screens.AllTimeStatsData {
        if (contributions.isEmpty()) return emptyAllTimeStatsSnapshot()

        val statsDates = contributions.keys.sorted()
        val dayEntryCounts = mutableMapOf<DayOfWeek, Int>()
        val monthEntryCounts = mutableMapOf<String, Int>()
        val scriptCounts = mutableMapOf<String, Int>()
        var totalEntries = 0
        var totalWords = 0
        var shortCount = 0
        var mediumCount = 0
        var longCount = 0
        var intenseCount = 0
        var morningCount = 0
        var afternoonCount = 0
        var eveningCount = 0
        var nightCount = 0

        contributions.values.forEach { dayAnalysis ->
            totalEntries += dayAnalysis.totalEntriesDelta
            totalWords += dayAnalysis.totalWordsDelta
            dayAnalysis.dayOfWeek?.let { dayOfWeek ->
                dayEntryCounts[dayOfWeek] = (dayEntryCounts[dayOfWeek] ?: 0) + dayAnalysis.totalEntriesDelta
            }
            dayAnalysis.monthKey?.let { monthKey ->
                monthEntryCounts[monthKey] = (monthEntryCounts[monthKey] ?: 0) + dayAnalysis.totalEntriesDelta
            }
            shortCount += dayAnalysis.shortCountDelta
            mediumCount += dayAnalysis.mediumCountDelta
            longCount += dayAnalysis.longCountDelta
            intenseCount += dayAnalysis.intenseCountDelta
            morningCount += dayAnalysis.morningCountDelta
            afternoonCount += dayAnalysis.afternoonCountDelta
            eveningCount += dayAnalysis.eveningCountDelta
            nightCount += dayAnalysis.nightCountDelta
            dayAnalysis.detectedScripts.forEach { script ->
                scriptCounts[script] = (scriptCounts[script] ?: 0) + 1
            }
        }

        val statsDateSet = statsDates.toSet()
        val favoritedInPeriod = _uiState.value.favoritedHighlights.count { it in statsDateSet }
        val languageDistribution =
            if (useMLKit) {
                preserveLanguageDistribution
            } else {
                buildLanguageDistributionSnapshot(
                    languageCounts = scriptCounts,
                    totalEntries = totalEntries
                )
            }

        return buildAllTimeStatsSnapshot(
            statsDates = statsDates,
            statsDateSet = statsDateSet,
            totalEntries = totalEntries,
            totalWords = totalWords,
            dayEntryCounts = dayEntryCounts,
            monthEntryCounts = monthEntryCounts,
            shortCount = shortCount,
            mediumCount = mediumCount,
            longCount = longCount,
            intenseCount = intenseCount,
            morningCount = morningCount,
            afternoonCount = afternoonCount,
            eveningCount = eveningCount,
            nightCount = nightCount,
            favoritedInPeriod = favoritedInPeriod,
            languageDistribution = languageDistribution,
            templateInsightsProvider = { totalEntriesSnapshot ->
                buildTemplateInsightsSnapshot(
                    totalEntries = totalEntriesSnapshot,
                    usageCounts = settingsRepository.templateUsageCounts.value,
                    followUpCounts = settingsRepository.templateFollowUpCounts.value
                )
            }
        )
    }

    fun updateHeatmapData(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force &&
            _heatmapData.value.isNotEmpty() &&
            now - lastHeatmapRefreshAt < HEATMAP_FRESHNESS_MS
        ) {
            return
        }
        heatmapJob?.cancel()
        heatmapJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
              try {
                  val heatmap = buildHeatmapSnapshot(
                      fileManager = fileManager,
                      knownDates = _uiState.value.datesWithEntries
                  )
                _heatmapData.value = heatmap
                lastHeatmapRefreshAt = System.currentTimeMillis()
                logPerf("heatmapData", System.currentTimeMillis() - startedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("JournalViewModel", "Heatmap update failed", e)
            }
        }
    }

    fun setStorageUri(uriString: String?) {
        storageMigrationJob?.cancel()
        storageMigrationJob = viewModelScope.launch {
            storageMigrationMutex.withLock {
                val oldUri = settingsRepository.storageUri.value
                if (oldUri == uriString) return@withLock
                appLogRepository.logInfo(
                    event = "Storage location change requested",
                    details = "fromCustom=${oldUri != null} toCustom=${uriString != null}"
                )
                _storageMigrationInProgress.value = true
                try {
                    runStorageMigrationWorkflow(
                        oldUri = oldUri,
                        newUri = uriString,
                        setLoading = { isLoading ->
                            _uiState.update { it.copy(isLoading = isLoading) }
                        },
                        migrateEntries = { previousUri, nextUri ->
                            withContext(Dispatchers.IO) {
                                // Invalidate cache before migration so it reads fresh from source.
                                fileManager.invalidateCache()
                                // Perform migration.
                                fileManager.migrateEntries(previousUri, nextUri)
                                ensureActive()

                                // MutableStateFlow and SharedPreferences writes are thread-safe.
                                settingsRepository.setStorageUri(nextUri)
                            }
                        },
                        clearLookbackCache = { clearLookbackSnapshotCache(lookbackSnapshotCache) },
                        reloadSelectedDate = { loadEntries(_uiState.value.selectedDate) },
                        refreshCalendarDates = { refreshCalendarDates(forceRefresh = true) },
                        refreshStarredLabels = {
                            refreshJournalMetaWorkflow(
                                scope = viewModelScope,
                                fileManager = fileManager,
                                selectedDate = _uiState.value.selectedDate,
                                starredDates = _starredDates,
                                favoritedDates = _favoritedDates,
                                starredLabels = _starredLabels,
                                revisitMarkers = _revisitMarkers,
                                revisitTargetDates = _revisitTargetDates,
                                currentRevisitDate = _currentRevisitDate,
                                currentRevisitNote = _currentRevisitNote,
                                dueRevisits = _dueRevisits
                            )
                        },
                        rebuildTodoIndex = {
                            viewModelScope.launch(Dispatchers.Default) {
                                rebuildTodoIndexWorkflow(
                                    fileManager = fileManager,
                                    todoIndexRepository = todoIndexRepository,
                                    eventIndexRepository = eventIndexRepository,
                                    emitBackgroundToast = ::emitBackgroundToast,
                                    publishCurrentTodos = { publishCurrentTodoAndEventIndexes() }
                                )
                            }
                        },
                        loadAllJournalDates = {
                            withContext(Dispatchers.IO) {
                                fileManager.getAllJournalDatesLightweight()
                            }
                        },
                        startIncrementalWarmup = { fileManager.startIncrementalWarmup(latestFirst = true) },
                        runDeferredStartupWork = { dateCount ->
                            runDeferredStartupWork(
                                date = _uiState.value.selectedDate,
                                dateCount = dateCount,
                                announceLargeJournal = false
                            )
                        },
                        persistJournalFingerprint = {
                            withContext(Dispatchers.IO) {
                                fileManager.persistCurrentJournalFingerprint(immediate = true)
                            }
                        },
                        markBackgroundRefreshComplete = settingsRepository::setLastBackgroundFullRefreshAt,
                        emitToast = { message -> _toastEvents.emit(message) }
                    )
                    appLogRepository.logInfo(
                        event = "Storage location change completed",
                        details = "customStorage=${settingsRepository.storageUri.value != null}"
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("JournalViewModel", "Storage migration failed", e)
                    appLogRepository.logError(
                        event = "Storage migration failed",
                        throwable = e,
                        details = "fromCustom=${oldUri != null} toCustom=${uriString != null}"
                    )
                    _toastEvents.emit("Storage change failed. Yaja kept the current data safely.")
                } finally {
                    _storageMigrationInProgress.value = false
                }
            }
        }
    }

    fun completeOnboarding() {
        settingsRepository.markOnboardingCompleted()
    }

    fun setShowOnboardingNextLaunch(enabled: Boolean) {
        settingsRepository.setShowOnboardingNextLaunch(enabled)
    }

    fun consumeOnboardingLaunchRequest() {
        settingsRepository.consumeOnboardingLaunchRequest()
    }

    fun refreshCache() {
        appLogRepository.logInfo("Cache rebuild requested")
        cacheRefreshJob?.cancel()
        val requestId = ++latestCacheRefreshRequestId
        cacheRefreshJob = viewModelScope.launch {
            runCacheRefreshWorkflow(
                fileManager = fileManager,
                selectedDate = _uiState.value.selectedDate,
                backgroundWorkLabel = _backgroundWorkLabel,
                uiState = _uiState,
                syncProgress = _syncProgress,
                toastEvents = _toastEvents,
                emitBackgroundToast = ::emitBackgroundToast,
                runSequence = { afterSequence ->
                    val outcome = runCacheRefreshSequence(
                        fileManager = fileManager,
                        selectedDate = _uiState.value.selectedDate,
                        updateProgress = { progress ->
                            if (requestId == latestCacheRefreshRequestId) {
                                _syncProgress.value = progress
                            }
                        },
                        clearLookbackCache = { clearLookbackSnapshotCache(lookbackSnapshotCache) },
                        reloadEntries = { date -> loadEntries(date) },
                        refreshCalendarDates = { refreshCalendarDates(forceRefresh = true) },
                        refreshStarredLabels = {
                            refreshJournalMetaWorkflow(
                                scope = viewModelScope,
                                fileManager = fileManager,
                                selectedDate = _uiState.value.selectedDate,
                                starredDates = _starredDates,
                                favoritedDates = _favoritedDates,
                                starredLabels = _starredLabels,
                                revisitMarkers = _revisitMarkers,
                                revisitTargetDates = _revisitTargetDates,
                                currentRevisitDate = _currentRevisitDate,
                                currentRevisitNote = _currentRevisitNote,
                                dueRevisits = _dueRevisits
                            )
                        },
                        queueDeferredStartup = { date, dateCount ->
                            runDeferredStartupWork(date, dateCount, announceLargeJournal = false)
                        }
                    )
                    afterSequence()
                    outcome
                },
                onRefreshCompleted = { completedAt ->
                    settingsRepository.setLastBackgroundFullRefreshAt(completedAt)
                },
                logError = { exception ->
                    Log.e("JournalViewModel", "Cache refresh failed", exception)
                },
                isDeferredStartupActive = { deferredStartupJob?.isActive == true },
                isRefreshCurrent = { requestId == latestCacheRefreshRequestId }
            )
        }
    }

    private fun runDeferredStartupWork(
        date: LocalDate,
        dateCount: Int,
        announceLargeJournal: Boolean
    ) {
        deferredStartupJob = launchDeferredStartupWorkflow(
            currentJob = deferredStartupJob,
            scope = viewModelScope,
            date = date,
            dateCount = dateCount,
            announceLargeJournal = announceLargeJournal,
            largeJournalDateThreshold = LARGE_JOURNAL_DATE_THRESHOLD,
            largeJournalSafeModeEnabled = settingsRepository.largeJournalSafeMode.value,
            isKeywordCacheLoaded = keywordLastIndexedAt.value != null,
            backgroundWorkLabel = _backgroundWorkLabel,
            emitBackgroundToast = ::emitBackgroundToast,
            emitToast = { message -> _toastEvents.emit(message) },
            launchLookbackRefresh = {
                lookbackJob = launchLookbackRefreshWorkflow(
                    scope = viewModelScope,
                    currentJob = lookbackJob,
                    date = date,
                    uiState = _uiState,
                    lookbackSnapshotCache = lookbackSnapshotCache,
                    fileManager = fileManager
                )
                lookbackJob
            },
            refreshHighlights = {
                highlightsJob = refreshFavoritedHighlightsWorkflow(
                    scope = viewModelScope,
                    currentJob = highlightsJob,
                    starredDates = _starredDates.value,
                    uiState = _uiState
                )
                highlightsJob
            },
            refreshMonthlyStats = {
                monthlyStatsJob = refreshMonthlyStatsWorkflow(
                    force = false,
                    currentStats = _monthlyStats.value,
                    lastMonthlyStatsMonth = lastMonthlyStatsMonth,
                    lastMonthlyStatsRefreshAt = lastMonthlyStatsRefreshAt,
                    freshnessMs = MONTHLY_STATS_FRESHNESS_MS,
                    fileManager = fileManager,
                    monthlyStatsState = _monthlyStats,
                    scope = viewModelScope,
                    currentJob = monthlyStatsJob,
                    onStatsLoaded = { refreshedMonth ->
                        lastMonthlyStatsMonth = refreshedMonth
                        lastMonthlyStatsRefreshAt = System.currentTimeMillis()
                    },
                    logPerf = ::logPerf
                )
                monthlyStatsJob
            },
            rebuildKeywords = ::rebuildKeywordIndex,
            logPerf = ::logPerf
        )
    }

    // --- Font scale settings delegation ---
    fun setDataFontScalePreference(preference: FontScalePreference) =
        settingsFeature.setDataFontScalePreference(preference)

    fun setFollowUiFontScale(follow: Boolean) =
        settingsFeature.setFollowUiFontScale(follow)
    // --- Recurring Tasks feature integration ---
    private val recurringTaskRepository = com.mj.yaja.data.RecurringTaskRepository.getInstance(fileManager.getContext())
    val recurringTasks: StateFlow<List<com.mj.yaja.data.RecurringTaskItem>> = recurringTaskRepository.items

    fun getRecurringTaskUpcomingDates(item: com.mj.yaja.data.RecurringTaskItem, limit: Int): List<LocalDate> =
        recurringTaskRepository.previewUpcomingDates(item, limit)

    fun getRecurringTaskCardSchedule(item: com.mj.yaja.data.RecurringTaskItem): com.mj.yaja.data.CardSchedule =
        recurringTaskRepository.cardSchedule(item)

    fun toggleRecurringTaskActive(id: String, active: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringTaskRepository.setActive(id, active, fileManager)
        }
    }

    fun deleteRecurringTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringTaskRepository.delete(id, fileManager)
        }
    }

    fun upsertRecurringTask(
        id: String?,
        title: String,
        description: String,
        isActive: Boolean,
        itemType: com.mj.yaja.data.RecurringTaskItemType,
        scheduleMode: com.mj.yaja.data.RecurringTaskScheduleMode,
        frequency: com.mj.yaja.data.RecurringTaskFrequency,
        dueDayOfMonth: Int?,
        dueDayOfWeek: Int?,
        leadDays: Int,
        endMode: com.mj.yaja.data.RecurringTaskEndMode,
        endDate: LocalDate?,
        endCount: Int?,
        anchorDate: LocalDate,
        startMonth: YearMonth,
        startTime: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringTaskRepository.upsert(
                id = id,
                title = title,
                description = description,
                isActive = isActive,
                itemType = itemType,
                scheduleMode = scheduleMode,
                frequency = frequency,
                dueDayOfMonth = dueDayOfMonth,
                dueDayOfWeek = dueDayOfWeek,
                leadDays = leadDays,
                endMode = endMode,
                endDate = endDate,
                endCount = endCount,
                anchorDate = anchorDate,
                startMonth = startMonth,
                startTime = startTime,
                fileManager = fileManager
            )
        }
    }

    // --- Home Screen visibility date entries refresh ---
    fun refreshSelectedDateEntries(showLoading: Boolean = false, reason: String = "unknown") {
        loadEntries(_uiState.value.selectedDate, showLoading = showLoading, reason = reason)
    }

    override fun onCleared() {
        super.onCleared()
        // Shut down the ML Kit callback executor to prevent thread leaks
        langDetectExecutor.shutdown()
    }

    class Factory(
            private val fileManager: MarkdownFileManager,
            private val settingsRepository: SettingsRepository,
            private val keywordRepository: KeywordRepository,
            private val keywordMatchCache: KeywordMatchCache
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return JournalViewModel(
                    fileManager,
                    settingsRepository,
                    keywordRepository,
                    keywordMatchCache
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
