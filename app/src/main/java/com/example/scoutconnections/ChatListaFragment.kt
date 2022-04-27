package com.example.scoutconnections

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth


/**
 * A simple [Fragment] subclass.
 * Use the [ChatListaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatListaFragment : Fragment() {

    val mAuth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_lista, container, false)
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
        menuInflater.inflate(R.menu.principal_menu, menu)

        menu.findItem(R.id.action_search).setVisible(false)
        menu.findItem(R.id.action_add_post).setVisible(false)


        super.onCreateOptionsMenu(menu, menuInflater)
    }

    //Función que indica la acción al seleccionar el menu del actionBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == R.id.action_logout){
            mAuth.signOut()
            comprobarEstadoUsuario()
        }else if(id == R.id.action_users){
            /*var actionBar = (activity as AppCompatActivity?)!!.supportActionBar
            actionBar?.setTitle("Usuarios")
            val fragment = UsuariosFragment()
            val ft = (activity as AppCompatActivity?)!!.supportFragmentManager.beginTransaction()
            ft.replace(R.id.contenido,fragment)
            ft.commit()*/
            startActivity(Intent(activity,UsuariosActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }



}