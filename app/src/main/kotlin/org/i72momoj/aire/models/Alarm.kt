package org.i72momoj.aire.models

import androidx.annotation.Keep
import org.i72momoj.aire.helpers.TODAY_BIT
import org.i72momoj.aire.helpers.TOMORROW_BIT
import kotlin.Int

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
@Keep
@kotlinx.serialization.Serializable
open class Alarm(
    var id: Int,
    var timeInMinutes: Int,
    var days: Int,
    var isEnabled: Boolean,
    var vibrate: Boolean,
    var soundTitle: String,
    var soundUri: String,
    var label: String,
    var oneShot: Boolean = false,
) {
    fun isRecurring() = days > 0
    fun isToday() = days == TODAY_BIT
    fun isTomorrow() = days == TOMORROW_BIT
}

@Keep
data class ObfuscatedAlarm(
    var a: Int,
    var b: Int,
    var c: Int,
    var d: Boolean,
    var e: Boolean,
    var f: String,
    var g: String,
    var h: String,
    var i: Boolean = false,
) {
    fun toAlarm() = Alarm(a, b, c, d, e, f, g, h, i)
}
