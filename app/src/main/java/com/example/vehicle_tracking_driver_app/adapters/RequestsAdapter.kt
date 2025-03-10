package com.example.vehicle_tracking_driver_app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.models.Request

class RequestsAdapter(
    private val requests: List<Request>,
    private val context: Context
) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        // Show the user's id here; you might want to look up user details separately if needed.
        holder.tvUserName.text = "User: ${request.user.name}"

        holder.btnAccept.setOnClickListener {
            // Implement your accept logic here (e.g., call an API to update the request status).
        }
        holder.btnReject.setOnClickListener {
            // Implement your reject logic here.
        }
    }

    override fun getItemCount(): Int = requests.size
}
