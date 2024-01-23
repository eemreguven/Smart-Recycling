package com.mrguven.smartrecycling.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.databinding.ActivityMapsBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var locationList: MutableList<Pair<LatLng, String>> = mutableListOf()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var currentLocationMarker: Marker
    private lateinit var currentLocation: LatLng
    private lateinit var selectedLocationMarker: Marker

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Read locations from the file
        readLocationsFromFile()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check and request location permissions if needed
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted, get current location
            setCurrentLocation()
        }

        binding.verifyButton.setOnClickListener {
            verifyButtonClickListener()
        }
    }

    private fun verifyButtonClickListener() {
        if (currentLocationMarker != selectedLocationMarker) {
            if (calculateDistance(
                    currentLocationMarker.position, selectedLocationMarker.position
                ) < LOCATION_VERIFICATION_DISTANCE
            ) {
                showLocationDialog(
                    R.string.location_verified,
                    R.string.location_verified_message,
                    R.raw.verified_animation
                ) {

                    val intent = Intent(this, LiveDetectionActivity::class.java)
                    intent.putExtra(CONTAINER_KEY, selectedLocationMarker.title)
                    startActivity(intent)
                }
            } else {
                showLocationDialog(
                    R.string.location_not_verified,
                    R.string.location_not_verified_message,
                    R.raw.not_verified_animation
                ) {}
            }
        }
    }

    private fun showLocationDialog(
        titleRes: Int,
        messageRes: Int,
        rawRes: Int,
        onAnimationEndCallback: () -> Unit
    ) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.location_dialog, null)

        // Set texts in the custom layout
        val titleTextView = dialogView.findViewById<TextView>(R.id.title)
        titleTextView.text = getString(titleRes)
        val messageTextView = dialogView.findViewById<TextView>(R.id.message)
        messageTextView.text = getString(messageRes)

        // Set animation from JSON file in the res/raw folder
        val lottieAnimationView = dialogView.findViewById<LottieAnimationView>(R.id.animationView)
        lottieAnimationView.setAnimation(rawRes)
        lottieAnimationView.playAnimation()

        builder.setView(dialogView)
        val dialog: AlertDialog = builder.create()
        dialog.show()
        Handler().postDelayed({
            dialog.dismiss()
            onAnimationEndCallback.invoke()
        }, 3000) //
    }

    private fun readLocationsFromFile(fileName: String = "locations.txt") {
        try {
            val inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(", ")
                if (parts.size == 3) {
                    val name = parts[0]
                    val latitude = parts[1].toDouble()
                    val longitude = parts[2].toDouble()
                    val location = Pair(LatLng(latitude, longitude), name)
                    locationList.add(location)
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setCurrentLocation() {
        // Check for location permissions
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are granted, get the last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    // Move and zoom the camera to the current location
                    if (::mMap.isInitialized) {
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLocation, DEFAULT_ZOOM_LEVEL
                            )
                        )
                        // Add a marker for the current location
                        currentLocationMarker = mMap.addMarker(
                            MarkerOptions().position(currentLocation).title("Current Location")
                                .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_AZURE
                                    )
                                )
                        )!!

                        if (!::selectedLocationMarker.isInitialized) {
                            selectedLocationMarker = currentLocationMarker
                        }

                        // Sort locations by distance to the current location
                        sortLocationsByDistance(currentLocation)
                    }
                }
            }
        } else {
            // Permission is not granted, handle accordingly (e.g., request permission)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setSelectedLocationTexts() {
        binding.selectedLocationName.text = selectedLocationMarker.title
        val distance = calculateDistance(
            selectedLocationMarker.position, currentLocation
        )
        val distanceText = "${distance.toInt()} m"
        binding.selectedLocationDistance.text = distanceText
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        for (location in locationList) {
            mMap.addMarker(MarkerOptions().position(location.first).title(location.second))
        }

        // If location permissions are granted, show the current location on the map
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.setOnMarkerClickListener { marker ->
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                marker.showInfoWindow()

                if (selectedLocationMarker != currentLocationMarker) {
                    selectedLocationMarker.setIcon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
                selectedLocationMarker = marker

                if (selectedLocationMarker != currentLocationMarker) {
                    selectedLocationMarker.setIcon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }
                setSelectedLocationTexts()
                false
            }
            mMap.setOnMyLocationButtonClickListener {
                currentLocationMarker.remove()
                setCurrentLocation()
                setSelectedLocationTexts()
                true
            }
        }
    }

    private fun sortLocationsByDistance(currentLocation: LatLng) {
        // Sort the locationList by distance to the current location
        locationList = locationList.sortedBy { location ->
            val distanceArray = floatArrayOf(0f)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                location.first.latitude,
                location.first.longitude,
                distanceArray
            )
            distanceArray[0]
        }.toMutableList()
    }

    private fun calculateDistance(location1: LatLng, location2: LatLng): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            location1.latitude, location1.longitude, location2.latitude, location2.longitude, result
        )
        return result[0]
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_ZOOM_LEVEL = 14f
        private const val LOCATION_VERIFICATION_DISTANCE = 20f
        const val CONTAINER_KEY = "CONTAINER_TITLE"
    }
}
