package org.i72momoj.aire.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ComandoAsistente: ViewModel() {
    private val _comando = MutableLiveData<Comando>()
    val comando: LiveData<Comando> get() = _comando
    val alarma = MutableLiveData<Alarm>()
    val alarmaActualizada = MutableLiveData<Alarm>()
    val temporizador =  MutableLiveData<Timer>()
    val interprete = MutableLiveData<Interprete>()
    val confirmacionAlarma = MutableLiveData<Boolean>()
    val confirmacionTemporizador = MutableLiveData<Boolean>()
    val confirmarVoz = MutableLiveData<Boolean>()
    val nuevoEstado = MutableLiveData<Boolean>()

    fun invocar(comando: Comando) {
        _comando.value = comando
    }
}

sealed class Comando {
    object CrearAlarma: Comando()
    object CrearAlarmaConfirmar : Comando()
    object ModificarAlarma: Comando()
    object BorrarAlarma: Comando()
    object CambiarEstadoAlarma: Comando()
    object CrearTemporizador: Comando()
    object CancelarTemporizadores: Comando()
}
