# Yaja V2 - Project Notes

## Import Feature Testing

**To test imports:**
1. Push JSON file to device:
   `adb push D:\ITT\Jorunal\Journal.json /sdcard/Download/Journal.json`
2. Open Yaja Settings > Data & Storage > Import
3. Tap "Day One" or "Journalistic" row
4. Navigate to `/sdcard/Download/` and select the file
5. Monitor progress bar — cancel button available during import
6. After success, navigate to a date and confirm entries appear
7. If entries don't appear, navigate to a different date and back (forces UI refresh)

**Common issue:** Imported files are written to the current journal's storage location (configured in Settings > Data & Storage > Storage Location). If using custom folder (Google Drive, SD card), ensure it has write permission via SAF picker.

## Multi-Journal Implementation Plan (max 3 journals)

### Phase 1 - Data Layer
- New `JournalConfig` data class (id, name, storageUri, createdAt) + `JournalRepository` stored as JSON in SharedPreferences
- Refactor `MarkdownFileManager` to decouple from `SettingsRepository`, accept storageUri directly
- New `MarkdownFileManagerRegistry` singleton managing journalId -> MarkdownFileManager map
- Per-journal disk cache (`journal_cache_{journalId}.json`)
- Migration: auto-create "default" journal from existing storage_uri on first launch

### Phase 2 - ViewModel
- Update `JournalViewModel` to accept `JournalRepository` + Registry
- Add `activeJournal` StateFlow, swap MarkdownFileManager on switch, reload all data
- Namespace favorited dates per journal
- Cancel in-flight jobs on switch

### Phase 3 - UI
- Long-press Journal nav icon -> `combinedClickable` onLongClick showing DropdownMenu/BottomSheet with journal list + "Open another folder" (SAF picker, if <3)
- Active journal indicator in HomeScreen header
- Nav drawer shows active journal name

### Phase 4 - Widgets
- Widgets use active journal (read `active_journal_id` from SharedPreferences)
- Update `QuickCaptureWidgetProvider`, `HeatmapWidgetProvider`, `QuickCaptureActivity` to use registry

### Phase 5 - Polish
- Journal management (rename/delete)
- Handle SAF permission revoked
- Backup/restore per journal

### Key Files
- `MarkdownFileManager.kt`, `SettingsRepository.kt`, `JournalViewModel.kt`, `MainActivity.kt`, `NavDrawer.kt`
