package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

        // Load queue and start worker
        if (myStudentNumber.isNotEmpty()) {
            loadQueueAndRequests()
            QueueCheckWorker.startWorker(requireContext(), myStudentNumber)
        }

        return view
    }

    private fun loadQueueAndRequests() {
        if (myStudentNumber.isEmpty()) return

        RetrofitClient.instance.getQueueWithRequests(myStudentNumber)
            .enqueue(object : Callback<QueueResponse> {
                override fun onResponse(
                    call: Call<QueueResponse>,
                    response: Response<QueueResponse>
                ) {
                    if (!isAdded || context == null) return
                    val queueResponse = response.body() ?: return
                    if (!queueResponse.success) return

                    // Update user queue info
                    val userRequest = queueResponse.requests?.firstOrNull {
                        it.student_number.trim() == myStudentNumber.trim()
                    }

                    queueNumberText.text = userRequest?.queueing_num ?: "--"
                    queueMessageText.text = when (userRequest?.status) {
                        "Serving" -> "\uD83C\uDF89 It's your turn! Please proceed to the counter."
                        "Pending" -> "You are ${userRequest.serving_position ?: "--"} in line."
                        else -> "Your request is ${userRequest?.status ?: "--"}."
                    }

                    // Update "Now Serving"
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

                    // Display requests
                    displayRequests(queueResponse.requests ?: emptyList())
                }

                override fun onFailure(call: Call<QueueResponse>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayRequests(requests: List<RequestItem>) {
        requestsContainer.removeAllViews()

        val scrollWrapper = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = true
        }

        val tableLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Table header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#008C45"))
            setPadding(12, 12, 12, 12)
        }

        fun createHeaderText(text: String, widthDp: Int) = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(widthDp.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }

        header.addView(createHeaderText("Date Requested", 140))
        header.addView(createHeaderText("Documents", 200))
        header.addView(createHeaderText("Status", 120))
        header.addView(createHeaderText("Serving Position", 120))
        header.addView(createHeaderText("Action", 100))
        tableLayout.addView(header)

        if (requests.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "You have not submitted any requests yet."
                setPadding(16, 16, 16, 16)
                setTextColor(Color.DKGRAY)
                textSize = 14f
                gravity = Gravity.CENTER
            }
            tableLayout.addView(emptyText)
        } else {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sdfOutput = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

            requests.forEach { req ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                }

                fun createCell(text: String?, widthDp: Int, maxLines: Int = 1) = TextView(context).apply {
                    this.text = text ?: ""
                    setTextColor(Color.BLACK)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(widthDp.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
                    gravity = Gravity.CENTER
                    this.maxLines = maxLines
                    ellipsize = TextUtils.TruncateAt.END
                }

                val formattedDate = try {
                    sdfInput.parse(req.created_at)?.let { sdfOutput.format(it) } ?: req.created_at
                } catch (e: Exception) { req.created_at }

                val statusCell = createCell(req.status, 120)

                row.addView(createCell(formattedDate, 140))
                row.addView(createCell(req.documents, 200, maxLines = 2))
                row.addView(statusCell)
                row.addView(createCell(req.serving_position?.toString() ?: "--", 120))

                val actionButton = Button(context).apply {
                    text = "Claim"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    setPadding(16, 6, 16, 6)
                    background = resources.getDrawable(R.drawable.rounded_button_green, null)
                    visibility = if (req.status == "To Be Claimed" && req.student_number == myStudentNumber)
                        View.VISIBLE else View.GONE
                    setOnClickListener {
                        claimRequest(req.id, statusCell, this)
                    }
                    layoutParams = LinearLayout.LayoutParams(100.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                row.addView(actionButton)

                tableLayout.addView(row)
            }
        }

        scrollWrapper.addView(tableLayout)
        requestsContainer.addView(scrollWrapper)
    }

    private fun claimRequest(requestId: Int, statusTextView: TextView, actionButton: Button) {
        val ctx = context ?: return
        RetrofitClient.instance.updateQueue(
            id = requestId,
            status = "In Queue Now",
            served_by = "Registrar"
        ).enqueue(object : Callback<QueueResponse> {
            override fun onResponse(call: Call<QueueResponse>, response: Response<QueueResponse>) {
                val json = response.body() ?: return
                if (json.success) {
                    statusTextView.text = "In Queue Now"
                    actionButton.visibility = View.GONE
                    Toast.makeText(ctx, "Request status updated to 'In Queue Now'", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Failed to update status.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<QueueResponse>, t: Throwable) {
                Toast.makeText(ctx, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Extension function to convert dp to px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
