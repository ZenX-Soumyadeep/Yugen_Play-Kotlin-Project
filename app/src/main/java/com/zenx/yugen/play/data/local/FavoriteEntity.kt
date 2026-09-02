package com.zenx.yugen.play.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val title: String, // Title is our unique key since AniList and Anikoto share it
    val posterUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)