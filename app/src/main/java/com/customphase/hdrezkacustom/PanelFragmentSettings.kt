package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PanelFragmentSettings : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.ic_settings
    override val title: String
        get() = "Настройки"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_settings, container, false)
        return view
    }
}