package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.QueueResponse
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.RequestItem
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.RetrofitClient
import com.example.olfuantipoloregistrarqueueingmanagementsystem.worker.QueueCheckWorker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class QueueFragment : Fragment() {

    private lateinit var requestsContainer: LinearLayout
    private lateinit var queueNumberText: TextView
    private lateinit var queueMessageText: TextView
    private lateinit var nowServingText: TextView
    private lateinit var lineText: TextView
    private lateinit var nameText: TextView

    private val myStudentNumber: String by lazy {
        arguments?.getString("student_number")?.trim() ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_queue, container, false)

        // Bind views
        requestsContainer = view.findViewById(R.id.requestsContainer)
        queueNumberText = view.findViewById(R.id.queueNumberText)
        queueMessageText = view.findViewById(R.id.queueMessageText)
        nowServingText = view.findViewById(R.id.nowServingText)
        lineText = view.findViewById(R.id.lineText)
        nameText = view.findViewById(R.id.nameText)

        // Only load queue and requests once on fragment creation
        if (myStudentNumber.isNotEmpty()) {
            loadQueueAndRequests()
            // Start the background worker for notifications
            QueueCheckWorker.startWorker(requireContext(), myStudentNumber)
        }

        return view
    }

    private fun loadQueueAndRequests() {
        if (myStudentNumber.isEmpty()) return

        RetrofitClient.instance.getQueueWithRequests(myStudentNumber)
            .enqueue(object : Callback<QueueResponse> {
                override fun onResponse(call: Call<QueueResponse>, response: Response<QueueResponse>) {
                    if (!isAdded || context == null) return
                    if (!response.isSuccessful) return
                    val queueResponse = response.body() ?: return
                    if (!queueResponse.success) return

                    // Flag user queue
                    queueResponse.queue?.forEach {
                        it.isUserQueue = it.student_number.trim() == myStudentNumber.trim()
                    }

                    // Get user request
                    val userRequest = queueResponse.requests?.firstOrNull {
                        it.student_number.trim() == myStudentNumber.trim()
                    }

                    // Update main queue info
                    queueNumberText.text = userRequest?.queueing_num ?: "--"
                    queueMessageText.text = when (userRequest?.status) {
                        "Serving" -> "\uD83C\uDF89 It's your turn! Please proceed to the counter."
                        "Pending" -> "You are ${userRequest.serving_position ?: "--"} in line."
                        else -> "Your request is ${userRequest?.status ?: "--"}."
                    }

                    val servingQueue = queueResponse.queue?.firstOrNull { it.status == "Serving" }
                    if (servingQueue != null) {
                        nowServingText.text = servingQueue.queueing_num ?: "--"
                        lineText.text = "Line: ${servingQueue.serving_position ?: "--"}"
                        nameText.text = "Name: ${servingQueue.first_name} ${servingQueue.last_name}"
                    } else {
                        nowServingText.text = "--"
                        lineText.text = "Line: --"
                        nameText.text = "Name: --"
                    }

                    displayRequests(queueResponse.requests ?: emptyList())
                }

                override fun onFailure(call: Call<QueueResponse>, t: Throwable) {
                    if (!isAdded || context == null) return
                }
            })
    }

    private fun displayRequests(requests: List<RequestItem>) {
        if (!isAdded || context == null) return

        requestsContainer.removeAllViews()
        val weightSum = 10f

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#008C45"))
            setPadding(12, 12, 12, 12)
            this.weightSum = weightSum
        }

        fun createHeaderText(text: String, weight: Float) = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            gravity = Gravity.CENTER
        }

        header.addView(createHeaderText("Date Requested", 3f))
        header.addView(createHeaderText("Documents", 3f))
        header.addView(createHeaderText("Status", 2f))
        header.addView(createHeaderText("Serving Position", 2f))
        requestsContainer.addView(header)

        if (requests.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "You have not submitted any requests yet."
                setPadding(16, 16, 16, 16)
                setTextColor(Color.DKGRAY)
                textSize = 14f
                gravity = Gravity.CENTER
            }
            requestsContainer.addView(emptyText)
            return
        }

        val sdfInput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

        requests.forEach { req ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12, 12, 12, 12)
                this.weightSum = weightSum
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }

            fun createCell(text: String?, weight: Float) = TextView(context).apply {
                this.text = text ?: ""
                setTextColor(Color.BLACK)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            }

            val formattedDate = try {
                sdfInput.parse(req.created_at)?.let { sdfOutput.format(it) } ?: req.created_at
            } catch (e: Exception) {
                req.created_at
            }

            row.addView(createCell(formattedDate, 3f))
            row.addView(createCell(req.documents, 3f))
            row.addView(createCell(req.status, 2f))
            row.addView(createCell(req.serving_position?.toString() ?: "--", 2f))

            requestsContainer.addView(row)
        }
    }
}
