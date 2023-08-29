package com.example.mapsapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "saved_distances.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "saved_distances"
        // Define column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_PATH_POINTS = "path_points"
        private const val COLUMN_DISTANCE = "distance"
        private const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_PATH_POINTS TEXT, $COLUMN_DISTANCE REAL, $COLUMN_DATE TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
        db.execSQL(dropTableQuery)
        onCreate(db)
    }

    fun addSavedDistance(savedDistance: SavedDistance) {
        val db = writableDatabase

        val contentValues = ContentValues().apply {
            put(COLUMN_PATH_POINTS, convertPathPointsToString(savedDistance.pathPoints))
            put(COLUMN_DISTANCE, savedDistance.distance)
            put(COLUMN_DATE, savedDistance.date.toString())
        }

        db.insert(TABLE_NAME, null, contentValues)
        db.close()
    }

    private fun convertPathPointsToString(pathPoints: List<LatLng>): String {
        val gson = Gson()
        return gson.toJson(pathPoints)
    }

    fun getSavedDistances(): List<SavedDistance> {
        val savedDistances = mutableListOf<SavedDistance>()
        val db = readableDatabase

        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val idIndex = cursor.getColumnIndex(COLUMN_ID)
            val pathPointsIndex = cursor.getColumnIndex(COLUMN_PATH_POINTS)
            val distanceIndex = cursor.getColumnIndex(COLUMN_DISTANCE)
            val dateIndex = cursor.getColumnIndex(COLUMN_DATE)

            if (idIndex >= 0 && pathPointsIndex >= 0 && distanceIndex >= 0 && dateIndex >= 0) {
                val id = cursor.getInt(idIndex)
                val pathPointsJson = cursor.getString(pathPointsIndex)
                val distance = cursor.getDouble(distanceIndex)
                val date = cursor.getString(dateIndex)
                val dateFormatted = LocalDate.parse(date)

                val pathPoints = convertStringToPathPoints(pathPointsJson)
                val savedDistance = SavedDistance(id, pathPoints, distance, dateFormatted)
                savedDistances.add(savedDistance)
            }
        }

        cursor.close()
        db.close()
        return savedDistances
    }

    private fun convertStringToPathPoints(pathPointsJson: String): List<LatLng> {
        val gson = Gson()
        val type = object : TypeToken<List<LatLng>>() {}.type
        return gson.fromJson(pathPointsJson, type)
    }

    fun deleteItemFromDatabase(adapter: SavedDistancesAdapter, itemId: Int) {
        val db = writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(itemId.toString())
        db.delete(TABLE_NAME, whereClause, whereArgs)
        db.close()

        val newSavedDistances = getSavedDistances()
        // Update the adapter's savedDistances list
        adapter.updateSavedDistances(newSavedDistances)
    }

    // Implement other database operations (update, delete) as needed
}