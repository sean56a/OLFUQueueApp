package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.RequestItem

class QueueAdapter(
    private val queueList: List<RequestItem>
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {

    inner class QueueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtQueueNum: TextView = itemView.findViewById(R.id.txtQueueNum)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtDocuments: TextView = itemView.findViewById(R.id.txtDocuments)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return QueueViewHolder(view)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val item = queueList[position]

        holder.txtQueueNum.text = item.queueing_num?.toString() ?: "-"
        holder.txtName.text = "${item.first_name} ${item.last_name}"
        holder.txtDocuments.text = item.documents
        holder.txtStatus.text = item.status
    }

    override fun getItemCount(): Int = queueList.size
}
