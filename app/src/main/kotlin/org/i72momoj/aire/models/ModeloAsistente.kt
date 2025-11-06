package org.i72momoj.aire.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.LongBuffer
import java.util.Optional

class ModeloAsistente private constructor() {

    private val TAG = "ModeloAsistente"
    private var env: OrtEnvironment? = null
    private var rutaModelo: String? = null
    private var sesion: OrtSession? = null
    private var tokenizador: Tokenizador? = null
    private val tags: Array<String> = arrayOf(
        "O",
        "B-DSEM",
        "I-DSEM",
        "B-HORA",
        "I-HORA",
        "B-MIN",
        "I-MIN",
        "B-RELH",
        "I-RELH",
        "B-RELM",
        "I-RELM",
        "B-RELS",
        "I-RELS",
        "B-MTN",
        "I-MTN",
        "B-TIT",
        "I-TIT"
    )

    companion object {
        @Volatile
        private var instance: ModeloAsistente? = null

        // Aplicamos Singleton
        fun crearInstancia(contexto: Context, nombreModelo: String) =
            // Si instancia no es null, devuelve instancia. En caso de ser null,
            // ejecuta el bloque de la derecha. El bloque de la derecha es de tipo
            // syncronized() lo que hace que solo sea accesible por un único hilo
            instance ?: synchronized(this) {

                // Segunda comprobación de que instance no es null en caso de que
                // otro hilo haya intentado accedido a la función syncronized()
                // al mismo tiempo
                instance ?: ModeloAsistente().also {

                    // Creamos una instancia de ModeloAsistente y además (.also {}) cargamos
                    // el modelo en memoria, guardamos la instancia y la ruta del modelo
                    // en memoria e inicializamos el tokenizer
                    val rutaModelo = cargar(contexto, nombreModelo)
                    instance = it
                    instance!!.rutaModelo = rutaModelo

                    Log.d("Modelo ONNX", "RUTA: $rutaModelo")

                    val vocab: InputStream = contexto.assets.open("vocab.txt")
                    instance!!.tokenizador = Tokenizador(vocab)
                }
            }

        private fun cargar(contexto: Context, fichero: String): String? {

            try {
                val gestor: AssetManager = contexto.assets
                val inputStream: InputStream = gestor.open(fichero)
                val ficheroExtraido = File(contexto.filesDir, fichero)
                val ficheroOutputStream: OutputStream = FileOutputStream(ficheroExtraido)
                val buffer = ByteArray(1024)
                var leido: Int

                leido = inputStream.read(buffer)
                while (leido != -1) {
                    ficheroOutputStream.write(buffer, 0, leido)
                    leido = inputStream.read(buffer)
                }

                inputStream.close()
                ficheroOutputStream.close()

                if(!ficheroExtraido.exists()) {
                    Log.e("ModeloAsistente", "Error al extraer el fichero del modelo")
                    return null
                }
                else {
                    Log.d("ModeloAsistente", "Modelo extraído con éxito")
                    return ficheroExtraido.absolutePath
                }
            }
            catch (e: Exception){
                Log.e("ModeloAsistente", "Error al extraer el fichero del modelo")
                e.printStackTrace()
                return null
            }
        }
    }

    fun crearSesion(): Boolean {
        try {
            this.env = OrtEnvironment.getEnvironment()
            Log.d(TAG, "Entorno del modelo inicializado")

            this.sesion = env?.createSession(this.rutaModelo)
            Log.d(TAG, "Sesión del modelo creada")

            return true
        }
        catch (e: Exception) {
            Log.e(TAG, "Error en la inicialización del modelo: ${e.message}")
            e.printStackTrace()
            this.sesion = null

            return false
        }
    }
    
    private fun id2tag(id: Int): String {
        return tags[id]
    }

