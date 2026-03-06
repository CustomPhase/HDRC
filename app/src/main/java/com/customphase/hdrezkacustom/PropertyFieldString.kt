package com.customphase.hdrezkacustom

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doOnTextChanged

class PropertyFieldString(getter: () -> String,
                          setter: (String) -> Unit,
                          private val inputType : Int
) : PropertyField<String>(getter, setter) {
    override fun createInputView(inflater : LayoutInflater, container : ViewGroup) {
        val inputView = inflater.inflate(R.layout.property_field_input_string, container, false) as EditText
        inputView.inputType = inputType
        inputView.setText(getValue())
        inputView.doOnTextChanged { text, start, before, count ->
            setValue(text.toString())
        }
        onChange += {
            if (inputView.text.toString() != getValue()) {
                inputView.setText(getValue())
            }
        }
        container.addView(inputView)
    }
}