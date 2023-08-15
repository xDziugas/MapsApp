package com.example.mapsapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager.EXTRA_LOCATION
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.mapsapp.MapsActivity.Companion.ACTION_UPDATE_LOCATION
import com.example.mapsapp.MapsActivity.Companion.EXTRA_DISTANCE
import com.example.mapsapp.MapsActivity.Companion.EXTRA_LATITUDE
import com.example.mapsapp.MapsActivity.Companion.EXTRA_LONGITUDE
import com.google.android.gms.location.*
import java.text.DecimalFormat

class LocationForegroundService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    private val binder = LocalBinder()
    private var locationUpdatesEnabled = false


    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "location_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
        const val ACTION_RESET_DISTANCE = "ACTION_RESET_DISTANCE"
        const val ACTION_STOP_LOCATION_UPDATES = "ACTION_STOP_LOCATION_UPDATES"
        const val ACTION_REMOVE_SERVICE = "ACTION_REMOVE_SERVICE"
    }

    private var totalDistance = 0.0
    private var previousLocation: Location? = null

    inner class LocalBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context?, intent: Intent?) {

            val location: Location? = intent?.getParcelableExtra(EXTRA_LOCATION)
            location?.let {
                // Send the LatLng to MapsActivity using LocalBroadcastManager
                val updateIntent = Intent(ACTION_UPDATE_LOCATION)
                updateIntent.putExtra(EXTRA_LOCATION, location)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(updateIntent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000)
            .setIntervalMillis(2000) // Update location every 2 seconds
            .setMinUpdateIntervalMillis(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(ACTION_LOCATION_UPDATE)
        )
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESET_DISTANCE -> {
                // Reset the distance when the ACTION_RESET_DISTANCE action is received
                resetDistance()
            }
            ACTION_STOP_LOCATION_UPDATES -> {
                // Stop location updates when the ACTION_STOP_LOCATION_UPDATES action is received
                resetNotification()
                stopLocationUpdates()
            }

            ACTION_REMOVE_SERVICE -> {
                removeNotification()
                stopLocationUpdates()
            }

            else -> {
                // Start location updates and foreground service by default
                startForegroundService()
                startLocationUpdates()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        removeNotification()
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Location Tracking")
            .setContentText("Tracking your location...")
            .setSmallIcon(R.drawable.mario)
            .setContentIntent(pendingIntent)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val decimalFormat = DecimalFormat("#.##")
        val formattedValue = decimalFormat.format(totalDistance)

        val notificationText = "Distance: $formattedValue meters"
        notificationBuilder.setContentText(notificationText)

        // Update the notification using the same notification ID
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun removeNotification(){
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun resetNotification(){
        val notificationText = "Tracking your location..."
        notificationBuilder.setContentText(notificationText)

        // Update the notification using the same notification ID
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun sendLocationUpdate(location: Location) {
        if(locationUpdatesEnabled){
            // Calculate the distance between previousLocation and current location
            if (previousLocation != null) {
                val distance = previousLocation!!.distanceTo(location)
                totalDistance += distance
            }

            updateNotification()

            val updateIntent = Intent(ACTION_UPDATE_LOCATION)

            updateIntent.putExtra(EXTRA_DISTANCE, totalDistance)
            updateIntent.putExtra(EXTRA_LATITUDE, location.latitude)
            updateIntent.putExtra(EXTRA_LONGITUDE, location.longitude)

            LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent)
            previousLocation = location
        }
    }

    private fun isServiceRunningInForeground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (checkLocationPermission() && locationUpdatesEnabled) {

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)

                    if(locationUpdatesEnabled){
                        val location = locationResult.lastLocation
                        if (location != null){
                            sendLocationUpdate(location)
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                null
            )
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopLocationUpdates() {
        locationUpdatesEnabled = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    private fun resetDistance() {
        totalDistance = 0.0
        previousLocation = null
        locationUpdatesEnabled = true
        startLocationUpdates()
    }
}
