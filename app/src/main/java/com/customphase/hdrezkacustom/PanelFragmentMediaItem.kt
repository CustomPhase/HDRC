package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanelFragmentMediaItem : PanelFragment() {

    enum class SelectionsType {
        Translator,
        Season,
        Episode
    }

    override val iconResource: Int
        get() = -1

    override val title: String
        get() = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_media_item, container, false)
        return view
    }

    private var currentTranslatorId : Int = 0
    private var currentSeasonId : Int = 0

    fun loadMedia(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val item = (activity as MainActivity).parser.getMediaItem(url)
            withContext(Dispatchers.Main) {
                currentTranslatorId = item.translators.first { mediaSelection -> mediaSelection.active }.translatorId
                currentSeasonId = item.seasons.first { mediaSelection -> mediaSelection.active }.seasonId
                val currentView = view
                if (currentView != null) {
                    currentView.findViewById<TextView>(R.id.media_item_title)?.text = item.title
                    currentView.findViewById<TextView>(R.id.media_item_desc)?.text = item.description

                    val translatorsView = currentView.findViewById<GridLayout>(R.id.translatorsView)
                    createMediaSelections(translatorsView, item, SelectionsType.Translator)

                    val seasonsView = currentView.findViewById<GridLayout>(R.id.seasonsView)
                    createMediaSelections(seasonsView, item, SelectionsType.Season)

                    val episodesView = currentView.findViewById<GridLayout>(R.id.episodesView)
                    createMediaSelections(episodesView, item, SelectionsType.Episode)
                }
            }
        }
    }

    private fun createMediaSelections(targetView : ViewGroup,
                                      mediaItem : MediaItem,
                                      selectionsType : SelectionsType) {
        val selections = when (selectionsType) {
            SelectionsType.Translator -> mediaItem.translators
            SelectionsType.Season -> mediaItem.seasons
            SelectionsType.Episode -> mediaItem.episodes
        }
        targetView.removeAllViews()

        for (sel in selections) {
            val btn = layoutInflater.inflate(R.layout.button_radio, targetView, false) as Button
            btn.text = sel.title

            val params = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                width = 0
                setMargins(4, 4, 4, 4)
            }
            btn.layoutParams = params

            btn.tag = sel
            if (selectionsType != SelectionsType.Episode && sel.active) btn.isActivated = true
            if (selectionsType == SelectionsType.Episode) {
                if (sel.seasonId != currentSeasonId) {
                    btn.visibility = View.GONE
                }
            }
            btn.setOnClickListener {
                if (selectionsType == SelectionsType.Translator) {
                    onTranslationClick(sel.translatorId, sel.url)
                }
                if (selectionsType == SelectionsType.Season) {
                    onSeasonClick(sel.seasonId)
                }
                if (selectionsType == SelectionsType.Episode) {
                    onEpisodeClick(sel.episodeId)
                }
            }

            targetView.addView(btn)
        }
    }

    private fun onTranslationClick(translatorId : Int, url : String) {
        if (currentTranslatorId == translatorId) return
        currentTranslatorId = translatorId
        updateMediaSelections(url)
    }

    private fun onSeasonClick(seasonId : Int) {
        if (currentSeasonId == seasonId) return
        val currentView = view
        if (currentView != null) {
            currentSeasonId = seasonId

            val seasonsView = currentView.findViewById<GridLayout>(R.id.seasonsView)
            for (child in seasonsView.children) {
                val childSel = child.tag as MediaSelection
                child.isActivated = childSel.seasonId == currentSeasonId
            }

            val episodesView = currentView.findViewById<GridLayout>(R.id.episodesView)
            for (child in episodesView.children) {
                val childSel = child.tag as MediaSelection
                child.visibility = if (childSel.seasonId == currentSeasonId) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onEpisodeClick(episodeId : Int) {
        println("OPEN: translator $currentTranslatorId, season $currentSeasonId, episode $episodeId");
    }

    private fun updateMediaSelections(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val item = (activity as MainActivity).parser.getMediaItem(url)
            withContext(Dispatchers.Main) {
                val currentView = view
                if (currentView != null) {
                    val translatorsView = currentView.findViewById<GridLayout>(R.id.translatorsView)
                    createMediaSelections(translatorsView, item, SelectionsType.Translator)
                    val seasonsView = currentView.findViewById<GridLayout>(R.id.seasonsView)
                    createMediaSelections(seasonsView, item, SelectionsType.Season)
                    val episodesView = currentView.findViewById<GridLayout>(R.id.episodesView)
                    createMediaSelections(episodesView, item, SelectionsType.Episode)
                }
            }
        }
    }
}