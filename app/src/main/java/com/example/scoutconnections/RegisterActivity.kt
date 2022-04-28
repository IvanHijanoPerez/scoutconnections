package com.example.scoutconnections

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()

        val emailEdt = findViewById<EditText>(R.id.email_edt)
        val passwordEdt = findViewById<EditText>(R.id.password_edt)
        val registerBtn = findViewById<Button>(R.id.registration_btn)
        val haveAccountTxt = findViewById<TextView>(R.id.have_account_txt)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.registering_user))

        registerBtn.setOnClickListener {
            val email = emailEdt.text.toString()
            val password = passwordEdt.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEdt.error = getString(R.string.invalid_email)
                emailEdt.isFocusable = true
            } else if (password.length < 6) {
                passwordEdt.error = getString(R.string.password_length)
                passwordEdt.isFocusable = true
            } else {
                registerUser(email, password)
            }
        }

        haveAccountTxt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }

    private fun registerUser(email: String, password: String) {
        progressDialog.show()
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                progressDialog.dismiss()

                val user = mAuth.currentUser

                val email = user?.email
                val uid = user?.uid
                val pos = email?.indexOf("@")
                val name = email?.substring(0, Integer.parseInt(pos.toString()))

                var hashMap = HashMap<String, Any>()
                if (email != null) {
                    hashMap["email"] = email
                }
                if (uid != null) {
                    hashMap["uid"] = uid
                }
                if (name != null) {
                    hashMap["name"] = name
                }
                hashMap["phone"] = "123456789"
                hashMap["image"] = ""
                hashMap["status"] = "online"
                hashMap["typingTo"] = "noOne"
                hashMap["cover"] = ""
                hashMap["monitor"] = false

                val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                val reference = db.getReference("Users")
                if (uid != null) {
                    reference.child(uid).setValue(hashMap)
                }

                Toast.makeText(this, getString(R.string.user_registered), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.authentication_error), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}