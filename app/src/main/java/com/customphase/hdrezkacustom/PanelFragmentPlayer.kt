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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PanelFragmentPlayer : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.icon_player
    override val title: String
        get() = "Плеер"

    private lateinit var player : ExoPlayer

    private var itemTitle : String = ""
    private var itemId : Int = 0
    private var translatorId : Int = 0
    private var seasonId : Int = 0
    private var episodeId : Int = 0

    private val speeds = arrayOf(0.5, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 2.0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_player, container, false)
        return view
    }

    fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            player = ExoPlayer.Builder(requireContext()).build()
            val playerView = view.findViewById<PlayerView>(R.id.playerView)
            playerView.player = player

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) {
                        saveToWatchHistory()
                    }
                }
            })

            for (speed in speeds) {
                val parent = playerView.findViewById<ViewGroup>(R.id.exoSpeedControls)
                val btn = layoutInflater.inflate(R.layout.button_radio, parent, false) as Button
                btn.text = "x$speed"
                btn.tag = speed
                val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    dpToPx(60),
                    dpToPx(34)
                )
                layoutParams.marginEnd = dpToPx(5)
                btn.setLayoutParams(layoutParams)
                if (speed > 0.99 && speed < 1.01) {
                    btn.isActivated = true
                }
                btn.setOnClickListener {
                    for(child in parent.children) {
                        (child as Button).isActivated = false
                        btn.isActivated = true
                        player.setPlaybackSpeed((btn.tag as Double).toFloat())
                    }
                }
                parent.addView(btn)
            }
        }
    }

    override fun onEnable() {
        lifecycleScope.launch(Dispatchers.Main) {
            (activity as MainActivity).findViewById<View>(R.id.navigationContainer).visibility = View.GONE
            view?.findViewById<View>(R.id.exoPlayPause)?.requestFocus()
        }
    }

    override fun onDisable() {
        (activity as MainActivity).findViewById<View>(R.id.navigationContainer).visibility = View.VISIBLE
        if (itemId == 0) return
        player.pause()
        saveToWatchHistory()
    }

    private fun saveToWatchHistory() {
        if (itemId == 0) return
        (activity as MainActivity).saveDataManager.addOrUpdateWatchHistoryItem(
            itemTitle,
            itemId,
            translatorId,
            seasonId,
            episodeId,
            player.currentPosition,
            player.duration
        )
    }

    fun play(itemTitle : String,
             itemId : Int,
             translatorId : Int,
             seasonId : Int,
             episodeId : Int,
             isDirector : Boolean,
             startTime : Long) {
        this.itemTitle = itemTitle
        this.itemId = itemId
        this.translatorId = translatorId
        this.seasonId = seasonId
        this.episodeId = episodeId
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                view?.findViewById<TextView>(R.id.exoTitle)?.text = itemTitle
                view?.findViewById<TextView>(R.id.exoInfo)?.text = getMediaInfoAsString(seasonId, episodeId)
            }
            val streams = (activity as MainActivity).hdrezkaApi.getMediaStreamUrl(
                itemId,
                translatorId,
                seasonId,
                episodeId,
                isDirector
            )
            withContext(Dispatchers.Main) {
                val unsafeClient = (activity as MainActivity).hdrezkaApi.client
                val dataSourceFactory = OkHttpDataSource.Factory(unsafeClient)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streams.entries.last().value))
                player.setMediaSource(mediaSource)
                player.seekTo(startTime)
                player.prepare()
                player.playWhenReady = true
            }
        }
    }
}