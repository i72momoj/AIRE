package org.i72momoj.aire.fragments

// TODO: CREAR FUNCION PARA MOSTRAR MENSAJES ROLLO TOAST

import android.Manifest
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.toast
import org.i72momoj.aire.databinding.FragmentClockBinding
import org.i72momoj.aire.extensions.createNewAlarm
import org.i72momoj.aire.extensions.createNewTimer
import org.i72momoj.aire.extensions.dataStore
import org.i72momoj.aire.helpers.TODAY_BIT
import org.i72momoj.aire.helpers.TOMORROW_BIT
import org.i72momoj.aire.helpers.getCurrentDayMinutes
import org.i72momoj.aire.models.Alarm
import org.i72momoj.aire.models.AlarmaAsistente
import org.i72momoj.aire.models.Comando
import org.i72momoj.aire.models.ComandoAsistente
import org.i72momoj.aire.models.Interprete
import org.i72momoj.aire.models.ModeloAsistente
import org.i72momoj.aire.models.TemporizadorAsistente
import org.i72momoj.aire.models.Timer
import org.i72momoj.aire.views.VisualizerView
import org.fossify.commons.views.MyTextView
import org.i72momoj.aire.helpers.ALARMA_ACTUALIZADA
import org.i72momoj.aire.helpers.DATOS_ALARMA
import org.i72momoj.aire.helpers.TIPO_ALARMA_ACTIVAR
import org.i72momoj.aire.helpers.TIPO_ALARMA_BORRAR
import org.i72momoj.aire.helpers.TIPO_ALARMA_DESACTIVAR
import org.vosk.Model
import java.io.File
import kotlin.getValue
import kotlin.math.abs
import kotlin.math.sin

class AsistenteFragment : ClockFragment() {

