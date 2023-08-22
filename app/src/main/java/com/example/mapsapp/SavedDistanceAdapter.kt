package com.example.mapsapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.DecimalFormat

class SavedDistancesAdapter(context: Context, savedDistances: List<SavedDistance>) : ArrayAdapter<SavedDistance>(context, 0, savedDistances) {

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

        // Bind the data to the UI elements in the list item layout

        //viewHolder.pathPointsTextView.text = savedDistance?.pathPoints.toString()
        updateDistance(savedDistance?.distance ?: 0.0)

        return itemView!!
    }

    @SuppressLint("SetTextI18n")
    private fun updateDistance(distance: Double){
        val decimalFormat = DecimalFormat("#.##")
        val formattedValue = decimalFormat.format(distance)
        viewHolder.distanceTextView.text = "Distance: $formattedValue meters"
    }

    private class ViewHolder(view: View) {
        val pathPointsTextView: TextView = view.findViewById(R.id.pathPointsTextView)
        val distanceTextView: TextView = view.findViewById(R.id.tv_savedDistance)
    }
}