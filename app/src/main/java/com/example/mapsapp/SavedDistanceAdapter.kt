package com.example.mapsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.mapsapp.MapsActivity.Companion.EXTRA_LATITUDE
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*

class SavedDistancesAdapter(
    context: Context,
    savedDistances: List<SavedDistance>,
    private val databaseHelper: DatabaseHelper,
    private val onItemClick: (SavedDistance?) -> Unit
) : ArrayAdapter<SavedDistance>(context, 0, savedDistances) {

    private lateinit var viewHolder: ViewHolder

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView

        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.list_item_saved_distance, parent, false)
            viewHolder = ViewHolder(itemView)
            itemView.tag = viewHolder
        } else {
            viewHolder = itemView.tag as ViewHolder
        }

        val savedDistance = getItem(position)

        viewHolder.deleteImageView.setOnClickListener {
            databaseHelper.deleteItemFromDatabase(this, savedDistance!!.id)
        }

        viewHolder.loadImageView.setOnClickListener {
            onItemClick(getItem(position))
        }

        //viewHolder.pathPointsTextView.text = savedDistance?.pathPoints.toString()
        updateDistance(savedDistance!!)

        return itemView!!
    }

    fun updateSavedDistances(newSavedDistances: List<SavedDistance>) {
        clear()
        addAll(newSavedDistances)
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    private fun updateDistance(savedDistance: SavedDistance){
        val distance = savedDistance.distance
        val decimalFormat = DecimalFormat("#.##")
        val formattedValue = decimalFormat.format(distance)
        viewHolder.distanceTextView.text = "Distance: $formattedValue meters"
        val date = savedDistance.date

        viewHolder.dateTextView.text = "Date: ${date.year}/${date.monthValue}/${date.dayOfMonth}"
    }

    private class ViewHolder(view: View) {
        val dateTextView: TextView = view.findViewById(R.id.tv_date)
        val distanceTextView: TextView = view.findViewById(R.id.tv_savedDistance)
        val deleteImageView: ImageView = view.findViewById(R.id.iv_delete)
        val loadImageView: ImageView = view.findViewById(R.id.iv_load)
    }

}

