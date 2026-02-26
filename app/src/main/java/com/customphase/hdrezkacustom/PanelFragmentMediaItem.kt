package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanelFragmentMediaItem : PanelFragment() {
    private var currentItemId : Int = 0

    override val iconResource: Int
        get() = -1

    override val title: String
        get() = ""

    private lateinit var mediaItemTitle : TextView
    private lateinit var mediaItemDescription : TextView
    private lateinit var mediaItemContent : View
    private lateinit var mediaItemTranslators : MediaItemSelectionsView
    private lateinit var mediaItemSeasons : MediaItemSelectionsView
    private lateinit var mediaItemEpisodes : MediaItemSelectionsView
    private lateinit var mediaItemLoadingIndicator : View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_media_item, container, false)
        mediaItemTitle = view.findViewById(R.id.mediaItemTitle)
        mediaItemDescription = view.findViewById(R.id.mediaItemDescription)
        mediaItemContent = view.findViewById(R.id.mediaItemContent)
        mediaItemTranslators = view.findViewById(R.id.mediaItemTranslators)
        mediaItemSeasons = view.findViewById(R.id.mediaItemSeasons)
        mediaItemEpisodes = view.findViewById(R.id.mediaItemEpisodes)
        mediaItemLoadingIndicator = view.findViewById(R.id.mediaItemLoadingIndicator)
        return view
    }

    fun loadMediaItem(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                mediaItemTitle.text = ""

                mediaItemContent.visibility = View.GONE
                mediaItemTranslators.visibility = View.GONE
                mediaItemSeasons.visibility = View.GONE
                mediaItemEpisodes.visibility = View.GONE

                mediaItemLoadingIndicator.visibility = View.VISIBLE
            }

            val item = (activity as MainActivity).parser.getMediaItem(url)
            currentItemId = item.id
            if (item.translators.isEmpty()) {
                item.translators = listOf(
                    MediaItemSelection(
                        getString(R.string.unknown_translator),
                        false,
                        item.defaultTranslatorId,
                        0,
                        0,
                        false
                    )
                )
            }
            val isMovie = item.seasons.isEmpty()
            var selectedSeasonId = -1;
            if (item.seasons.isNotEmpty()) {
                selectedSeasonId = item.seasons.firstOrNull { it.active }?.seasonId ?: -1
            }
            withContext(Dispatchers.Main) {
                mediaItemLoadingIndicator.visibility = View.GONE
                mediaItemContent.visibility = View.VISIBLE

                mediaItemTitle.text = item.title
                mediaItemDescription.text = item.description

                createMediaSelections(
                    R.id.mediaItemTranslators,
                    R.string.translators,
                    item.translators,
                    selectedSeasonId,
                    isMovie
                ) { p1, p2 ->
                    onTranslatorClick(p1, p2)
                }

                if (!isMovie) {
                    createMediaSelections(
                        R.id.mediaItemSeasons,
                        R.string.seasons,
                        item.seasons,
                        selectedSeasonId,
                        isMovie
                    ) { p1, p2 ->
                        onSeasonClick(p1, p2)
                    }

                    createMediaSelections(
                        R.id.mediaItemEpisodes,
                        R.string.episodes,
                        item.episodes,
                        selectedSeasonId,
                        isMovie
                    ) { p1, p2 ->
                        onEpisodeClick(p1, p2)
                    }
                }
            }
        }
    }

    private fun createMediaSelections(parentId : Int,
                                      titleId : Int,
                                      selections : List<MediaItemSelection>,
                                      selectedSeasonId: Int,
                                      isMovie : Boolean,
                                      onClick: (View, Boolean) -> Unit) {
        val currentView = view ?: return

        val parentView = currentView.findViewById<MediaItemSelectionsView>(parentId)
        parentView.visibility = View.VISIBLE

        parentView.setTitle(getString(titleId))
        parentView.clear()

        for (sel in selections) {
            val selected = if (isMovie || parentId == R.id.mediaItemEpisodes) false else sel.active
            val btn = parentView.addItem(sel, selected)
            if (parentId == R.id.mediaItemEpisodes) {
                btn.visibility = if (sel.seasonId == selectedSeasonId) View.VISIBLE else View.GONE
            }
            btn.setOnClickListener {
                onClick(btn, isMovie)
            }
        }
    }

    private fun onTranslatorClick(btn : View, isMovie : Boolean) {
        if (btn.isActivated) return
        val sel = btn.tag as MediaItemSelection
        if (isMovie) {
            lifecycleScope.launch(Dispatchers.IO) {
                val streams = (activity as MainActivity).parser.getMediaStreamUrl(
                    true,
                    currentItemId,
                    sel.translatorId,
                    0,
                    0,
                    sel.isDirector
                )
                (activity as MainActivity).showPlayerPanel(streams)
            }
        } else {
            mediaItemTranslators.setSelected(btn)
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    mediaItemSeasons.startLoading()
                    mediaItemEpisodes.startLoading()
                }
                val item = (activity as MainActivity).parser.getMediaEpisodes(currentItemId, sel.translatorId)

                withContext(Dispatchers.Main) {
                    val selectedSeasonId = item.seasons.first { it.active }.seasonId

                    createMediaSelections(
                        R.id.mediaItemSeasons,
                        R.string.seasons,
                        item.seasons,
                        selectedSeasonId,
                        isMovie
                    ) { p1, p2 ->
                        onSeasonClick(p1, p2)
                    }

                    createMediaSelections(
                        R.id.mediaItemEpisodes,
                        R.string.episodes,
                        item.episodes,
                        selectedSeasonId,
                        isMovie
                    ) { p1, p2 ->
                        onEpisodeClick(p1, p2)
                    }
                    mediaItemSeasons.stopLoading()
                    mediaItemEpisodes.stopLoading()
                }
            }
        }
    }

    private fun onSeasonClick(btn : View, isMovie : Boolean) {
        if (btn.isActivated) return
        val sel = btn.tag as MediaItemSelection
        mediaItemSeasons.setSelected(btn)
        mediaItemEpisodes.setItemsVisibility { it.seasonId == sel.seasonId }
    }

    private fun onEpisodeClick(btn : View, isMovie : Boolean) {
        val translatorId = mediaItemTranslators.selectedItem.second?.translatorId!!
        val seasonId = mediaItemSeasons.selectedItem.second?.seasonId!!
        lifecycleScope.launch(Dispatchers.IO) {
            val sel = btn.tag as MediaItemSelection
            val streams = (activity as MainActivity).parser.getMediaStreamUrl(
                false,
                currentItemId,
                translatorId,
                seasonId,
                sel.episodeId,
                false
            )
            (activity as MainActivity).showPlayerPanel(streams)
        }
    }
}