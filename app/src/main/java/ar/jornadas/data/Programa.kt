package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Entity(tableName = "estado_programa")
data class EstadoPrograma(@PrimaryKey val id: Int = 1, val descargado: Boolean = true)

@Dao
interface CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    fun observar(): Flow<List<Charla>>

    @Query("SELECT * FROM charlas")
    suspend fun actuales(): List<Charla>

    @Query("SELECT EXISTS(SELECT 1 FROM estado_programa WHERE descargado = 1)")
    fun observarDescarga(): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM estado_programa WHERE descargado = 1)")
    suspend fun hayDescarga(): Boolean

    @Query("DELETE FROM charlas")
    suspend fun borrarPrograma()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEstado(estado: EstadoPrograma)

    // Leer las marcas DESPUÉS de la red y dentro de la misma transacción.
    @Transaction
    suspend fun reemplazarPrograma(fichas: List<Charla>) {
        val favoritas = actuales().filter { it.favorita }.map { it.id }.toSet()
        borrarPrograma()
        guardar(fichas.map { it.copy(favorita = it.id in favoritas) })
        guardarEstado(EstadoPrograma())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fichas: List<Charla>)

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    suspend fun alternar(id: String)
}

@Database(entities = [Charla::class, EstadoPrograma::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}

// Conserva las filas y favoritas de instalaciones anteriores.
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS estado_programa (id INTEGER NOT NULL, descargado INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("INSERT INTO estado_programa (id, descargado) SELECT 1, 1 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}
