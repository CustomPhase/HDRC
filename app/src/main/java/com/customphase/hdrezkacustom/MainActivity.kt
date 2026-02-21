package com.customphase.hdrezkacustom

import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private val parser = HDRezkaParser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
            Toast.makeText(this, "Клик: ${searchResult.title}", Toast.LENGTH_SHORT).show()
            // TODO: Переход к просмотру
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Обработка поиска
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    performSearch(query)
                }
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
            }
        }
    }
}