package com.customphase.hdrezkacustom

data class MediaItem(
    val title : String = "None",
    val description : String = "None",
    val translators : List<MediaSelection> = listOf(),
    val seasons : List<MediaSelection> = listOf(),
    val episodes : List<MediaSelection> = listOf()
)

data class MediaSelection(
    val title : String = "None",
    val url : String = "",
    val active : Boolean = false
)
