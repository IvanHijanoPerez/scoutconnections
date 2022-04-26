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

        //Esconder el actionBar superior
        var actionBar = getSupportActionBar()
        if(actionBar != null){
            actionBar.hide();
        }

        //Botón de registro
        val registerBtn = findViewById<Button>(R.id.registro_btn)
        registerBtn.setOnClickListener {
            startActivity(Intent(this,RegistroActivity::class.java))
        }

        //Botón de inicio de sesión
        val loginBtn = findViewById<Button>(R.id.login_btn)
        loginBtn.setOnClickListener {
            startActivity(Intent(this,InicioSesionActivity::class.java))
        }

    }

    private fun comprobarEstadoUsuario(){
        val usuario = mAuth.currentUser
        if(usuario != null){
            startActivity(Intent(this,PanelActivity::class.java))
            finish()
        }else{
            //correoTxt.setText(usuario.email)
        }
    }

    //Función que indica que hacer al iniciar la app
    override fun onStart() {
        comprobarEstadoUsuario()
        super.onStart()
    }

}