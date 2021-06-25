package edu.tuk.satellitesarereal.model

import androidx.room.*
import com.rtbishop.look4sat.domain.predict4kotlin.TLE
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tle_entries")
data class TleEntry (
    @PrimaryKey val name: String,
    val epoch: Double,
    val meanmo: Double,
    val eccn: Double,
    val incl: Double,
    val raan: Double,
    val argper: Double,
    val meanan: Double,
    val catnum: Int,
    val bstar: Double,
    var isSelected: Boolean,
) {
    fun toTLE() : TLE {
        return TLE(
        name,
        epoch,
        meanmo,
        eccn,
        incl,
        raan,
        argper,
        meanan,
        catnum,
        bstar,
        )
    }

    companion object {
        fun fromTLE(tle: TLE, isSelected: Boolean = false): TleEntry {
            return TleEntry(
               tle.name,
               tle.epoch,
               tle.meanmo,
               tle.eccn,
               tle.incl,
               tle.raan,
               tle.argper,
               tle.meanan,
               tle.catnum,
               tle.bstar,
               isSelected,
            )
        }

        // Convenience method to make the VM easier. Probably, it will be removed as it is used
        // to circumvent some strange behaviour.
        fun deepCopy(other: TleEntry): TleEntry {
            return TleEntry(
                other.name,
                other.epoch,
                other.meanmo,
                other.eccn,
                other.incl,
                other.raan,
                other.argper,
                other.meanan,
                other.catnum,
                other.bstar,
                other.isSelected,
            )
        }
    }
}

@Dao
interface TleEntryDao {
    @Query("SELECT * FROM tle_entries ORDER BY name")
    fun getAll(): Flow<List<TleEntry>>

    @Query("SELECT * FROM tle_entries WHERE name LIKE :subString")
    fun getFilteredEntries(subString: String): Flow<List<TleEntry>>

    @Query("SELECT * FROM tle_entries WHERE isSelected = 1")
    fun getSelectedEntries(): Flow<List<TleEntry>>

    @Query("DELETE FROM tle_entries")
    suspend fun clear()

    @Update
    suspend fun updateTles(vararg tles: TleEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTles(vararg tles: TleEntry)
}