package com.example.vehicle_tracking_driver_app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.models.DriverRequest
import com.example.vehicle_tracking_driver_app.models.GenericResponse
import com.example.vehicle_tracking_driver_app.models.Request
import com.example.vehicle_tracking_driver_app.models.UserResponse
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var driverLocation: Location? = null

    // To update driver's location using GeoFire on "driver_locations" node.
    private lateinit var driverGeoFire: GeoFire
    // For user location updates on "user_locations" node.
    private lateinit var userGeoFire: GeoFire

    // A map to keep track of user markers (accepted users) keyed by userId.
    private val userMarkersMap = mutableMapOf<String, Marker>()

    // Bottom sheet views (to display selected user's details)
    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var tvUserName: TextView
    private lateinit var tvUserContact: TextView
    private lateinit var dragHandle: View

    // Bottom navigation view.
    private lateinit var bottomNavigationView: BottomNavigationView

    // For continuous location updates.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // The driver's unique MongoDB ObjectId.
    private var driverId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize bottom sheet views.
        bottomSheet = findViewById(R.id.bottomSheet)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserContact = findViewById(R.id.tvUserContact)
        dragHandle = findViewById(R.id.dragHandle)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.skipCollapsed = false
        bottomSheetBehavior.peekHeight = 120

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val alpha = 0.5f + slideOffset / 2
                tvUserContact.alpha = alpha
            }
        })

        dragHandle.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        // Bottom navigation setup.
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
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

        // Initialize FusedLocationProviderClient.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get driverId from SharedPreferences.
        driverId = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("driverId", null)
        if (driverId == null) {
            Toast.makeText(this, "Driver ID not set. Please log in again.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initialize GeoFire for driver's and user's locations.
        val driverRef = FirebaseDatabase.getInstance().getReference("driver_locations")
        driverGeoFire = GeoFire(driverRef)
        val userRef = FirebaseDatabase.getInstance().getReference("user_locations")
        userGeoFire = GeoFire(userRef)

        // Set up the map fragment.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Start continuous location updates.
        setupLocationUpdates()

        // Fetch accepted requests (and thus accepted user IDs) for this driver.
        fetchAcceptedRequests()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        mMap.isMyLocationEnabled = true

        // Get driver's current location and update driver's marker.
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                driverLocation = location
                val driverLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng, 15f))
                updateDriverLocation(location)
            } else {
                Toast.makeText(this, "Unable to retrieve driver location.", Toast.LENGTH_SHORT).show()
            }
        }

        mMap.setOnMarkerClickListener { marker: Marker ->
            val userID = marker.title
            if (userID != null) {
                fetchUserDetailsFromBackend(userID)
            }
            true
        }
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000L
            fastestInterval = 5000L
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateDriverLocation(location: Location) {
        driverId?.let {
            driverGeoFire.setLocation(it, GeoLocation(location.latitude, location.longitude)) { key, error ->
                if (error != null) {
                    Log.e("GeoFire", "Error updating driver location: ${error.message}")
                } else {
                    Log.d("GeoFire", "Driver location updated for: $key")
                }
            }
        }
    }

    // ----------------- Accepted Requests and Live User Location ------------------

    // Fetch accepted requests for this driver from the backend.
    // The endpoint should return a list of Request objects where each Request.status is "accepted"
    // and Request.user contains the accepted user's details.
    private fun fetchAcceptedRequests() {
        val token = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getAcceptedRequests("Bearer $token").enqueue(object : Callback<List<Request>> {
            override fun onResponse(call: Call<List<Request>>, response: Response<List<Request>>) {
                if (response.isSuccessful) {
                    val acceptedRequests = response.body() ?: emptyList()
                    acceptedRequests.forEach { request ->
                        // For each accepted request, start listening for the user's location.
                        // Assume request.user._id contains the user's MongoDB ObjectId.
                        startUserLocationListener(request.user._id)
                    }
                } else {
                    Toast.makeText(this@HomeActivity, "Failed to fetch accepted requests.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Request>>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Listen for live location updates for an accepted user.
    private fun startUserLocationListener(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("user_locations").child(userId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val typeIndicator = object : GenericTypeIndicator<List<Double>>() {}
                    val locationList = snapshot.child("l").getValue(typeIndicator)
                    if (locationList != null && locationList.size >= 2) {
                        val lat = locationList[0]
                        val lng = locationList[1]
                        val userLatLng = LatLng(lat, lng)
                        // Update or create marker for this accepted user.
                        if (userMarkersMap.containsKey(userId)) {
                            userMarkersMap[userId]?.position = userLatLng
                        } else {
                            val marker = mMap.addMarker(MarkerOptions().position(userLatLng).title(userId))
                            if (marker != null) {
                                userMarkersMap[userId] = marker
                            }
                        }
//                        // Also fetch user details from backend to update bottom sheet.
//                        fetchUserDetailsFromBackend(userId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fetch user details from backend using GET /api/user/{id}.
    private fun fetchUserDetailsFromBackend(userId: String) {
        val token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getUserById("Bearer $token", userId).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val userData = response.body()
                    if (userData != null) {
                        tvUserName.text = "User: ${userData.name}"
                        tvUserContact.text = "Phone: ${userData.phone}"
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                } else {
                    Toast.makeText(this@HomeActivity, "User details not found (API).", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // (Optional) Method to send a driver request.
    private fun sendDriverRequest(driverId: String) {
        val token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.requestDriver("Bearer $token", DriverRequest(driverId = driverId))
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@HomeActivity, "Request sent successfully.", Toast.LENGTH_SHORT).show()
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    } else {
                        Toast.makeText(this@HomeActivity, "Failed to send request.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@HomeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
