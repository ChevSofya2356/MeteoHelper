package com.example.meteohelper.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.abs

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "meteo_helper.db"
        const val DATABASE_VERSION = 1

        // Таблицы
        const val TABLE_USERS = "users"
        const val TABLE_WEATHER_HISTORY = "weather_history"
        const val TABLE_USER_WELLBEING = "user_wellbeing"

        // Колонки профиля
        const val COL_US_ID = "user_id"
        const val COL_US_NAME = "name"
        const val COL_US_AGE = "age"
        const val COL_US_GENDER = "gender"
        const val COL_US_DISEASES = "diseases"
        const val COL_US_WEATHER_TYPE = "weather_dependence_type"
        const val COL_US_SYMPTOMS = "typical_symptoms"

        // Колонки погоды
        const val COL_WH_ID = "id"
        const val COL_WH_TIMESTAMP = "timestamp"
        const val COL_WH_PRESSURE = "pressure_mm"
        const val COL_WH_TEMP = "temp"
        const val COL_WH_HUMIDITY = "humidity"
        const val COL_WH_KP = "kp_index"

        // Колонки дневника
        const val COL_UW_ID = "entry_id"
        const val COL_UW_TIMESTAMP = "timestamp"
        const val COL_UW_DATE = "date"
        const val COL_UW_SCORE = "score_general"
        const val COL_UW_SYMPTOMS = "symptoms"
        const val COL_UW_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Таблица пользователей
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_US_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_US_NAME TEXT,
                $COL_US_AGE INTEGER,
                $COL_US_GENDER TEXT,
                $COL_US_DISEASES TEXT,
                $COL_US_WEATHER_TYPE TEXT,
                $COL_US_SYMPTOMS TEXT
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // 2. Таблица истории погоды
        val createWeatherTable = """
            CREATE TABLE $TABLE_WEATHER_HISTORY (
                $COL_WH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WH_TIMESTAMP LONG NOT NULL,
                $COL_WH_PRESSURE REAL,
                $COL_WH_TEMP REAL,
                $COL_WH_HUMIDITY REAL,
                $COL_WH_KP REAL
            )
        """.trimIndent()
        db.execSQL(createWeatherTable)

        // 3. Таблица записей самочувствия
        val createWellbeingTable = """
            CREATE TABLE $TABLE_USER_WELLBEING (
                $COL_UW_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_UW_TIMESTAMP LONG NOT NULL,
                $COL_UW_DATE TEXT NOT NULL,
                $COL_UW_SCORE INTEGER NOT NULL,
                $COL_UW_SYMPTOMS TEXT,
                $COL_UW_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(createWellbeingTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEATHER_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_WELLBEING")
        onCreate(db)
    }

    // ==================== ПРОФИЛЬ ====================
    fun getUserProfile(): Map<String, Any>? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS LIMIT 1"
        val cursor = db.rawQuery(query, null)
        return if (cursor.moveToFirst()) {
            val profile = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COL_US_ID)),
                "name" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_US_NAME)) ?: ""),
                "age" to cursor.getInt(cursor.getColumnIndexOrThrow(COL_US_AGE)),
                "gender" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_US_GENDER)) ?: ""),
                "diseases" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_US_DISEASES)) ?: ""),
                "weatherType" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_US_WEATHER_TYPE)) ?: ""),
                "typicalSymptoms" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_US_SYMPTOMS)) ?: "")
            )
            cursor.close()
            profile
        } else {
            cursor.close()
            null
        }
    }

    fun saveUserProfile(name: String, age: Int, gender: String, diseases: String, weatherType: String, symptoms: String): Long {
        val db = this.writableDatabase
        db.delete(TABLE_USERS, null, null) // Заменяем старый профиль
        val values = ContentValues().apply {
            put(COL_US_NAME, name)
            put(COL_US_AGE, age)
            put(COL_US_GENDER, gender)
            put(COL_US_DISEASES, diseases)
            put(COL_US_WEATHER_TYPE, weatherType)
            put(COL_US_SYMPTOMS, symptoms)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    // ==================== ПОГОДА ====================
    fun saveWeatherData(timestamp: Long, pressure: Float, temp: Float, humidity: Float, kp: Float): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_WH_TIMESTAMP, timestamp)
            put(COL_WH_PRESSURE, pressure)
            put(COL_WH_TEMP, temp)
            put(COL_WH_HUMIDITY, humidity)
            put(COL_WH_KP, kp)
        }
        return db.insert(TABLE_WEATHER_HISTORY, null, values)
    }

    /**
     * Возвращает историю погоды с рассчитанными дельтами изменений.
     * Нужно для ML-модели, чтобы сопоставлять скачки показателей с самочувствием.
     */
    fun getWeatherHistory(limit: Int): List<Map<String, Any>> {
        val db = this.readableDatabase
        val result = mutableListOf<Map<String, Any>>()

        // Берём на 1 запись больше, чтобы посчитать дельту для последней
        val query = """
            SELECT $COL_WH_TIMESTAMP, $COL_WH_PRESSURE, $COL_WH_TEMP, $COL_WH_HUMIDITY, $COL_WH_KP
            FROM $TABLE_WEATHER_HISTORY
            ORDER BY $COL_WH_TIMESTAMP DESC
            LIMIT ${limit + 1}
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            val timestamps = mutableListOf<Long>()
            val pressures = mutableListOf<Float>()
            val temps = mutableListOf<Float>()
            val humidities = mutableListOf<Float>()
            val kps = mutableListOf<Float>()

            do {
                timestamps.add(cursor.getLong(0))
                pressures.add(cursor.getFloat(1))
                temps.add(cursor.getFloat(2))
                humidities.add(cursor.getFloat(3))
                kps.add(cursor.getFloat(4))
            } while (cursor.moveToNext())
            cursor.close()

            // Формируем результат с дельтами (новое - старое)
            for (i in 0 until timestamps.size - 1) {
                result.add(mapOf(
                    "timestamp" to timestamps[i],
                    "pressure" to pressures[i],
                    "temp" to temps[i],
                    "humidity" to humidities[i],
                    "kp" to kps[i],
                    "pressure_change" to abs(pressures[i] - pressures[i + 1]),
                    "temp_change" to abs(temps[i] - temps[i + 1]),
                    "humidity_change" to abs(humidities[i] - humidities[i + 1])
                ))
            }
        } else {
            cursor.close()
        }
        return result
    }

    // ==================== ДНЕВНИК ====================
    fun addWellbeingEntry(timestamp: Long, date: String, score: Int, symptoms: String, notes: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_UW_TIMESTAMP, timestamp)
            put(COL_UW_DATE, date)
            put(COL_UW_SCORE, score)
            put(COL_UW_SYMPTOMS, symptoms)
            put(COL_UW_NOTES, notes)
        }
        return db.insert(TABLE_USER_WELLBEING, null, values)
    }

    fun getAllWellbeingEntries(): List<Map<String, Any>> {
        val db = this.readableDatabase
        val result = mutableListOf<Map<String, Any>>()
        val query = "SELECT * FROM $TABLE_USER_WELLBEING ORDER BY $COL_UW_TIMESTAMP DESC"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                result.add(mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COL_UW_ID)),
                    "timestamp" to cursor.getLong(cursor.getColumnIndexOrThrow(COL_UW_TIMESTAMP)),
                    "date" to cursor.getString(cursor.getColumnIndexOrThrow(COL_UW_DATE)),
                    "score" to cursor.getInt(cursor.getColumnIndexOrThrow(COL_UW_SCORE)),
                    "symptoms" to cursor.getString(cursor.getColumnIndexOrThrow(COL_UW_SYMPTOMS)),
                    "notes" to cursor.getString(cursor.getColumnIndexOrThrow(COL_UW_NOTES))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    fun deleteWellbeingEntry(entryId: Int): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_USER_WELLBEING, "$COL_UW_ID = ?", arrayOf(entryId.toString()))
    }
}