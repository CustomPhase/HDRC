package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanelFragmentMediaItem : PanelFragment() {
    override val iconResource: Int
        get() = -1
    override val title: String
        get() = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_media_item, container, false)
        return view
    }

    fun loadMedia(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val item = (activity as MainActivity).parser.getMediaItem(url)
            withContext(Dispatchers.Main) {
                val currentView = view
                if (currentView != null) {
                    currentView.findViewById<TextView>(R.id.media_item_title)?.text = item.title
                    currentView.findViewById<TextView>(R.id.media_item_desc)?.text = item.description

                    val translatorsView = currentView.findViewById<GridLayout>(R.id.translatorsView)
                    createMediaSelections(translatorsView, item.translators)

                    val seasonsView = currentView.findViewById<GridLayout>(R.id.seasonsView)
                    createMediaSelections(seasonsView, item.seasons)

                    val episodesView = currentView.findViewById<GridLayout>(R.id.episodesView)
                    createMediaSelections(episodesView, item.episodes)
                }
            }
        }
    }

    private fun createMediaSelections(targetView : ViewGroup, selections : List<MediaSelection>) {
        targetView.removeAllViews()
        for (n in selections.indices) {
            val btn = layoutInflater.inflate(R.layout.button_radio, targetView, false) as Button
            btn.text = selections[n].title

            val params = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                width = 0
                setMargins(4, 4, 4, 4)
            }
            btn.layoutParams = params

            if (selections[n].active) btn.isActivated = true

            targetView.addView(btn)
        }
    }
}