package com.example.vehicle_tracking_driver_app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.activities.RequestsActivity
import com.example.vehicle_tracking_driver_app.models.Request


class RequestsAdapter(
    private val requests: List<Request>,
    private val context: Context,
    private val listener: RequestsActivity
) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvUserPhone: TextView = itemView.findViewById(R.id.tvUserPhone)
        val tvUserStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        holder.tvUserName.text = "User: ${request.user.name}"
        holder.tvUserPhone.text = "Phone: ${request.user.phone}"
        holder.tvUserStatus.text = "Status: ${request.status}"

        holder.btnAccept.setOnClickListener {
            listener.onAcceptRequest(request)
        }
        holder.btnReject.setOnClickListener {
            listener.onRejectRequest(request)
        }
    }

    override fun getItemCount(): Int = requests.size
}
