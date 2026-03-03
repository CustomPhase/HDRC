package com.customphase.hdrezkacustom

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

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

    private lateinit var playerView : PlayerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_player, container, false)
        playerView = view.findViewById(R.id.playerView)
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
            playerView.player = player
            playerView.useController = true
            playerView.controllerAutoShow = false

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
            playerView.requestFocus()
            playerView.hideController()
        }
    }

    override fun onDisable() {
        (activity as MainActivity).findViewById<View>(R.id.navigationContainer).visibility = View.VISIBLE
        if (itemId == 0) return
        player.pause()
        saveToWatchHistory()
    }

    fun handleInput(keyCode : Int) : Boolean {
        if (playerView.isControllerVisible) return false
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                seekRelative(-10000)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                seekRelative(10000)
                true
            }
            else -> {
                playerView.useController = true
                false
            }
        }
    }

    fun seekRelative(offsetMs: Long) {
        val newPosition = player.currentPosition + offsetMs
        val duration = player.duration
        val targetPosition = if (duration != C.TIME_UNSET) {
            newPosition.coerceIn(0, duration)
        } else {
            newPosition.coerceAtLeast(0)
        }
        playerView.useController = false
        player.seekTo(targetPosition)
        lifecycleScope.launch {
            delay(16)
            playerView.useController = true
        }
    }

    private fun saveToWatchHistory() {
        if (itemId == 0) return
        captureScreenshot {
            val finalScreenshot = it ?: createBitmap(1, 1)

            (activity as MainActivity).saveDataManager.addOrUpdateWatchHistoryItem(
                itemTitle,
                itemId,
                translatorId,
                seasonId,
                episodeId,
                player.currentPosition,
                player.duration,
                finalScreenshot
            )
        }
    }

    fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        val view = playerView.videoSurfaceView as SurfaceView
        val bitmap = createBitmap(160, 90)

        PixelCopy.request(view, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                callback(bitmap)
            } else {
                callback(null)
            }
        }, Handler(Looper.getMainLooper()))
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