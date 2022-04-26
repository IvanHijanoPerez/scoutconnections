package com.example.scoutconnections

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.example.scoutconnections.notifications.Token
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

class PanelActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    //private lateinit var correoTxt: TextView
    private lateinit var navegacionMenu: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel)

        //Base de datos Firebase
        mAuth = FirebaseAuth.getInstance()

        //Fragmento de Principal por defecto al iniciar
        var actionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setTitle("Principal")
        }
        val fragment = PrincipalFragment()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.contenido,fragment)
        ft.commit()

        //Inicializar variables de los objetos visibles
        //correoTxt = findViewById<TextView>(R.id.correo_txt)

        //Menu de navegacion
        navegacionMenu = findViewById(R.id.menu_navegacion)
        navegacionMenu.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (actionBar != null) {
                        actionBar.setTitle("Principal")
                    }
                    val fragment = PrincipalFragment()
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.contenido,fragment)
                    ft.commit()

                    true
                }
                R.id.nav_perfil -> {
                    if (actionBar != null) {
                        actionBar.setTitle("Perfil")
                    }
                    val fragment = PerfilFragment()
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.contenido,fragment)
                    ft.commit()

                    true
                }
                R.id.nav_chats -> {
                    if (actionBar != null) {
                        actionBar.setTitle("Chats")
                    }
                    val fragment = ChatListaFragment()
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.contenido,fragment)
                    ft.commit()

                    true
                }
                else -> false
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(object: OnCompleteListener<String>{
            override fun onComplete(p0: Task<String>) {
                actualizarToken(p0.result)
            }

        })

        //actualizarToken(FirebaseMessaging.getInstance().token.result)

    }

    //Actualizar token notificaciones
    public fun actualizarToken(token: String){
        val ref = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Tokens")
        val nToken = Token(token)
        val usuario = mAuth.currentUser
        ref.child(usuario!!.uid).setValue(nToken)
    }

    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario(){
        val usuario = mAuth.currentUser
        if(usuario == null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }else{
            val sp = getSharedPreferences("SP_USER", MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("Current_USERID", usuario.uid)
            editor.apply()
        }
    }

    //Función que indica que hacer al iniciar la app
    override fun onStart() {
        comprobarEstadoUsuario()
        super.onStart()
    }

    override fun onResume() {
        comprobarEstadoUsuario()
        super.onResume()
    }

}