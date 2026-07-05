package com.mj.yaja.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import org.json.JSONObject
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.mj.yaja.ui.widget.WidgetRefreshCoordinator

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class ColorSource {
    MATERIAL_YOU,
    CUSTOM
}

enum class CustomPalette {
    YAJA,
    OCEAN,
    FOREST,
    ROSE,
    AMBER,
    MONO,
    SUNSET,
    LAVENDER,
    EARTH,
    CYBER,
    PERSONAL
}

enum class ThemeColorIntensity(val level: Float) {
    MUTED(0.7f),
    NORMAL(1.0f),
    VIVID(1.18f),
    POP(1.36f)
}

enum class BackgroundTintLevel(val amount: Float) {
    CLEAN(0.02f),
    SOFT(0.06f),
    RICH(0.11f),
    DEEP(0.17f)
}

enum class PersonalAccentStyle {
    COMPLEMENTARY,
    ANALOGOUS,
    TRIADIC,
    SOFT
}

data class PersonalThemeSlot(
    val slotId: Int,
    val name: String,
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
    val accentStyle: PersonalAccentStyle
)

enum class FontScalePreference(val scale: Float) {
    SMALLER(0.8f),
    SMALL(0.92f),
    NORMAL(1.05f),
    LARGE(1.16f),
    LARGER(1.28f)
}

enum class AppFontFamily {
    SANS_SERIF,
    SERIF,
    MONO,
    CUSTOM
}

enum class EntryStyle {
    CARDS,
    FLAT
}

enum class AnimationPreference {
    FULL,
    REDUCED,
    OFF
}

/**
 * A user-defined keyword mapping a word (in any language) to an English date concept,
 * e.g. keyword="ഇന്നലെ" meaning="yesterday".
 */
data class DateKeywordEntry(val keyword: String, val meaning: String)

/** How the app interprets ambiguous numeric dates like 03/05/24. */
enum class DateOrderPreference {
    /** Read the ordering from the device's regional locale setting. */
    AUTO,
    /** Day first — DD/MM/YYYY (common in Europe, India, etc.) */
    DMY,
    /** Month first — MM/DD/YYYY (common in the US) */
    MDY
}

/** Which direction triggers the swipe-to-delete gesture. */
enum class SwipeDirection {
    /** Swipe from start → end (right). */
    START_TO_END,
    /** Swipe from end → start (left). Default. */
    END_TO_START
}

enum class NavigationChromeMode {
    FLOATING_BAR,
    EXPRESSIVE_PANEL
}

