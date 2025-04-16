package com.example.vehicle_tracking_driver_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.adapters.RequestsAdapter
import com.example.vehicle_tracking_driver_app.models.GenericResponse
import com.example.vehicle_tracking_driver_app.models.Request
import com.example.vehicle_tracking_driver_app.network.ApiService
import com.example.vehicle_tracking_driver_app.network.RetrofitClient
import com.google.android.material.color.DynamicColors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RequestsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestsAdapter
    private var requestsList = mutableListOf<Request>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_requests)

        recyclerView = findViewById(R.id.recyclerViewRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RequestsAdapter(requestsList, this,this)
        recyclerView.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        val token = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getRequests("Bearer $token").enqueue(object : Callback<List<Request>> {
            override fun onResponse(call: Call<List<Request>>, response: Response<List<Request>>) {
                if (response.isSuccessful) {
                    // Filter to only include pending requests
                    val allRequests = response.body() ?: emptyList()
                    val pendingRequests = allRequests.filter { it.status.equals("pending", ignoreCase = true) }
                    requestsList.clear()
                    requestsList.addAll(pendingRequests)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@RequestsActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Request>>, t: Throwable) {
                Toast.makeText(this@RequestsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    fun onAcceptRequest(request: Request) {
        val token = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.acceptRequest("Bearer $token", request._id).enqueue(object : Callback<com.example.vehicle_tracking_driver_app.models.GenericResponse> {
            override fun onResponse(call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>, response: Response<com.example.vehicle_tracking_driver_app.models.GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RequestsActivity, "Request accepted.", Toast.LENGTH_SHORT).show()
                    // Remove accepted request from list
                    // Send broadcast to refresh the map
                    val intent = Intent("com.example.vehicle_tracking_driver_app.REFRESH_MAP")
                    LocalBroadcastManager.getInstance(this@RequestsActivity).sendBroadcast(intent)
                    requestsList.remove(request)
                    adapter.notifyDataSetChanged()
                    finish()
                } else {
                    Toast.makeText(this@RequestsActivity, "Failed to accept request.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>, t: Throwable) {
                Toast.makeText(this@RequestsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun onRejectRequest(request: Request) {
        val token = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        // The backend endpoint for rejection should update status and remove the request.
        apiService.rejectRequest("Bearer $token", request._id).enqueue(object : Callback<com.example.vehicle_tracking_driver_app.models.GenericResponse> {
            override fun onResponse(call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>, response: Response<com.example.vehicle_tracking_driver_app.models.GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RequestsActivity, "Request rejected and removed.", Toast.LENGTH_SHORT).show()
                    // Remove rejected request from list
                    requestsList.remove(request)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@RequestsActivity, "Failed to reject request.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>, t: Throwable) {
                Toast.makeText(this@RequestsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
