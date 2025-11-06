package org.i72momoj.aire.models

import android.util.Log

class TemporizadorAsistente(
    id: Int,
    seconds: Int,
    state: TimerState,
    vibrate: Boolean,
    soundUri: String,
    soundTitle: String,
    label: String,
    createdAt: Long,
    channelId: String?,
    oneShot: Boolean
): Timer(
    id = id,
    seconds = seconds,
    state = state,
    vibrate = vibrate,
    soundUri = soundUri,
    soundTitle = soundTitle,
    label = label,
    createdAt = createdAt,
    channelId = channelId,
    oneShot = oneShot
) {
    companion object {

        private fun extraerValor(valor: String): Int {
            var valorExtraido = 0
            val datosValor = valor.split(" ")

            if (datosValor.size == 1) {
                when(datosValor[0]) {
                    "un" -> valorExtraido = 1
                    "uno" -> valorExtraido = 1
                    "una" -> valorExtraido = 1
                    "dos" -> valorExtraido = 2
                    "tres" -> valorExtraido = 3
                    "cuatro" -> valorExtraido = 4
                    "cinco" -> valorExtraido = 5
                    "seis" -> valorExtraido = 6
                    "siete" -> valorExtraido = 7
                    "ocho" -> valorExtraido = 8
                    "nueve" -> valorExtraido = 9
                    "diez" -> valorExtraido = 10
                    "once" -> valorExtraido = 11
                    "doce" -> valorExtraido = 12
                    "trece" -> valorExtraido = 13
                    "catorce" -> valorExtraido = 14
                    "quince" -> valorExtraido = 15
                    "dieciséis" -> valorExtraido = 16
                    "diecisiete" -> valorExtraido = 17
                    "dieciocho" -> valorExtraido = 18
                    "diecinueve" -> valorExtraido = 19
                    "veinte" -> valorExtraido = 20
                    "veintiuno" -> valorExtraido = 21
                    "veintidós" -> valorExtraido = 22
                    "veintitrés" -> valorExtraido = 23
                    "veinticuatro" -> valorExtraido = 24
                    "veinticinco" -> valorExtraido = 25
                    "veintiséis" -> valorExtraido = 26
                    "veintisiete" -> valorExtraido = 27
                    "veintiocho" -> valorExtraido = 28
                    "veintinueve" -> valorExtraido = 29
                    "treinta" -> valorExtraido = 30
                    "cuarenta" -> valorExtraido = 40
                    "cincuenta" -> valorExtraido = 50

                    "cuarto" -> valorExtraido = 15
                    "media" -> valorExtraido = 30
                    "medio" -> valorExtraido = 30
                }
            }
            else {
                when(datosValor[0]){
                    "treinta" -> valorExtraido += 30
                    "cuarenta" -> valorExtraido += 40
                    "cincuenta" -> valorExtraido += 50
                }

                when(datosValor[2]) {
                    "un" -> valorExtraido += 1
                    "uno" -> valorExtraido += 1
                    "dos" -> valorExtraido += 2
                    "tres" -> valorExtraido += 3
                    "cuatro" -> valorExtraido += 4
                    "cinco" -> valorExtraido += 5
                    "seis" -> valorExtraido += 6
                    "siete" -> valorExtraido += 7
                    "ocho" -> valorExtraido += 8
                    "nueve" -> valorExtraido += 9
                }
            }

            return valorExtraido
        }

        fun desdeDatosModelo(datosExtraidos: ArrayList<Pair<String, String>>?, temporizador: Timer): Timer {

            var horas = ""
            var minutos = ""
            var segundos = ""

            Log.d("Timer", "datos extraidos -> ${datosExtraidos.toString()}")

            try {
                for(datos in datosExtraidos!!) {
                    Log.d("Alarma desde Modelo", "DATOS: ${datos.first}, ${datos.second}")

                    when(datos.first){
                        "B-RELH" -> horas += datos.second
                        "I-RELH" -> horas += " ${datos.second}"
                        "B-RELM" -> minutos += datos.second
                        "I-RELM" -> minutos += " ${datos.second}"
                        "B-RELS" -> segundos += datos.second
                        "I-RELS" -> segundos += " ${datos.second}"
                    }
                }

                val horasInt = if (horas.isEmpty()) 0 else extraerValor(horas)
                val minutosInt = if (minutos.isEmpty()) 0 else extraerValor(minutos)
                val segundosInt = if (segundos.isEmpty()) 0 else extraerValor(segundos)

                Log.d("Timer", "horasInt: $horasInt, minutosInt: $minutosInt, segundosInt: $segundosInt")

                val tiempoASegundos = horasInt * 3600 + minutosInt * 60 + segundosInt

                if(tiempoASegundos < 86400 && tiempoASegundos != 0)
                    temporizador.seconds = tiempoASegundos
                else
                    temporizador.seconds = -1

                temporizador.vibrate = true
            }
            catch (e: Exception){
                Log.e("desdeDatosModelo", "Error al extraer información del temporizador")
                e.printStackTrace()
            }

            return temporizador
        }
    }
}
