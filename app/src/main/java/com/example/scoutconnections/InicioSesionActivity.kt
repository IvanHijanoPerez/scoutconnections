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

class InicioSesionActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var googleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_sesion)

        //Inicio de sesion google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //Base de datos Firebase
        mAuth = FirebaseAuth.getInstance()

        //Esconder el actionBar superior
        var actionBar = getSupportActionBar()
        if(actionBar != null){
            actionBar.hide();
        }

        //Inicializar variables de los objetos visibles
        val correoEdt = findViewById<EditText>(R.id.correo_edt)
        val contraEdt = findViewById<EditText>(R.id.contrasena_edt)
        val loginBtn = findViewById<Button>(R.id.iniciarsesion_btn)
        val googleLoginBtn = findViewById<SignInButton>(R.id.googleLogin_btn)
        val noTienesCuentaTxt = findViewById<TextView>(R.id.notienescuenta_txt)
        val recuperarContrasenaTxt = findViewById<TextView>(R.id.recuperarcontrasena_txt)


        //Botón de login
        loginBtn.setOnClickListener {
            val correo = correoEdt.text.toString()
            val contrasena = contraEdt.text.toString()

            if(!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
                correoEdt.setError("Correo electrónico inválido")
                correoEdt.setFocusable(true)
            }else{
                loginUsuario(correo,contrasena)
            }
        }

        //Botón de no tener contraseña
        noTienesCuentaTxt.setOnClickListener {
            startActivity(Intent(this,RegistroActivity::class.java))
            finish()
        }

        //Botón de recuperar contraseña
        recuperarContrasenaTxt.setOnClickListener {
            showRecuperarContrasena()
        }

        //Botón de login google
        googleLoginBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        progressDialog = ProgressDialog(this)

    }

    //Función para hacer login de usuario normal
    private fun loginUsuario(correo: String, contrasena: String){
        progressDialog.setMessage("Iniciando sesión...")
        progressDialog.show()
        mAuth.signInWithEmailAndPassword(correo,contrasena).addOnCompleteListener{ task ->
            if(task.isSuccessful){
                progressDialog.dismiss()
                startActivity(Intent(this,PanelActivity::class.java))
                finish()
            }else{
                progressDialog.dismiss()
                Toast.makeText(this, "Error en la autenticación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Función que muestra el campo de recuperar contraseña
    private fun showRecuperarContrasena(){
        val customDialog = AlertDialog.Builder(this)
        customDialog.setTitle("Recuperar contraseña")

        val linearLayout = LinearLayout(this)
        val emailEt = EditText(this)
        emailEt.setHint("Correo electrónico")
        emailEt.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        emailEt.minEms = 16

        linearLayout.addView(emailEt)
        linearLayout.setPadding(10,10,10,10)

        customDialog.setView(linearLayout)

        customDialog.setPositiveButton("Recuperar",DialogInterface.OnClickListener { dialogInterface, i ->
            var email = emailEt.text.toString().trim()
            if(email.isEmpty()){
                Toast.makeText(this, "Por favor, rellena el campo con un correo electrónico existente", Toast.LENGTH_SHORT).show()
                showRecuperarContrasena()
            }else{
                empezarRecuperacion(email)
            }
        })

        customDialog.setNegativeButton("Cancelar",DialogInterface.OnClickListener { dialogInterface, i ->
            dialogInterface.dismiss()
        })

        customDialog.create().show()
    }

    //Función auxiliar para recuperar contraseña
    private fun empezarRecuperacion(email: String){
        progressDialog.setMessage("Enviando correo...")
        progressDialog.show()

        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Correo enviado", Toast.LENGTH_SHORT).show()
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Ha fallado la recuperación", Toast.LENGTH_SHORT).show()
                    showRecuperarContrasena()
                }
            }
    }

    //Función auxiliar de login Google
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Función auxiliar de login Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information

                    val usuario = mAuth.currentUser

                    if(task.result.additionalUserInfo?.isNewUser == true){

                        val email = usuario?.email
                        val uid = usuario?.uid
                        val pos = email?.indexOf("@")
                        val nombre = email?.substring(0,Integer.parseInt(pos.toString()))

                        var hashMap = HashMap<String, Any>()
                        if (email != null) {
                            hashMap.put("correo", email)
                        }
                        if (uid != null) {
                            hashMap.put("uid",uid)
                        }
                        if (nombre != null) {
                            hashMap.put("nombre",nombre)
                        }
                        hashMap.put("telefono","123456789")
                        hashMap.put("imagen","")
                        hashMap.put("estado","en línea")
                        hashMap.put("escribiendo","nadie")
                        hashMap.put("fondo","")
                        hashMap.put("monitor",false)

                        val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                        val referencia = baseDatos.getReference("Usuarios")
                        if (uid != null) {
                            referencia.child(uid).setValue(hashMap)
                        }
                    }

                    startActivity(Intent(this,PanelActivity::class.java))
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Error en la autenticación", Toast.LENGTH_SHORT).show()
                }
            }
    }

}