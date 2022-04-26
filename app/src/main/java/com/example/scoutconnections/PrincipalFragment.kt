package com.example.scoutconnections

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class PrincipalFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mAuth = FirebaseAuth.getInstance()
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_principal, container, false)

        return view
    }

    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario(){
        val usuario = mAuth.currentUser
        if(usuario == null){
            startActivity(Intent(activity,MainActivity::class.java))
            activity?.finish()
        }else{
            //correoTxt.setText(usuario.email)
        }
    }

    //Función que añade menu al actionBar
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_principal, menu)

        menu.findItem(R.id.accion_buscar).setVisible(false)
        menu.findItem(R.id.accion_usuarios).setVisible(false)

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    //Función que indica la acción al seleccionar el menu del actionBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == R.id.accion_logout){
            mAuth.signOut()
            comprobarEstadoUsuario()
        }

        if(id == R.id.accion_anadir_post){
            startActivity(Intent(activity,PublicarPostActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

}