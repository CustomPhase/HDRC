package com.customphase.hdrezkacustom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PanelFragmentSettings : PanelFragment() {
    override val iconResource: Int
        get() = R.drawable.icon_settings
    override val title: String
        get() = "Настройки"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.panel_settings, container, false)

        val deleteDataButton = view.findViewById<Button>(R.id.deleteDataButton)
        val saveDataManager = (activity as MainActivity).saveDataManager
        val settingsContainer = view.findViewById<ViewGroup>(R.id.settingsContainer)

        settings.loadImagesProp.createView(layoutInflater, settingsContainer, getString(R.string.load_images))
        settings.brightnessProp.createView(layoutInflater, settingsContainer, getString(R.string.brightness))
        settings.byeDpiStrategyProp.createView(layoutInflater, settingsContainer, getString(R.string.byedpi_strategy))
        settings.loginNameProp.createView(layoutInflater, settingsContainer, "E-mail для входа")
        settings.loginPassProp.createView(layoutInflater, settingsContainer, "Пароль для входа")

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