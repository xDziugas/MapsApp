package com.example.mapsapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var pathPoints = mutableListOf<LatLng>()
    private var previousLocation: Location? = null
    private var isTracking = false
    private lateinit var startStopButton: Button
    private lateinit var distanceTextView: TextView
    var firstUpdate = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val MINIMUM_DISTANCE = 0.0 // offset in meters
        const val EXTRA_DISTANCE = "extra_distance"
        const val ACTION_UPDATE_LOCATION = "ACTION_UPDATE_LOCATION"
        const val EXTRA_LATITUDE = "extra_lat"
        const val EXTRA_LONGITUDE = "extra_lng"
    }

    private val locationReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {

            val distance: Double? = intent?.getDoubleExtra(EXTRA_DISTANCE, 0.0)
            distance?.let {
                if(distance != 0.0)
                    updateDistance(distance)
            }

            val lat = intent?.getDoubleExtra(EXTRA_LATITUDE, 200.0)
            val lng = intent?.getDoubleExtra(EXTRA_LONGITUDE, 200.0)

            if(lat != null && lat < 200.0
                && lng != null && lng < 200.0){
                val location = Location("Location")
                location.latitude = lat
                location.longitude = lng
                updatePathPoints(location)
            }

        }
    }

    private fun updatePathPoints(location: Location){
        if (previousLocation == null ||
            location.distanceTo(previousLocation) >= MINIMUM_DISTANCE
        ) {
            val latLng = LatLng(location.latitude, location.longitude)
            pathPoints.add(latLng)
            updateMapLocation(latLng)
            previousLocation = location

            if(!firstUpdate){
                addPointer(latLng, "Start")
                firstUpdate = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mw_map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        startStopButton = findViewById(R.id.btn_start)
        distanceTextView = findViewById(R.id.tv_distance)

        startService(Intent(this, LocationForegroundService::class.java))

        startStopButton.setOnClickListener {
            if (isTracking) {
                startStopButton.text = "Start"
                distanceTextView.visibility = View.GONE

                stopTracking()
            } else {
                startStopButton.text = "Stop"
                distanceTextView.visibility = View.VISIBLE

                updateDistance(0.0)
                startTracking()
            }
        }

        // Register the locationReceiver to receive location updates from the service
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter(ACTION_UPDATE_LOCATION)
        )
    }

    private fun addPointer(latLng: LatLng, text: String){
        val markerOptions = MarkerOptions().position(latLng)
            .snippet(text)
        mMap.addMarker(markerOptions)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkPermission()
    }

    @SuppressLint("MissingPermission")
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission if it's not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is already granted, start tracking
            mMap.isMyLocationEnabled = true
        }
    }

    private fun resetService(){
        val resetIntent = Intent(this, LocationForegroundService::class.java)
        resetIntent.action = LocationForegroundService.ACTION_RESET_DISTANCE
        startService(resetIntent)
    }

    private fun startTracking() {
        pathPoints.clear()
        previousLocation = null
        mMap.clear()
        resetService()
        isTracking = true
    }

    private fun stopTracking() {
        if(isTracking){
            mMap.clear()
            stopService()
            isTracking = false
            firstUpdate = false
        }
    }

    private fun stopService(){
        val stopIntent = Intent(this, LocationForegroundService::class.java)
        stopIntent.action = LocationForegroundService.ACTION_STOP_LOCATION_UPDATES
        startService(stopIntent)
    }

    private fun removeService(){
        val removeIntent = Intent(this, LocationForegroundService::class.java)
        removeIntent.action = LocationForegroundService.ACTION_REMOVE_SERVICE
        startService(removeIntent)
    }

    private fun updateMapLocation(latLng: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        drawPolyline()
    }

    @SuppressLint("SetTextI18n")
    private fun drawPolyline() {
        val polylineOptions = PolylineOptions().addAll(pathPoints)
        mMap.addPolyline(polylineOptions)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start tracking
                startTracking()
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission denied,")
            }
        }
    }

    private var isServiceBound = false
    private var locationService: LocationForegroundService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationForegroundService.LocalBinder
            locationService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isServiceBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, LocationForegroundService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the locationReceiver to avoid leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        //stopService()

        removeService()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    @SuppressLint("SetTextI18n")
    private fun updateDistance(distance: Double){
        val decimalFormat = DecimalFormat("#.##")
        val formattedValue = decimalFormat.format(distance)
        distanceTextView.text = "Distance: $formattedValue meters"
    }
}