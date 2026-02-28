package com.customphase.hdrezkacustom

data class MediaItem(
    val id : Int = 0,
    val title : String = "None",
    val description : String = "None",
    val defaultTranslatorId : Int = 0,
    var translators : List<MediaItemSelection> = listOf(),
    val seasons : List<MediaItemSelection> = listOf(),
    val episodes : List<MediaItemSelection> = listOf()
)

data class MediaItemSelection(
    val title : String = "None",
    val active : Boolean = false,
    var translatorId : Int = 0,
    var seasonId : Int = 0,
    var episodeId : Int = 0,
    var isDirector : Boolean = false
)
