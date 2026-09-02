package com.zenx.yugen.play.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_sync")
data class OfflineSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaId: Int,
    val progress: Int
)