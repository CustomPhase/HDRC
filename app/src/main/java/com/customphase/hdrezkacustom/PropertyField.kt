package com.customphase.hdrezkacustom

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

abstract class PropertyField<T>(protected val getter: () -> T, protected val setter: (T) -> Unit) {
    class ChangedEvent {
        private val observers = mutableSetOf<() -> Unit>()
        operator fun plusAssign(observer: () -> Unit) {
            observers.add(observer)
        }
        operator fun minusAssign(observer: () -> Unit) {
            observers.remove(observer)
        }
        operator fun invoke() {
            for (observer in observers)
                observer()
        }
    }

    var onChange = ChangedEvent()

    fun setValue(value : T) {
        setter.invoke(value)
        onChange.invoke()
    }

    fun getValue() : T {
        return getter.invoke()
    }

    fun createView(inflater : LayoutInflater, container : ViewGroup, title : String) {
        val propView = inflater.inflate(R.layout.property_field_line, container, false)
        val labelView = propView.findViewById<TextView>(R.id.propertyFieldLabel)
        labelView.text = title

        val inputParent = propView.findViewById<LinearLayout>(R.id.propertyFieldInput)
        createInputView(inflater, inputParent)

        container.addView(propView)
    }

    abstract fun createInputView(inflater: LayoutInflater, container: ViewGroup)
}