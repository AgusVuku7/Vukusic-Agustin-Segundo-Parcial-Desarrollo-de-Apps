package ar.jornadas.data

import androidx.room.*
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

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    protected abstract suspend fun favoritas(): List<String>

    @Query("DELETE FROM charlas")
    protected abstract suspend fun vaciar()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertar(fichas: List<Charla>)

    // Instantánea completa: las marcas se leen DENTRO de la transacción (respeta cambios hechos
    // durante la petición), se conservan por id y lo que no vino en el snapshot se elimina.
    @Transaction
    open suspend fun reemplazar(fichas: List<Charla>) {
        val marcadas = favoritas().toSet()
        vaciar()
        insertar(fichas.map { it.copy(favorita = it.id in marcadas) })
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)
}

@Database(entities = [Charla::class], version = 1, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
