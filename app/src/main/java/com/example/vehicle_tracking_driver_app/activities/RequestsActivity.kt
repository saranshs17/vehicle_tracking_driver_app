package com.example.vehicle_tracking_driver_app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.adapters.RequestsAdapter
import com.example.vehicle_tracking_driver_app.models.Request
import com.example.vehicle_tracking_driver_app.network.ApiService
import com.example.vehicle_tracking_driver_app.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RequestsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestsAdapter
    private var requestsList = mutableListOf<Request>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_requests)

        recyclerView = findViewById(R.id.recyclerViewRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RequestsAdapter(requestsList, this)
        recyclerView.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        val token = getSharedPreferences("driver_prefs", MODE_PRIVATE).getString("token", "") ?: ""
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getRequests("Bearer $token").enqueue(object : Callback<List<Request>> {
            override fun onResponse(call: Call<List<Request>>, response: Response<List<Request>>) {
                if (response.isSuccessful) {
                    requestsList.clear()
                    response.body()?.let { list ->
                        requestsList.addAll(list)
                    }
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
}
