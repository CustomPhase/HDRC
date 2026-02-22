package com.customphase.hdrezkacustom

import androidx.fragment.app.Fragment

abstract class PanelFragment : Fragment() {
    abstract val iconResource: Int
    abstract val title: String
    open fun onEnable() {}
    open fun onDisable() {}
}