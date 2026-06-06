package com.mj.yaja.data.storage

data class JournalStorageFingerprint(
    val storageKey: String,
    val fileCount: Int,
    val newestModifiedAt: Long,
    val oldestModifiedAt: Long,
    val metadataChecksum: Long,
    val computedAt: Long
)
