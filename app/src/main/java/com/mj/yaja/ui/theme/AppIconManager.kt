package com.mj.yaja.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mj.yaja.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Switches the home-screen launcher icon between Yaja's built-in variants.
 *
 * Each variant is a separate `activity-alias` targeting MainActivity — MainActivity's
 * own component is never enabled/disabled (it stays permanently enabled so widgets,
 * the "Today" app shortcut, and Tasker can keep addressing it directly by class).
 * Exactly one alias is left enabled at a time; the rest are disabled.
 *
 * Android identifies a running activity by whichever component it was actually
 * launched through — so the app is currently running "as" MainActivityAliasClassic
 * (say), not as MainActivity. Disabling that alias while it's the resumed activity
 * makes the OS tear it down immediately, which reads as "the app just closed." To
 * avoid that, [AppIconManager.setSelectedIcon] enables the new alias and relaunches
 * through it *before* disabling the old one, so the app hands off to itself instead
 * of dying.
 */
enum class AppIcon(
    val id: String,
    val aliasSuffix: String,
    @param:DrawableRes val foregroundRes: Int,
    @param:StringRes val labelRes: Int
) {
    CLASSIC("classic", "Classic", R.drawable.ic_launcher_foreground, R.string.settings_app_icon_classic),
    NOTEBOOK("notebook", "Notebook", R.drawable.ic_launcher_foreground_notebook, R.string.settings_app_icon_notebook),
    QUILL("quill", "Quill", R.drawable.ic_launcher_foreground_quill, R.string.settings_app_icon_quill),
    QUICK_NOTES("quick_notes", "QuickNotes", R.drawable.ic_launcher_foreground_quicknotes, R.string.settings_app_icon_quick_notes),
    STICKY_NOTE("sticky_note", "StickyNote", R.drawable.ic_launcher_foreground_sticky_note, R.string.settings_app_icon_sticky_note),
    LIBRARY("library", "Library", R.drawable.ic_launcher_foreground_library, R.string.settings_app_icon_library),
    INK_PEN("ink_pen", "InkPen", R.drawable.ic_launcher_foreground_ink_pen, R.string.settings_app_icon_ink_pen),
    ARTICLE("article", "Article", R.drawable.ic_launcher_foreground_article, R.string.settings_app_icon_article),
    EDIT_SQUARE("edit_square", "EditSquare", R.drawable.ic_launcher_foreground_edit_square, R.string.settings_app_icon_edit_square),
    NOTE_STACK("note_stack", "NoteStack", R.drawable.ic_launcher_foreground_note_stack, R.string.settings_app_icon_note_stack),
    SUBJECT("subject", "Subject", R.drawable.ic_launcher_foreground_subject, R.string.settings_app_icon_subject),
    FEED("feed", "Feed", R.drawable.ic_launcher_foreground_feed, R.string.settings_app_icon_feed),
    BOOK_STYLE("book_style", "BookStyle", R.drawable.ic_launcher_foreground_book_style, R.string.settings_app_icon_book_style),
    BORDER_PEN("border_pen", "BorderPen", R.drawable.ic_launcher_foreground_border_pen, R.string.settings_app_icon_border_pen),
    POST_ADD("post_add", "PostAdd", R.drawable.ic_launcher_foreground_post_add, R.string.settings_app_icon_post_add),
    NOTEBOOK_FILLED("notebook_filled", "NotebookFilled", R.drawable.ic_launcher_foreground_notebook_filled, R.string.settings_app_icon_notebook_filled),
    QUILL_FILLED("quill_filled", "QuillFilled", R.drawable.ic_launcher_foreground_quill_filled, R.string.settings_app_icon_quill_filled),
    QUICK_NOTES_FILLED("quick_notes_filled", "QuickNotesFilled", R.drawable.ic_launcher_foreground_quicknotes_filled, R.string.settings_app_icon_quick_notes_filled),
    STICKY_NOTE_FILLED("sticky_note_filled", "StickyNoteFilled", R.drawable.ic_launcher_foreground_sticky_note_filled, R.string.settings_app_icon_sticky_note_filled),
    LIBRARY_FILLED("library_filled", "LibraryFilled", R.drawable.ic_launcher_foreground_library_filled, R.string.settings_app_icon_library_filled),
    INK_PEN_FILLED("ink_pen_filled", "InkPenFilled", R.drawable.ic_launcher_foreground_ink_pen_filled, R.string.settings_app_icon_ink_pen_filled),
    ARTICLE_FILLED("article_filled", "ArticleFilled", R.drawable.ic_launcher_foreground_article_filled, R.string.settings_app_icon_article_filled),
    EDIT_SQUARE_FILLED("edit_square_filled", "EditSquareFilled", R.drawable.ic_launcher_foreground_edit_square_filled, R.string.settings_app_icon_edit_square_filled),
    NOTE_STACK_FILLED("note_stack_filled", "NoteStackFilled", R.drawable.ic_launcher_foreground_note_stack_filled, R.string.settings_app_icon_note_stack_filled),
    SUBJECT_FILLED("subject_filled", "SubjectFilled", R.drawable.ic_launcher_foreground_subject_filled, R.string.settings_app_icon_subject_filled),
    FEED_FILLED("feed_filled", "FeedFilled", R.drawable.ic_launcher_foreground_feed_filled, R.string.settings_app_icon_feed_filled),
    BOOK_STYLE_FILLED("book_style_filled", "BookStyleFilled", R.drawable.ic_launcher_foreground_book_style_filled, R.string.settings_app_icon_book_style_filled),
    BORDER_PEN_FILLED("border_pen_filled", "BorderPenFilled", R.drawable.ic_launcher_foreground_border_pen_filled, R.string.settings_app_icon_border_pen_filled),
    POST_ADD_FILLED("post_add_filled", "PostAddFilled", R.drawable.ic_launcher_foreground_post_add_filled, R.string.settings_app_icon_post_add_filled),
    WORDMARK("wordmark", "Wordmark", R.drawable.ic_launcher_foreground_wordmark, R.string.settings_app_icon_wordmark)
}

