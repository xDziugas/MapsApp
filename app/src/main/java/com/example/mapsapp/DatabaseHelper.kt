package com.example.mapsapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "saved_distances.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "saved_distances"
        // Define column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_PATH_POINTS = "path_points"
        private const val COLUMN_DISTANCE = "distance"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_PATH_POINTS TEXT, $COLUMN_DISTANCE REAL)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database schema changes if needed
    }

    fun addSavedDistance(savedDistance: SavedDistance) {
        val db = writableDatabase

        val contentValues = ContentValues().apply {
            put(COLUMN_PATH_POINTS, convertPathPointsToString(savedDistance.pathPoints))
            put(COLUMN_DISTANCE, savedDistance.distance)
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

            if (idIndex >= 0 && pathPointsIndex >= 0 && distanceIndex >= 0) {
                val id = cursor.getInt(idIndex)
                val pathPointsJson = cursor.getString(pathPointsIndex)
                val distance = cursor.getDouble(distanceIndex)

                val pathPoints = convertStringToPathPoints(pathPointsJson)
                val savedDistance = SavedDistance(id, pathPoints, distance)
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

    // Implement other database operations (update, delete) as needed
}