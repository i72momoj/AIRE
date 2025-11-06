package org.i72momoj.aire.models

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.i72momoj.aire.views.VisualizerView
import org.fossify.commons.views.MyTextView
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.LibVosk
import org.vosk.LogLevel
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Interprete private constructor() {

    private val bufferSize = AudioRecord.getMinBufferSize(
        16000,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var audioRecord: AudioRecord? = null
    private lateinit var modelo: Model
    private lateinit var recognizer: Recognizer
    private var grabando: Boolean = false


    companion object {
        @Volatile
        private var instance: Interprete? = null

        // Aplicamos Singleton
        fun crearInstancia(contexto: Context, modelo: Model) =
            // Si instancia no es null, devuelve instancia. En caso de ser null,
            // ejecuta el bloque de la derecha. El bloque de la derecha es de tipo
            // syncronized() lo que hace que solo sea accesible por un único hilo
            instance ?: synchronized(this) {

                // Segunda comprobación de que instance no es null en caso de que
                // otro hilo haya accedido a la función syncronized() al mismo tiempo
                instance ?: Interprete().also {

                    // Creamos una instancia de ModeloAsistente y además (.also {}) cargamos
                    // el modelo en memoria, guardamos la instancia y la ruta del modelo
                    // en memoria e inicializamos el tokenizer

                    if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Interprete", "Permisos concedidos")
                        it.init(modelo)
                        instance = it
                        Log.d("Interprete", "Instancia creada")
                    }
                    else
                        Log.e("Interprete", "Permisos denegados, instanciación cancelada")
                }
            }

    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun init(modelo: Model){
        try {
            Log.d("Interprete", "Inicializando intérprete...")

            LibVosk.setLogLevel(LogLevel.DEBUG)

            this.audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            this.modelo = modelo
            recognizer = Recognizer(modelo, 16000f)
        }
        catch (e: Exception){
            Log.e("Interprete", "Error al inicializar intérprete")
            e.printStackTrace()
        }

    }

    suspend fun grabar(visualizer: VisualizerView? = null, palabrasView: MyTextView? = null): String {
        Log.d("Interprete", "Comenzando grabación")
        this.grabando = true

        // Esperamos a que audioRecord esté cargado
        do {
            continue
        } while(audioRecord == null)
        audioRecord!!.startRecording()

        Log.d("Interprete", "audioRecord cargado. Grabando...")

        val buffer = ByteArray(bufferSize)
        var frase = ""
        while (grabando) {
            val read = audioRecord!!.read(buffer, 0, buffer.size)

            if (read > 0) {
                // Convert buffer to amplitude (here assuming PCM 16-bit)
                val shortBuffer = ShortArray(read / 2)
                ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)
                val max = shortBuffer.maxOrNull()?.toFloat() ?: 0f

                val amplitude = max / 1000 // Short.MAX_VALUE

                Log.d("Visualizer", "amplitud: $amplitude")

                visualizer?.actualizarAmplitud(amplitude)
            }

            if (recognizer.acceptWaveForm(buffer, read)) {
                //Log.d("Interprete IF", recognizer.result)
                val resultado: String = JSONObject(recognizer.result)["text"] as String

                Log.d("Interprete Completo", resultado)

                if(resultado != "") {
                    withContext(Dispatchers.Main){
                        palabrasView?.text = resultado
                    }

                    frase = resultado
                    grabando = false
                    visualizer?.actualizarAmplitud(-1f)
                }
            }
            else {
                //Log.d("Interprete ELSE", recognizer.partialResult)
                val nuevaFrase: String = JSONObject(recognizer.partialResult)["partial"] as String

                if (nuevaFrase != frase) {
                    Log.d("Interprete Parcial", nuevaFrase)
                    withContext(Dispatchers.Main){
                        palabrasView?.text = nuevaFrase
                    }
                }
            }
        }

        visualizer?.detener()

        Log.d("Interprete", "Parando grabación...")

        withContext(Dispatchers.Main) {
            palabrasView?.text = ""
        }
        audioRecord!!.stop()

        return frase
    }

    fun estaGrabando(): Boolean {
        return this.grabando
    }

    fun stop() {
            this.grabando = false;
    }
}
