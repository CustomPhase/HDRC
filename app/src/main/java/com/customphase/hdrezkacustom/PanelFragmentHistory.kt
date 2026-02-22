package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PanelFragmentHistory : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.icon_history
    override val title: String
        get() = "История"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_history, container, false)
        return view
    }
}