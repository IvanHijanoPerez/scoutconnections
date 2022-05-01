package com.example.scoutconnections

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.scoutconnections.notifications.Token
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class DashboardActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var navigationMenu: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        mAuth = FirebaseAuth.getInstance()

        val actionBar = supportActionBar
        actionBar?.title = getString(R.string.home)

        val fragment = HomeFragment()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content, fragment)
        ft.commit()

        navigationMenu = findViewById(R.id.navigation_menu)
        navigationMenu.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    actionBar?.title = getString(R.string.home)
                    val fragment = HomeFragment()
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.content, fragment)
                    ft.commit()
                    true
                }
                R.id.nav_perfil -> {
                    actionBar?.title = getString(R.string.profile)
                    val fragment = ProfileFragment()
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.content, fragment)
                    ft.commit()
                    true
                }
                R.id.nav_chats -> {
                    actionBar?.title = getString(R.string.chats)
                    val fragment = ChatListFragment()
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.content, fragment)
                    ft.commit()
                    true
                }
                else -> false
            }
        }



    }

    private fun updateToken(token: String) {
        val ref =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Tokens")
        val nToken = Token(token)
        val user = mAuth.currentUser
        ref.child(user!!.uid).setValue(nToken)
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            val sp = getSharedPreferences("SP_USER", MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("Current_USERID", user.uid)
            editor.apply()

            FirebaseMessaging.getInstance().token.addOnCompleteListener { p0 -> updateToken(p0.result) }
        }
    }

    override fun onStart() {
        checkUserStatus()
        super.onStart()
    }

    override fun onResume() {
        checkUserStatus()
        super.onResume()
    }

}