object AppIconManager {
    private const val PREFS_NAME = "journal_settings"
    private const val KEY_SELECTED_APP_ICON = "selected_app_icon"

    private fun componentName(context: Context, icon: AppIcon): ComponentName =
        ComponentName(context.packageName, "${context.packageName}.MainActivityAlias${icon.aliasSuffix}")

    fun getSelectedIcon(context: Context): AppIcon {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_SELECTED_APP_ICON, null) ?: return AppIcon.CLASSIC
        return AppIcon.entries.firstOrNull { it.id == savedId } ?: AppIcon.CLASSIC
    }

    fun setSelectedIcon(context: Context, icon: AppIcon) {
        if (icon == getSelectedIcon(context)) return
        val appContext = context.applicationContext
        val pm = appContext.packageManager

        // Enable the target alias and relaunch through it first, so the app is
        // already running under the new component by the time the old ones get
        // disabled below (see class doc for why order matters here). This part
        // stays synchronous — it's just one or two Binder calls and the user is
        // about to see the app restart anyway.
        pm.setComponentEnabledSetting(
            componentName(appContext, icon),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        context.startActivity(
            Intent().apply {
                component = componentName(appContext, icon)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_APP_ICON, icon.id)
            .apply()

        // Disabling every other alias is bookkeeping the user never waits on — the
        // new icon is already active by this point. With this many icon choices,
        // doing it as N sequential setComponentEnabledSetting Binder calls on the
        // caller's thread is a real jank risk, so it runs off-thread; on API 31+
        // it's also batched into a single call via setComponentEnabledSettings
        // instead of one round-trip per icon.
        CoroutineScope(Dispatchers.IO).launch {
            val others = AppIcon.entries.filter { it != icon }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pm.setComponentEnabledSettings(
                    others.map { other ->
                        PackageManager.ComponentEnabledSetting(
                            componentName(appContext, other),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                )
            } else {
                others.forEach { other ->
                    pm.setComponentEnabledSetting(
                        componentName(appContext, other),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }
}
