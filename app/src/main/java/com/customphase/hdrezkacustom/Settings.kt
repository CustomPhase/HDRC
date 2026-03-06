package com.customphase.hdrezkacustom

import android.text.InputType
import androidx.annotation.Keep

@Keep
class Settings {
    @Transient // non-serialized
    var onUpdate: (() -> Unit)? = null

    private var loadImages: Boolean = true
    @Transient
    val loadImagesProp = PropertyFieldBoolean(
        {loadImages},
        {loadImages = it; onUpdate?.invoke()}
    )

    private var brightness: Float = 0f
    @Transient
    val brightnessProp = PropertyFieldFloatSlider(
        {brightness},
        {brightness = it; onUpdate?.invoke()},
        0f,
        100f,
        5f
    )

    private var byeDpiStrategy = "-s1 -q1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1"
    @Transient
    val byeDpiStrategyProp = PropertyFieldString(
        {byeDpiStrategy},
        {byeDpiStrategy = it; onUpdate?.invoke()},
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
    )

    private var loginName : String = ""
    @Transient
    val loginNameProp = PropertyFieldString(
        {loginName},
        {loginName = it; onUpdate?.invoke()},
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    )

    private var loginPass : String = ""
    @Transient
    val loginPassProp = PropertyFieldString(
        {loginPass},
        {loginPass = it; onUpdate?.invoke()},
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    )
}