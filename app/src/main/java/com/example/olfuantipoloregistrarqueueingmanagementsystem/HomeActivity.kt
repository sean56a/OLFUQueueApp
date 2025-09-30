package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // just a FrameLayout container

        val bundle = intent.extras

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, HomeFragment().apply { arguments = bundle })
        }
    }
}
