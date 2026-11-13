package ar.jornadas.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarteleraTest {
    private lateinit var db: Archivo
    private lateinit var dao: CarteleraDao
    private fun charla(id: String, titulo: String = "Original", sala: String = "Sala A") =
        Charla(id, titulo, "Datos", 2, "10:00", sala, "Ponente", "Resumen")

    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), Archivo::class.java).build()
        dao = db.cartelera()
    }
    @After fun cerrar() { db.close() }

    @Test fun actualizaContenidoConservaMarcasYOlvidaRetiradas() = runBlocking {
        dao.reemplazarPrograma(listOf(charla("11"), charla("12")))
        dao.alternar("11")
        dao.alternar("12")
        dao.reemplazarPrograma(listOf(charla("11", "Compose en producción avanzada", "Sala Puerto"), charla("12")))
        val editada = dao.actuales().first { it.id == "11" }
        assertTrue(editada.favorita)
        assertEquals("Compose en producción avanzada", editada.titulo)
        assertEquals("Sala Puerto", editada.sala)
        dao.reemplazarPrograma(listOf(charla("12"), charla("90")))
        assertEquals(setOf("12", "90"), dao.actuales().map { it.id }.toSet())
        assertTrue(dao.actuales().first { it.id == "12" }.favorita)
        dao.reemplazarPrograma(listOf(charla("11"), charla("12")))
        assertFalse(dao.actuales().first { it.id == "11" }.favorita)
    }

    @Test fun respuestaVaciaEsUnaDescargaValida() = runBlocking {
        assertFalse(dao.hayDescarga())
        dao.reemplazarPrograma(listOf(charla("11")))
        dao.alternar("11")
        dao.reemplazarPrograma(emptyList())
        assertTrue(dao.hayDescarga())
        assertTrue(dao.actuales().isEmpty())
    }

    @Test fun respetaMarcasCambiadasMientrasSePreparabaLaRespuesta() = runBlocking {
        dao.reemplazarPrograma(listOf(charla("11"), charla("12")))
        dao.alternar("12")
        val respuesta = listOf(charla("11", "Nuevo"), charla("12"))
        dao.alternar("11")
        dao.alternar("12")
        dao.reemplazarPrograma(respuesta)
        assertTrue(dao.actuales().first { it.id == "11" }.favorita)
        assertFalse(dao.actuales().first { it.id == "12" }.favorita)
    }
}
