package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var ivOlfuLogo: ImageView
    private lateinit var tvWelcomeUser: TextView
    private lateinit var tvCounterInfo: TextView
    private lateinit var registrarStatus: TextView
    private lateinit var btnGetQueue: AppCompatButton
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusUpdater: Runnable

    private var firstName: String = "Guest"
    private var lastName: String = ""
    private var role: String = "user"
    private var counterNo: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        ivOlfuLogo = view.findViewById(R.id.ivOlfuLogo)
        tvWelcomeUser = view.findViewById(R.id.tvWelcomeUser)
        tvCounterInfo = view.findViewById(R.id.tvCounterInfo)
        registrarStatus = view.findViewById(R.id.tvRegistrarStatus)
        btnGetQueue = view.findViewById(R.id.btnGetQueue)

        // Display PNG logo as-is (original colors)
        try {
            ivOlfuLogo.setImageResource(R.drawable.logo) // your colored PNG
        } catch (e: Exception) {
            ivOlfuLogo.setImageResource(R.drawable.ic_launcher_foreground) // fallback
        }

        // Extract user info from arguments safely
        arguments?.let { bundle ->
            firstName = bundle.getString("first_name", "Guest")
            lastName = bundle.getString("last_name", "")
            role = bundle.getString("role", "user")
            counterNo = bundle.getInt("counter_no", -1)
        }

        // Display personalized welcome
        tvWelcomeUser.text = "Welcome, $firstName $lastName"

        // Show staff counter info if applicable
        if (role == "staff" && counterNo != -1) {
            tvCounterInfo.visibility = View.VISIBLE
            tvCounterInfo.text = "Assigned to Counter #$counterNo"
        }

        // Initial registrar status
        updateRegistrarStatus()

        // Auto-update registrar status every minute
        statusUpdater = object : Runnable {
            override fun run() {
                updateRegistrarStatus()
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(statusUpdater)

        // Queue button click
        btnGetQueue.setOnClickListener {
            if (role == "staff" && counterNo != -1) {
                Toast.makeText(requireContext(), "You are assigned to Counter #$counterNo", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Please wait for your queue number", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateRegistrarStatus() {
        val tz = TimeZone.getTimeZone("Asia/Manila")
        val now = Calendar.getInstance(tz)
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val morningStart = 8 * 60
        val morningEnd = 12 * 60
        val afternoonStart = 13 * 60
        val afternoonEnd = 17 * 60

        val isOpen = (currentMinutes in morningStart until morningEnd) ||
                (currentMinutes in afternoonStart until afternoonEnd)

        registrarStatus.text = if (isOpen) "Registrar is: Open" else "Registrar is: Closed"
        registrarStatus.setTextColor(if (isOpen) Color.parseColor("#008c45") else Color.parseColor("#D32F2F"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(statusUpdater)
    }
}
