package org.i72momoj.aire.fragments

import androidx.fragment.app.activityViewModels
import org.i72momoj.aire.models.ComandoAsistente
import android.os.Bundle
import android.util.Log
import android.view.View
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.toast
import org.i72momoj.aire.extensions.secondsToMillis
import org.i72momoj.aire.extensions.timerHelper
import org.i72momoj.aire.models.Comando
import org.i72momoj.aire.models.TimerEvent
import org.greenrobot.eventbus.EventBus
import org.i72momoj.aire.models.TimerState
import kotlin.getValue

class TemporizadorAsistenteFragment: TimerFragment() {

    private val comandoModel: ComandoAsistente by activityViewModels()

    // Comprobamos si se ha hecho alguna llamada desde otro fragmento (el principal)
    // a alguna de las funciones del AlarmFragment
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observamos que se invoque a un comando
        comandoModel.comando.observe(viewLifecycleOwner) { comando ->
            // Si el comando está relacionado con temporizadores, lo ejecutamos
            when(comando) {
                // Si el usuario ha seleccionado crear temporizador, lo añadimos al sistema, refrescamos
                // el listado de temporizadores e iniciamos la cuenta atrás
                is Comando.CrearTemporizador -> {
                    crearTemporizador()
                }
                // Si el usuario ha seleccionado crear temporizador, lo eliminamos del sistema
                is Comando.CancelarTemporizadores -> {
                    cancelarTemporizadores()
                }
                else -> { }
            }
        }
    }

    private fun crearTemporizador() {
        val temporizador = comandoModel.temporizador.value
        val confirmacion = comandoModel.confirmacionTemporizador.value
        // TODO METER CONFIRMACION POR VOZ
        val confirmacionVoz = comandoModel.confirmarVoz.value

        if (confirmacion?: false) {
            if(temporizador != null) {
                activity?.run {
                    hideKeyboard()
                    openEditTimer(temporizador, restore = false) {
                        // Comprobamos que no han habido errores al añadir el temporizador a la BD
                        if(it > -1) {
                            // Iniciamos la cuenta atrás nada más añadido el nuevo temporizador
                            EventBus.getDefault().post(
                                TimerEvent.Start(it, temporizador.seconds.secondsToMillis)
                            )

                            // Mostramos mensaje de éxito
                            activity?.toast("Temporizador añadido con éxito")
                        }
                        // Si ha ocurrido algún error, se lo indicamos al usuario
                        else
                            activity?.toast("Error en la creación. Intentelo de nuevo")
                    }
                }
            }
            else
                Log.e("TemporizadorAsistenteFragment", "crearTemporizadorConfirmacion -> no se ha pasado ningún temporizador")
        }
        else {
            if (temporizador != null) {
                activity?.timerHelper?.insertOrUpdateTimer(temporizador) {
                    refreshTimers()
                    Log.d("Timer Fragment", "ID del temporizador creado: $it")

                    // Comprobamos que no han habido errores al añadir el temporizador a la BD
                    if (it > -1) {
                        // Iniciamos la cuenta atrás nada más añadido el nuevo temporizador
                        activarTemporizador(it.toInt(), temporizador.seconds.secondsToMillis)

                        // Mostramos mensaje de éxito
                        activity?.toast("Temporizador añadido con éxito")
                    }
                    // Si ha ocurrido algún error, se lo indicamos al usuario
                    else
                        activity?.toast("Error en la creación. Intentelo de nuevo")
                }
            }
            else
                Log.e("TemporizadorAsistenteFragment", "CrearTemporizador -> no se ha pasado ningún temporizador")
        }
    }

    private fun activarTemporizador(id: Int, duracion: Long) {
        // Paramos el temporizador
        EventBus.getDefault().post(
            TimerEvent.Start(id, duracion)
        )
    }

    private fun detenerTemporizador(id: Int, duracion: Long) {
        // Paramos el temporizador
        EventBus.getDefault().post(
            TimerEvent.Finish(id, duracion)
        )
    }
    private fun cancelarTemporizadores() {
        // Obtenemos todos los temporizadores
        activity?.timerHelper?.getTimers { temporizadores ->

            temporizadores.forEach {
                // Encontramos aquellos que están activos
                if(it.state == TimerState.Running) {
                    // Los detenemos
                    detenerTemporizador(it.id!!, it.seconds.secondsToMillis)

                    // Y los eliminamos
                    activity?.timerHelper?.deleteTimer(it.id!!)
                    refreshTimers()
                }
            }

            activity?.toast("Temporizadores cancelados")
        }
    }
}
