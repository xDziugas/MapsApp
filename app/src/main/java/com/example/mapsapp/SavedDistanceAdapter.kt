package com.example.mapsapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import java.text.DecimalFormat

class SavedDistancesAdapter(
    context: Context,
    private val savedDistances: MutableList<SavedDistance>,
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
            deleteItemFromDatabase(savedDistance?.id)
        }

        viewHolder.loadImageView.setOnClickListener {
            onItemClick(getItem(position))
        }

        updateDistance(savedDistance!!)

        return itemView!!
    }

    fun updateSavedDistances(newSavedDistances: List<SavedDistance>) {
        clear()
        addAll(newSavedDistances)
        notifyDataSetChanged()
    }

    private fun deleteItemFromDatabase(itemId: Int?) {
        itemId?.let { it ->
            databaseHelper.deleteItemFromDatabase(this, it)
            val itemToRemove = savedDistances.find { savedDistance -> savedDistance.id == it }
            itemToRemove?.let {
                savedDistances.remove(it)
                notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDistance(savedDistance: SavedDistance?) {
        savedDistance?.let {
            val distance = it.distance
            val decimalFormat = DecimalFormat("#.##")
            val formattedValue = decimalFormat.format(distance)
            viewHolder.distanceTextView.text = "Distance: $formattedValue meters"
            val date = it.date
            viewHolder.dateTextView.text = "Date: ${date.year}/${date.monthValue}/${date.dayOfMonth}"
        }
    }

    private class ViewHolder(view: View) {
        val dateTextView: TextView = view.findViewById(R.id.tv_date)
        val distanceTextView: TextView = view.findViewById(R.id.tv_savedDistance)
        val deleteImageView: ImageView = view.findViewById(R.id.iv_delete)
        val loadImageView: ImageView = view.findViewById(R.id.iv_load)
    }
}