/** App display language. SYSTEM follows the device locale. */
enum class AppLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    FRENCH("fr", "Français"),
    ITALIAN("it", "Italiano"),
    GERMAN("de", "Deutsch"),
    JAPANESE("ja", "日本語"),
    SIMPLIFIED_CHINESE("zh", "简体中文"),
    HINDI("hi", "हिन्दी"),
    MALAYALAM("ml", "മലയാളം"),
    TAMIL("ta", "தமிழ்"),
    KANNADA("kn", "ಕನ್ನಡ"),
    TELUGU("te", "తెలుగు"),
    BENGALI("bn", "বাংলা"),
    MARATHI("mr", "मराठी"),
    KOREAN("ko", "한국어"),
    INDONESIAN("in", "Bahasa Indonesia"),
    TURKISH("tr", "Türkçe"),
    VIETNAMESE("vi", "Tiếng Việt"),
    RUSSIAN("ru", "Русский"),
    FILIPINO("fil", "Filipino"),
    SWAHILI("sw", "Kiswahili"),
    POLISH("pl", "Polski"),
    THAI("th", "ไทย"),
    UKRAINIAN("uk", "Українська"),
    DUTCH("nl", "Nederlands"),
    MALAY("ms", "Bahasa Melayu"),
    ROMANIAN("ro", "Română"),
    ARABIC("ar", "العربية"),
    URDU("ur", "اردو"),
    PERSIAN("fa", "فارسی"),
    HEBREW("he", "עברית"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    SWEDISH("sv", "Svenska"),
    GREEK("el", "Ελληνικά"),
    CZECH("cs", "Čeština"),
    DANISH("da", "Dansk"),
    NORWEGIAN("nb", "Norsk Bokmål"),
    FINNISH("fi", "Suomi"),
    HUNGARIAN("hu", "Magyar"),
    PUNJABI("pa", "ਪੰਜਾਬੀ"),
    GUJARATI("gu", "ગુજરાતી"),
    ODIA("or", "ଓଡ଼ିଆ"),
    AMHARIC("am", "አማርኛ"),
    ZULU("zu", "isiZulu")
}

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
            context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(getSavedThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _colorSource = MutableStateFlow(getSavedColorSource())
    val colorSource: StateFlow<ColorSource> = _colorSource.asStateFlow()

    private val _customPalette = MutableStateFlow(getSavedCustomPalette())
    val customPalette: StateFlow<CustomPalette> = _customPalette.asStateFlow()

    private val _themeColorIntensity = MutableStateFlow(getSavedThemeColorIntensity())
    val themeColorIntensity: StateFlow<ThemeColorIntensity> = _themeColorIntensity.asStateFlow()

    private val _backgroundTintLevel = MutableStateFlow(getSavedBackgroundTintLevel())
    val backgroundTintLevel: StateFlow<BackgroundTintLevel> = _backgroundTintLevel.asStateFlow()

    private val _personalThemeSlots = MutableStateFlow(getSavedPersonalThemeSlots())
    val personalThemeSlots: StateFlow<List<PersonalThemeSlot>> = _personalThemeSlots.asStateFlow()

    private val _activePersonalThemeSlotId = MutableStateFlow(getSavedActivePersonalThemeSlotId())
    val activePersonalThemeSlotId: StateFlow<Int> = _activePersonalThemeSlotId.asStateFlow()

    private val _appFontFamily = MutableStateFlow(getSavedAppFontFamily())
    val appFontFamily: StateFlow<AppFontFamily> = _appFontFamily.asStateFlow()

    private val _monoFontWeight = MutableStateFlow(getSavedMonoFontWeight())
    val monoFontWeight: StateFlow<Int> = _monoFontWeight.asStateFlow()

    private val _customFontPath = MutableStateFlow(prefs.getString(KEY_CUSTOM_FONT_PATH, null))
    val customFontPath: StateFlow<String?> = _customFontPath.asStateFlow()

    private val _customFontName = MutableStateFlow(prefs.getString(KEY_CUSTOM_FONT_NAME, null))
    val customFontName: StateFlow<String?> = _customFontName.asStateFlow()

    private val _entryStyle = MutableStateFlow(getSavedEntryStyle())
    val entryStyle: StateFlow<EntryStyle> = _entryStyle.asStateFlow()

    private val _storageUri = MutableStateFlow(getSavedStorageUri())
    val storageUri: StateFlow<String?> = _storageUri.asStateFlow()

    private val _hasCompletedOnboarding =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _showOnboardingNextLaunch =
        MutableStateFlow(prefs.getBoolean(KEY_SHOW_ONBOARDING_NEXT_LAUNCH, false))
    val showOnboardingNextLaunch: StateFlow<Boolean> = _showOnboardingNextLaunch.asStateFlow()

    private val _shouldShowOnboarding =
        MutableStateFlow(
            !_hasCompletedOnboarding.value || _showOnboardingNextLaunch.value
        )
    val shouldShowOnboarding: StateFlow<Boolean> = _shouldShowOnboarding.asStateFlow()

    private val _showTimestamps = MutableStateFlow(getSavedShowTimestamps())
    val showTimestamps: StateFlow<Boolean> = _showTimestamps.asStateFlow()

    private val _showDayHeaderStats = MutableStateFlow(getSavedShowDayHeaderStats())
    val showDayHeaderStats: StateFlow<Boolean> = _showDayHeaderStats.asStateFlow()

    private val _renderCheckboxesAsText = MutableStateFlow(getSavedRenderCheckboxesAsText())
    val renderCheckboxesAsText: StateFlow<Boolean> = _renderCheckboxesAsText.asStateFlow()

    private val _fontScalePreference = MutableStateFlow(getSavedFontScalePreference())
    val fontScalePreference: StateFlow<FontScalePreference> = _fontScalePreference.asStateFlow()

    private val _dataFontScalePreference = MutableStateFlow(getSavedDataFontScalePreference())
    val dataFontScalePreference: StateFlow<FontScalePreference> = _dataFontScalePreference.asStateFlow()

    private val _followUiFontScale = MutableStateFlow(getSavedFollowUiFontScale())
    val followUiFontScale: StateFlow<Boolean> = _followUiFontScale.asStateFlow()

    private val _appLanguage = MutableStateFlow(getCurrentAppLanguage())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _animationPreference = MutableStateFlow(getSavedAnimationPreference())
    val animationPreference: StateFlow<AnimationPreference> = _animationPreference.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(getSavedLastBackupTimestamp())
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _backupReminderDays = MutableStateFlow(getSavedBackupReminderDays())
    val backupReminderDays: StateFlow<Int> = _backupReminderDays.asStateFlow()

    private val _appLogRetentionDays = MutableStateFlow(getSavedAppLogRetentionDays())
    val appLogRetentionDays: StateFlow<Int> = _appLogRetentionDays.asStateFlow()

    private val _firstDayOfWeek = MutableStateFlow(getSavedFirstDayOfWeek())
    val firstDayOfWeek: StateFlow<DayOfWeek> = _firstDayOfWeek.asStateFlow()

    private val _dateOrderPreference = MutableStateFlow(getSavedDateOrderPreference())
    val dateOrderPreference: StateFlow<DateOrderPreference> = _dateOrderPreference.asStateFlow()

    private val _customDateKeywords = MutableStateFlow(getSavedCustomDateKeywords())
    val customDateKeywords: StateFlow<List<DateKeywordEntry>> = _customDateKeywords.asStateFlow()

    private val _favoritedDates = MutableStateFlow(getSavedFavoritedDates())
    val favoritedDates: StateFlow<Set<String>> = _favoritedDates.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeoutMinutes = MutableStateFlow(prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 5))
    val autoLockTimeoutMinutes: StateFlow<Int> = _autoLockTimeoutMinutes.asStateFlow()

    private val _allowFutureEntries = MutableStateFlow(getSavedAllowFutureEntries())
    val allowFutureEntries: StateFlow<Boolean> = _allowFutureEntries.asStateFlow()

    private val _allowTaskerAccess = MutableStateFlow(getSavedAllowTaskerAccess())
    val allowTaskerAccess: StateFlow<Boolean> = _allowTaskerAccess.asStateFlow()

    private val _allowTaskerEvents = MutableStateFlow(getSavedAllowTaskerEvents())
    val allowTaskerEvents: StateFlow<Boolean> = _allowTaskerEvents.asStateFlow()

    private val _includeEntryTextInTaskerEvents =
        MutableStateFlow(getSavedIncludeEntryTextInTaskerEvents())
    val includeEntryTextInTaskerEvents: StateFlow<Boolean> =
        _includeEntryTextInTaskerEvents.asStateFlow()

    private val _swipeToDeleteEnabled = MutableStateFlow(getSavedSwipeToDeleteEnabled())
    val swipeToDeleteEnabled: StateFlow<Boolean> = _swipeToDeleteEnabled.asStateFlow()

    private val _swipeToNavigateDatesEnabled =
            MutableStateFlow(getSavedSwipeToNavigateDatesEnabled())
    val swipeToNavigateDatesEnabled: StateFlow<Boolean> =
            _swipeToNavigateDatesEnabled.asStateFlow()

    private val _swipeDeleteDirection = MutableStateFlow(getSavedSwipeDeleteDirection())
    val swipeDeleteDirection: StateFlow<SwipeDirection> = _swipeDeleteDirection.asStateFlow()

    private val _swipeToSyncEnabled = MutableStateFlow(getSavedSwipeToSyncEnabled())
    val swipeToSyncEnabled: StateFlow<Boolean> = _swipeToSyncEnabled.asStateFlow()

    private val _largeJournalSafeMode = MutableStateFlow(getSavedLargeJournalSafeMode())
    val largeJournalSafeMode: StateFlow<Boolean> = _largeJournalSafeMode.asStateFlow()

    private val _versionHistoryEnabled = MutableStateFlow(getSavedVersionHistoryEnabled())
    val versionHistoryEnabled: StateFlow<Boolean> = _versionHistoryEnabled.asStateFlow()

    private val _versionHistoryMaxVersions = MutableStateFlow(getSavedVersionHistoryMaxVersions())
    val versionHistoryMaxVersions: StateFlow<Int> = _versionHistoryMaxVersions.asStateFlow()

    private val _versionHistoryRetentionDays = MutableStateFlow(getSavedVersionHistoryRetentionDays())
    val versionHistoryRetentionDays: StateFlow<Int> = _versionHistoryRetentionDays.asStateFlow()

    private val _lastKnownEntryCount = MutableStateFlow(prefs.getInt(KEY_LAST_ENTRY_COUNT, -1))
    val lastKnownEntryCount: StateFlow<Int> = _lastKnownEntryCount.asStateFlow()

    private val _lastBackgroundFullRefreshAt =
            MutableStateFlow(prefs.getLong(KEY_LAST_BACKGROUND_FULL_REFRESH_AT, 0L))
    val lastBackgroundFullRefreshAt: StateFlow<Long> = _lastBackgroundFullRefreshAt.asStateFlow()

    private val _widgetCornerRadius = MutableStateFlow(getSavedWidgetCornerRadius())
    val widgetCornerRadius: StateFlow<Int> = _widgetCornerRadius.asStateFlow()

    private val _showWidgetLabel = MutableStateFlow(getSavedShowWidgetLabel())
    val showWidgetLabel: StateFlow<Boolean> = _showWidgetLabel.asStateFlow()

    private val _hasActiveWidgets = MutableStateFlow(false)
    val hasActiveWidgets: StateFlow<Boolean> = _hasActiveWidgets.asStateFlow()

    private val _showBottomBar = MutableStateFlow(getSavedShowBottomBar())
    val showBottomBar: StateFlow<Boolean> = _showBottomBar.asStateFlow()

    private val _navigationChromeMode = MutableStateFlow(getSavedNavigationChromeMode())
    val navigationChromeMode: StateFlow<NavigationChromeMode> = _navigationChromeMode.asStateFlow()

    private val _showBottomPanelLabels = MutableStateFlow(getSavedShowBottomPanelLabels())
    val showBottomPanelLabels: StateFlow<Boolean> = _showBottomPanelLabels.asStateFlow()

    private val _customShortcodes = MutableStateFlow(getSavedCustomShortcodes())
    val customShortcodes: StateFlow<Map<String, String>> = _customShortcodes.asStateFlow()

    private val _recentTemplateIds = MutableStateFlow(getSavedRecentTemplateIds())
    val recentTemplateIds: StateFlow<List<String>> = _recentTemplateIds.asStateFlow()

    private val _favoriteTemplateIds = MutableStateFlow(getSavedFavoriteTemplateIds())
    val favoriteTemplateIds: StateFlow<Set<String>> = _favoriteTemplateIds.asStateFlow()

    private val _templateUsageCounts = MutableStateFlow(getSavedTemplateUsageCounts())
    val templateUsageCounts: StateFlow<Map<String, Int>> = _templateUsageCounts.asStateFlow()

    private val _templateFollowUpCounts = MutableStateFlow(getSavedTemplateFollowUpCounts())
    val templateFollowUpCounts: StateFlow<Map<String, Int>> = _templateFollowUpCounts.asStateFlow()

    private val _entryReviewEnabled = MutableStateFlow(getSavedEntryReviewEnabled())
    val entryReviewEnabled: StateFlow<Boolean> = _entryReviewEnabled.asStateFlow()

    private val _keywordHighlightingEnabled = MutableStateFlow(getSavedKeywordHighlightingEnabled())
    val keywordHighlightingEnabled: StateFlow<Boolean> = _keywordHighlightingEnabled.asStateFlow()

    private val _isPreviewLimitEnabled = MutableStateFlow(getSavedPreviewLimitEnabled())
    val isPreviewLimitEnabled: StateFlow<Boolean> = _isPreviewLimitEnabled.asStateFlow()

    private val _previewLimitLength = MutableStateFlow(getSavedPreviewLimitLength())
    val previewLimitLength: StateFlow<Int> = _previewLimitLength.asStateFlow()

    private val _showStatistics = MutableStateFlow(getSavedShowStatistics())
    val showStatistics: StateFlow<Boolean> = _showStatistics.asStateFlow()

    private val _showLookbackInNavBar = MutableStateFlow(getSavedShowLookbackInNavBar())
    val showLookbackInNavBar: StateFlow<Boolean> = _showLookbackInNavBar.asStateFlow()

    private val _showKeywordsInNavBar = MutableStateFlow(getSavedShowKeywordsInNavBar())
    val showKeywordsInNavBar: StateFlow<Boolean> = _showKeywordsInNavBar.asStateFlow()

    private val _showTodosInNavBar = MutableStateFlow(getSavedShowTodosInNavBar())
    val showTodosInNavBar: StateFlow<Boolean> = _showTodosInNavBar.asStateFlow()

    private val _showCompletedTodos = MutableStateFlow(getSavedShowCompletedTodos())
    val showCompletedTodos: StateFlow<Boolean> = _showCompletedTodos.asStateFlow()

    private val _showStatisticsInNavBar = MutableStateFlow(getSavedShowStatisticsInNavBar())
    val showStatisticsInNavBar: StateFlow<Boolean> = _showStatisticsInNavBar.asStateFlow()

    private val _enableDragAndDrop = MutableStateFlow(getSavedEnableDragAndDrop())
    val enableDragAndDrop: StateFlow<Boolean> = _enableDragAndDrop.asStateFlow()

    private val _entryDeleteSelectionEnabled =
        MutableStateFlow(getSavedEntryDeleteSelectionEnabled())
    val entryDeleteSelectionEnabled: StateFlow<Boolean> =
        _entryDeleteSelectionEnabled.asStateFlow()

    private val _statisticsSectionOrder = MutableStateFlow(getSavedStatisticsSectionOrder())
    val statisticsSectionOrder: StateFlow<List<String>> = _statisticsSectionOrder.asStateFlow()

    private val _visibleStatisticsSections = MutableStateFlow(getSavedVisibleStatisticsSections())
    val visibleStatisticsSections: StateFlow<Set<String>> = _visibleStatisticsSections.asStateFlow()

    private val _useMLKitDetection = MutableStateFlow(prefs.getBoolean(KEY_USE_MLKIT_DETECTION, false))
    val useMLKitDetection: StateFlow<Boolean> = _useMLKitDetection.asStateFlow()

    init {
        validatePersistedStorageUri()
        normalizeNavigationItems()
    }

    private fun validatePersistedStorageUri() {
        val uriString = _storageUri.value ?: return
        val hasPersistedAccess =
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri.toString() == uriString &&
                    permission.isReadPermission &&
                    permission.isWritePermission
            }
        if (!hasPersistedAccess) {
            android.util.Log.w(
                "SettingsRepository",
                "Clearing storage URI because persisted SAF permission is missing"
            )
            prefs.edit().remove(KEY_STORAGE_URI).apply()
            _storageUri.value = null
        }
    }

    fun getHomeScreenSnapshot(): HomeScreenSnapshot? {
        val raw = prefs.getString(KEY_HOME_SCREEN_SNAPSHOT, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val date = LocalDate.parse(json.getString("selectedDate"))
            val dayLabel = json.optString("dayLabel", "")
            val entriesJson = json.optJSONArray("entries") ?: org.json.JSONArray()
            val entries = buildList {
                for (i in 0 until entriesJson.length()) {
                    add(entriesJson.getString(i))
                }
            }
            HomeScreenSnapshot(
                selectedDate = date,
                entries = entries,
                dayLabel = dayLabel
            )
        } catch (_: Exception) {
            null
        }
    }

    fun setHomeScreenSnapshot(snapshot: HomeScreenSnapshot) {
        val json = JSONObject().apply {
            put("selectedDate", snapshot.selectedDate.toString())
            put("dayLabel", snapshot.dayLabel)
            put("entries", org.json.JSONArray(snapshot.entries))
        }
        prefs.edit().putString(KEY_HOME_SCREEN_SNAPSHOT, json.toString()).apply()
    }

    /** Sets a new PIN — generates a fresh salt and stores PBKDF2(pin, salt). */
    fun setPin(plain: String) {
        val salt = generateSalt()
        val hash = hashPin(plain, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, "${salt.toHex()}:$hash")
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
        _isPinEnabled.value = true
    }

    /** Disables the PIN lock and clears the stored hash. */
    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).putBoolean(KEY_PIN_ENABLED, false).apply()
        _isPinEnabled.value = false
    }

    /** Enables biometric authentication as an alternative to PIN. */
    fun enableBiometric() {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, true).apply()
        _isBiometricEnabled.value = true
    }

    /** Disables biometric authentication. */
    fun disableBiometric() {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, false).apply()
        _isBiometricEnabled.value = false
    }

    /** Sets the auto-lock timeout duration in minutes. */
    fun setAutoLockTimeout(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, minutes).apply()
        _autoLockTimeoutMinutes.value = minutes
    }

    /**
     * Returns true if [plain] matches the stored hash.
     * Transparently migrates legacy unsalted SHA-256 hashes to PBKDF2 on first successful login.
     */
    fun checkPin(plain: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false

        return if (stored.contains(':')) {
            // Modern format: "<saltHex>:<hashHex>"
            val (saltHex, hashHex) = stored.split(':', limit = 2)
            val salt = saltHex.fromHex()
            hashPin(plain, salt) == hashHex
        } else {
            // Legacy: unsalted SHA-256 — migrate on success
            val legacyHash = legacySha256(plain)
            if (legacyHash == stored) {
                // Upgrade to PBKDF2 transparently
                setPin(plain)
                true
            } else {
                false
            }
        }
    }

    /** PBKDF2WithHmacSHA256 with a caller-supplied salt. */
    private fun hashPin(plain: String, salt: ByteArray): String {
        val spec = PBEKeySpec(plain.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded.toHex()
    }

    /** Generates a cryptographically random 16-byte salt. */
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /** Legacy SHA-256 used only to detect and migrate old hashes. */
    private fun legacySha256(plain: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(plain.toByteArray())
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0) { "Odd hex length" }
        return ByteArray(length / 2) { i -> this.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    fun setThemePreference(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
        _themePreference.value = preference
    }

    fun setColorSource(source: ColorSource) {
        prefs.edit().putString(KEY_COLOR_SOURCE, source.name).apply()
        _colorSource.value = source
    }

    fun setCustomPalette(palette: CustomPalette) {
        prefs.edit().putString(KEY_CUSTOM_PALETTE, palette.name).apply()
        _customPalette.value = palette
    }

    fun setThemeColorIntensity(intensity: ThemeColorIntensity) {
        prefs.edit().putString(KEY_THEME_COLOR_INTENSITY, intensity.name).apply()
        _themeColorIntensity.value = intensity
    }

    fun setBackgroundTintLevel(level: BackgroundTintLevel) {
        prefs.edit().putString(KEY_BACKGROUND_TINT_LEVEL, level.name).apply()
        _backgroundTintLevel.value = level
    }

    fun setActivePersonalThemeSlotId(slotId: Int) {
        val normalized = slotId.coerceIn(1, 3)
        prefs.edit().putInt(KEY_ACTIVE_PERSONAL_THEME_SLOT_ID, normalized).apply()
        _activePersonalThemeSlotId.value = normalized
    }

    fun updatePersonalThemeSlot(
        slotId: Int,
        transform: (PersonalThemeSlot) -> PersonalThemeSlot
    ) {
        val normalized = slotId.coerceIn(1, 3)
        val updated = _personalThemeSlots.value.map { slot ->
            if (slot.slotId == normalized) {
                val next = transform(slot)
                next.copy(
                    slotId = normalized,
                    hue = next.hue.coerceIn(0f, 360f),
                    saturation = next.saturation.coerceIn(0f, 1f),
                    brightness = next.brightness.coerceIn(0f, 1f)
                )
            } else {
                slot
            }
        }
        persistPersonalThemeSlots(updated)
        _personalThemeSlots.value = updated
    }

    fun renamePersonalThemeSlot(slotId: Int, name: String) {
        updatePersonalThemeSlot(slotId) { it.copy(name = name.ifBlank { "Personal ${it.slotId}" }) }
    }

    fun setPersonalThemeHue(slotId: Int, hue: Float) {
        updatePersonalThemeSlot(slotId) { it.copy(hue = hue.coerceIn(0f, 360f)) }
    }

    fun setPersonalThemeSaturation(slotId: Int, saturation: Float) {
        updatePersonalThemeSlot(slotId) { it.copy(saturation = saturation.coerceIn(0f, 1f)) }
    }

    fun setPersonalThemeBrightness(slotId: Int, brightness: Float) {
        updatePersonalThemeSlot(slotId) { it.copy(brightness = brightness.coerceIn(0f, 1f)) }
    }

    fun setPersonalThemeAccentStyle(slotId: Int, style: PersonalAccentStyle) {
        updatePersonalThemeSlot(slotId) { it.copy(accentStyle = style) }
    }

    fun setAppFontFamily(fontFamily: AppFontFamily) {
        prefs.edit().putString(KEY_APP_FONT, fontFamily.name).apply()
        _appFontFamily.value = fontFamily
    }

    fun setMonoFontWeight(weight: Int) {
        val clamped = weight.coerceIn(MONO_FONT_WEIGHT_MIN, MONO_FONT_WEIGHT_MAX)
        prefs.edit().putInt(KEY_MONO_FONT_WEIGHT, clamped).apply()
        _monoFontWeight.value = clamped
    }

    fun setCustomFont(path: String, displayName: String) {
        prefs.edit()
                .putString(KEY_CUSTOM_FONT_PATH, path)
                .putString(KEY_CUSTOM_FONT_NAME, displayName)
                .apply()
        _customFontPath.value = path
        _customFontName.value = displayName
    }

    fun clearCustomFont() {
        prefs.edit()
                .remove(KEY_CUSTOM_FONT_PATH)
                .remove(KEY_CUSTOM_FONT_NAME)
                .apply()
        _customFontPath.value = null
        _customFontName.value = null
    }

    fun setEntryStyle(style: EntryStyle) {
        prefs.edit().putString(KEY_ENTRY_STYLE, style.name).apply()
        _entryStyle.value = style
    }

    fun setStorageUri(uriString: String?) {
        if (uriString == null) {
            prefs.edit().remove(KEY_STORAGE_URI).apply()
        } else {
            prefs.edit().putString(KEY_STORAGE_URI, uriString).apply()
        }
        _storageUri.value = uriString
    }

    fun markOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        _hasCompletedOnboarding.value = true
        _shouldShowOnboarding.value = false
    }

    fun setShowOnboardingNextLaunch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ONBOARDING_NEXT_LAUNCH, enabled).apply()
        _showOnboardingNextLaunch.value = enabled
    }

    fun consumeOnboardingLaunchRequest() {
        if (!_showOnboardingNextLaunch.value) return
        prefs.edit().putBoolean(KEY_SHOW_ONBOARDING_NEXT_LAUNCH, false).apply()
        _showOnboardingNextLaunch.value = false
    }

    fun setShowTimestamps(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TIMESTAMPS, show).apply()
        _showTimestamps.value = show
    }

    fun setShowDayHeaderStats(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DAY_HEADER_STATS, show).apply()
        _showDayHeaderStats.value = show
    }

    fun setRenderCheckboxesAsText(renderAsText: Boolean) {
        prefs.edit().putBoolean(KEY_RENDER_CHECKBOXES_AS_TEXT, renderAsText).apply()
        _renderCheckboxesAsText.value = renderAsText
    }

    fun setShowBottomBar(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BOTTOM_BAR, show).apply()
        _showBottomBar.value = show
    }

    fun setNavigationChromeMode(mode: NavigationChromeMode) {
        prefs.edit().putString(KEY_NAVIGATION_CHROME_MODE, mode.name).apply()
        _navigationChromeMode.value = mode
        normalizeNavigationItems()
    }

    fun setShowBottomPanelLabels(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BOTTOM_PANEL_LABELS, show).apply()
        _showBottomPanelLabels.value = show
    }

    fun setFontScalePreference(preference: FontScalePreference) {
        prefs.edit().putString(KEY_FONT_SCALE, preference.name).apply()
        _fontScalePreference.value = preference
    }

    fun setDataFontScalePreference(preference: FontScalePreference) {
        prefs.edit().putString(KEY_DATA_FONT_SCALE, preference.name).apply()
        _dataFontScalePreference.value = preference
    }

    fun setFollowUiFontScale(follow: Boolean) {
        prefs.edit().putBoolean(KEY_FOLLOW_UI_FONT_SCALE, follow).apply()
        _followUiFontScale.value = follow
    }

    fun setAppLanguage(language: AppLanguage) {
        val localeList = language.tag?.let { LocaleListCompat.forLanguageTags(it) }
                ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(localeList)
        _appLanguage.value = language
    }

    fun setAnimationPreference(preference: AnimationPreference) {
        prefs.edit().putString(KEY_ANIMATION_PREFERENCE, preference.name).apply()
        _animationPreference.value = preference
    }

    fun setLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
        _lastBackupTimestamp.value = timestamp
    }

    fun setBackupReminderDays(days: Int) {
        val clamped = days.coerceIn(0, 30)
        prefs.edit().putInt(KEY_BACKUP_REMINDER_DAYS, clamped).apply()
        _backupReminderDays.value = clamped
    }

    fun setAppLogRetentionDays(days: Int) {
        val clamped = days.coerceIn(1, 30)
        prefs.edit().putInt(KEY_APP_LOG_RETENTION_DAYS, clamped).apply()
        _appLogRetentionDays.value = clamped
    }

    fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
        prefs.edit().putString(KEY_FIRST_DAY_OF_WEEK, dayOfWeek.name).apply()
        _firstDayOfWeek.value = dayOfWeek
    }

    fun setDateOrderPreference(pref: DateOrderPreference) {
        prefs.edit().putString(KEY_DATE_ORDER, pref.name).apply()
        _dateOrderPreference.value = pref
    }

    fun setCustomDateKeywords(entries: List<DateKeywordEntry>) {
        prefs.edit().putString(KEY_CUSTOM_DATE_KEYWORDS, serializeDateKeywords(entries)).apply()
        _customDateKeywords.value = entries
    }

    fun setAllowFutureEntries(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_FUTURE_ENTRIES, allow).apply()
        _allowFutureEntries.value = allow
    }

    fun setAllowTaskerAccess(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_TASKER_ACCESS, allow).apply()
        _allowTaskerAccess.value = allow
    }

    fun setAllowTaskerEvents(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_TASKER_EVENTS, allow).apply()
        _allowTaskerEvents.value = allow
    }

    fun setIncludeEntryTextInTaskerEvents(include: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_ENTRY_TEXT_IN_TASKER_EVENTS, include).apply()
        _includeEntryTextInTaskerEvents.value = include
    }

    fun setSwipeToDeleteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_TO_DELETE, enabled).apply()
        _swipeToDeleteEnabled.value = enabled
        if (enabled && _swipeToNavigateDatesEnabled.value) {
            prefs.edit().putBoolean(KEY_SWIPE_TO_NAVIGATE_DATES, false).apply()
            _swipeToNavigateDatesEnabled.value = false
        }
    }

    fun setSwipeToNavigateDatesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_TO_NAVIGATE_DATES, enabled).apply()
        _swipeToNavigateDatesEnabled.value = enabled
        if (enabled && _swipeToDeleteEnabled.value) {
            prefs.edit().putBoolean(KEY_SWIPE_TO_DELETE, false).apply()
            _swipeToDeleteEnabled.value = false
        }
    }

    fun setSwipeDeleteDirection(direction: SwipeDirection) {
        prefs.edit().putString(KEY_SWIPE_DELETE_DIRECTION, direction.name).apply()
        _swipeDeleteDirection.value = direction
    }

    fun setSwipeToSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_TO_SYNC, enabled).apply()
        _swipeToSyncEnabled.value = enabled
    }

    fun setLargeJournalSafeMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LARGE_JOURNAL_SAFE_MODE, enabled).apply()
        _largeJournalSafeMode.value = enabled
    }

    fun setVersionHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VERSION_HISTORY_ENABLED, enabled).apply()
        _versionHistoryEnabled.value = enabled
    }

    fun setVersionHistoryMaxVersions(count: Int) {
        val clamped = count.coerceIn(1, 10)
        prefs.edit().putInt(KEY_VERSION_HISTORY_MAX_VERSIONS, clamped).apply()
        _versionHistoryMaxVersions.value = clamped
    }

    fun setVersionHistoryRetentionDays(days: Int) {
        val clamped = days.coerceIn(2, 30)
        prefs.edit().putInt(KEY_VERSION_HISTORY_RETENTION_DAYS, clamped).apply()
        _versionHistoryRetentionDays.value = clamped
    }

    fun setLastKnownEntryCount(count: Int) {
        prefs.edit().putInt(KEY_LAST_ENTRY_COUNT, count).apply()
        _lastKnownEntryCount.value = count
    }

    fun setLastBackgroundFullRefreshAt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_FULL_REFRESH_AT, timestamp).apply()
        _lastBackgroundFullRefreshAt.value = timestamp
    }

    fun setWidgetCornerRadius(radius: Int) {
        prefs.edit().putInt(KEY_WIDGET_CORNER_RADIUS, radius).apply()
        _widgetCornerRadius.value = radius
        WidgetRefreshCoordinator.requestQuickCaptureUpdate(context)
    }

    fun setShowWidgetLabel(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_WIDGET_LABEL, show).apply()
        _showWidgetLabel.value = show
        WidgetRefreshCoordinator.requestQuickCaptureUpdate(context)
    }

    fun refreshActiveWidgetsStatus() {
        try {
            val manager = android.appwidget.AppWidgetManager.getInstance(context)
            if (manager == null) {
                _hasActiveWidgets.value = false
                return
            }
            val quickCaptureComponent =
                    android.content.ComponentName(
                            context,
                            "com.mj.yaja.ui.widget.QuickCaptureWidgetProvider"
                    )
            val quickTodoComponent =
                    android.content.ComponentName(
                            context,
                            "com.mj.yaja.ui.widget.QuickTodoWidgetProvider"
                    )
            val quickCaptureIds = manager.getAppWidgetIds(quickCaptureComponent)
            val quickTodoIds = manager.getAppWidgetIds(quickTodoComponent)
            _hasActiveWidgets.value =
                    (quickCaptureIds != null && quickCaptureIds.isNotEmpty()) ||
                            (quickTodoIds != null && quickTodoIds.isNotEmpty())
        } catch (e: Throwable) {
            android.util.Log.e("SettingsRepository", "Error refreshing widgets", e)
            _hasActiveWidgets.value = false
        }
    }

    /** Invalidates the heatmap word-count cache and broadcasts a widget update. */
    fun requestHeatmapWidgetUpdate() {
        try {
            WidgetRefreshCoordinator.requestHeatmapUpdate(context, invalidateCache = true)
        } catch (e: Throwable) {
            android.util.Log.e("SettingsRepository", "Error updating heatmap widget", e)
        }
    }

    fun toggleFavorite(date: LocalDate) {
        val dateStr = date.toString()
        _favoritedDates.update { current ->
            val updated = current.toMutableSet()
            if (!updated.add(dateStr)) updated.remove(dateStr)
            prefs.edit().putStringSet(KEY_FAVORITED_DATES, updated).apply()
            updated
        }
    }

    fun setFavoritedDate(date: LocalDate, isFavorited: Boolean) {
        val dateStr = date.toString()
        _favoritedDates.update { current ->
            val updated = current.toMutableSet()
            if (isFavorited) {
                updated.add(dateStr)
            } else {
                updated.remove(dateStr)
            }
            prefs.edit().putStringSet(KEY_FAVORITED_DATES, updated).apply()
            updated
        }
    }

    fun isFavorited(date: LocalDate): Boolean {
        return _favoritedDates.value.contains(date.toString())
    }

    fun setCustomShortcodes(shortcodes: Map<String, String>) {
        prefs.edit()
            .putString(KEY_CUSTOM_SHORTCODES, serializeShortcodes(shortcodes))
            .apply()
        _customShortcodes.value = shortcodes
    }

    fun markTemplateUsed(templateId: String) {
        val updated =
                buildList {
                    add(templateId)
                    addAll(_recentTemplateIds.value.filter { it != templateId })
                }.take(6)
        prefs.edit().putString(KEY_RECENT_TEMPLATE_IDS, updated.joinToString(",")).apply()
        _recentTemplateIds.value = updated
    }

    fun toggleFavoriteTemplate(templateId: String) {
        val updated =
                _favoriteTemplateIds.value.toMutableSet().apply {
                    if (!add(templateId)) remove(templateId)
                }
        prefs.edit().putStringSet(KEY_FAVORITE_TEMPLATE_IDS, updated).apply()
        _favoriteTemplateIds.value = updated
    }

    fun incrementTemplateUsage(templateId: String) {
        val updated =
            _templateUsageCounts.value.toMutableMap().apply {
                this[templateId] = (this[templateId] ?: 0) + 1
            }.toMap()
        prefs.edit()
            .putString(KEY_TEMPLATE_USAGE_COUNTS, serializeTemplateCounts(updated))
            .apply()
        _templateUsageCounts.value = updated
    }

    fun incrementTemplateFollowUp(templateId: String) {
        val updated =
            _templateFollowUpCounts.value.toMutableMap().apply {
                this[templateId] = (this[templateId] ?: 0) + 1
            }.toMap()
        prefs.edit()
            .putString(KEY_TEMPLATE_FOLLOWUP_COUNTS, serializeTemplateCounts(updated))
            .apply()
        _templateFollowUpCounts.value = updated
    }

    fun setEntryReviewEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENTRY_REVIEW_ENABLED, enabled).apply()
        _entryReviewEnabled.value = enabled
    }

    fun setKeywordHighlightingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEYWORD_HIGHLIGHTING_ENABLED, enabled).apply()
        _keywordHighlightingEnabled.value = enabled
    }

    fun setPreviewLimitEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREVIEW_LIMIT_ENABLED, enabled).apply()
        _isPreviewLimitEnabled.value = enabled
    }

    fun setPreviewLimitLength(length: Int) {
        prefs.edit().putInt(KEY_PREVIEW_LIMIT_LENGTH, length).apply()
        _previewLimitLength.value = length
    }

    fun setShowStatistics(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_STATISTICS, show).apply()
        _showStatistics.value = show
        normalizeNavigationItems()
    }

    fun setShowLookbackInNavBar(show: Boolean) {
        _showLookbackInNavBar.value = show
        normalizeNavigationItems()
    }

    fun setShowKeywordsInNavBar(show: Boolean) {
        _showKeywordsInNavBar.value = show
        normalizeNavigationItems()
    }

    fun setShowTodosInNavBar(show: Boolean) {
        _showTodosInNavBar.value = show
        normalizeNavigationItems()
    }

    fun setShowCompletedTodos(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_COMPLETED_TODOS, show).apply()
        _showCompletedTodos.value = show
    }

    fun setShowStatisticsInNavBar(show: Boolean) {
        _showStatisticsInNavBar.value = show
        normalizeNavigationItems()
    }

    fun setEnableDragAndDrop(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_DRAG_AND_DROP, enable).apply()
        _enableDragAndDrop.value = enable
    }

    fun setEntryDeleteSelectionEnabled(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENTRY_DELETE_SELECTION_ENABLED, enable).apply()
        _entryDeleteSelectionEnabled.value = enable
    }

    fun setStatisticsSectionOrder(order: List<String>) {
        prefs.edit().putString(KEY_STATISTICS_SECTION_ORDER, order.joinToString(",")).apply()
        _statisticsSectionOrder.value = order
    }

    fun setVisibleStatisticsSections(sectionNames: Set<String>) {
        prefs.edit().putStringSet(KEY_VISIBLE_STATISTICS_SECTIONS, sectionNames).apply()
        _visibleStatisticsSections.value = sectionNames
    }

    private data class NavigationSelection(
        val lookback: Boolean,
        val keywords: Boolean,
        val todos: Boolean,
        val statistics: Boolean
    )

    private fun normalizeNavigationItems() {
        val normalized =
            normalizeNavigationSelection(
                mode = _navigationChromeMode.value,
                lookback = _showLookbackInNavBar.value,
                keywords = _showKeywordsInNavBar.value,
                todos = _showTodosInNavBar.value,
                statistics = _showStatisticsInNavBar.value,
                showStatistics = _showStatistics.value
            )
        prefs.edit()
            .putBoolean(KEY_SHOW_LOOKBACK_IN_NAV_BAR, normalized.lookback)
            .putBoolean(KEY_SHOW_KEYWORDS_IN_NAV_BAR, normalized.keywords)
            .putBoolean(KEY_SHOW_TODOS_IN_NAV_BAR, normalized.todos)
            .putBoolean(KEY_SHOW_STATISTICS_IN_NAV_BAR, normalized.statistics)
            .apply()
        _showLookbackInNavBar.value = normalized.lookback
        _showKeywordsInNavBar.value = normalized.keywords
        _showTodosInNavBar.value = normalized.todos
        _showStatisticsInNavBar.value = normalized.statistics
    }

    private fun normalizeNavigationSelection(
        mode: NavigationChromeMode,
        lookback: Boolean,
        keywords: Boolean,
        todos: Boolean,
        statistics: Boolean,
        showStatistics: Boolean
    ): NavigationSelection {
        val maxOptionalItems =
            if (mode == NavigationChromeMode.FLOATING_BAR) 2 else 3
        val selectedRoutes =
            buildList {
                if (lookback) add("lookback")
                if (keywords) add("keywords")
                if (todos) add("todos")
                if (statistics && showStatistics) add("statistics")
            }.take(maxOptionalItems).toSet()
        return NavigationSelection(
            lookback = "lookback" in selectedRoutes,
            keywords = "keywords" in selectedRoutes,
            todos = "todos" in selectedRoutes,
            statistics = "statistics" in selectedRoutes
        )
    }

    fun setUseMLKitDetection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_MLKIT_DETECTION, enabled).apply()
        _useMLKitDetection.value = enabled
    }

    private fun getSavedThemePreference(): ThemePreference =
            getEnum(prefs.getString(KEY_THEME, null), ThemePreference.SYSTEM)

    private fun getSavedColorSource(): ColorSource =
            getEnum(prefs.getString(KEY_COLOR_SOURCE, null), ColorSource.MATERIAL_YOU)

    private fun getSavedCustomPalette(): CustomPalette =
            getEnum(prefs.getString(KEY_CUSTOM_PALETTE, null), CustomPalette.YAJA)

    private fun getSavedThemeColorIntensity(): ThemeColorIntensity =
            getEnum(
                    prefs.getString(KEY_THEME_COLOR_INTENSITY, null),
                    ThemeColorIntensity.NORMAL
            )

    private fun getSavedBackgroundTintLevel(): BackgroundTintLevel =
            getEnum(
                    prefs.getString(KEY_BACKGROUND_TINT_LEVEL, null),
                    BackgroundTintLevel.SOFT
            )

    private fun getSavedPersonalThemeSlots(): List<PersonalThemeSlot> {
        val raw = prefs.getString(KEY_PERSONAL_THEME_SLOTS, null)
        if (raw.isNullOrBlank()) return defaultPersonalThemeSlots()
        return try {
            val arr = org.json.JSONArray(raw)
            val restored =
                (0 until arr.length()).mapNotNull { index ->
                    val item = arr.optJSONObject(index) ?: return@mapNotNull null
                    val slotId = item.optInt("slotId", index + 1).coerceIn(1, 3)
                    PersonalThemeSlot(
                        slotId = slotId,
                        name = item.optString("name", "Personal $slotId"),
                        hue = item.optDouble("hue", defaultPersonalThemeSlots()[slotId - 1].hue.toDouble()).toFloat(),
                        saturation = item.optDouble("saturation", defaultPersonalThemeSlots()[slotId - 1].saturation.toDouble()).toFloat(),
                        brightness = item.optDouble("brightness", defaultPersonalThemeSlots()[slotId - 1].brightness.toDouble()).toFloat(),
                        accentStyle = getEnum(if (item.isNull("accentStyle")) null else item.optString("accentStyle"), defaultPersonalThemeSlots()[slotId - 1].accentStyle)
                    )
                }.sortedBy { it.slotId }
            if (restored.size == 3) restored else defaultPersonalThemeSlots()
        } catch (_: Exception) {
            defaultPersonalThemeSlots()
        }
    }

    private fun getSavedActivePersonalThemeSlotId(): Int =
        prefs.getInt(KEY_ACTIVE_PERSONAL_THEME_SLOT_ID, 1).coerceIn(1, 3)

    private fun persistPersonalThemeSlots(slots: List<PersonalThemeSlot>) {
        val arr = org.json.JSONArray()
        slots.sortedBy { it.slotId }.forEach { slot ->
            val item = JSONObject()
            item.put("slotId", slot.slotId)
            item.put("name", slot.name)
            item.put("hue", slot.hue.toDouble())
            item.put("saturation", slot.saturation.toDouble())
            item.put("brightness", slot.brightness.toDouble())
            item.put("accentStyle", slot.accentStyle.name)
            arr.put(item)
        }
        prefs.edit().putString(KEY_PERSONAL_THEME_SLOTS, arr.toString()).apply()
    }

    private fun getSavedAppFontFamily(): AppFontFamily =
            getEnum(prefs.getString(KEY_APP_FONT, null), AppFontFamily.MONO)

    private fun getSavedMonoFontWeight(): Int =
            prefs.getInt(KEY_MONO_FONT_WEIGHT, MONO_FONT_WEIGHT_DEFAULT)
                    .coerceIn(MONO_FONT_WEIGHT_MIN, MONO_FONT_WEIGHT_MAX)

    private fun getSavedEntryStyle(): EntryStyle =
            getEnum(prefs.getString(KEY_ENTRY_STYLE, null), EntryStyle.CARDS)

    private fun getSavedStorageUri(): String? = prefs.getString(KEY_STORAGE_URI, null)

    private fun getSavedShowTimestamps(): Boolean = prefs.getBoolean(KEY_SHOW_TIMESTAMPS, true)

    private fun getSavedShowDayHeaderStats(): Boolean =
            prefs.getBoolean(KEY_SHOW_DAY_HEADER_STATS, true)

    private fun getSavedRenderCheckboxesAsText(): Boolean =
            prefs.getBoolean(KEY_RENDER_CHECKBOXES_AS_TEXT, false)

    private fun getSavedShowBottomBar(): Boolean = prefs.getBoolean(KEY_SHOW_BOTTOM_BAR, true)

    private fun getSavedNavigationChromeMode(): NavigationChromeMode =
            getEnum(
                    prefs.getString(KEY_NAVIGATION_CHROME_MODE, null),
                    NavigationChromeMode.FLOATING_BAR
            )

    private fun getSavedShowBottomPanelLabels(): Boolean =
            prefs.getBoolean(KEY_SHOW_BOTTOM_PANEL_LABELS, true)

    private fun getSavedFontScalePreference(): FontScalePreference =
            getEnum(prefs.getString(KEY_FONT_SCALE, null), FontScalePreference.NORMAL)

    private fun getSavedDataFontScalePreference(): FontScalePreference =
            getEnum(prefs.getString(KEY_DATA_FONT_SCALE, null), FontScalePreference.NORMAL)

    private fun getSavedFollowUiFontScale(): Boolean =
            prefs.getBoolean(KEY_FOLLOW_UI_FONT_SCALE, true)

    private fun getCurrentAppLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val tag = locales[0]?.language
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
    }

    private fun getSavedLastBackupTimestamp(): Long = prefs.getLong(KEY_LAST_BACKUP, 0L)

    private fun getSavedBackupReminderDays(): Int =
            prefs.getInt(KEY_BACKUP_REMINDER_DAYS, 7).coerceIn(0, 30)

    private fun getSavedAppLogRetentionDays(): Int =
            prefs.getInt(KEY_APP_LOG_RETENTION_DAYS, 7).coerceIn(1, 30)

    private fun getSavedAnimationPreference(): AnimationPreference =
            getEnum(prefs.getString(KEY_ANIMATION_PREFERENCE, null), AnimationPreference.FULL)

    private fun getSavedFirstDayOfWeek(): DayOfWeek =
            getEnum(prefs.getString(KEY_FIRST_DAY_OF_WEEK, null), DayOfWeek.MONDAY)

    private fun getSavedDateOrderPreference(): DateOrderPreference =
            getEnum(prefs.getString(KEY_DATE_ORDER, null), DateOrderPreference.AUTO)

    private fun getSavedCustomDateKeywords(): List<DateKeywordEntry> {
        val json = prefs.getString(KEY_CUSTOM_DATE_KEYWORDS, null) ?: return emptyList()
        return deserializeDateKeywords(json)
    }

    private fun getSavedFavoritedDates(): Set<String> =
            prefs.getStringSet(KEY_FAVORITED_DATES, emptySet()) ?: emptySet()

    private fun getSavedAllowFutureEntries(): Boolean =
            prefs.getBoolean(KEY_ALLOW_FUTURE_ENTRIES, false)

    private fun getSavedAllowTaskerAccess(): Boolean =
            prefs.getBoolean(KEY_ALLOW_TASKER_ACCESS, false)

    private fun getSavedAllowTaskerEvents(): Boolean =
            prefs.getBoolean(KEY_ALLOW_TASKER_EVENTS, false)

    private fun getSavedIncludeEntryTextInTaskerEvents(): Boolean =
            prefs.getBoolean(KEY_INCLUDE_ENTRY_TEXT_IN_TASKER_EVENTS, false)

    private fun getSavedSwipeToDeleteEnabled(): Boolean =
            prefs.getBoolean(KEY_SWIPE_TO_DELETE, true)

    private fun getSavedSwipeToNavigateDatesEnabled(): Boolean =
            prefs.getBoolean(KEY_SWIPE_TO_NAVIGATE_DATES, false)

    private fun getSavedSwipeDeleteDirection(): SwipeDirection =
            getEnum(prefs.getString(KEY_SWIPE_DELETE_DIRECTION, null), SwipeDirection.END_TO_START)

    private fun getSavedSwipeToSyncEnabled(): Boolean = prefs.getBoolean(KEY_SWIPE_TO_SYNC, true)

    private fun getSavedLargeJournalSafeMode(): Boolean =
            prefs.getBoolean(KEY_LARGE_JOURNAL_SAFE_MODE, true)

    private fun getSavedVersionHistoryEnabled(): Boolean =
            prefs.getBoolean(KEY_VERSION_HISTORY_ENABLED, true)

    private fun getSavedVersionHistoryMaxVersions(): Int =
            prefs.getInt(KEY_VERSION_HISTORY_MAX_VERSIONS, 3).coerceIn(1, 10)

    private fun getSavedVersionHistoryRetentionDays(): Int =
            prefs.getInt(KEY_VERSION_HISTORY_RETENTION_DAYS, 30).coerceIn(2, 30)

    private fun getSavedWidgetCornerRadius(): Int = prefs.getInt(KEY_WIDGET_CORNER_RADIUS, 24)

    private fun getSavedShowWidgetLabel(): Boolean = prefs.getBoolean(KEY_SHOW_WIDGET_LABEL, true)

    private fun getSavedCustomShortcodes(): Map<String, String> {
        val migrated = prefs.getBoolean(KEY_SHORTCODES_V2_MIGRATED, false)
        val serialized = prefs.getString(KEY_CUSTOM_SHORTCODES, null)

        var shortcodes: Map<String, String> =
                if (serialized.isNullOrBlank()) {
                    emptyMap()
                } else if (serialized.startsWith(SHORTCODE_CODEC_PREFIX)) {
                    deserializeShortcodes(serialized)
                } else if (serialized.trimStart().startsWith("{")) {
                    // New JSON format
                    try {
                        val json = JSONObject(serialized)
                        json.keys().asSequence().associateWith { json.getString(it) }
                    } catch (e: Exception) {
                        emptyMap()
                    }
                } else {
                    // Legacy colon-semicolon format — migrate on read
                    serialized
                            .split(";")
                            .mapNotNull {
                                val parts = it.split(":", limit = 2)
                                if (parts.size == 2) parts[0] to parts[1] else null
                            }
                            .toMap()
                }

        if (!migrated) {
            val defaults =
                    mapOf(
                            "@today" to "{{today:dd-MMM-yy}}",
                            "@now" to "{{now:HH:mm}}",
                            "@week" to "Week {{today:ww}}",
                            "@day" to "{{today:EEEE}}",
                            "@t " to "[ ] "
                    )
            val merged = defaults + shortcodes

            prefs.edit()
                    .putString(KEY_CUSTOM_SHORTCODES, serializeShortcodes(merged))
                    .putBoolean(KEY_SHORTCODES_V2_MIGRATED, true)
                    .apply()

            shortcodes = merged
        }

        return shortcodes
    }

    private fun getSavedRecentTemplateIds(): List<String> =
            prefs.getString(KEY_RECENT_TEMPLATE_IDS, null)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

    private fun getSavedFavoriteTemplateIds(): Set<String> =
            prefs.getStringSet(KEY_FAVORITE_TEMPLATE_IDS, emptySet()) ?: emptySet()

    private fun getSavedTemplateUsageCounts(): Map<String, Int> =
            deserializeTemplateCounts(prefs.getString(KEY_TEMPLATE_USAGE_COUNTS, null))

    private fun getSavedTemplateFollowUpCounts(): Map<String, Int> =
            deserializeTemplateCounts(prefs.getString(KEY_TEMPLATE_FOLLOWUP_COUNTS, null))

    private fun getSavedPreviewLimitEnabled(): Boolean =
            prefs.getBoolean(KEY_PREVIEW_LIMIT_ENABLED, true)

    private fun getSavedEntryReviewEnabled(): Boolean =
            prefs.getBoolean(KEY_ENTRY_REVIEW_ENABLED, true)

    private fun getSavedKeywordHighlightingEnabled(): Boolean =
            prefs.getBoolean(KEY_KEYWORD_HIGHLIGHTING_ENABLED, true)

    private fun getSavedPreviewLimitLength(): Int = prefs.getInt(KEY_PREVIEW_LIMIT_LENGTH, 200)

    private fun getSavedShowStatistics(): Boolean = prefs.getBoolean(KEY_SHOW_STATISTICS, true)

    private fun getSavedShowLookbackInNavBar(): Boolean = prefs.getBoolean(KEY_SHOW_LOOKBACK_IN_NAV_BAR, true)

    private fun getSavedShowKeywordsInNavBar(): Boolean = prefs.getBoolean(KEY_SHOW_KEYWORDS_IN_NAV_BAR, false)

    private fun getSavedShowTodosInNavBar(): Boolean = prefs.getBoolean(KEY_SHOW_TODOS_IN_NAV_BAR, false)

    private fun getSavedShowCompletedTodos(): Boolean =
            prefs.getBoolean(KEY_SHOW_COMPLETED_TODOS, true)

    private fun getSavedShowStatisticsInNavBar(): Boolean = prefs.getBoolean(KEY_SHOW_STATISTICS_IN_NAV_BAR, false)

    private fun getSavedEnableDragAndDrop(): Boolean = prefs.getBoolean(KEY_ENABLE_DRAG_AND_DROP, true)
    private fun getSavedEntryDeleteSelectionEnabled(): Boolean =
        prefs.getBoolean(KEY_ENTRY_DELETE_SELECTION_ENABLED, true)

    private fun getSavedStatisticsSectionOrder(): List<String> {
        val saved = prefs.getString(KEY_STATISTICS_SECTION_ORDER, null)
        return if (saved.isNullOrBlank()) {
            listOf("WRITING_INSIGHTS", "DISTRIBUTION", "WHEN_YOU_WRITE", "MONTHLY_ACTIVITY", "HEATMAP")
        } else {
            saved.split(",").filter { it.isNotBlank() }
        }
    }

    private fun getSavedVisibleStatisticsSections(): Set<String> =
            prefs.getStringSet(KEY_VISIBLE_STATISTICS_SECTIONS, emptySet()) ?: emptySet()

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        /**
         * Returns the process-wide singleton, creating it on first call.
         * Using applicationContext prevents Activity leaks.
         */
        fun getInstance(context: Context): SettingsRepository =
                instance
                        ?: synchronized(this) {
                            instance
                                    ?: SettingsRepository(context.applicationContext).also {
                                        instance = it
                                    }
                        }

        private const val KEY_THEME = "theme_preference"
        private const val KEY_COLOR_SOURCE = "theme_color_source"
        private const val KEY_CUSTOM_PALETTE = "theme_custom_palette"
        private const val KEY_THEME_COLOR_INTENSITY = "theme_color_intensity"
        private const val KEY_BACKGROUND_TINT_LEVEL = "theme_background_tint_level"
        private const val KEY_PERSONAL_THEME_SLOTS = "theme_personal_slots"
        private const val KEY_ACTIVE_PERSONAL_THEME_SLOT_ID = "theme_personal_active_slot_id"
        private const val KEY_APP_FONT = "app_font_preference"
        private const val KEY_MONO_FONT_WEIGHT = "mono_font_weight"
        private const val KEY_CUSTOM_FONT_PATH = "custom_font_path"
        private const val KEY_CUSTOM_FONT_NAME = "custom_font_name"
        const val MONO_FONT_WEIGHT_MIN = 100
        const val MONO_FONT_WEIGHT_MAX = 800
        const val MONO_FONT_WEIGHT_DEFAULT = 400
        private const val KEY_ENTRY_STYLE = "entry_style"
        private const val KEY_STORAGE_URI = "storage_uri"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SHOW_ONBOARDING_NEXT_LAUNCH = "show_onboarding_next_launch"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        private const val KEY_SHOW_DAY_HEADER_STATS = "show_day_header_stats"
        private const val KEY_RENDER_CHECKBOXES_AS_TEXT = "render_checkboxes_as_text"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_DATA_FONT_SCALE = "data_font_scale"
        private const val KEY_FOLLOW_UI_FONT_SCALE = "follow_ui_font_scale"
        private const val KEY_ANIMATION_PREFERENCE = "animation_preference"
        private const val KEY_LAST_BACKUP = "last_backup_timestamp"
        private const val KEY_BACKUP_REMINDER_DAYS = "backup_reminder_days"
        private const val KEY_APP_LOG_RETENTION_DAYS = "app_log_retention_days"
        private const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week"
        private const val KEY_FAVORITED_DATES = "favorited_dates"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout_minutes"
        private const val KEY_LARGE_JOURNAL_SAFE_MODE = "large_journal_safe_mode"
        private const val KEY_VERSION_HISTORY_ENABLED = "version_history_enabled"
        private const val KEY_VERSION_HISTORY_MAX_VERSIONS = "version_history_max_versions"
        private const val KEY_VERSION_HISTORY_RETENTION_DAYS = "version_history_retention_days"
        // PBKDF2 parameters for PIN hashing
        private const val PBKDF2_ITERATIONS = 10_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val KEY_ALLOW_FUTURE_ENTRIES = "allow_future_entries"
        private const val KEY_ALLOW_TASKER_ACCESS = "allow_tasker_access"
        private const val KEY_ALLOW_TASKER_EVENTS = "allow_tasker_events"
        private const val KEY_INCLUDE_ENTRY_TEXT_IN_TASKER_EVENTS =
                "include_entry_text_in_tasker_events"
        private const val KEY_SWIPE_TO_DELETE = "swipe_to_delete"
        private const val KEY_SWIPE_TO_NAVIGATE_DATES = "swipe_to_navigate_dates"
        private const val KEY_SWIPE_DELETE_DIRECTION = "swipe_delete_direction"
        private const val KEY_SWIPE_TO_SYNC = "swipe_to_sync"
        private const val KEY_LAST_ENTRY_COUNT = "last_entry_count"
        private const val KEY_LAST_BACKGROUND_FULL_REFRESH_AT = "last_background_full_refresh_at"
        private const val KEY_WIDGET_CORNER_RADIUS = "widget_corner_radius"
        private const val KEY_SHOW_WIDGET_LABEL = "show_widget_label"
        private const val KEY_SHOW_BOTTOM_BAR = "show_bottom_bar"
        private const val KEY_NAVIGATION_CHROME_MODE = "navigation_chrome_mode"
        private const val KEY_SHOW_BOTTOM_PANEL_LABELS = "show_bottom_panel_labels"
        private const val SHORTCODE_CODEC_PREFIX = "v3|"
        private const val KEY_CUSTOM_SHORTCODES = "custom_shortcodes"
        private const val KEY_ENTRY_REVIEW_ENABLED = "entry_review_enabled"
        private const val KEY_KEYWORD_HIGHLIGHTING_ENABLED = "keyword_highlighting_enabled"
        private const val KEY_SHORTCODES_V2_MIGRATED = "shortcodes_v2_migrated"
        private const val KEY_RECENT_TEMPLATE_IDS = "recent_template_ids"
        private const val KEY_FAVORITE_TEMPLATE_IDS = "favorite_template_ids"
        private const val KEY_TEMPLATE_USAGE_COUNTS = "template_usage_counts"
        private const val KEY_TEMPLATE_FOLLOWUP_COUNTS = "template_followup_counts"
        private const val KEY_PREVIEW_LIMIT_ENABLED = "preview_limit_enabled"
        private const val KEY_PREVIEW_LIMIT_LENGTH = "preview_limit_length"
        private const val KEY_SHOW_STATISTICS = "show_statistics"
        private const val KEY_SHOW_LOOKBACK_IN_NAV_BAR = "show_lookback_in_nav_bar"
        private const val KEY_SHOW_KEYWORDS_IN_NAV_BAR = "show_keywords_in_nav_bar"
        private const val KEY_SHOW_TODOS_IN_NAV_BAR = "show_todos_in_nav_bar"
        private const val KEY_SHOW_COMPLETED_TODOS = "show_completed_todos"
        private const val KEY_SHOW_STATISTICS_IN_NAV_BAR = "show_statistics_in_nav_bar"
        private const val KEY_ENABLE_DRAG_AND_DROP = "enable_drag_and_drop"
        private const val KEY_ENTRY_DELETE_SELECTION_ENABLED = "entry_delete_selection_enabled"
        private const val KEY_STATISTICS_SECTION_ORDER = "statistics_section_order"
        private const val KEY_VISIBLE_STATISTICS_SECTIONS = "visible_statistics_sections"
        private const val KEY_USE_MLKIT_DETECTION = "use_mlkit_detection"
        private const val KEY_DATE_ORDER = "date_order_preference"
        private const val KEY_HOME_SCREEN_SNAPSHOT = "home_screen_snapshot"
        private const val KEY_CUSTOM_DATE_KEYWORDS = "custom_date_keywords"

        /** Parses an enum by name from SharedPreferences, falling back to [default] on error. */
        private inline fun <reified T : Enum<T>> getEnum(value: String?, default: T): T {
            if (value == null) return default
            return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
        }

        fun serializeShortcodes(shortcodes: Map<String, String>): String {
            if (shortcodes.isEmpty()) return SHORTCODE_CODEC_PREFIX
            val encodedEntries = shortcodes.entries.joinToString("&") { (key, value) ->
                "${encodeShortcodeComponent(key)}=${encodeShortcodeComponent(value)}"
            }
            return SHORTCODE_CODEC_PREFIX + encodedEntries
        }

        fun deserializeShortcodes(serialized: String): Map<String, String> {
            val payload = serialized.removePrefix(SHORTCODE_CODEC_PREFIX)
            if (payload.isBlank()) return emptyMap()
            return payload.split("&")
                .mapNotNull { token ->
                    val parts = token.split("=", limit = 2)
                    if (parts.size != 2) return@mapNotNull null
                    decodeShortcodeComponent(parts[0]) to decodeShortcodeComponent(parts[1])
                }
                .toMap()
        }

        fun serializeDateKeywords(entries: List<DateKeywordEntry>): String {
            val arr = org.json.JSONArray()
            entries.forEach { e ->
                val obj = JSONObject()
                obj.put("k", e.keyword)
                obj.put("m", e.meaning)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun deserializeDateKeywords(serialized: String): List<DateKeywordEntry> {
            if (serialized.isBlank()) return emptyList()
            return try {
                val arr = org.json.JSONArray(serialized)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    DateKeywordEntry(obj.getString("k"), obj.getString("m"))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun serializeTemplateCounts(counts: Map<String, Int>): String {
            if (counts.isEmpty()) return "{}"
            val json = JSONObject()
            counts.toSortedMap().forEach { (id, count) ->
                json.put(id, count)
            }
            return json.toString()
        }

        private fun deserializeTemplateCounts(serialized: String?): Map<String, Int> {
            if (serialized.isNullOrBlank()) return emptyMap()
            return try {
                val json = JSONObject(serialized)
                json.keys().asSequence()
                    .mapNotNull { key ->
                        val value = json.optInt(key, Int.MIN_VALUE)
                        if (value == Int.MIN_VALUE || value <= 0) null else key to value
                    }
                    .toMap()
            } catch (_: Exception) {
                emptyMap()
            }
        }

        private fun encodeShortcodeComponent(value: String): String =
             URLEncoder.encode(value, Charsets.UTF_8.name())

        private fun decodeShortcodeComponent(value: String): String =
            URLDecoder.decode(value, Charsets.UTF_8.name())

        private fun defaultPersonalThemeSlots(): List<PersonalThemeSlot> =
            listOf(
                PersonalThemeSlot(
                    slotId = 1,
                    name = "Personal 1",
                    hue = 198f,
                    saturation = 0.68f,
                    brightness = 0.95f,
                    accentStyle = PersonalAccentStyle.SOFT
                ),
                PersonalThemeSlot(
                    slotId = 2,
                    name = "Personal 2",
                    hue = 18f,
                    saturation = 0.62f,
                    brightness = 1.0f,
                    accentStyle = PersonalAccentStyle.COMPLEMENTARY
                ),
                PersonalThemeSlot(
                    slotId = 3,
                    name = "Personal 3",
                    hue = 274f,
                    saturation = 0.5f,
                    brightness = 0.9f,
                    accentStyle = PersonalAccentStyle.ANALOGOUS
                )
            )
    }
}
