package com.customphase.hdrezkacustom

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    internal val panels = mutableMapOf<Class<out PanelFragment>, PanelFragment>()
    internal var currentPanel: PanelFragment? = null
    internal val defaultPanel = PanelFragmentHistory::class.java
    internal var backPressedTime: Long = 0
    internal var exitInterval = 2000.0

    val parser by lazy { HDRezkaParser(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Register the OnBackPressedCallback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime < exitInterval) {
                    // Exit the app on the second back press within the interval
                    finish()
                } else {
                    // Show a toast message on the first back press
                    Toast.makeText(this@MainActivity, "Нажмите еще раз чтобы выйти", Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })

        lifecycleScope.launch(Dispatchers.IO) {
            parser.warmup()
        }

        initializeFocusDebug()
        initializePanels()
        switchToPanel(defaultPanel)
    }

    private fun initializeFocusDebug() {
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
            var oldName = "none"
            var newName = "none"
            if (oldFocus != null && oldFocus.id >= 0) oldName = resources.getResourceEntryName(oldFocus.id)
            if (newFocus != null && newFocus.id >= 0) newName = resources.getResourceEntryName(newFocus.id)
            println("FOCUS_CHANGE: Focus moved from $oldName to $newName")
        }
    }

    private fun initializePanels() {
        panels[PanelFragmentHistory::class.java] = PanelFragmentHistory()
        panels[PanelFragmentSearch::class.java] = PanelFragmentSearch()
        panels[PanelFragmentSettings::class.java] = PanelFragmentSettings()

        val transaction = supportFragmentManager.beginTransaction()
        for (panel in panels.values) {
            transaction.add(R.id.panel_container, panel).hide(panel)
        }
        transaction.commit()

        createPanelsNavigation()
    }

    private fun createPanelsNavigation() {
        val navigationContainer = findViewById<LinearLayout>(R.id.navigation_container)
        panels.forEach { (panelClass, fragment) ->
            val panel = fragment as PanelFragment
            val itemView = layoutInflater.inflate(R.layout.navigation_button, navigationContainer, false)
            val icon = itemView.findViewById<ImageView>(R.id.nav_icon)
            val text = itemView.findViewById<TextView>(R.id.nav_title)
            icon.setImageResource(panel.iconResource)
            text.text = panel.title
            itemView.setOnClickListener {
                switchToPanel(panelClass)
            }
            navigationContainer.addView(itemView)
        }
    }

    private fun switchToPanel(panelClass: Class<out PanelFragment>) {
        val targetPanel = panels[panelClass] ?: return
        if (targetPanel == currentPanel) return

        val transaction = supportFragmentManager.beginTransaction()
        currentPanel?.onDisable()
        currentPanel?.let { transaction.hide(it)}
        transaction.show(targetPanel)
        transaction.commit()
        targetPanel.onEnable()
        currentPanel = targetPanel

        if (panelClass == PanelFragmentSearch::class.java) {
            targetPanel.view?.findViewById<SearchView>(R.id.searchView)?.requestFocus()
        }
    }
}