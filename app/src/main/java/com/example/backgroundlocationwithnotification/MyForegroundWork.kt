package com.example.backgroundlocationwithnotification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MyForegroundWork(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    var progress = "Starting work..."
    var NOTIFICATION_ID = 1
    private var locationRequest: LocationRequest? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        var isStopped = false
    }

    init {
        Companion.isStopped = false
    }

    override fun doWork(): Result {
        Log.d("TAG", "1")
        setForegroundAsync(showNotification(progress))

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations){
                    val location =
                        "Latitude: " + location.latitude + "\n" + "Longitude: " + location.longitude

                    updateNotification(location)
                }
            }
        }
        startLocationUpdates()
        return Result.success()
    }

    private fun showNotification(progress: String): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, createNotification(progress))
    }

    private fun createNotification(progress: String): Notification {
        val CHANNEL_ID = "100"
        val title = "Foreground Work"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, title,
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_android)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(progress: String) {
        val notification = createNotification(progress)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).setMinUpdateIntervalMillis(5000).build()

        for (i in 0..99) {
            if (MyForegroundWork.isStopped) {
                break
            }
            try {
                Thread.sleep(5000)
                locationRequest?.let {
                    fusedLocationClient.requestLocationUpdates(
                        it,
                        locationCallback,
                        Looper.getMainLooper())
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
