package com.example.mapsapp

import com.google.android.gms.maps.model.LatLng
import java.time.LocalDate

data class SavedDistance(
    val id: Int,
    val pathPoints: List<LatLng>,
    val distance: Double,
    val date: LocalDate
)
