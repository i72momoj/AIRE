package org.i72momoj.aire.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import org.fossify.commons.extensions.toStringSet
import org.fossify.commons.extensions.toast
import org.i72momoj.aire.extensions.dbHelper
import org.i72momoj.aire.extensions.getEnabledAlarms
import org.i72momoj.aire.helpers.CONFIRMAR_BORRADO
import org.i72momoj.aire.helpers.CONFIRMAR_CREACION
import org.i72momoj.aire.helpers.CONFIRMAR_MODIFICACION
import org.i72momoj.aire.helpers.CONFIRMAR_VOZ
import org.i72momoj.aire.models.Alarm
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
                is Comando.CrearAlarma -> {
                    if(comandoModel.confirmacionAlarma.value == true)
                        crearAlarmaConfirmacion()
                    else
                        crearAlarma()
                }
                is Comando.ModificarAlarma -> {
                    if(comandoModel.confirmacionAlarma.value == true)
                        modificarAlarmaConfirmacion()
                    else
                        modificarAlarma()
                }
                is Comando.CambiarEstadoAlarma -> {
                    cambiarEstadoAlarma()
                }
                is Comando.BorrarAlarma -> {
                    if(comandoModel.confirmacionAlarma.value == true)
                        borrarAlarmaConfirmacion()
                    else
                        borrarAlarma()
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
        val layout = if(confirmacionVoz == true) CONFIRMAR_VOZ or CONFIRMAR_CREACION else CONFIRMAR_CREACION;

        // Llamamos a openEditAlarm para permitir al usuario confirmar/modificar/cancelar
        // la alarma creada
        if (comandoModel.alarma.value != null) {
            openEditAlarm(
                alarm = comandoModel.alarma.value!!,
                restore = false,
                layout = layout,
                interprete = interprete
            ) {
                // Comprobamos que no han habido errores al añadir la alarma a la BD
                if (it > -1)
                    activity?.toast("Alarma añadida con éxito")
            }
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
        // Comprobamos si el usuario ha configurado el asistente para usar confirmación por voz
        val confirmacionVoz = comandoModel.confirmarVoz.value

        // Ajustamos ciertos valores a la elección del usuario
        val interprete = if(confirmacionVoz == true) comandoModel.interprete.value else null;
        val layout = if(confirmacionVoz == true) CONFIRMAR_VOZ or CONFIRMAR_MODIFICACION else CONFIRMAR_MODIFICACION

        val alarmaOriginal = comandoModel.alarma.value!!
        val alarmaActualizada = comandoModel.alarmaActualizada.value!!

        val coincidencias = ArrayList<Alarm>()

        getSortedAlarms { alarmas ->
            for(alarma in alarmas) {
                Log.d("modificarAlarmaConfirmacion", "ORIGINAL: ${alarmaOriginal.timeInMinutes}, ALARMA -> ID: ${alarma.id}, TIEMPO: ${alarma.timeInMinutes}")

                if (alarma.timeInMinutes == alarmaOriginal.timeInMinutes) {
                    coincidencias.add(alarma)
                }
            }
            if (!coincidencias.isEmpty()) {
                for(alarma in coincidencias) {
                    alarma.timeInMinutes = alarmaActualizada.timeInMinutes

                    openEditAlarm(
                        alarm = alarma,
                        restore = false,
                        layout = layout,
                        interprete = interprete
                    ) {
                        // Comprobamos que no han habido errores al añadir la alarma a la BD
                        if (it > -1)
                            activity?.toast("Alarma modificada con éxito")
                    }
                }
            }
            else
                activity?.toast("No se ha encontrado la alarma")
        }
    }

    private fun modificarAlarma() {
        val alarmaOriginal = comandoModel.alarma.value!!
        val alarmaActualizada = comandoModel.alarmaActualizada.value!!
        val coincidencias = ArrayList<Alarm>()

        getSortedAlarms { alarmas ->
            for(alarma in alarmas) {
                Log.d("modificarAlarmaConfirmacion", "ORIGINAL: ${alarmaOriginal.timeInMinutes}, ALARMA -> ID: ${alarma.id}, TIEMPO: ${alarma.timeInMinutes}")

                if (alarma.timeInMinutes == alarmaOriginal.timeInMinutes) {
                    coincidencias.add(alarma)
                }
            }
            if (!coincidencias.isEmpty()) {
                for(alarma in coincidencias) {
                    alarma.timeInMinutes = alarmaActualizada.timeInMinutes

                    val actualizada = requireContext().dbHelper.updateAlarm(alarma)

                    checkAlarmState(alarma)

                    // Comprobamos que no han habido errores al añadir la alarma a la BD
                    if (actualizada)
                        activity?.toast("Alarma actualizada con éxito")
                    else
                        activity?.toast("Error al actualizar la alarma")
                }
                setupAlarms()

            }
            else
                activity?.toast("No se ha encontrado la alarma")
        }
    }

    private fun borrarAlarmaConfirmacion() {
        // Comprobamos si el usuario ha configurado el asistente para usar confirmación por voz
        val confirmacionVoz = comandoModel.confirmarVoz.value

        // Ajustamos ciertos valores a la elección del usuario
        val interprete = if(confirmacionVoz == true) comandoModel.interprete.value else null;
        val layout = if(confirmacionVoz == true) CONFIRMAR_VOZ  or CONFIRMAR_BORRADO else CONFIRMAR_BORRADO

        val alarmaOriginal = comandoModel.alarma.value!!

        val coincidencias = ArrayList<Alarm>()

        getSortedAlarms { alarmas ->
            for(alarma in alarmas) {
                Log.d("modificarAlarmaConfirmacion", "ORIGINAL: ${alarmaOriginal.timeInMinutes}, ALARMA -> ID: ${alarma.id}, TIEMPO: ${alarma.timeInMinutes}")

                if (alarma.timeInMinutes == alarmaOriginal.timeInMinutes) {
                    coincidencias.add(alarma)
                }
            }
            if (!coincidencias.isEmpty()) {
                for(alarma in coincidencias) {
                    openEditAlarm(
                        alarm = alarma,
                        restore = false,
                        layout = layout,
                        interprete = interprete
                    ) {
                        // Comprobamos que no han habido errores al añadir la alarma a la BD
                        if (it > -1)
                            activity?.toast("Alarma eliminada con éxito")
                    }
                }
            }
            else
                activity?.toast("No se ha encontrado la alarma")
        }
    }

    private fun borrarAlarma() {
        val alarmaObjetivo = comandoModel.alarma.value!!

        val coincidencias = ArrayList<Alarm>()

        getSortedAlarms { alarmas ->
            for(alarma in alarmas) {
                Log.d("borrarAlarma", "OBJETIVO: ${alarmaObjetivo.timeInMinutes}, ALARMA -> ID: ${alarma.id}, TIEMPO: ${alarma.timeInMinutes}")

                if (alarma.timeInMinutes == alarmaObjetivo.timeInMinutes) {
                    coincidencias.add(alarma)
                }
            }
            if (!coincidencias.isEmpty()) {
                requireContext().dbHelper.deleteAlarms(coincidencias)

                activity?.toast("Alarma actualizada con éxito")
                setupAlarms()
            }
            else
                activity?.toast("No se ha encontrado la alarma")
        }
    }

    private fun cambiarEstadoAlarma() {

        val alarmaObjetivo = comandoModel.alarma.value!!
        val activada = comandoModel.nuevoEstado.value!!

        val coincidencias = ArrayList<Alarm>()

        getSortedAlarms { alarmas ->
            for(alarma in alarmas) {
                Log.d("modificarAlarmaConfirmacion", "OBJETIVO: ${alarmaObjetivo.timeInMinutes}, ALARMA -> ID: ${alarma.id}, TIEMPO: ${alarma.timeInMinutes}")

                if (alarma.timeInMinutes == alarmaObjetivo.timeInMinutes) {
                    coincidencias.add(alarma)
                }
            }
            if (!coincidencias.isEmpty()) {
                for(alarma in coincidencias)
                    alarmToggled(alarma.id, activada)

                setupAlarms()

                if(activada)
                    activity?.toast("Alarma activada")
                else
                    activity?.toast("Alarma desactivada")
            }
            else
                activity?.toast("No se ha encontrado la alarma")
        }

    }
}
