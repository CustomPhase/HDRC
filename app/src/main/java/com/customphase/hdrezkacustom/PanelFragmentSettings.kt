package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ToggleButton
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PanelFragmentSettings : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.icon_settings
    override val title: String
        get() = "Настройки"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_settings, container, false)

        val loadImages = view.findViewById<ToggleButton>(R.id.loadImages)
        val loginNameField = view.findViewById<EditText>(R.id.loginNameField)
        val loginPassField = view.findViewById<EditText>(R.id.loginPassField)
        val deleteDataButton = view.findViewById<Button>(R.id.deleteDataButton)
        val saveDataManager = (activity as MainActivity).saveDataManager

        loadImages.isChecked = settings.loadImages
        loadImages.setOnClickListener {
            settings.loadImages = loadImages.isChecked
        }

        lifecycleScope.launch {
            val name = settings.loginName
            loginNameField.setText(name)
            loginNameField.setSelection(name.length)

            val pass = settings.loginPass
            loginPassField.setText(pass)
            loginPassField.setSelection(pass.length)
        }

        loginNameField.addTextChangedListener {
            settings.loginName = it.toString()
        }

        loginPassField.addTextChangedListener {
            settings.loginPass = it.toString()
        }

        deleteDataButton.setOnClickListener {
            showDeleteConfirmation() {
                saveDataManager.delete()
            }
        }

        return view
    }

    private fun showDeleteConfirmation(onConfirm : () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_data))
            .setMessage(getString(R.string.delete_data_confirmation))
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Удалить") { _, _ ->
                onConfirm()
            }
            .show()
    }
}