package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios.de(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda

    // true cuando Room ya entregó su primera lectura (evita mostrar "vacío" por error).
    val listo = MutableStateFlow(false)
    val charlas = repo.programa.onEach { listo.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val hayEdicion: StateFlow<Boolean> = combine(repo.hayEdicion, charlas) { flag, lista ->
        flag || lista.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repo.hayEdicion.value)

    // Filtros en SavedStateHandle: sobreviven a rotación, ficha abierta y muerte de proceso.
    val dia: StateFlow<Int> = estado.getStateFlow("dia", 0)
    val tema: StateFlow<String> = estado.getStateFlow("tema", "Todos")
    fun elegirDia(i: Int) { estado["dia"] = i }
    fun elegirTema(t: String) { estado["tema"] = t }

    val fallo = MutableStateFlow(false)
    val cargando = MutableStateFlow(false)

    /** Una carga automática por proceso. */
    fun cargaAutomatica() { if (servicios.reclamarCargaAutomatica()) recargar() }

    /** Si ya hay una petición en curso, el toque se ignora. */
    fun recargar() {
        if (!cargando.compareAndSet(false, true)) return
        fallo.value = false
        viewModelScope.launch {
            try {
                repo.sincronizar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fallo.value = true
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
