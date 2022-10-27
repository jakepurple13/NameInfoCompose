package com.programmersbox.nameinfocompose

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Dao
interface NameInfoDao {
    @Query("select * from IfyInfo")
    fun getAll(): Flow<List<IfyInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg info: IfyInfo)

    @Delete
    suspend fun delete(info: IfyInfo)
}

@Database(entities = [IfyInfo::class], version = 1)
@TypeConverters(Converters::class)
abstract class NameInfoDatabase : RoomDatabase() {
    abstract fun nameInfoDao(): NameInfoDao

    companion object {

        @Volatile
        private var INSTANCE: NameInfoDatabase? = null

        fun getInstance(context: Context): NameInfoDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, NameInfoDatabase::class.java, "nameinfo.db").build()
    }
}

class Converters {
    @TypeConverter
    fun fromGender(value: String?): Gender? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun toGender(gender: Gender): String {
        return Json.encodeToString(gender)
    }

    @TypeConverter
    fun fromCountry(value: String?): List<Country> {
        return value?.let { Json.decodeFromString<List<Country>>(it) }.orEmpty()
    }

    @TypeConverter
    fun toCountry(country: List<Country>): String {
        return Json.encodeToString(country)
    }
}