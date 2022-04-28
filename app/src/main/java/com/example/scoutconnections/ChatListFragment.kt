package com.example.scoutconnections

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth


class ChatListFragment : Fragment() {

    val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        } else {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.principal_menu, menu)

        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        super.onCreateOptionsMenu(menu, menuInflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            mAuth.signOut()
            checkUserStatus()
        } else if (id == R.id.action_users) {
            /*var actionBar = (activity as AppCompatActivity?)!!.supportActionBar
            actionBar?.setTitle("Usuarios")
            val fragment = UsuariosFragment()
            val ft = (activity as AppCompatActivity?)!!.supportFragmentManager.beginTransaction()
            ft.replace(R.id.contenido,fragment)
            ft.commit()*/
            startActivity(Intent(activity, UsersActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }


}