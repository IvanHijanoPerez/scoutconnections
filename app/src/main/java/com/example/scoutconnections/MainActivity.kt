package com.example.scoutconnections

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        supportActionBar?.hide()

        val registerBtn = findViewById<Button>(R.id.register_btn)
        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val loginBtn = findViewById<Button>(R.id.login_btn)
        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user != null) {
            startActivity(Intent(this, PanelActivity::class.java))
            finish()
        } else {
        }
    }

    override fun onStart() {
        checkUserStatus()
        super.onStart()
    }

}