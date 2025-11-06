package org.i72momoj.aire.models


import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.pow

/**
 * Representa una alarma creada por la aplicación
 * @property id     Identificador de la alarma
 * @property days   Entero que actúa como vector de bits {D,S,V,J,X,M,L} que indica el día que se repetirá la alarma.
 *                  Si su valor es menor que 0, según la hora asignada sonará solo hoy, o solo mañana
 * @property isEnabled  Indica si está activa (true) o no desactivada (false)
 * @property vibrate    Indica si la vibración está activada (true) o no (false)
 * @property soundTitle Indica el título del sonido de alarma
 * @property soundUri   Indica el URI del sonido de alarma
 * @property label      Indica la etiqueta de la alarma
 * @property oneShot    Indica si la alarma debe ser eliminada una vez haya sonado
 */
class AlarmaAsistente(
    id: Int,
    timeInMinutes: Int,
    days: Int,
    isEnabled: Boolean,
    vibrate: Boolean,
    soundTitle: String,
    soundUri: String,
    label: String,
    oneShot: Boolean = false,
) : Alarm(
    id,
    timeInMinutes,
    days,
    isEnabled,
    vibrate,
    soundTitle,
    soundUri,
    label,
    oneShot,
) {
    companion object {
        private const val MTN_SIN_ESPECIFICAR : Int = -1
        private const val MTN_MANIANA : Int = 0
        private const val MTN_TARDE : Int = 1
        private const val MTN_NOCHE : Int = 2
        private const val DSEM_LUNES : Int = 1
        private const val DSEM_MARTES : Int = 2
        private const val DSEM_MIERCOLES : Int = 4
        private const val DSEM_JUEVES : Int = 8
        private const val DSEM_VIERNES : Int = 16
        private const val DSEM_SABADO : Int = 32
        private const val DSEM_DOMINGO : Int = 64

        private fun extraerValor(valor: String): Int {

            val valorExtraido = when(valor) {
                "un" -> 1
                "una" -> 1
                "uno" -> 1
                "dos" -> 2
                "tres" -> 3
                "cuatro" -> 4
                "cinco" -> 5
                "seis" -> 6
                "siete" -> 7
                "ocho" -> 8
                "nueve" -> 9
                "diez" -> 10
                "once" -> 11
                "doce" -> 12
                "trece" -> 13
                "catorce" -> 14
                "quince" -> 15
                "dieciséis" -> 16
                "diecisiete" -> 17
                "dieciocho" -> 18
                "diecinueve" -> 19
                "veinte" -> 20
                "veintiuno" -> 21
                "veintidós" -> 22
                "veintitrés" -> 23
                "veinticuatro" -> 24
                "veinticinco" -> 25
                "veintiséis" -> 26
                "veintisiete" -> 27
                "veintiocho" -> 28
                "veintinueve" -> 29
                "treinta" -> 30
                "cuarenta" -> 40
                "cincuenta" -> 50
                "cuarto" -> 15
                "media" -> 30
                else -> -1
            }

            if (valorExtraido  < 0)
                throw ValorException("Valor no reconocido")

            return valorExtraido
        }
        private fun extraerHora(hora: String, mtn: Int): Int {
            var horaExtraida: Int
            try {
                horaExtraida = extraerValor(hora)

                if(horaExtraida > 23)
                    horaExtraida = -1

                if (horaExtraida < 12 && (mtn == MTN_TARDE || mtn == MTN_NOCHE))
                    horaExtraida += 12

                val formateador = DateTimeFormatter.ofPattern("HH")
                val horaAhora = LocalTime.now().format(formateador).toInt()

                if(horaExtraida < 12 && horaExtraida < horaAhora && (horaExtraida + 12) > horaAhora && mtn == MTN_SIN_ESPECIFICAR)
                    horaExtraida += 12
            }
            catch (e: ValorException) {
                horaExtraida = -1
                Log.e("AlarmaAsistente", e.message!!)
            }

            return horaExtraida
        }

        private fun extraerMinutos(minutos: String): Int {
            val datosMinutos = minutos.split(" ")
            var minutosExtraidos = 0

            try {
                // Si no es un número compuesto
                if (datosMinutos.size == 1)
                    minutosExtraidos = extraerValor(minutos)

                else if(datosMinutos.size == 2) {

                    if(datosMinutos[0] == "menos")
                        minutosExtraidos = - extraerValor(datosMinutos[1])

                    else if(datosMinutos[0] == "cero")
                        minutosExtraidos = extraerValor(datosMinutos[1])
                }
                // El único caso restante es que sean minutos en frases de tres palabras, por
                // ejemplo "cuarenta y cinco", pero de todas formas lo comprobamos
                else if(datosMinutos.size == 3 && datosMinutos[1] == "y") {
                    minutosExtraidos = extraerValor(datosMinutos[0])
                    minutosExtraidos += extraerValor(datosMinutos[2])
                }
            }
            catch (e: ValorException) {
                minutosExtraidos = -1
                Log.e("AlarmaAsistente", e.message!!)
            }

            return minutosExtraidos
        }

        fun desdeDatosModelo(datosExtraidos: ArrayList<Pair<String, String>>?, alarma: Alarm?): Alarm? {
            var hora = ""
            var minutos = ""
            var mtn: Int = -1
            var diasSemana: Int = 0

            val hoy = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val potenciasDias = mapOf("lunes" to 0, "martes" to 1, "miércoles" to 2, "jueves" to 3, "viernes" to 4, "sábado" to 5, "domingo" to 6)

            val hoyAlarma = potenciasDias[hoy]!!
            val manianaAlarma = hoyAlarma + 1
            val pasadoAlarma = manianaAlarma + 1

            for(datos in datosExtraidos!!) {
                Log.d("AlarmaAsistente", "desdeDatosModelo -> DATOS: ${datos.first}, ${datos.second}")

                when(datos.first)
                {
                    "B-HORA" -> hora += datos.second
                    "I-HORA" -> hora += " ${datos.second}"
                    "B-MIN" -> minutos += datos.second
                    "I-MIN" -> minutos += " ${datos.second}"
                    "B-MTN" -> {
                        mtn = when(datos.second)
                        {
                            "mañana" -> MTN_MANIANA
                            "tarde" -> MTN_TARDE
                            "noche" -> MTN_NOCHE
                            else -> -1
                        }
                    }
                    "B-DSEM" -> {

                        diasSemana += when(datos.second) {
                            "lunes" -> DSEM_LUNES
                            "martes" -> DSEM_MARTES
                            "miércoles" -> DSEM_MIERCOLES
                            "jueves" -> DSEM_JUEVES
                            "viernes" -> DSEM_VIERNES
                            "sábado" -> DSEM_SABADO
                            "domingo" -> DSEM_DOMINGO
                            "hoy" -> 2f.pow(hoyAlarma).toInt()
                            "mañana" -> 2f.pow(manianaAlarma).toInt()
                            "pasado" -> 2f.pow(pasadoAlarma).toInt()
                            else -> 0
                        }
                    }
                }
            }

            val horaInt = extraerHora(hora, mtn)
            val minutosInt = if(minutos.isNotEmpty()) extraerMinutos(minutos) else 0

            Log.d("Alarma desde Modelo", "HORA: $horaInt, MINUTOS: $minutosInt")

            if(horaInt != -1 && minutosInt != -1) {
                alarma?.id = 0
                alarma?.timeInMinutes = horaInt * 60 + minutosInt
                alarma?.days = if(diasSemana > 0) diasSemana else -1
                alarma?.isEnabled = true
                alarma?.vibrate = true

                return alarma
            }
            else return null
        }
    }
}

class ValorException(message: String): Exception(message)
