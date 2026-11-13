package ar.jornadas.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val iniciada = AtomicBoolean(false)
    private val enCurso = AtomicBoolean(false)
    val programa = dao.observar()
    val descargado = dao.observarDescarga()
    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()
    private val _aviso = MutableStateFlow("")
    val aviso = _aviso.asStateFlow()

    fun iniciarSesion() {
        if (iniciada.compareAndSet(false, true)) recargar()
    }

    fun recargar() {
        if (!enCurso.compareAndSet(false, true)) return
        _cargando.value = true
        _aviso.value = ""
        // Vive durante el proceso, aunque se destruya la Activity.
        scope.launch {
            try {
                val fichas = api.consultar().map {
                    Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
                }
                dao.reemplazarPrograma(fichas)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aviso.value = if (dao.hayDescarga())
                    "No se pudo actualizar. Se están usando datos guardados."
                else "No se pudo consultar el programa. Reintentá la descarga."
            } finally {
                _cargando.value = false
                enCurso.set(false)
            }
        }
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
