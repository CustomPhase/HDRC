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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanelFragmentSearch : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.ic_search
    override val title: String
        get() = "Поиск"

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private lateinit var parser : HDRezkaParser

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_search, container, false)

        if (activity == null) return view;
        parser = (activity as MainActivity).parser

        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.searchRecyclerView)

        adapter = SearchAdapter { searchResult ->
            lifecycleScope.launch(Dispatchers.IO) {
                parser.getItemCard(searchResult.url)
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
        lifecycleScope.launch(Dispatchers.IO) {
            val results = parser.search(query)
            withContext(Dispatchers.Main) {
                adapter.submitList(results)
                recyclerView.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
}