    private val TAG = this.javaClass.simpleName
    private var modeloAsistente: ModeloAsistente? = null
    private var modeloSTT: Model? = null
    private var interprete: Interprete? = null
    override lateinit var binding: FragmentClockBinding
    private var visualizerActivo: VisualizerView? = null
    private var palabrasView: MyTextView? = null
    val visibilidad = MutableStateFlow(0f)
    @RequiresApi(Build.VERSION_CODES.S)
    private val grabarAudioRequestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido?.invoke(concedido)
    }
    private var permisoConcedido: ((Boolean) -> Unit)? = null
    private val comandoAsistente: ComandoAsistente by activityViewModels()
    private var cargandoModelos: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargamos el intérprete de comandos y el modelo de reconocimiento de voz
        // en segundo plano
        lifecycleScope.launch(Dispatchers.IO) {

            launch {
                var alfa: Float
                while (cargandoModelos) {
                    alfa = abs(sin(System.currentTimeMillis().toDouble() / 200 )).toFloat()
                    visibilidad.value = alfa
                }
            }

            // Cargamos el modelo para extraer información de los comandos mediante un hilo
            val modelo = launch{ cargarModeloAsistente() }

            // Cargamos el modelo para el reconocimiento de voz mediante otro hilo
            val interpreteSTT = launch { cargarInterprete() }

            // Esperamos a que ambos modelos estén cargados
            modelo.join()
            interpreteSTT.join()
            cargandoModelos = false;

            // Una vez cargados ambos modelos desbloqueamos el botón de hablar
            launch(Dispatchers.Main) {
                binding.apply {
                    pulseGrabar.text = "¡Listo!"
                    pulseGrabar.alpha = 0.9f
                    comandoVoz.alpha = 1f
                    comandoVoz.isEnabled = true
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentClockBinding.inflate(inflater, container, false).apply {

            pulseGrabar.text = "Cargando asistente..."
            pulseGrabar.alpha = 1f

            // El botón de comando por voz estará deshabilitado hasta que se carguen los modelos
            // de reconocimiento de voz de interpretación de comandos
            comandoVoz.alpha = 0.5f
            comandoVoz.isEnabled = false
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            visibilidad.collect { valor ->
                binding.apply {
                    pulseGrabar.alpha = valor
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun setupViews() {
        super.setupViews()

        binding.apply {

            // Obtenemos los elementos del layout que mostrarán el visualizer y
            // el comando por voz
            visualizerActivo = visualizer
            palabrasView = palabrasAudio

            // Se ha pulsado el botón de comando por voz
            comandoVoz.setOnClickListener { introducirComandoVoz() }
        }
    }

    private fun extraerRecurso(elemento: String, destino: File) {
        try {
            val assetManager = requireContext().assets
            val files = assetManager.list(elemento) ?: return
            destino.mkdirs()

            for (filename in files) {
                val assetPath = if (elemento.isEmpty()) filename else "$elemento/$filename"
                val outFile = File(destino, filename)

                Log.d(TAG, "Fichero copiado: $assetPath")

                // If it's a directory, recurse
                if ((assetManager.list(assetPath)?.isNotEmpty() == true)) {
                    extraerRecurso( assetPath, outFile)
                } else {
                    assetManager.open(assetPath).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        catch (e: Exception){
            Log.e(TAG, "Error al copiar una carpeta/fichero")
            e.printStackTrace()
        }

    }

    private fun cargarModeloAsistente(){
        // Creamos una instancia en caso de no existir (aplicamos Singleton)
        modeloAsistente = ModeloAsistente.crearInstancia(requireContext(), "modelo.onnx")

        // En caso de que no haya una sesión abierta, la abrimos
        if (!modeloAsistente!!.sesionAbierta())
            modeloAsistente!!.crearSesion()
    }

    private fun cargarInterprete() {
        val modeloVoskStr = "vosk-model-small-es-0.42"

        try {
            // Copiamos el modelo de reconocimiento de la carpeta de assets a memoria interna,
            // siempre que no se encuentre ya copiado
            val internalModelPath = File(requireContext().filesDir, modeloVoskStr)
            if (!internalModelPath.exists()) {
                extraerRecurso(modeloVoskStr, internalModelPath)
            }

            // Creamos una instancia del modelo
            modeloSTT = Model(internalModelPath.absolutePath)

            // Creamos una instancia del intérprete
            modeloSTT?.let { interprete = Interprete.crearInstancia(requireContext(), it) }

        }
        catch (e: Exception) {
            Log.e(TAG, "Error al inicializar el modelo STT")
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun comprobarPermisoAudio(): Boolean {
        var concedidos = false

        // Comprobamos si tenemos el permiso de RECORD_AUDIO
        if (checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PermissionChecker.PERMISSION_GRANTED) {

            // Si no los tenemos, se pide desde una corrutina
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    concedidos = pedirPermisoAudio()

                    // Si el usuario ha concedido los permisos a través del launcher, se crea
                    // una instancia del intérprete
                    if(concedidos)
                        modeloSTT?.let { interprete = Interprete.crearInstancia(requireContext(), it) }
                }
                catch (e: Exception){
                    Log.e(TAG, "abrirVisualizadorYObtenerComando -> no se pudo pedir los permisos necesarios")
                    e.printStackTrace()
                }
            }
        }
        else concedidos = true

        return concedidos
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun pedirPermisoAudio(): Boolean {

        val dataStore = requireContext().dataStore
        var permiso = false

        // Comprobamos si se ha solicitado previamente el permiso
        val AUDIO_KEY = booleanPreferencesKey("permiso_audio_solicitado")
        val solicitado = dataStore.data.first()[AUDIO_KEY] ?: false

        if (solicitado){
            // Comprobamos que aun se pueda solicitar permisos
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                permiso = suspendCancellableCoroutine { cont ->
                    grabarAudioRequestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    permisoConcedido = { concedido ->
                        cont.resume(concedido) { cause, _, _ -> { } }
                    }
                }
                return permiso
            }
            // Si el usuario ha rechazado los permisos demasiadas veces, le informamos
            else {
                lifecycleScope.launch(Dispatchers.Main) {
                    // Vamos a configurar un mensaje por Dialog
                    val builder = AlertDialog.Builder(requireContext())

                    // Establecemos el mensaje de la ventana
                    builder.setMessage("Los permisos para grabar audio han sido rechazados demasiadas veces. Para poder usar el micrófono, debe otorgar estos permisos manualmente desde ajustes del teléfono")

                    // Establecemos el título
                    builder.setTitle("Permiso denegado")

                    // Hacemos equivalente el tocar fuera de la ventana a rechazar el mensaje
                    builder.setCancelable(true)

                    // Creamos un botón para cerrar la ventana
                    builder.setNegativeButton("De acuerdo") {
                            dialog, which -> dialog.cancel()
                    }

                    // Una vez configurado, lo creamos y mostramos
                    val alertDialog = builder.create()
                    alertDialog.show()
                }
            }
        }
        else {
            dataStore.edit { settings ->
                settings[AUDIO_KEY] = true
            }

            permiso = suspendCancellableCoroutine { cont ->
                grabarAudioRequestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                permisoConcedido = { concedido ->
                    cont.resume(concedido) { cause, _, _ -> { } }
                }
            }
            return permiso
        }

        return permiso
    }

    private suspend fun grabarComando(): String {

        var resultado = ""

        if(visualizerActivo != null){
            Log.d(TAG, "Visualizer inicializado")

            // Comprobamos que el intérprete esté inicializado
            if(interprete != null) {
                if (!interprete!!.estaGrabando())
                    resultado = interprete!!.grabar(visualizerActivo, palabrasView)
                else
                    interprete!!.stop()
            }
            else
                Log.e(TAG, "El intérprete no ha sido inicializado")
        }
        else
            Log.e(TAG, "El visualizer no está inicializado")

        return resultado
    }

    private suspend fun ejecutarOrdenTemporizador(
        resultado: ArrayList<Pair<String, String>>?,
        confirmarAccion: Boolean,
        confirmarPorVoz: Boolean = false
    ) {
        var nuevoTemporizador: Timer? = null
        activity?.run { nuevoTemporizador = createNewTimer() }

        val temporizadorActualizado = TemporizadorAsistente.desdeDatosModelo(resultado, nuevoTemporizador!!)

        if (temporizadorActualizado.seconds < 0)
        // TODO: mostrar mensaje de error
            Log.e(TAG, "El temporizador no puede ser mayor a un día")
        else {

            // Usamos el View compartido para invocar la función del fragmento de temporizadores
            // para crear un nuevo temporizador
            withContext(Dispatchers.Main) {
                // Le pasamos a la clase la información necesaria y la configuración
                comandoAsistente.temporizador.value = temporizadorActualizado
                comandoAsistente.confirmacionTemporizador.value = confirmarAccion

                // Invocamos la función deseada
                comandoAsistente.invocar(Comando.CrearTemporizador)
            }
        }
        //}
    }

    private suspend fun ejecutarOrdenAlarma(
        datos: ArrayList<Pair<String, String>>,
        confirmarAccion: Boolean,
        confirmarPorVoz: Boolean = false
    ) {
        val ordenModificar = datos.any {  it.first.contains("ACTH", ignoreCase = true)  }
        val ordenAccion = datos.any {  it.first.contains("ACCION", ignoreCase = true)  }

        // Creamos una alarma mediante la información extraída
        val alarma: Alarm = requireContext().createNewAlarm(0, 0)
        val alarmaDesdeModelo = AlarmaAsistente.desdeDatosModelo(datos, AlarmaAsistente.copiar(alarma), DATOS_ALARMA)

        // Si no se han podido recuperar ni los datos de una alarma, o bien el comando no ha sido
        // bien introducido o no ha sido bien recogido
        if(alarmaDesdeModelo == null)
            activity?.toast("Comando no reconocido. Inténtelo de nuevo")
        else {
            // No pueden ser las dos a la vez
            if (ordenModificar && ordenAccion) {
                activity?.toast("Comando no reconocido. Inténtelo de nuevo")

            }
            else if (ordenModificar) {
                // Extraemos los datos actualizados de la alarma
                val alarmaActualizada = AlarmaAsistente.desdeDatosModelo(datos, AlarmaAsistente.copiar(alarma), ALARMA_ACTUALIZADA)

                if (alarmaActualizada != null) {
                    // Los valores mutables MutableData tienen que ser establecidos/modificados desde el hilo principal
                    withContext(Dispatchers.Main) {
                        // Usamos el View compartido para invocar la función del fragmento de Alarmas
                        // para modificar una alarma.
                        comandoAsistente.alarma.value = alarmaDesdeModelo
                        comandoAsistente.alarmaActualizada.value = alarmaActualizada
                        comandoAsistente.confirmacionAlarma.value = confirmarAccion
                        comandoAsistente.confirmarVoz.value = confirmarPorVoz
                        comandoAsistente.interprete.value = interprete

                        // Invocamos la acción de modificar
                        comandoAsistente.invocar(Comando.ModificarAlarma)
                    }
                } else
                    activity?.toast("Lo siento, no te he entendido. Inténtalo de nuevo")
            }
            else if (ordenAccion) {
                val accion = datos.firstOrNull { it.first.contains("ACCION") }?.second ?: ""

                Log.d(TAG, "ejecutarOrdenAlarma -> accion: $accion")

                val acciones = arrayListOf(
                    Pair("par", TIPO_ALARMA_DESACTIVAR),
                    Pair("borr", TIPO_ALARMA_BORRAR),
                    Pair("apag", TIPO_ALARMA_DESACTIVAR),
                    Pair("suprim", TIPO_ALARMA_BORRAR),
                    Pair("quit",TIPO_ALARMA_DESACTIVAR),
                    Pair("elimin", TIPO_ALARMA_BORRAR),
                    Pair("anul", TIPO_ALARMA_DESACTIVAR),
                    Pair("destru",TIPO_ALARMA_BORRAR),
                    Pair("desactiv", TIPO_ALARMA_DESACTIVAR),
                    Pair("cancel",TIPO_ALARMA_DESACTIVAR),
                    Pair("encend",TIPO_ALARMA_ACTIVAR),
                    Pair("enciend", TIPO_ALARMA_ACTIVAR),
                    Pair("activ", TIPO_ALARMA_ACTIVAR),
                    Pair("reactiv", TIPO_ALARMA_ACTIVAR),
                    Pair("pon",TIPO_ALARMA_ACTIVAR),
                    Pair("desanul",TIPO_ALARMA_ACTIVAR),
                    Pair("inici",TIPO_ALARMA_ACTIVAR),
                    Pair("reanud",TIPO_ALARMA_ACTIVAR),
                    Pair("accion",TIPO_ALARMA_ACTIVAR)
                )

                val tipo = acciones.firstOrNull {accion.contains(it.first)}?.second ?: -1

                if(tipo > -1) {
                    withContext(Dispatchers.Main) {
                        comandoAsistente.alarma.value = alarmaDesdeModelo
                        comandoAsistente.confirmacionAlarma.value = confirmarAccion
                        comandoAsistente.confirmarVoz.value = confirmarPorVoz
                        comandoAsistente.nuevoEstado.value = tipo == TIPO_ALARMA_ACTIVAR

                        when (tipo) {
                            TIPO_ALARMA_BORRAR -> comandoAsistente.invocar(Comando.BorrarAlarma)
                            TIPO_ALARMA_ACTIVAR -> comandoAsistente.invocar(Comando.CambiarEstadoAlarma)
                            TIPO_ALARMA_DESACTIVAR -> comandoAsistente.invocar(Comando.CambiarEstadoAlarma)
                        }
                    }
                }
                else
                    activity?.toast("Acción no reconocida, inténtelo de nuevo")

                //comandoAsistente.alarma.value = alarmaDesdeModelo
            }
            // Se ha invocado la CREACIÓN de la alarma
            else {
                // Si la alarma no se ha establecido un día concreto, a través de la hora especificada
                // y la hora actual determinamos si se establece para hoy o para mañana
                if (alarmaDesdeModelo.days <= 0) {
                    alarmaDesdeModelo.days = if (alarmaDesdeModelo.timeInMinutes > getCurrentDayMinutes()) {
                        TODAY_BIT
                    } else {
                        TOMORROW_BIT
                    }
                }

                // Los valores mutables MutableData tienen que ser establecidos/modificados desde el hilo principal
                withContext(Dispatchers.Main) {
                    // Usamos el View compartido para invocar la función del fragmento de Alarmas
                    // para crear la nueva alarma.
                    comandoAsistente.alarma.value = alarmaDesdeModelo
                    comandoAsistente.confirmacionAlarma.value = confirmarAccion
                    comandoAsistente.confirmarVoz.value = confirmarPorVoz
                    comandoAsistente.interprete.value = interprete
                    comandoAsistente.invocar(Comando.CrearAlarma)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun introducirComandoVoz() {
        // Se comprueba que se tienen los permisos necesarios
        if (comprobarPermisoAudio()) {

            // Si tenemos los permisos necesarios:
            lifecycleScope.launch(Dispatchers.IO) {
                if (modeloAsistente != null) {

                    // Obtenemos configuraciones necesarias
                    val conf = requireContext().dataStore
                    val confirmarAlarmas: Boolean = conf.data.map { preferences ->
                        preferences[booleanPreferencesKey("confirmarAlarmas")] ?: true
                    }.first()
                    val confirmarTemporizadores: Boolean = conf.data.map { preferences ->
                        preferences[booleanPreferencesKey("confirmarTemporizadores")] ?: true
                    }.first()
                    val confirmarVoz: Boolean = conf.data.map { preferences ->
                        preferences[booleanPreferencesKey("confirmarPorVoz")] ?: false
                    }.first()

                    // Obtenemos el comando
                    var frase = ""
                    launch { frase = grabarComando() }.join()
                    Log.d(TAG, "Comando obtenido: $frase")

                    // Extraemos la información mediante el modelo de clasificación de Tokens
                    val resultado: ArrayList<Pair<String, String>>? = modeloAsistente!!.inferir(frase)

                    if (resultado != null && !resultado.isEmpty()) {
                        // Distinguimos entre si es una petición de creación de alarma o de temporizador
                        // a partir del contenido extraído del comando
                        val esTemporizador = resultado.any {  it.first.contains("REL", ignoreCase = true)  }

                        // Si es una orden sobre temporizadores
                        if(esTemporizador) {
                            Log.d(TAG, "Comando sobre temporizadores")
                            ejecutarOrdenTemporizador(
                                resultado = resultado,
                                confirmarAccion = confirmarTemporizadores,
                                confirmarPorVoz = confirmarVoz
                            )
                        }
                        // Si no es de temporizadores, es de alarmas
                        else{
                            Log.d(TAG, "Comando sobre alarmas")

                            ejecutarOrdenAlarma(
                                datos = resultado,
                                confirmarAccion = confirmarAlarmas,
                                confirmarPorVoz = confirmarVoz
                            )
                        }
                    }
                    else {
                        activity?.toast("Comando no reconocido. Inténtelo de nuevo")
                    }
                } else
                    Log.e(TAG, "El modelo de clasificación no ha sido inicizalizado")
            }
        }
    }
}
