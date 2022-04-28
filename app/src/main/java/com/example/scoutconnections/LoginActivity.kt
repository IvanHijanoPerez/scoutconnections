package com.example.scoutconnections

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        mAuth = FirebaseAuth.getInstance()

        supportActionBar?.hide()

        val emailEdt = findViewById<EditText>(R.id.email_edt)
        val passwordEdt = findViewById<EditText>(R.id.password_edt)
        val loginBtn = findViewById<Button>(R.id.log_btn)
        val googleLoginBtn = findViewById<SignInButton>(R.id.googleLogin_btn)
        val noAccountTxt = findViewById<TextView>(R.id.no_account_txt)
        val recoverPassTxt = findViewById<TextView>(R.id.recover_password_txt)

        loginBtn.setOnClickListener {
            val email = emailEdt.text.toString()
            val password = passwordEdt.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEdt.error = getString(R.string.invalid_email)
                emailEdt.isFocusable = true
            } else {
                loginUser(email, password)
            }
        }

        noAccountTxt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        recoverPassTxt.setOnClickListener {
            showRecoverPassword()
        }

        googleLoginBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        progressDialog = ProgressDialog(this)

    }

    private fun loginUser(email: String, password: String) {
        progressDialog.setMessage(getString(R.string.logging_in))
        progressDialog.show()
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                progressDialog.dismiss()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.authentication_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRecoverPassword() {
        val customDialog = AlertDialog.Builder(this)
        customDialog.setTitle(getString(R.string.recover_password))

        val linearLayout = LinearLayout(this)
        val emailEt = EditText(this)
        emailEt.hint = getString(R.string.email)
        emailEt.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        emailEt.minEms = 16

        linearLayout.addView(emailEt)
        linearLayout.setPadding(10, 10, 10, 10)

        customDialog.setView(linearLayout)

        customDialog.setPositiveButton(
            getString(R.string.recover),
            DialogInterface.OnClickListener { _, _ ->
                val email = emailEt.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.existing_email),
                        Toast.LENGTH_SHORT
                    ).show()
                    showRecoverPassword()
                } else {
                    startRecovery(email)
                }
            })

        customDialog.setNegativeButton(
            getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            })

        customDialog.create().show()
    }

    private fun startRecovery(email: String) {
        progressDialog.setMessage(getString(R.string.sending_email))
        progressDialog.show()

        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.email_sent), Toast.LENGTH_SHORT).show()
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.recovery_failed), Toast.LENGTH_SHORT).show()
                    showRecoverPassword()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {

                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {

                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val user = mAuth.currentUser

                    if (task.result.additionalUserInfo?.isNewUser == true) {

                        val email = user?.email
                        val uid = user?.uid
                        val pos = email?.indexOf("@")
                        val name = email?.substring(0, Integer.parseInt(pos.toString()))

                        val hashMap = HashMap<String, Any>()
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

                        val db =
                            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                        val reference = db.getReference("Users")
                        if (uid != null) {
                            reference.child(uid).setValue(hashMap)
                        }
                    }

                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.authentication_error), Toast.LENGTH_SHORT).show()
                }
            }
    }

}