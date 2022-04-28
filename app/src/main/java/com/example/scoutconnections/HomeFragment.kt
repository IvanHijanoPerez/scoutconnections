package com.example.scoutconnections

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

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

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        return view
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.principal_menu, menu)
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_users).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        val user = mAuth.currentUser
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Users")

        val query = reference.orderByChild("email").equalTo(user?.email)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (ds: DataSnapshot in snapshot.children) {

                        val monitor = ds.child("monitor").value

                        if (monitor == false) {
                            menu.findItem(R.id.action_add_post).isVisible = false
                        }
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
            }

        })



        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            mAuth.signOut()
            checkUserStatus()
        }
        if (id == R.id.action_add_post) {
            startActivity(Intent(activity, AddPostActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

}