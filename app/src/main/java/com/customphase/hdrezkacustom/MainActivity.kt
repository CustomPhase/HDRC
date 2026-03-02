package com.customphase.hdrezkacustom

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

const val BYEDPI_PROXY_ADDRESS = "127.0.0.1"
const val BYEDPI_PROXY_PORT = 1080

var byeDpiStarted = false

var settings = Settings()

fun getMediaInfoAsString(seasonId : Int, episodeId : Int) : String {
    return (if (seasonId > 0) " ($seasonId сезон" else "") +
            (if (episodeId > 0) ", $episodeId эпизод)" else "");
}

fun loadImageFromUrlIntoView(target : ImageView, url : String) {
    Glide.with(target.context)
        .load(url)
        .error(android.R.drawable.ic_menu_help)
        .into(target)
}

class MainActivity : AppCompatActivity() {

    private val panels = mutableMapOf<Class<out PanelFragment>, PanelFragment>()
    private val panelHistory = java.util.ArrayDeque<Class<out PanelFragment>>()
    private var currentPanel: PanelFragment? = null
    private val defaultPanel = PanelFragmentHistory::class.java
    private var backPressedTime: Long = 0
    private var exitInterval = 2000.0

    val hdrezkaApi by lazy { HDRezkaApi(this) }
    val saveDataManager by lazy {SaveDataManager(this, hdrezkaApi, lifecycleScope)}
    val byeDpiManager = ByeDpiManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panelHistory.isNotEmpty()) {
                    switchToPanel(panelHistory.pop(), false)
                } else {
                    if (System.currentTimeMillis() - backPressedTime < exitInterval) {
                        byeDpiManager.stop()
                        finishAffinity()
                        exitProcess(0)
                    } else {
                        clearGlideCache(this@MainActivity)
                        // Show a toast message on the first back press
                        Toast.makeText(this@MainActivity, getString(R.string.exit_confirm), Toast.LENGTH_SHORT).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        })

        lifecycleScope.launch {
            byeDpiManager.start(this@MainActivity)
            saveDataManager.loadSettings()
            withContext(Dispatchers.IO) {
                hdrezkaApi.warmup()
                hdrezkaApi.login(settings.loginName, settings.loginPass)
            }
            saveDataManager.loadWatchHistory()
            initializeFocusDebug()
            initializePanels()
            switchToPanel(defaultPanel, false)
        }
    }

    override fun onDestroy() {
        byeDpiManager.stop()
        super.onDestroy()
        clearGlideCache(this)
    }

    fun clearGlideCache(context: Context) {
        // Clear memory cache (must be called on the main thread)
        Glide.get(context).clearMemory()
        // Clear disk cache (must be called on a background/worker thread)
        GlobalScope.launch(Dispatchers.IO) {
            Glide.get(context).clearDiskCache()
        }
    }

    private fun initializeFocusDebug() {
        /*window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
            var oldName = "none"
            var newName = "none"
            if (oldFocus != null && oldFocus.id >= 0) oldName = resources.getResourceEntryName(oldFocus.id)
            if (newFocus != null && newFocus.id >= 0) newName = resources.getResourceEntryName(newFocus.id)
            println("FOCUS_CHANGE: Focus moved from $oldName to $newName")
        }*/
    }

    private fun initializePanels() {
        panels[PanelFragmentHistory::class.java] = PanelFragmentHistory()
        panels[PanelFragmentSearch::class.java] = PanelFragmentSearch()
        panels[PanelFragmentPlayer::class.java] = PanelFragmentPlayer()
        panels[PanelFragmentSettings::class.java] = PanelFragmentSettings()
        panels[PanelFragmentMediaItem::class.java] = PanelFragmentMediaItem()

        val transaction = supportFragmentManager.beginTransaction()
        for (panel in panels.values) {
            val parentViewId = if (panel is PanelFragmentPlayer) R.id.playerContainer else R.id.panelContainer
            transaction.add(parentViewId, panel).hide(panel)
        }
        transaction.commit()

        initializePanelsNavigation()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (currentPanel is PanelFragmentPlayer) {
                if ((currentPanel as PanelFragmentPlayer).handleInput(event.keyCode)) return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun initializePanelsNavigation() {
        val navigationContainer = findViewById<LinearLayout>(R.id.navigationContainer)
        for ((panelClass, fragment) in panels) {
            if (fragment.iconResource < 0) continue

            val panel = fragment as PanelFragment
            val itemView = layoutInflater.inflate(R.layout.navigation_button, navigationContainer, false)
            val icon = itemView.findViewById<ImageView>(R.id.nav_icon)
            val text = itemView.findViewById<TextView>(R.id.nav_title)
            icon.setImageResource(panel.iconResource)
            text.text = panel.title
            itemView.setOnClickListener {
                switchToPanel(panelClass, true)
            }
            navigationContainer.addView(itemView)
        }
    }

    private fun switchToPanel(panelClass: Class<out PanelFragment>, addToHistory: Boolean) {
        if (addToHistory && currentPanel != null) {
            if (panelHistory.size > 16) panelHistory.removeLast()
            panelHistory.push(currentPanel!!::class.java)
        }

        val targetPanel = panels[panelClass] ?: return
        if (targetPanel == currentPanel) return

        val transaction = supportFragmentManager.beginTransaction()
        currentPanel?.onDisable()
        currentPanel?.let { transaction.hide(it)}
        transaction.show(targetPanel)
        transaction.commit()
        targetPanel.onEnable()
        currentPanel = targetPanel
    }

    fun showMediaItemPanel(url: String) {
        switchToPanel(PanelFragmentMediaItem::class.java, true)
        val mediaItemPanel = panels[PanelFragmentMediaItem::class.java] as PanelFragmentMediaItem
        mediaItemPanel.loadMediaItem(url)
    }

    fun showPlayerPanel(itemTitle : String,
                        itemId : Int,
                        translatorId : Int,
                        seasonId : Int,
                        episodeId : Int,
                        isDirector : Boolean,
                        startTime : Long) {
        switchToPanel(PanelFragmentPlayer::class.java, true)
        (panels[PanelFragmentPlayer::class.java] as PanelFragmentPlayer).play(
            itemTitle,
            itemId,
            translatorId,
            seasonId,
            episodeId,
            isDirector,
            startTime
        )
    }
}