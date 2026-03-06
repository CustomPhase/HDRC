package com.customphase.hdrezkacustom

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.slider.Slider
import kotlin.math.ceil

class PropertyFieldFloatSlider(getter: () -> Float,
                               setter: (Float) -> Unit,
                               private val rangeMin : Float,
                                private val rangeMax : Float,
                                private val stepSize : Float

) : PropertyField<Float>(getter, setter) {
    override fun createInputView(inflater : LayoutInflater, container : ViewGroup) {
        val inputView = inflater.inflate(R.layout.property_field_input_float_slider, container, false) as ViewGroup

        val slider = inputView.findViewById<Slider>(R.id.propertyFieldInputSlider)
        val txt = inputView.findViewById<TextView>(R.id.propertyFieldInputValueText)

        slider.valueFrom = rangeMin
        slider.valueTo = rangeMax
        slider.stepSize = stepSize
        slider.value = getValue()
        setText(txt)
        slider.addOnChangeListener { slider, f, bool ->
            setValue(f)
        }
        onChange += {
            if (slider.value != getValue()) {
                slider.value = getValue()
            }
            setText(txt)
        }

        container.addView(inputView)
    }

    private fun setText(txt : TextView) {
        txt.text = ceil(100f + getValue()).toInt().toString() + "%"
    }
}