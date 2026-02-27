package com.customphase.hdrezkacustom

class WatchHistory(
    private val map: MutableMap<Int, Pair<MediaItemSelection, Double>> = mutableMapOf()
) : MutableMap<Int, Pair<MediaItemSelection, Double>> by map {

    //fun getProgress(id: Int) = get(id)?.second ?: 0.0
    @Transient // non-serialized
    var onUpdate: (() -> Unit)? = null

    fun addOrUpdateItem(id : Int,
                translatorId : Int,
                seasonId : Int,
                episodeId : Int,
                progress : Double)
    {
        onUpdate?.invoke()
    }
}
