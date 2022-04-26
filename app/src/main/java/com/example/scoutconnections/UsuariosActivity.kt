package com.example.scoutconnections

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.UsuarioAdapter
import com.example.scoutconnections.models.UsuarioModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UsuariosActivity : AppCompatActivity() {

    lateinit var recyclerView : RecyclerView
    val mAuth = FirebaseAuth.getInstance()
    val usuario = mAuth.currentUser
    val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    val referencia = baseDatos.getReference("Usuarios")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios)

        recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.usuarios_recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val actionBar = getSupportActionBar()
        actionBar!!.setTitle("Usuarios")
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        obtenerUsuarios()
    }

    private fun obtenerUsuarios() {
        var listaUsuarios : MutableList<UsuarioModel> = ArrayList()

        referencia.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                listaUsuarios.clear()
                dataSnapshot.children.forEach {
                    val usuarioModel = it.getValue(UsuarioModel::class.java)
                    if (usuario != null) {
                        if (usuarioModel != null) {
                            if(!usuarioModel.uid.equals(usuario.uid)){
                                listaUsuarios.add(usuarioModel)
                            }
                        }
                    }
                    val usuarioAdapters = UsuarioAdapter(applicationContext,listaUsuarios)

                    recyclerView.adapter = usuarioAdapters
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    private fun buscarUsuarios(consulta: String) {
        var listaUsuarios : MutableList<UsuarioModel> = ArrayList()

        referencia.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                listaUsuarios.clear()
                dataSnapshot.children.forEach {
                    val usuarioModel = it.getValue(UsuarioModel::class.java)
                    if (usuario != null) {
                        if (usuarioModel != null) {
                            if(!usuarioModel.uid.equals(usuario.uid)){

                                if(usuarioModel.nombre?.toLowerCase()?.contains(consulta.toLowerCase()) == true || usuarioModel.correo?.toLowerCase()?.contains(consulta.toLowerCase()) == true){
                                    listaUsuarios.add(usuarioModel)
                                }


                            }
                        }
                    }
                    val usuarioAdapters = UsuarioAdapter(applicationContext,listaUsuarios)

                    if (usuarioAdapters != null) {
                        usuarioAdapters.notifyDataSetChanged()
                    }

                    recyclerView.adapter = usuarioAdapters
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }


    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario(){
        val usuario = mAuth.currentUser
        if(usuario == null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }else{
            //correoTxt.setText(usuario.email)
        }
    }

    //Función que añade menu al actionBar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)

        val item = menu?.findItem(R.id.accion_buscar)
        val searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.queryHint = "Busca..."
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (p0 != null) {
                    if(!TextUtils.isEmpty(p0.trim())){
                        buscarUsuarios(p0)
                    }
                }else{
                    obtenerUsuarios()
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (p0 != null) {
                    if(!TextUtils.isEmpty(p0.trim())){
                        buscarUsuarios(p0)
                    }
                }else{
                    obtenerUsuarios()
                }
                return false
            }

        })
        searchView.setOnCloseListener(SearchView.OnCloseListener {
            obtenerUsuarios()
            false
        })

        menu?.findItem(R.id.accion_usuarios)?.setVisible(false)
        menu?.findItem(R.id.accion_anadir_post)?.setVisible(false)
        menu?.findItem(R.id.accion_logout)?.setVisible(false)
        return super.onCreateOptionsMenu(menu)
    }


    //Función que indica la acción al seleccionar el menu del actionBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == R.id.accion_logout){
            mAuth.signOut()
            comprobarEstadoUsuario()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

}