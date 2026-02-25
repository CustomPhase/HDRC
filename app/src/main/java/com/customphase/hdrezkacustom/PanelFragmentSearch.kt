package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanelFragmentSearch : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.icon_search
    override val title: String
        get() = "Поиск"

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchLoading : View
    private lateinit var adapter: SearchAdapter
    private lateinit var parser : HDRezkaParser
    private var searchJob : Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_search, container, false)

        if (activity == null) return view;
        val mainActivity = (activity as MainActivity)
        parser = mainActivity.parser

        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.searchRecyclerView)
        searchLoading = view.findViewById(R.id.searchLoadingIndicator)

        adapter = SearchAdapter { searchResult ->
            lifecycleScope.launch(Dispatchers.IO) {
                mainActivity.showMediaPanel(searchResult.url)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter

        // Обработка поиска
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                recyclerView.post{
                    recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
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

        return view
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchLoading.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        searchJob = lifecycleScope.launch(Dispatchers.IO) {
            val results = parser.search(query)
            withContext(Dispatchers.Main) {
                adapter.submitList(results)
                searchLoading.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }
}