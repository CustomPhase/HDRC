package com.customphase.hdrezkacustom

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var parser : HDRezkaApi
    private var searchJob : Job? = null

    fun View.isKeyboardVisible(): Boolean {
        val insets = ViewCompat.getRootWindowInsets(this)
        return insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_search, container, false)

        if (activity == null) return view;
        val mainActivity = (activity as MainActivity)
        parser = mainActivity.hdrezkaApi

        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.searchRecyclerView)
        searchLoading = view.findViewById(R.id.searchLoadingIndicator)

        adapter = SearchAdapter { searchResult ->
            lifecycleScope.launch(Dispatchers.IO) {
                mainActivity.showMediaItemPanel(searchResult.url)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter

        setupSearchViewControls()

        return view
    }

    private fun setupSearchViewControls() {
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        searchEditText.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                return@setOnKeyListener true
            }
            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (searchEditText.isKeyboardVisible()) {
                    searchView.clearFocus()
                    recyclerView.requestFocus()
                } else {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
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