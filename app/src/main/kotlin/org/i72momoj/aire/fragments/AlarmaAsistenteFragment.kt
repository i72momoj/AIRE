package org.i72momoj.aire.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import org.fossify.commons.extensions.toast
import org.i72momoj.aire.extensions.dbHelper
import org.i72momoj.aire.helpers.CONFIRMAR_CREACION
import org.i72momoj.aire.helpers.CONFIRMAR_VOZ
import org.i72momoj.aire.models.Comando
import org.i72momoj.aire.models.ComandoAsistente
import kotlin.getValue

class AlarmaAsistenteFragment: AlarmFragment() {

    private val comandoModel: ComandoAsistente by activityViewModels()

    // Comprobamos si se ha hecho alguna llamada desde otro fragmento (el principal)
    // a alguna de las funciones del AlarmFragment
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observamos que se invoque a un comando
        comandoModel.comando.observe(viewLifecycleOwner) { comando ->
            // Si el comando está relacionado con alarmas, ejecutamos
            when(comando) {
                // Si el usuario quiere confirmación antes de crear la alarma, la crearemos mediante un Dialog
                // donde el usuario podrá modificar los datos, como cuando se edita una alarma o cuando se crea
                // desde la pestaña de Alarmas
                is Comando.CrearAlarmaConfirmar -> {
                    crearAlarmaConfirmacion()
                }
                is Comando.CrearAlarma -> {
                    crearAlarma()
                }
                else -> { }
            }
        }
    }

    private fun crearAlarmaConfirmacion() {
        // Comprobamos si el usuario ha configurado el asistente para usar confirmación por voz
        val confirmacionVoz = comandoModel.confirmarVoz.value

        // Ajustamos ciertos valores a la elección del usuario
        val interprete = if(confirmacionVoz == true) comandoModel.interprete.value else null;
        val layout = if(confirmacionVoz == true) CONFIRMAR_VOZ else CONFIRMAR_CREACION;

        // Llamamos a openEditAlarm para permitir al usuario confirmar/modificar/cancelar
        // la alarma creada
        if (comandoModel.alarma.value != null) {
            openEditAlarm(
                alarm = comandoModel.alarma.value!!,
                restore = false,
                layout = layout,
                interprete = interprete
            )
        }
        else
            Log.e("AlarmaAsistenteFragment", "editarAlarma -> no se ha pasado ninguna alarma sobre la que operar")
    }

    private fun crearAlarma() {
        val alarma = comandoModel.alarma.value

        if (alarma != null) {
            val id = requireContext().dbHelper.insertAlarm(alarma)
            alarma.id = id

            setupAlarms()
            checkAlarmState(alarma)

            // Comprobamos que no han habido errores al añadir la alarma a la BD
            if (comandoModel.alarma.value!!.id > -1)
                activity?.toast("Alarma añadida con éxito")
        }
        else
            Log.e("Llamada a AlarmFragment", "crearAlarma -> no se ha pasado ninguna alarma sobre la que operar")
    }

    private fun modificarAlarmaConfirmacion() {

    }

    private fun modificarAlarma() {

    }

    private fun borrarAlarmaConfirmacion() {

    }

    private fun borrarAlarma() {

    }

    private fun cambiarEstadoAlarma(nuevoEstado: Boolean) {
        alarmToggled(1, true)
    }
}
