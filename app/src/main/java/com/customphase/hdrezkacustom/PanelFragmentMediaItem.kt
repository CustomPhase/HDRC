package com.customphase.hdrezkacustom

import android.os.Bundle
import android.service.credentials.Action
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_media_item, container, false)
        return view
    }

    fun loadMediaItem(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                view?.findViewById<TextView>(R.id.media_item_title)?.text = ""
                view?.findViewById<View>(R.id.media_item_content)?.visibility = View.GONE
                view?.findViewById<View>(R.id.mediaItemLoadingIndicator)?.visibility = View.VISIBLE

                view?.findViewById<View>(R.id.media_item_translators)?.visibility = View.GONE
                view?.findViewById<View>(R.id.media_item_seasons)?.visibility = View.GONE
                view?.findViewById<View>(R.id.media_item_episodes)?.visibility = View.GONE
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
                view?.findViewById<View>(R.id.media_item_content)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.mediaItemLoadingIndicator)?.visibility = View.GONE

                view?.findViewById<TextView>(R.id.media_item_title)?.text = item.title
                view?.findViewById<TextView>(R.id.media_item_description)?.text = item.description

                createMediaSelections(
                    R.id.media_item_translators,
                    R.string.translators,
                    item.translators,
                    selectedSeasonId,
                    isMovie
                ) { p1, p2 ->
                    onTranslatorClick(p1, p2)
                }

                if (!isMovie) {
                    createMediaSelections(
                        R.id.media_item_seasons,
                        R.string.seasons,
                        item.seasons,
                        selectedSeasonId,
                        isMovie
                    ) { p1, p2 ->
                        onSeasonClick(p1, p2)
                    }

                    createMediaSelections(
                        R.id.media_item_episodes,
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

        val parentView = currentView.findViewById<LinearLayout>(parentId)
        parentView.visibility = View.VISIBLE

        val title = parentView.children.first{it is TextView} as TextView
        title.text = getString(titleId)
        val grid = parentView.children.first{it is GridLayout} as GridLayout
        grid.removeAllViews()

        for (sel in selections) {
            val btn = layoutInflater.inflate(R.layout.button_radio, grid, false) as Button
            btn.text = sel.title
            if (parentId != R.id.media_item_episodes) {
                btn.isActivated = if (isMovie) false else sel.active
            } else {
                btn.visibility = if (sel.seasonId == selectedSeasonId) View.VISIBLE else View.GONE
            }

            val params = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                width = 0
                setMargins(0, 4, 8, 4)
            }
            btn.layoutParams = params
            btn.tag = sel
            btn.setOnClickListener {
                onClick(btn, isMovie)
            }
            grid.addView(btn)
        }
    }

    private fun onTranslatorClick(btn : View, isMovie : Boolean) {
        if (btn.isActivated) return

        val parent = btn.parent as GridLayout
        for(other in parent.children) {
            other.isActivated = false
        }
        btn.isActivated = true
        val sel = btn.tag as MediaItemSelection

        if (isMovie) {
            lifecycleScope.launch(Dispatchers.IO) {
                val url = (activity as MainActivity).parser.fetchCdnSeries(
                    true,
                    currentItemId,
                    sel.translatorId,
                    0,
                    0,
                    sel.isDirector
                )
                if (url != null) {
                    (activity as MainActivity).showPlayerPanel(url)
                }
            }
        } else {

        }

        /*lifecycleScope.launch(Dispatchers.IO) {
            val item = (activity as MainActivity).parser.getMediaItem(sel.url)
            println(item.seasons.size)
            var selectedSeasonId = -1;
            if (item.seasons.isNotEmpty()) {
                selectedSeasonId = item.seasons.firstOrNull { it.active }?.seasonId ?: -1
            }
            withContext(Dispatchers.Main) {
                val currentView = view
                if (currentView != null) {
                    createMediaSelections(
                        R.id.media_item_seasons,
                        R.string.seasons,
                        item.seasons,
                        selectedSeasonId
                    ) {
                        onSeasonClick(it)
                    }

                    createMediaSelections(
                        R.id.media_item_episodes,
                        R.string.episodes,
                        item.episodes,
                        selectedSeasonId
                    ) {
                        onEpisodeClick(it)
                    }
                }
            }
        }*/
    }

    private fun onSeasonClick(btn : View, isMovie : Boolean) {
        /*if (currentSeasonId == seasonId) return
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
        }*/
    }

    private fun onEpisodeClick(btn : View, isMovie : Boolean) {
        /*lifecycleScope.launch(Dispatchers.IO) {
            val streamUrl = (activity as MainActivity).parser.fetchCdnSeries(
                currentItemId,
                currentTranslatorId,
                currentSeasonId,
                episodeId
            )
            (activity as MainActivity).showPlayerPanel(streamUrl ?: "")
        }*/
    }

    private fun updateMediaSelections(url: String) {
        /*lifecycleScope.launch(Dispatchers.IO) {
            val item = (activity as MainActivity).parser.getMediaItem(url)
            withContext(Dispatchers.Main) {
                val currentView = view
                if (currentView != null) {
                    //val translatorsView = currentView.findViewById<GridLayout>(R.id.translatorsView)
                    //createMediaSelections(translatorsView, item, SelectionsType.Translator)
                    val seasonsView = currentView.findViewById<GridLayout>(R.id.seasonsView)
                    createMediaSelections(seasonsView, item, SelectionsType.Season)
                    val episodesView = currentView.findViewById<GridLayout>(R.id.episodesView)
                    createMediaSelections(episodesView, item, SelectionsType.Episode)
                }
            }
        }*/
    }
}