package com.example.vehicle_tracking_driver_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.models.DriverLoginRequest
import com.example.vehicle_tracking_driver_app.models.LoginResponse
import com.example.vehicle_tracking_driver_app.models.TokenUpdateRequest
import com.example.vehicle_tracking_driver_app.network.ApiService
import com.example.vehicle_tracking_driver_app.network.RetrofitClient
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.material.color.DynamicColors
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignup = findViewById(R.id.btnSignup)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val loginRequest = DriverLoginRequest(email, password)
            apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val token = response.body()?.token ?: ""
                        val driverId = response.body()?.driverId ?: ""

                        // Save token and driverId in SharedPreferences
                        val prefs = getSharedPreferences("driver_prefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("token", token)
                            putString("driverId", driverId)
                            apply()
                        }

                        // Create or update driver's node in "driver_locations" using GeoFire.
                        // Here we initialize the node with a default location (0.0, 0.0); HomeActivity will update it later.
                        val driverRef = FirebaseDatabase.getInstance().getReference("driver_locations")
                        val geoFire = GeoFire(driverRef)
                        geoFire.setLocation(driverId, GeoLocation(0.0, 0.0)) { key, error ->
                            if (error != null) {
                                // Log error if needed.
                            } else {
                                // Optionally log success.
                            }
                        }

                        // Update FCM token after login.
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fcmToken = task.result
                                apiService.updateToken("Bearer $token", TokenUpdateRequest(fcmToken))
                                    .enqueue(object : Callback<com.example.vehicle_tracking_driver_app.models.GenericResponse> {
                                        override fun onResponse(
                                            call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>,
                                            response: Response<com.example.vehicle_tracking_driver_app.models.GenericResponse>
                                        ) {
                                            // Optionally log success.
                                        }
                                        override fun onFailure(
                                            call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>,
                                            t: Throwable
                                        ) { }
                                    })
                            }
                        }

                        // Navigate to HomeActivity.
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
