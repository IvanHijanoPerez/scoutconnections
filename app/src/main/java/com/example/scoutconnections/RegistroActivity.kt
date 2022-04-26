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

class RegistroActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        //Esconder el actionBar superior
        var actionBar = getSupportActionBar()
        if(actionBar != null){
            actionBar.hide();
        }

        //Base de datos Firebase
        mAuth = FirebaseAuth.getInstance()

        //Inicializar variables de los objetos visibles
        val correoEdt = findViewById<EditText>(R.id.correo_edt)
        val contraEdt = findViewById<EditText>(R.id.contrasena_edt)
        val registerBtn = findViewById<Button>(R.id.registrarse_btn)
        val tienesCuentaTxt = findViewById<TextView>(R.id.tienescuenta_txt)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Registrando usuario...")

        //Botón de registro
        registerBtn.setOnClickListener {
            val correo = correoEdt.text.toString()
            val contrasena = contraEdt.text.toString()

            if(!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
                correoEdt.setError("Correo electrónico inválido")
                correoEdt.setFocusable(true)
            }else if(contrasena.length < 6){
                contraEdt.setError("La longitud de la contraseña debe tener al menos 6 carácteres")
                contraEdt.setFocusable(true)
            }else{
                registrarUsuario(correo,contrasena)
            }
        }

        //Botón de tienes cuenta
        tienesCuentaTxt.setOnClickListener {
            startActivity(Intent(this,InicioSesionActivity::class.java))
            finish()
        }

    }

    //Función para registrar usuario
    private fun registrarUsuario(correo: String, contrasena: String){
        progressDialog.show()
        mAuth.createUserWithEmailAndPassword(correo,contrasena).addOnCompleteListener{ task ->
            if(task.isSuccessful){
                progressDialog.dismiss()

                val usuario = mAuth.currentUser

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

                Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,PanelActivity::class.java))
                finish()
            }else{
                progressDialog.dismiss()
                Toast.makeText(this, "Error en la autenticación", Toast.LENGTH_SHORT).show()
            }
        }
    }

}