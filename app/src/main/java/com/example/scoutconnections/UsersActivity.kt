package com.example.scoutconnections

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.UserAdapter
import com.example.scoutconnections.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UsersActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    private val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    private val reference = db.getReference("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        recyclerView = findViewById(R.id.users_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.users)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        getUsers()
    }

    private fun getUsers() {
        var listUsers: MutableList<UserModel> = ArrayList()

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listUsers.clear()
                dataSnapshot.children.forEach {
                    val userModel = it.getValue(UserModel::class.java)
                    if (user != null) {
                        if (userModel != null) {
                            if (!userModel.uid.equals(user.uid)) {
                                listUsers.add(userModel)
                            }
                        }
                    }
                    val userAdapters = UserAdapter(applicationContext, listUsers)

                    recyclerView.adapter = userAdapters
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    private fun searchUsers(query: String) {
        var listUsers: MutableList<UserModel> = ArrayList()

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listUsers.clear()
                dataSnapshot.children.forEach {
                    val userModel = it.getValue(UserModel::class.java)
                    if (user != null) {
                        if (userModel != null) {
                            if (!userModel.uid.equals(user.uid)) {

                                if (userModel.name?.toLowerCase()
                                        ?.contains(query.toLowerCase()) == true || userModel.email?.toLowerCase()
                                        ?.contains(query.toLowerCase()) == true
                                ) {
                                    listUsers.add(userModel)
                                }


                            }
                        }
                    }
                    val userAdapters = UserAdapter(
                        applicationContext,
                        listUsers
                    )

                    if (userAdapters != null) {
                        userAdapters.notifyDataSetChanged()
                    }

                    recyclerView.adapter = userAdapters
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.principal_menu, menu)

        val item = menu?.findItem(R.id.action_search)
        val searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (p0 != null) {
                    if (!TextUtils.isEmpty(p0.trim())) {
                        searchUsers(p0)
                    }
                } else {
                    getUsers()
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (p0 != null) {
                    if (!TextUtils.isEmpty(p0.trim())) {
                        searchUsers(p0)
                    }
                } else {
                    getUsers()
                }
                return false
            }

        })
        searchView.setOnCloseListener(SearchView.OnCloseListener {
            getUsers()
            false
        })

        menu?.findItem(R.id.action_users)?.isVisible = false
        menu?.findItem(R.id.action_add_post)?.isVisible = false
        menu?.findItem(R.id.action_logout)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            mAuth.signOut()
            checkUserStatus()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

}