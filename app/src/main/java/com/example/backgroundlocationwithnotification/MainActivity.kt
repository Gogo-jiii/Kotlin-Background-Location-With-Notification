package com.example.backgroundlocationwithnotification

import android.Manifest
import android.annotation.SuppressLint
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.backgroundlocationwithnotification.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest? = null
    private var foregroundWorkRequest: WorkRequest? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val backgroundLocationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                    Toast.makeText(this, "background location access granted", Toast.LENGTH_SHORT)
                        .show()

                    val result = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token
                    )
                    result.addOnCompleteListener {
                        val location =
                            "Latitude: " + it.result.latitude + "\n" + "Longitude: " + it.result.longitude

                        binding.textView.text = location

                        startBackgroundWork()
                    }
                }

                else -> {
                    Toast.makeText(this, "no background location access", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val foregroundLocationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                        permissions.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            false
                        ) -> {
                    Toast.makeText(
                        this,
                        "foreground location access granted",
                        Toast.LENGTH_SHORT
                    ).show()

                    backgroundLocationPermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    )
                }

                else -> {
                    Toast.makeText(this, "no location access", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnGetLocation.setOnClickListener {
            if (isLocationEnabled()) {
                foregroundLocationPermissionRequest.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            } else {
                Toast.makeText(this, "Please turn ON the location.", Toast.LENGTH_SHORT)
                    .show()
                createLocationRequest()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startBackgroundWork() {
        foregroundWorkRequest = OneTimeWorkRequest.Builder(MyForegroundWork::class.java)
            .addTag("foregroundwork" + System.currentTimeMillis())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(this).enqueue(foregroundWorkRequest!!)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).setMinUpdateIntervalMillis(5000).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(
                        this,
                        100
                    )
                } catch (_: java.lang.Exception) {
                }
            }
        }
    }
}