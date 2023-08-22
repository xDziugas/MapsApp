package com.example.mapsapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SavedDistancesAdapter(context: Context, savedDistances: List<SavedDistance>) : ArrayAdapter<SavedDistance>(context, 0, savedDistances) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        val viewHolder: ViewHolder

        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.list_item_saved_distance, parent, false)
            viewHolder = ViewHolder(itemView)
            itemView.tag = viewHolder
        } else {
            viewHolder = itemView.tag as ViewHolder
        }

        val savedDistance = getItem(position)

        // Bind the data to the UI elements in the list item layout
        viewHolder.pathPointsTextView.text = savedDistance?.pathPoints.toString()
        viewHolder.distanceTextView.text = savedDistance?.distance.toString()

        return itemView!!
    }

    private class ViewHolder(view: View) {
        val pathPointsTextView: TextView = view.findViewById(R.id.pathPointsTextView)
        val distanceTextView: TextView = view.findViewById(R.id.tv_savedDistance)
    }
}