package com.example.vehicle_tracking_driver_app.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.models.DriverProfile
import com.example.vehicle_tracking_driver_app.models.UpdateProfileRequest
import com.example.vehicle_tracking_driver_app.network.ApiService
import com.example.vehicle_tracking_driver_app.network.RetrofitClient
import com.google.android.material.color.DynamicColors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPlate: EditText
    private lateinit var btnUpdate: Button
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPlate = findViewById(R.id.etPlate)
        btnUpdate = findViewById(R.id.btnUpdate)

        token = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("token", "") ?: ""

        loadProfile()

        btnUpdate.setOnClickListener {
            val updateRequest = UpdateProfileRequest(
                name = etName.text.toString(),
                email = etEmail.text.toString(),
                phone = etPhone.text.toString(),
                plateNumber = etPlate.text.toString(),
                photo = null
            )
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.updateProfile("Bearer $token", updateRequest)
                .enqueue(object : Callback<DriverProfile> {
                    override fun onResponse(call: Call<DriverProfile>, response: Response<DriverProfile>) {
                        if(response.isSuccessful){
                            Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<DriverProfile>, t: Throwable) {
                        Toast.makeText(this@ProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun loadProfile() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getProfile("Bearer $token").enqueue(object : Callback<DriverProfile> {
            override fun onResponse(call: Call<DriverProfile>, response: Response<DriverProfile>) {
                if(response.isSuccessful){
                    val profile = response.body()
                    etName.setText(profile?.name)
                    etEmail.setText(profile?.email)
                    etPhone.setText(profile?.phone)
                    etPlate.setText(profile?.plateNumber)
                }
            }
            override fun onFailure(call: Call<DriverProfile>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
