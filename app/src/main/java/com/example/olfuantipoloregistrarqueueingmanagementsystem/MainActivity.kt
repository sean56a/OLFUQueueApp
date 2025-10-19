package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.olfuantipoloregistrarqueueingmanagementsystem.worker.QueueCheckWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var inputIdentifier: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: AppCompatButton
    private lateinit var txtSwitchToRegister: TextView
    private lateinit var overlayView: View
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var userBundle: Bundle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // Bind views
        inputIdentifier = findViewById(R.id.inputEmail) // still called inputEmail in XML
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtSwitchToRegister = findViewById(R.id.txtSwitchToRegister)
        overlayView = findViewById(R.id.overlayView)
        bottomNav = findViewById(R.id.bottomNav)

        // Switch to registration
        txtSwitchToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Login button
        btnLogin.setOnClickListener {
            val identifier = inputIdentifier.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email or student number and password", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(identifier, password)
            }
        }

        // Bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_form -> FormFragment()
                R.id.nav_queue -> QueueFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            fragment.arguments = userBundle
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, fragment)
            }
            true
        }
    }

    private fun loginUser(identifier: String, password: String) {
        val url = "http://10.0.2.2/queue/api/login.php" // emulator localhost
        Log.d("LoginRequest", "URL: $url, identifier=$identifier")

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                Log.d("LoginResponse", "Response: $response")
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    val message = json.getString("message")

                    if (status == "success") {
                        val user = json.getJSONObject("user")
                        userBundle = Bundle().apply {
                            putInt("user_id", user.getInt("id"))
                            putString("first_name", user.getString("first_name"))
                            putString("last_name", user.getString("last_name"))
                            putString("email", user.getString("email"))
                            putString("role", user.getString("role"))
                            putInt("department_id", user.optInt("department_id", -1))
                            putInt("counter_no", user.optInt("counter_no", -1))
                            putString("student_number", user.optString("student_num", "")) // matches PHP
                        }

                        // Hide login form
                        findViewById<LinearLayout>(R.id.formBox).visibility = View.GONE
                        findViewById<TextView>(R.id.title).visibility = View.GONE
                        findViewById<TextView>(R.id.schoolTitle).visibility = View.GONE
                        overlayView.visibility = View.GONE

                        // Show fragment container & bottom nav
                        findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragmentContainer).visibility = View.VISIBLE
                        bottomNav.visibility = View.VISIBLE

                        // Load HomeFragment
                        supportFragmentManager.commit {
                            replace(R.id.fragmentContainer, HomeFragment().apply { arguments = userBundle })
                        }

                        // Start QueueCheckWorker
                        val studentNumber = user.optString("student_num", "")
                        if (studentNumber.isNotEmpty()) {
                            QueueCheckWorker.startWorker(this, studentNumber)
                        }

                        // Show notification
                        showLoginNotification(user.getString("first_name"))

                    } else {
                        Toast.makeText(this, "Login failed: $message", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Response parsing error", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                val code = error.networkResponse?.statusCode
                Log.e("LoginError", "Volley error: ${error.message}, HTTP code: $code", error)
                Toast.makeText(this, "Network error: ${error.message} (HTTP $code)", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "identifier" to identifier, // matches PHP
                    "password" to password
                )
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "queue_status_channel",
                "Queue Status",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for your queue status"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showLoginNotification(firstName: String) {
        val builder = NotificationCompat.Builder(this, "queue_status_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Login Successful")
            .setContentText("Welcome back, $firstName!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1001, builder.build())
        }
    }
}
