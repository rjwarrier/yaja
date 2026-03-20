package com.mj.yaja.data

import android.content.Context
import android.content.SharedPreferences
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

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class FontScalePreference(val scale: Float) {
    SMALLER(0.75f),
    NORMAL(1.0f),
    LARGER(1.25f)
}

enum class AppFontFamily {
    SANS_SERIF,
    SERIF,
    MONO
}

/** Which direction triggers the swipe-to-delete gesture. */
enum class SwipeDirection {
    /** Swipe from start → end (right). */
    START_TO_END,
    /** Swipe from end → start (left). Default. */
    END_TO_START
}

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
            context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(getSavedThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _appFontFamily = MutableStateFlow(getSavedAppFontFamily())
    val appFontFamily: StateFlow<AppFontFamily> = _appFontFamily.asStateFlow()

    private val _storageUri = MutableStateFlow(getSavedStorageUri())
    val storageUri: StateFlow<String?> = _storageUri.asStateFlow()

    private val _showTimestamps = MutableStateFlow(getSavedShowTimestamps())
    val showTimestamps: StateFlow<Boolean> = _showTimestamps.asStateFlow()

    private val _fontScalePreference = MutableStateFlow(getSavedFontScalePreference())
    val fontScalePreference: StateFlow<FontScalePreference> = _fontScalePreference.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(getSavedLastBackupTimestamp())
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _firstDayOfWeek = MutableStateFlow(getSavedFirstDayOfWeek())
    val firstDayOfWeek: StateFlow<DayOfWeek> = _firstDayOfWeek.asStateFlow()

    private val _favoritedDates = MutableStateFlow(getSavedFavoritedDates())
    val favoritedDates: StateFlow<Set<String>> = _favoritedDates.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _allowFutureEntries = MutableStateFlow(getSavedAllowFutureEntries())
    val allowFutureEntries: StateFlow<Boolean> = _allowFutureEntries.asStateFlow()

    private val _swipeToDeleteEnabled = MutableStateFlow(getSavedSwipeToDeleteEnabled())
    val swipeToDeleteEnabled: StateFlow<Boolean> = _swipeToDeleteEnabled.asStateFlow()

    private val _swipeDeleteDirection = MutableStateFlow(getSavedSwipeDeleteDirection())
    val swipeDeleteDirection: StateFlow<SwipeDirection> = _swipeDeleteDirection.asStateFlow()

    private val _swipeToSyncEnabled = MutableStateFlow(getSavedSwipeToSyncEnabled())
    val swipeToSyncEnabled: StateFlow<Boolean> = _swipeToSyncEnabled.asStateFlow()

    private val _lastKnownEntryCount = MutableStateFlow(prefs.getInt(KEY_LAST_ENTRY_COUNT, -1))
    val lastKnownEntryCount: StateFlow<Int> = _lastKnownEntryCount.asStateFlow()

    private val _widgetCornerRadius = MutableStateFlow(getSavedWidgetCornerRadius())
    val widgetCornerRadius: StateFlow<Int> = _widgetCornerRadius.asStateFlow()

    private val _showWidgetLabel = MutableStateFlow(getSavedShowWidgetLabel())
    val showWidgetLabel: StateFlow<Boolean> = _showWidgetLabel.asStateFlow()

    private val _hasActiveWidgets = MutableStateFlow(false)
    val hasActiveWidgets: StateFlow<Boolean> = _hasActiveWidgets.asStateFlow()

    private val _showBottomBar = MutableStateFlow(getSavedShowBottomBar())
    val showBottomBar: StateFlow<Boolean> = _showBottomBar.asStateFlow()

    private val _customShortcodes = MutableStateFlow(getSavedCustomShortcodes())
    val customShortcodes: StateFlow<Map<String, String>> = _customShortcodes.asStateFlow()

    private val _isPreviewLimitEnabled = MutableStateFlow(getSavedPreviewLimitEnabled())
    val isPreviewLimitEnabled: StateFlow<Boolean> = _isPreviewLimitEnabled.asStateFlow()

    private val _previewLimitLength = MutableStateFlow(getSavedPreviewLimitLength())
    val previewLimitLength: StateFlow<Int> = _previewLimitLength.asStateFlow()

    private val _showStatistics = MutableStateFlow(getSavedShowStatistics())
    val showStatistics: StateFlow<Boolean> = _showStatistics.asStateFlow()

    private val _enableDragAndDrop = MutableStateFlow(getSavedEnableDragAndDrop())
    val enableDragAndDrop: StateFlow<Boolean> = _enableDragAndDrop.asStateFlow()

    private val _statisticsSectionOrder = MutableStateFlow(getSavedStatisticsSectionOrder())
    val statisticsSectionOrder: StateFlow<List<String>> = _statisticsSectionOrder.asStateFlow()

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

    fun setAppFontFamily(fontFamily: AppFontFamily) {
        prefs.edit().putString(KEY_APP_FONT, fontFamily.name).apply()
        _appFontFamily.value = fontFamily
    }

    fun setStorageUri(uriString: String?) {
        if (uriString == null) {
            prefs.edit().remove(KEY_STORAGE_URI).apply()
        } else {
            prefs.edit().putString(KEY_STORAGE_URI, uriString).apply()
        }
        _storageUri.value = uriString
    }

    fun setShowTimestamps(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TIMESTAMPS, show).apply()
        _showTimestamps.value = show
    }

    fun setShowBottomBar(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BOTTOM_BAR, show).apply()
        _showBottomBar.value = show
    }

    fun setFontScalePreference(preference: FontScalePreference) {
        prefs.edit().putString(KEY_FONT_SCALE, preference.name).apply()
        _fontScalePreference.value = preference
    }

    fun setLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
        _lastBackupTimestamp.value = timestamp
    }

    fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
        prefs.edit().putString(KEY_FIRST_DAY_OF_WEEK, dayOfWeek.name).apply()
        _firstDayOfWeek.value = dayOfWeek
    }

    fun setAllowFutureEntries(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_FUTURE_ENTRIES, allow).apply()
        _allowFutureEntries.value = allow
    }

    fun setSwipeToDeleteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_TO_DELETE, enabled).apply()
        _swipeToDeleteEnabled.value = enabled
    }

    fun setSwipeDeleteDirection(direction: SwipeDirection) {
        prefs.edit().putString(KEY_SWIPE_DELETE_DIRECTION, direction.name).apply()
        _swipeDeleteDirection.value = direction
    }

    fun setSwipeToSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_TO_SYNC, enabled).apply()
        _swipeToSyncEnabled.value = enabled
    }

    fun setLastKnownEntryCount(count: Int) {
        prefs.edit().putInt(KEY_LAST_ENTRY_COUNT, count).apply()
        _lastKnownEntryCount.value = count
    }

    fun setWidgetCornerRadius(radius: Int) {
        prefs.edit().putInt(KEY_WIDGET_CORNER_RADIUS, radius).apply()
        _widgetCornerRadius.value = radius

        // Trigger widget update
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val componentName =
                android.content.ComponentName(
                        context,
                        "com.mj.yaja.ui.widget.QuickCaptureWidgetProvider"
                )
        val ids = manager.getAppWidgetIds(componentName)

        val intent =
                android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .apply {
                            component = componentName
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
        context.sendBroadcast(intent)
    }

    fun setShowWidgetLabel(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_WIDGET_LABEL, show).apply()
        _showWidgetLabel.value = show

        // Trigger widget update
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val componentName =
                android.content.ComponentName(
                        context,
                        "com.mj.yaja.ui.widget.QuickCaptureWidgetProvider"
                )
        val ids = manager.getAppWidgetIds(componentName)

        val intent =
                android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .apply {
                            component = componentName
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
        context.sendBroadcast(intent)
    }

    fun refreshActiveWidgetsStatus() {
        try {
            val manager = android.appwidget.AppWidgetManager.getInstance(context)
            if (manager == null) {
                _hasActiveWidgets.value = false
                return
            }
            val componentName =
                    android.content.ComponentName(
                            context,
                            "com.mj.yaja.ui.widget.QuickCaptureWidgetProvider"
                    )
            val ids = manager.getAppWidgetIds(componentName)
            _hasActiveWidgets.value = ids != null && ids.isNotEmpty()
        } catch (e: Throwable) {
            android.util.Log.e("SettingsRepository", "Error refreshing widgets", e)
            _hasActiveWidgets.value = false
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

    fun isFavorited(date: LocalDate): Boolean {
        return _favoritedDates.value.contains(date.toString())
    }

    fun setCustomShortcodes(shortcodes: Map<String, String>) {
        val json = JSONObject()
        shortcodes.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_CUSTOM_SHORTCODES, json.toString()).apply()
        _customShortcodes.value = shortcodes
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
    }

    fun setEnableDragAndDrop(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_DRAG_AND_DROP, enable).apply()
        _enableDragAndDrop.value = enable
    }

    fun setStatisticsSectionOrder(order: List<String>) {
        prefs.edit().putString(KEY_STATISTICS_SECTION_ORDER, order.joinToString(",")).apply()
        _statisticsSectionOrder.value = order
    }

    private fun getSavedThemePreference(): ThemePreference =
            getEnum(prefs.getString(KEY_THEME, null), ThemePreference.SYSTEM)

    private fun getSavedAppFontFamily(): AppFontFamily =
            getEnum(prefs.getString(KEY_APP_FONT, null), AppFontFamily.MONO)

    private fun getSavedStorageUri(): String? = prefs.getString(KEY_STORAGE_URI, null)

    private fun getSavedShowTimestamps(): Boolean = prefs.getBoolean(KEY_SHOW_TIMESTAMPS, true)

    private fun getSavedShowBottomBar(): Boolean = prefs.getBoolean(KEY_SHOW_BOTTOM_BAR, true)

    private fun getSavedFontScalePreference(): FontScalePreference =
            getEnum(prefs.getString(KEY_FONT_SCALE, null), FontScalePreference.NORMAL)

    private fun getSavedLastBackupTimestamp(): Long = prefs.getLong(KEY_LAST_BACKUP, 0L)

    private fun getSavedFirstDayOfWeek(): DayOfWeek =
            getEnum(prefs.getString(KEY_FIRST_DAY_OF_WEEK, null), DayOfWeek.MONDAY)

    private fun getSavedFavoritedDates(): Set<String> =
            prefs.getStringSet(KEY_FAVORITED_DATES, emptySet()) ?: emptySet()

    private fun getSavedAllowFutureEntries(): Boolean =
            prefs.getBoolean(KEY_ALLOW_FUTURE_ENTRIES, false)

    private fun getSavedSwipeToDeleteEnabled(): Boolean =
            prefs.getBoolean(KEY_SWIPE_TO_DELETE, true)

    private fun getSavedSwipeDeleteDirection(): SwipeDirection =
            getEnum(prefs.getString(KEY_SWIPE_DELETE_DIRECTION, null), SwipeDirection.END_TO_START)

    private fun getSavedSwipeToSyncEnabled(): Boolean = prefs.getBoolean(KEY_SWIPE_TO_SYNC, true)

    private fun getSavedWidgetCornerRadius(): Int = prefs.getInt(KEY_WIDGET_CORNER_RADIUS, 24)

    private fun getSavedShowWidgetLabel(): Boolean = prefs.getBoolean(KEY_SHOW_WIDGET_LABEL, true)

    private fun getSavedCustomShortcodes(): Map<String, String> {
        val migrated = prefs.getBoolean(KEY_SHORTCODES_V2_MIGRATED, false)
        val serialized = prefs.getString(KEY_CUSTOM_SHORTCODES, null)

        var shortcodes: Map<String, String> =
                if (serialized.isNullOrBlank()) {
                    emptyMap()
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

            // Save merged list in the new JSON format
            val json = JSONObject()
            merged.forEach { (key, value) -> json.put(key, value) }
            prefs.edit()
                    .putString(KEY_CUSTOM_SHORTCODES, json.toString())
                    .putBoolean(KEY_SHORTCODES_V2_MIGRATED, true)
                    .apply()

            shortcodes = merged
        }

        return shortcodes
    }

    private fun getSavedPreviewLimitEnabled(): Boolean =
            prefs.getBoolean(KEY_PREVIEW_LIMIT_ENABLED, true)

    private fun getSavedPreviewLimitLength(): Int = prefs.getInt(KEY_PREVIEW_LIMIT_LENGTH, 200)

    private fun getSavedShowStatistics(): Boolean = prefs.getBoolean(KEY_SHOW_STATISTICS, true)

    private fun getSavedEnableDragAndDrop(): Boolean = prefs.getBoolean(KEY_ENABLE_DRAG_AND_DROP, true)

    private fun getSavedStatisticsSectionOrder(): List<String> {
        val saved = prefs.getString(KEY_STATISTICS_SECTION_ORDER, null)
        return if (saved.isNullOrBlank()) {
            listOf("WRITING_INSIGHTS", "DISTRIBUTION", "WHEN_YOU_WRITE", "MONTHLY_ACTIVITY", "HEATMAP")
        } else {
            saved.split(",").filter { it.isNotBlank() }
        }
    }

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
        private const val KEY_APP_FONT = "app_font_preference"
        private const val KEY_STORAGE_URI = "storage_uri"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_LAST_BACKUP = "last_backup_timestamp"
        private const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week"
        private const val KEY_FAVORITED_DATES = "favorited_dates"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        // PBKDF2 parameters for PIN hashing
        private const val PBKDF2_ITERATIONS = 10_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val KEY_ALLOW_FUTURE_ENTRIES = "allow_future_entries"
        private const val KEY_SWIPE_TO_DELETE = "swipe_to_delete"
        private const val KEY_SWIPE_DELETE_DIRECTION = "swipe_delete_direction"
        private const val KEY_SWIPE_TO_SYNC = "swipe_to_sync"
        private const val KEY_LAST_ENTRY_COUNT = "last_entry_count"
        private const val KEY_WIDGET_CORNER_RADIUS = "widget_corner_radius"
        private const val KEY_SHOW_WIDGET_LABEL = "show_widget_label"
        private const val KEY_SHOW_BOTTOM_BAR = "show_bottom_bar"
        private const val KEY_CUSTOM_SHORTCODES = "custom_shortcodes"
        private const val KEY_SHORTCODES_V2_MIGRATED = "shortcodes_v2_migrated"
        private const val KEY_PREVIEW_LIMIT_ENABLED = "preview_limit_enabled"
        private const val KEY_PREVIEW_LIMIT_LENGTH = "preview_limit_length"
        private const val KEY_SHOW_STATISTICS = "show_statistics"
        private const val KEY_ENABLE_DRAG_AND_DROP = "enable_drag_and_drop"
        private const val KEY_STATISTICS_SECTION_ORDER = "statistics_section_order"

        /** Parses an enum by name from SharedPreferences, falling back to [default] on error. */
        private inline fun <reified T : Enum<T>> getEnum(value: String?, default: T): T {
            if (value == null) return default
            return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
        }
    }
}
