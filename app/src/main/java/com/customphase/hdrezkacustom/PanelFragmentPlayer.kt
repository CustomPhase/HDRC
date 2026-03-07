package com.customphase.hdrezkacustom

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

@OptIn(UnstableApi::class)
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

    private lateinit var prevButton : ImageButton
    private lateinit var nextButton : ImageButton

    private var autoSaveJob : Job? = null

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
        prevButton = view.findViewById(R.id.exoPrevCustom)
        nextButton = view.findViewById(R.id.exoNextCustom)
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
        var effects = listOf<Effect>()
        if (settings.brightnessProp.getValue() > 0.1f) {
            effects = listOf(BrightnessEffect(1 + settings.brightnessProp.getValue()))
        }
        lifecycleScope.launch(Dispatchers.Main) {
            player.setVideoEffects(effects)
            (activity as MainActivity).findViewById<View>(R.id.navigationContainer).visibility = View.GONE
            playerView.requestFocus()
        }
        autoSaveJob = lifecycleScope.launch {
            while (true) {
                if (player.isPlaying) {
                    saveToWatchHistory()
                }
                delay(10000)
            }
        }
    }

    override fun onDisable() {
        (activity as MainActivity).findViewById<View>(R.id.navigationContainer).visibility = View.VISIBLE
        if (itemId == 0) return
        player.pause()
        saveToWatchHistory()
        autoSaveJob?.cancel()
    }

    fun handleInput(keyCode : Int) : Boolean {
        if (playerView.isControllerFullyVisible) return false
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

    private var seekCoroutine : Job? = null
    fun seekRelative(offsetMs: Long) {
        val newPosition = player.currentPosition + offsetMs
        val duration = player.duration
        val targetPosition = if (duration != C.TIME_UNSET) {
            newPosition.coerceIn(0, duration)
        } else {
            newPosition.coerceAtLeast(0)
        }
        playerView.useController = false
        seekCoroutine?.cancel()
        player.seekTo(targetPosition)
        seekCoroutine = lifecycleScope.launch {
            delay(50)
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

    fun setButtonState(btn : ImageButton, enabled : Boolean) {
        btn.setOnClickListener {  }
        lifecycleScope.launch {
            btn.isEnabled = enabled
            btn.isFocusable = enabled
            val color = if (enabled)
                ContextCompat.getColor((activity as MainActivity), R.color.main_text)
                else Color.DKGRAY
            btn.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
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
                player.stop()
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

            setButtonState(prevButton, false)
            setButtonState(nextButton, false)

            if (seasonId != 0) {
                val episodes = (activity as MainActivity).hdrezkaApi.getMediaEpisodes(itemId, translatorId)

                var seasonsAndEpisodes = mutableMapOf<Int, MutableList<Int>>()
                for(ep in episodes.episodes) {
                    if (!seasonsAndEpisodes.containsKey(ep.seasonId)) seasonsAndEpisodes[ep.seasonId] = mutableListOf()
                    seasonsAndEpisodes[ep.seasonId]?.add(ep.episodeId)
                }
                seasonsAndEpisodes.forEach { (_, episodes) ->
                    episodes.sort()
                }
                seasonsAndEpisodes = seasonsAndEpisodes.toSortedMap()

                val currentSeason = seasonsAndEpisodes[seasonId]!!

                var prevSeason = seasonId
                var prevEpisode = episodeId
                if (currentSeason.contains(episodeId - 1)) {
                    prevEpisode = episodeId - 1
                } else {
                    if (episodeId == 1 && seasonsAndEpisodes.containsKey(seasonId - 1)) {
                        prevSeason = seasonId - 1
                        prevEpisode = seasonsAndEpisodes[seasonId - 1]?.last()!!
                    }
                }

                var nextSeason = seasonId
                var nextEpisode = episodeId
                if (currentSeason.contains(episodeId + 1)) {
                    nextEpisode = episodeId + 1
                } else {
                    if (seasonsAndEpisodes.containsKey(seasonId + 1) &&
                        seasonsAndEpisodes[seasonId + 1]?.first()!! == 1) {
                        nextSeason = seasonId + 1
                        nextEpisode = 1
                    }
                }

                if (prevSeason != seasonId || prevEpisode != episodeId) {
                    setButtonState(prevButton, true)
                    prevButton.setOnClickListener {
                        play(itemTitle, itemId, translatorId, prevSeason,
                            prevEpisode, isDirector, 0)
                    }
                }

                if (nextSeason != seasonId || nextEpisode != episodeId) {
                    setButtonState(nextButton, true)
                    nextButton.setOnClickListener {
                        play(itemTitle, itemId, translatorId, nextSeason,
                            nextEpisode, isDirector, 0)
                    }
                }
            }
        }
    }
}