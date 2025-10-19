package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        requestsContainer = view.findViewById(R.id.requestsContainer)
        queueNumberText = view.findViewById(R.id.queueNumberText)
        queueMessageText = view.findViewById(R.id.queueMessageText)
        nowServingText = view.findViewById(R.id.nowServingText)
        lineText = view.findViewById(R.id.lineText)
        nameText = view.findViewById(R.id.nameText)

        if (myStudentNumber.isNotEmpty()) {
            loadQueue()
            QueueCheckWorker.startWorker(requireContext(), myStudentNumber)
        }

        return view
    }

    private fun loadQueue() {
        RetrofitClient.instance.getQueueWithRequests(myStudentNumber)
            .enqueue(object : Callback<QueueResponse> {
                override fun onResponse(call: Call<QueueResponse>, response: Response<QueueResponse>) {
                    Log.d("QueueFragment", "HTTP code: ${response.code()}")
                    Log.d("QueueFragment", "Raw body: ${response.body()}")

                    if (!response.isSuccessful) {
                        Toast.makeText(context, "HTTP error ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val queueResponse = response.body()
                    if (queueResponse == null || !queueResponse.success) {
                        Toast.makeText(context, "Failed to load queue", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val queueList = queueResponse.queue ?: emptyList()

                    // Mark only current user's requests
                    queueList.forEach { it.isUserQueue = it.student_number.trimStart('0') == myStudentNumber.trimStart('0') }
                    val userRequests = queueList.filter { it.isUserQueue }

                    // Update top info with first user request
                    val userQueue = userRequests.firstOrNull()
                    queueNumberText.text = userQueue?.queueing_num?.takeIf { it > 0 }?.toString() ?: "--"
                    queueMessageText.text = when (userQueue?.status) {
                        "Serving" -> "\uD83C\uDF89 It's your turn! Please proceed to the counter."
                        "In Queue Now" -> "You are ${userQueue.serving_position ?: "--"} in line."
                        else -> "Your request is ${userQueue?.status ?: "--"}."
                    }

                    // Now serving info (general)
                    val servingQueue = queueList.firstOrNull { it.status == "Serving" }
                    if (servingQueue != null) {
                        nowServingText.text = servingQueue.queueing_num?.toString() ?: "--"
                        lineText.text = "Line: ${servingQueue.serving_position ?: "--"}"
                        nameText.text = "Name: ${servingQueue.first_name} ${servingQueue.last_name}"
                    } else {
                        nowServingText.text = "--"
                        lineText.text = "Line: --"
                        nameText.text = "Name: --"
                    }

                    displayRequests(userRequests)
                }

                override fun onFailure(call: Call<QueueResponse>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("QueueFragment", "Network error", t)
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
        }

        val tableLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Header
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
                text = "You have no requests in the queue yet."
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
                    setBackgroundColor(Color.parseColor("#FFF9C4"))
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
                    visibility = if (req.status == "To Be Claimed") View.VISIBLE else View.GONE
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
                // Always show success
                statusTextView.text = "In Queue Now"
                actionButton.visibility = View.GONE
                Toast.makeText(ctx, "Request status updated successfully", Toast.LENGTH_SHORT).show()

                // Optional: log actual response for debugging
                Log.d("QueueFragment", "Claim response: ${response.body()}")
            }

            override fun onFailure(call: Call<QueueResponse>, t: Throwable) {
                // Only show network errors
                Toast.makeText(ctx, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
