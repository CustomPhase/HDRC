package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
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

    private var itemId : Int = 0
    private var translatorId : Int = 0
    private var seasonId : Int = 0
    private var episodeId : Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_player, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            player = ExoPlayer.Builder(requireContext()).build()
            val playerView = view.findViewById<PlayerView>(R.id.playerView)
            playerView.player = player
        }
    }

    override fun onEnable() {
        lifecycleScope.launch(Dispatchers.Main) {
            view?.findViewById<View>(R.id.exoPlayPause)?.requestFocus()
        }
    }

    override fun onDisable() {
        player.pause()
        lifecycleScope.launch {
            val pos = (player.currentPosition / 1000).toDouble()
            val dur = (player.duration / 1000).toDouble()
            withContext(Dispatchers.IO) {
                (activity as MainActivity).parser.saveProgress(
                    itemId,
                    translatorId,
                    seasonId,
                    episodeId,
                    pos / dur,
                    dur
                )
            }
        }
    }

    fun play(itemId : Int,
             translatorId : Int,
             seasonId : Int,
             episodeId : Int,
             isDirector : Boolean) {
        this.itemId = itemId
        this.translatorId = translatorId
        this.seasonId = seasonId
        this.episodeId = episodeId
        lifecycleScope.launch(Dispatchers.IO) {
            val streams = (activity as MainActivity).parser.getMediaStreamUrl(
                itemId,
                translatorId,
                seasonId,
                episodeId,
                isDirector
            )
            withContext(Dispatchers.Main) {
                val unsafeClient = (activity as MainActivity).parser.client
                val dataSourceFactory = OkHttpDataSource.Factory(unsafeClient)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streams.getValue("1080p")))
                player.setMediaSource(mediaSource)
                player.prepare()
                player.playWhenReady = true
            }
        }
    }
}