package org.i72momoj.aire.activities

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.i72momoj.aire.activities.SettingsActivity
import org.i72momoj.aire.extensions.dataStore
import org.i72momoj.aire.databinding.ActivitySettingsBinding
import org.fossify.commons.extensions.viewBinding

class AjustesAsistenteActivity: SettingsActivity() {
    override val binding: ActivitySettingsBinding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onResume() {
        super.onResume()

        setupConfirmarAlarmas()
        setupConfirmarTemporizadores()
        setupConfirmarPorVoz()
    }

    private fun setupConfirmarAlarmas() = binding.apply {
        val conf = root.context.dataStore
        runBlocking {
            val confirmarAlarmas: Boolean = conf.data.map { preferences ->
                preferences[booleanPreferencesKey("confirmarAlarmas")] ?: true
            }.first()

            settingsConfirmarAlarmas.isChecked = confirmarAlarmas
        }

        settingsConfirmarAlarmasHolder.setOnClickListener {
            settingsConfirmarAlarmas.toggle()

            lifecycleScope.launch(Dispatchers.IO) {
                conf.edit { preferences ->
                    preferences[booleanPreferencesKey("confirmarAlarmas")] = settingsConfirmarAlarmas.isChecked
                }
            }
        }
    }

    private fun setupConfirmarTemporizadores() = binding.apply {
        val conf = root.context.dataStore
        runBlocking {
            val confirmarTemporizadores: Boolean = conf.data.map { preferences ->
                preferences[booleanPreferencesKey("confirmarTemporizadores")] ?: true
            }.first()

            settingsConfirmarTemporizadores.isChecked = confirmarTemporizadores
        }

        settingsConfirmarTemporizadoresHolder.setOnClickListener {
            settingsConfirmarTemporizadores.toggle()

            lifecycleScope.launch(Dispatchers.IO) {
                conf.edit { preferences ->
                    preferences[booleanPreferencesKey("confirmarTemporizadores")] = settingsConfirmarTemporizadores.isChecked
                }
            }
        }
    }

    private fun setupConfirmarPorVoz() = binding.apply {
        val conf = root.context.dataStore
        runBlocking {
            val confirmarVoz: Boolean = conf.data.map { preferences ->
                    preferences[booleanPreferencesKey("confirmarPorVoz")] ?: false
            }.first()

            settingsConfirmarVoz.isChecked = confirmarVoz
        }

        settingsConfirmarVozHolder.setOnClickListener {
            settingsConfirmarVoz.toggle()

            lifecycleScope.launch(Dispatchers.IO) {
                conf.edit { preferences ->
                    preferences[booleanPreferencesKey("confirmarPorVoz")] = settingsConfirmarVoz.isChecked
                }
            }
        }
    }
}
