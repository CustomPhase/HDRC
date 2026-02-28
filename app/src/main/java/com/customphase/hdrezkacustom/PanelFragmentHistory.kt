package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PanelFragmentHistory : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.icon_history
    override val title: String
        get() = "История"

    private var watchHistoryView : LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_history, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        watchHistoryView = view.findViewById(R.id.watchHistoryView)
        rebuildWatchHistory()
    }

    override fun onEnable() {
        rebuildWatchHistory()
    }

    fun rebuildWatchHistory() {
        if (watchHistoryView == null) return
        lifecycleScope.launch {
            watchHistoryView?.removeAllViews()
            val watchHistory = (activity as MainActivity).saveDataManager.getWatchHistory()
            for (item in watchHistory) {
                val itemView = layoutInflater.inflate(R.layout.watch_history_item, watchHistoryView, false)
                val title = itemView.findViewById<TextView>(R.id.watchHistoryItemTitle)
                title.text = item.value.title
                val info = itemView.findViewById<TextView>(R.id.watchHistoryItemInfo)
                info.text = getMediaInfoAsString(item.value.selection.seasonId, item.value.selection.episodeId)
                val resumeButton = itemView.findViewById<Button>(R.id.watchHistoryItemResume)
                resumeButton.setOnClickListener {
                    (activity as MainActivity).showPlayerPanel(
                        item.value.title,
                        item.key,
                        item.value.selection.translatorId,
                        item.value.selection.seasonId,
                        item.value.selection.episodeId,
                        item.value.selection.isDirector,
                        item.value.currentTime
                    )
                }
                val deleteButton = itemView.findViewById<Button>(R.id.watchHistoryItemDelete)
                deleteButton.setOnClickListener {
                    (activity as MainActivity).saveDataManager.deleteWatchHistoryItem(item.key)
                    rebuildWatchHistory()
                }
                watchHistoryView?.addView(itemView)
            }
        }
    }
}