    private fun tag2id(etiqueta: String): Int? {
        var indice = 0
        for (tag in tags) {
            if (tag == etiqueta)
                return indice

            indice++
        }

        return null
    }
    private fun filtrar(resultados: ArrayList<Pair<String, String>>): ArrayList<Pair<String, String>> {
        val resultadosFiltrados: ArrayList<Pair<String, String>> = ArrayList()

        // Eliminamos los que no tengan etiquetas
        for(resultado in resultados) {
            if (resultado.first != "O" || resultado.second == "[SEP]") {
                resultadosFiltrados.add(resultado)
            }
        }

        var tagAnterior = ""
        var valorAnterior = ""
        val resultadosFiltradosUnidos: ArrayList<Pair<String, String>> = ArrayList()

        // Unificamos los tokens que conforman una única palabra
        for(resultado in resultadosFiltrados) {

            if (resultado.second.contains("##")) {

                if (resultado.first == tagAnterior)
                    valorAnterior += resultado.second.substring(2)
            }
            else {
                if(tagAnterior != "")
                    resultadosFiltradosUnidos.add(Pair(tagAnterior, valorAnterior))

                tagAnterior = resultado.first
                valorAnterior = resultado.second
            }
        }

        return resultadosFiltradosUnidos
    }

    fun inferir(frase: String): ArrayList<Pair<String, String>>? {
        if (this.sesion == null) {
            Log.e(TAG, "Error: no existe sesión ONNX inicializada")
            return null
        }

        try {
            // Tokenizamos la frase
            val idsTokens: ArrayList<Int> = tokenizador!!.tokenizar(frase)
            val tokens: ArrayList<String> = idsTokens.map { tokenizador!!.decode(it) } as ArrayList<String>

            // Extraemos los IDs de los tokens del input
            val ids: LongArray = idsTokens.map { it.toLong() }.toLongArray()

            // Creamos un Tensor a partir de los IDs del input
            val batch = longArrayOf(1, ids.size.toLong())
            val inputTensor: OnnxTensor = OnnxTensor.createTensor(this.env, LongBuffer.wrap(ids), batch)

            val maskValues = LongArray(ids.size) { 1 } // or set 0 for pad tokens if padding exists
            val maskShape = longArrayOf(1, ids.size.toLong())
            val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(maskValues), maskShape)

            var informacionExtraida = ArrayList<Pair<String, String>>()

            // Ejecutamos el modelo
            val inputMap = mapOf(
                "input_ids" to inputTensor,
                "attention_mask" to attentionMaskTensor
            )

            val resultado: OrtSession.Result = this.sesion!!.run(inputMap)

            val optionalOutput: Optional<OnnxValue> = resultado.get("logits")
            if(optionalOutput.isPresent) {
                val outputTensor: OnnxTensor =  optionalOutput.get() as OnnxTensor
                val resultados: Array<Array<FloatArray?>?>? = outputTensor.value as Array<Array<FloatArray?>?>?

                for(secuencia in resultados!!){

                    var indiceToken = 0
                    for(probabilidades in secuencia!!) {

                        var max = probabilidades!![0];
                        var maxIndice = 0
                        var indice = 1

                        while (indice < probabilidades.size) {
                            if (max < probabilidades[indice]) {
                                max = probabilidades[indice]
                                maxIndice = indice
                            }
                            indice++
                        }

                        val valor: Pair<String, String> = Pair(id2tag(maxIndice), tokens[indiceToken])
                        informacionExtraida.add(valor)
                        indiceToken++
                    }
                }

                Log.d("ModeloAsistente", "informacion extraida -> ${informacionExtraida.toString()}")

                // Eliminamos la información que no nos intereas (todos los tags "O") y unificamos los tokens
                // que pertenecen a la misma categoría
                informacionExtraida = filtrar(informacionExtraida)

                for(pair in informacionExtraida)
                    Log.d(TAG, "TAG: ${pair.first}, VAL: ${pair.second}")

                return informacionExtraida
            }
            else {
                Log.e(TAG, "Error al obtener un resultado del modelo")
                return null
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "Error al hacer una inferencia: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    fun sesionAbierta(): Boolean {
        return this.sesion != null
    }

    fun close() {
        try {
            if (this.sesion != null)
                this.sesion!!.close()

            if (this.env != null)
                this.env!!.close()
        }
        catch (e: Exception){
            Log.e(TAG, "Error al cerrar el modelo: ${e.message}")
            e.printStackTrace()
        }
    }
}
