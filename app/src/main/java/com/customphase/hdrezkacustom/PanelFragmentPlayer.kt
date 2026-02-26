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
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanelFragmentPlayer : PanelFragment() {
    override val iconResource: Int
        get() = -1
    override val title: String
        get() = ""

    private lateinit var player : ExoPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_player, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            player = ExoPlayer.Builder(requireContext()).build()
            val playerView = view.findViewById<StyledPlayerView>(R.id.playerView)
            playerView.player = player
        }
    }

    override fun onDisable() {
        player.stop()
    }

    fun play(streams : Map<String, String>) {
        lifecycleScope.launch(Dispatchers.Main) {
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