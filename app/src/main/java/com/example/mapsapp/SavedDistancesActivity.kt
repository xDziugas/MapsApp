package com.example.mapsapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView

class SavedDistancesActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var navMaps: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_distances_activity)

        databaseHelper = DatabaseHelper(this)

        val savedDistances = databaseHelper.getSavedDistances()

        val adapter = SavedDistancesAdapter(this, savedDistances)
        val listView = findViewById<ListView>(R.id.lv_dataList)
        listView.adapter = adapter

        // Display the saved distances in the activity
        // You can use a RecyclerView, ListView, or any other UI component to display the data
        // Iterate over the savedDistances list and populate your UI accordingly

        navMaps = findViewById(R.id.btn_NavMaps)

        navMaps.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_NavMaps -> {
                val intent = Intent(this, MapsActivity::class.java)
                databaseHelper.close()
                startActivity(intent)
                finish() //??
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
    }
}