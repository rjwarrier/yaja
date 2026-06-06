# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\ranji\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom keep rules here that are specific to your project.

# If you use Gson or other reflection-based libraries, add rules here:
# -keep class com.mj.yaja.models.** { *; }

# Jetpack Compose rules are mostly handled by the compiler now,
# but you can add specific ones if you see issues with missing classes.

# ── AndroidX WorkManager (transitive dep from ML Kit) ────────────────
# WorkManager auto-initialises via InitializationProvider and uses Room's
# generated WorkDatabase_Impl via reflection.  R8 strips it because no
# code in *this* app references it directly → instant crash on startup.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.DatabaseConfiguration { *; }
-dontwarn androidx.room.paging.**

# ── AndroidX Startup (hosts WorkManager init) ───────────────────────
-keep class * extends androidx.startup.Initializer { *; }

# ── ML Kit Language Identification ──────────────────────────────────
# Public API + internal implementation classes R8 strips aggressively
-keep class com.google.mlkit.nl.languageid.** { *; }
-keep class com.google.mlkit.nl.languageid.internal.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.mlkit.common.sdkinternal.** { *; }

# GMS Tasks — async callback infrastructure used by ML Kit
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_language_id.** { *; }

# Firebase components used internally by ML Kit for module loading
-keep class com.google.firebase.components.** { *; }
-keep class com.google.firebase.inject.** { *; }

# Suppress warnings for optional classes
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# ── Enums persisted to SharedPreferences ─────────────────────────────────────
# The app stores enum .name strings (e.g. "SYSTEM") in SharedPreferences and
# reconstructs them via enumValueOf<T>(). R8 renames enum entries, breaking
# the lookup and causing IllegalArgumentException crashes.
-keepclassmembers enum com.mj.yaja.data.ThemePreference { *; }
-keepclassmembers enum com.mj.yaja.data.FontScalePreference { *; }
-keepclassmembers enum com.mj.yaja.data.AppFontFamily { *; }
-keepclassmembers enum com.mj.yaja.data.SwipeDirection { *; }
-keepclassmembers enum com.mj.yaja.ui.widget.CellShape { *; }
-keepclassmembers enum com.mj.yaja.ui.screens.StatisticsSection { *; }

# ── State Management Classes (ViewModel sealed classes) ──────────────────────
# ImportState sealed class and its data classes used in StateFlow
-keep class com.mj.yaja.ui.viewmodel.JournalViewModel$ImportState { *; }
-keep class com.mj.yaja.ui.viewmodel.JournalViewModel$ImportState$* { *; }

# ── Widget providers, receivers, and config activities ───────────────────────
-keep class com.mj.yaja.ui.widget.HeatmapWidgetProvider
-keep class com.mj.yaja.ui.widget.QuickCaptureWidgetProvider
-keep class com.mj.yaja.ui.widget.HeatmapWidgetConfigActivity
-keep class com.mj.yaja.ui.widget.QuickCaptureWidgetConfigActivity
-keep class com.mj.yaja.ui.screens.QuickCaptureActivity
-keep class com.mj.yaja.TaskerReceiver
-keep class com.mj.yaja.YajaApplication
