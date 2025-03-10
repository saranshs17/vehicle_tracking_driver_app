package com.example.vehicle_tracking_driver_app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.models.DriverRequest
import com.example.vehicle_tracking_driver_app.models.GenericResponse
import com.example.vehicle_tracking_driver_app.network.ApiService
import com.example.vehicle_tracking_driver_app.network.RetrofitClient
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var driverLocation: Location? = null

    // GeoFire instance for updating driver's location (using "driver_locations" node)
    private lateinit var driverGeoFire: GeoFire

    // We'll use a separate GeoFire instance for querying user requests (from "user_locations")
    private lateinit var userGeoFire: GeoFire

    // Bottom sheet views to display request details
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    // Currently selected request ID (key) when a marker is tapped
    private var selectedRequestId: String? = null

    // Bottom navigation view
    private lateinit var bottomNavigationView: BottomNavigationView

    // For continuous location updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Bottom Sheet views
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // (If you want to use request details, you might add TextViews/buttons inside the bottom sheet.)

        // Bottom navigation setup
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true // Already on home screen
                R.id.nav_requests -> {
                    startActivity(Intent(this, RequestsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Initialize FusedLocationProviderClient for continuous location updates.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize GeoFire for updating driver's location using "driver_locations" node.
        val driverRef = FirebaseDatabase.getInstance().getReference("driver_locations")
        driverGeoFire = GeoFire(driverRef)

        // Initialize GeoFire for querying user requests from "user_locations" node.
        val userRef = FirebaseDatabase.getInstance().getReference("user_locations")
        userGeoFire = GeoFire(userRef)

        // Set up the map fragment.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up continuous location updates.
        setupLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true

        // Get the current location (one-time) and start location updates.
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                driverLocation = location
                val driverLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng, 15f))
                updateDriverLocation(location)
                queryNearbyUserRequests(location)
            } else {
                Toast.makeText(this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle marker clicks if needed.
        mMap.setOnMarkerClickListener { marker ->
            // Example: if a marker is tapped, you could fetch additional details.
            // For now, we simply log the driver ID.
            val driverId = marker.title
            if (driverId != null) {
                Toast.makeText(this, "Driver ID: $driverId", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    driverLocation = location
                    updateDriverLocation(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateDriverLocation(location: Location) {
        // Retrieve the driver's unique MongoDB ObjectId from SharedPreferences.
        val prefs = getSharedPreferences("driver_prefs", MODE_PRIVATE)
        val driverId = prefs.getString("driverId", null)
        if (driverId == null) {
            Toast.makeText(this, "Driver ID not set. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Update the location in Firebase using GeoFire with the actual driverId.
        driverGeoFire.setLocation(driverId, GeoLocation(location.latitude, location.longitude)) { key, error ->
            if (error != null) {
                Log.e("GeoFire", "Error updating location for driver: $key, error: ${error.message}")
            } else {
                Log.d("GeoFire", "Location updated successfully for driver: $key")
            }
        }
    }

    private fun queryNearbyUserRequests(location: Location) {
        // Query for user requests within a 5 km radius from the driver's current location.
        val geoQuery = userGeoFire.queryAtLocation(GeoLocation(location.latitude, location.longitude), 5.0)
        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                val requestLatLng = LatLng(location.latitude, location.longitude)
                runOnUiThread {
                    val markerOptions = MarkerOptions().position(requestLatLng).title(key)
                    mMap.addMarker(markerOptions)
                }
            }
            override fun onKeyExited(key: String) {}
            override fun onKeyMoved(key: String, location: GeoLocation) {}
            override fun onGeoQueryReady() {}
            override fun onGeoQueryError(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "GeoQuery error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // (Optional) If you want to send a request for a driver, use this function.
    private fun sendDriverRequest(driverId: String) {
        // Retrieve stored JWT token from SharedPreferences.
        val token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.requestDriver("Bearer $token", DriverRequest(driverId = driverId))
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@HomeActivity, "Request sent successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@HomeActivity, "Failed to send request.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@HomeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMapReady(mMap)
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
