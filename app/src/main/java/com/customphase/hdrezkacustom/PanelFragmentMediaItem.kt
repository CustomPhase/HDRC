package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class PanelFragmentMediaItem : PanelFragment() {
    override val iconResource: Int
        get() = -1
    override val title: String
        get() = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_media_item, container, false)
        return view
    }

    fun loadMedia(url: String) {
        view?.findViewById<TextView>(R.id.media_item_title)?.text = url
    }
}