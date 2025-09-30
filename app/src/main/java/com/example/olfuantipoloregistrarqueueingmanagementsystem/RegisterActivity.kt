package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerBtn: Button
    private lateinit var loginLink: TextView

    private val url = "https://olfu-registrar.ellequin.com/api/register.php"
    // Use your PC's LAN IP if testing on a real device, e.g. "http://192.168.1.5/queue/register.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Reference XML elements
        firstName = findViewById(R.id.inputFirstName)
        lastName = findViewById(R.id.inputLastName)
        email = findViewById(R.id.inputEmailRegister)
        password = findViewById(R.id.inputPasswordRegister)
        registerBtn = findViewById(R.id.btnRegister)
        loginLink = findViewById(R.id.txtSwitchToLogin)

        // Register button click
        registerBtn.setOnClickListener {
            val fname = firstName.text.toString().trim()
            val lname = lastName.text.toString().trim()
            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()

            if (fname.isEmpty() || lname.isEmpty() || mail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                sendRegistrationData(fname, lname, mail, pass)
            }
        }

        // Switch to login activity
        loginLink.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun sendRegistrationData(fname: String, lname: String, mail: String, pass: String) {
        val requestQueue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    val message = json.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                    if (status.equals("success", ignoreCase = true)) {
                        // Registration successful, redirect to login
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid response from server", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "first_name" to fname,
                    "last_name" to lname,
                    "email" to mail,
                    "password" to pass
                )
            }
        }

        requestQueue.add(stringRequest)
    }
}
