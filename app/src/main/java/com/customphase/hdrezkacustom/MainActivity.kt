package com.customphase.hdrezkacustom

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.isVisible
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private val parser by lazy { HDRezkaParser(this) }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus() // Optional: Also clear focus from the SearchView
    }

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
                if (recyclerView.isVisible) {
                    recyclerView.visibility = View.GONE
                    searchView.setQuery("", false)
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
            var oldName = "none"
            var newName = "none"
            if (oldFocus != null && oldFocus.id >= 0) oldName = resources.getResourceEntryName(oldFocus.id)
            if (newFocus != null && newFocus.id >= 0) newName = resources.getResourceEntryName(newFocus.id)
            println("FOCUS_CHANGE: Focus moved from $oldName to $newName")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Здесь выполняется сетевой запрос в фоновом потоке
            parser.warmup()
        }

        searchView = findViewById(R.id.searchView)
        recyclerView = findViewById(R.id.recyclerView)

        // Настройка RecyclerView
        adapter = SearchAdapter { searchResult ->
            // Здесь можно открыть экран с деталями фильма
            Toast.makeText(this, "Клик: ${searchResult.url}", Toast.LENGTH_SHORT).show()
            // TODO: Переход к просмотру
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Обработка поиска
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    performSearch(query)
                }
                return false
            }
        })
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val results = parser.search(query)
            withContext(Dispatchers.Main) {
                adapter.submitList(results)
                recyclerView.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
}