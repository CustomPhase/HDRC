package com.customphase.hdrezkacustom

class Settings {
    @Transient // non-serialized
    var onUpdate: (() -> Unit)? = null

    var loadImages: Boolean = true
        set(value) { field = value; onUpdate?.invoke() }

    var loginName : String = ""
        set(value) { field = value; onUpdate?.invoke() }

    var loginPass : String = ""
        set(value) { field = value; onUpdate?.invoke() }
}