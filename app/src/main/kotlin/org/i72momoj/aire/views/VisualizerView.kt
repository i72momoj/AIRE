package org.i72momoj.aire.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class VisualizerView(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private var picos = ArrayList<RectF>()
    private var radio = 6f
    private var anchuraRect = 9f
    private var distanciaRect = 6f
    private var anchuraPantalla = 0f
    private var alturaPantalla = 0f
    private val color = Paint()
    private var checkColor: Boolean = false
    private var npicos: Int

    init {
        // Dado que el color principal es modificable, vamos a obtener el color que se está usando para
        // colorear el visualizer de forma dinámica
        val ty = TypedValue()
        val c = context.theme.resolveAttribute(android.R.attr.statusBarColor, ty, true)
        color.color = 0xFF000000.toInt() or ty.data // Convertimos el color de un color con alfa (transparencia) a un color normal

        anchuraPantalla = resources.displayMetrics.widthPixels.toFloat()
        alturaPantalla = 300f * resources.displayMetrics.ydpi / 160f
        Log.d("Visualizer", "XDPI -> ${resources.displayMetrics.xdpi}, YDPI -> ${resources.displayMetrics.ydpi}, Height -> ${height}, MeasuredHeight -> ${measuredHeight}")

        // Calculamos el número de picos que caben en lo ancho de la pantalla
        npicos = (anchuraPantalla / (anchuraRect + distanciaRect)).toInt()

        // Vamos añadiendo rectángulos de derecha a izquierda. Establecemos la anchura de cada uno
        // pero inicialmente establecemos que su altura es 0
        while(npicos > 0) {
            val izquierda: Float = npicos * (anchuraRect + distanciaRect)
            val arriba = 0f
            val derecha: Float = (izquierda + anchuraRect)
            val abajo = 0f

            picos.add(RectF(izquierda, arriba, derecha, abajo))

            npicos--
        }
    }


    fun actualizarAmplitud(nuevaAmplitud: Float) {
        if(checkColor) {
            val ty = TypedValue()
            val c = context.theme.resolveAttribute(android.R.attr.statusBarColor, ty, true)
            color.color = 0xFF000000.toInt() or ty.data
            checkColor = false

            Log.d("VisualizerView", "Color del visualizer actualizado")
        }

        // Obtenemos la altura y la anchura del view
        anchuraPantalla = measuredWidth.toFloat()
        alturaPantalla = measuredHeight.toFloat()

        // Calculamos cuantos picos caben en el view
        npicos = (anchuraPantalla / (anchuraRect + distanciaRect)).toInt()

        // Calculamos el número de picos que caben en lo ancho de la pantalla
        npicos = (anchuraPantalla / (anchuraRect + distanciaRect)).toInt()
        var npicosCont = npicos

        // Vamos añadiendo rectángulos de derecha a izquierda. Establecemos la anchura de cada uno
        // pero inicialmente establecemos que su altura es 0
        picos.clear()
        while(npicosCont > 0) {
            val izquierda: Float = npicosCont * (anchuraRect + distanciaRect)
            val arriba = 0f
            val derecha: Float = (izquierda + anchuraRect)
            val abajo = 0f

            picos.add(RectF(izquierda, arriba, derecha, abajo))
            npicosCont--
        }

        var cont: Float = -npicos / 2f

        //Log.d("VisualizerMetricas", "XDPI -> ${resources.displayMetrics.xdpi}, YDPI -> ${resources.displayMetrics.ydpi}, Height -> $height, Width -> $width, npicos -> $npicos")

        // Calculamos √2π una única vez para ahorrar cómputo
        val p = sqrt(2f*3.141592f)

        // La amplitud que ha captado el micrófono es invérsamente proporcional al efecto de la varianza
        // en la forma de la distribución normal; cuanto más grande es la varianza, la campana es más
        // platicúrtica (más llana) y cuanto menor es la varianza, la campana es más leptocúrtica (más puntiaguda).
        // Por el contrario, nosotros buscamos que cuanto más alto sea el volumen al que habla el usuario (es decir,
        // la amplitud y por tanto la varianza), más puntiaguda debería ser la campana.
        // Es por eso que tranformamos la amplitud en su inversa, y la elevamos a un valor que se adapta mejor
        // a la forma que le queremos dar de acuerdo al nivel de voz con la que habla el usuario
        val amplitudTransformada: Float = (1f/nuevaAmplitud).pow(2.3f)
        //Log.d("VisualizerView", "Amplitud transformada = $amplitudTransformada, Altura pantalla = $alturaPantalla")

        /*val amplitudMedia: Float = (amplitudTransformada + ultimaAmplitud) / 6
        ultimaAmplitud = amplitudTransformada
        amplitudTransformada = amplitudMedia*/

        picos.forEach {
            if(amplitudTransformada >= 0f) {
                // Modificamos el rango del eje de coordenadas x de la distribución normal para adaptarla al ancho
                // de la pantalla
                val x = (cont + npicos.toFloat()/2f) * 0.08f/npicos.toFloat() - 0.04f // Cambiamos el rango de cont a [-0'4, 0'4]

                // Por cada rectángulo que conforma el visualizer, calculamos qué altura debería tener de acuerdo a la
                // fórmula de la distribución normal con media = 0 y varianza igual a la amplitud transformada
                var alturaRectangulo: Float = 1f/(amplitudTransformada*p) * 2.718281f.pow(-(x.pow(2))/(2*amplitudTransformada*amplitudTransformada))

                /*if(cont == 0f)
                    Log.d("VisualizerView", "Amplitud res = $alturaRectangulo")*/

                // En caso de que no se esté diciendo nada, mostraremos una línea con una ligera distorsión.
                // Esta línea hace más intuitivo para el usuario el saber que la aplicación está grabando
                alturaRectangulo = if (nuevaAmplitud < 1f) 4f + (1..15).random().toFloat() / 10 else alturaRectangulo

                // Una vez calculada la altura del rectángulo, lo adaptamos al tamaño del View
                it.top = (alturaPantalla - alturaRectangulo) / 2
                it.bottom = (alturaPantalla + alturaRectangulo) / 2
                //Log.d("VisualizerView", "it.top -> ${it.top}")

            }
            else{
                it.top = 0f
                it.bottom = 0f
            }

            cont++
        }

        // Invalidamos la visualización actual y llamamos a Draw()
        postInvalidate()
    }

    fun detener() {
        picos.map {
            it.top = 0f
            it.bottom = 0f
        }
        postInvalidate()
        checkColor = true
    }

    override fun onDraw(canvas: Canvas) {
        Log.d("Visualizer Draw", "Pintando patron")
        super.onDraw(canvas)
        // Draw a bar whose height is proportional to amplitude
        picos.forEach {
            canvas.drawRoundRect(it, radio, radio, color)
        }
    }
}
