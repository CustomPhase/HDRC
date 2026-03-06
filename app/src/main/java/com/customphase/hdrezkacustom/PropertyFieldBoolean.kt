package com.customphase.hdrezkacustom

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton

class PropertyFieldBoolean (getter: () -> Boolean,
                            setter: (Boolean) -> Unit
) : PropertyField<Boolean>(getter, setter) {
    override fun createInputView(inflater : LayoutInflater, container : ViewGroup) {
        val inputView = inflater.inflate(R.layout.property_field_input_boolean, container, false) as ToggleButton
        inputView.isChecked = getValue()
        inputView.setOnClickListener {
            setValue(inputView.isChecked)
        }
        onChange += {
            if (inputView.isChecked != getValue()) {
                inputView.isChecked = getValue()
            }
        }
        container.addView(inputView)
    }
}