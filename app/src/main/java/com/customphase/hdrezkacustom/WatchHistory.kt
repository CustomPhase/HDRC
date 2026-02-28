package com.customphase.hdrezkacustom

data class WatchHistoryItem(
    var selection: MediaItemSelection = MediaItemSelection(),
    var title : String = "",
    var currentTime: Long = 0
)

class WatchHistory(
    private val map: MutableMap<Int, WatchHistoryItem> = mutableMapOf()
) : MutableMap<Int, WatchHistoryItem> by map {
}
