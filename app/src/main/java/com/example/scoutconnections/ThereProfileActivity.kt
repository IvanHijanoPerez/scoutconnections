package com.example.scoutconnections

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.PostAdapter
import com.example.scoutconnections.models.PostModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.lang.Exception

class ThereProfileActivity : AppCompatActivity() {

    lateinit var userId: String
    val mAuth = FirebaseAuth.getInstance()
    lateinit var postsRecyclerView: RecyclerView
    val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_there_profile)

        postsRecyclerView = findViewById<RecyclerView>(R.id.recycler_view_posts_profile)
        checkUserStatus()

        userId = intent.getStringExtra("uid").toString()

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.profile)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val imageProfile = findViewById<ImageView>(R.id.image_profile)
        val coverProfile = findViewById<ImageView>(R.id.cover_profile)
        val nameProfile = findViewById<TextView>(R.id.name_profile)
        val emailProfile = findViewById<TextView>(R.id.email_profile)
        val phoneProfile = findViewById<TextView>(R.id.phone_profile)
        val roleProfile = findViewById<TextView>(R.id.role_profile)

        val reference = db.getReference("Users")
        val query = reference.orderByChild("uid").equalTo(userId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val name = ds.child("name").value.toString()
                    val image = ds.child("image").value.toString()
                    val email = ds.child("email").value.toString()
                    val phone = ds.child("phone").value.toString()
                    val role = ds.child("monitor").value
                    val cover = ds.child("cover").value.toString()
                    nameProfile.text = name
                    emailProfile.text = String(Character.toChars(0x1F4EC)) + " " + email
                    phoneProfile.text = String(Character.toChars(0x1F4DE)) + " " + phone
                    if (role != null) {
                        if (role == false) {
                            roleProfile.text = String(Character.toChars(0x1F530)) + " Scout"
                        } else {
                            roleProfile.text = String(Character.toChars(0x1F464)) + " Monitor"
                        }
                    }
                    try {
                        if (image != "") {
                            Picasso.get().load(image).into(imageProfile)
                        }
                    } catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_profile_24).into(imageProfile)
                    }

                    try {
                        Picasso.get().load(cover).into(coverProfile)
                    } catch (e: Exception) {
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ThereProfileActivity, getString(R.string.profile_problem), Toast.LENGTH_SHORT)
                    .show()
            }

        })


        loadPosts()

    }

    private fun loadPosts() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        postsRecyclerView.layoutManager = layoutManager

        var listPosts: MutableList<PostModel> = ArrayList()

        val reference = db.getReference("Posts")

        val query = reference.orderByChild("creator").equalTo(userId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listPosts.clear()
                dataSnapshot.children.forEach {
                    val postModel = it.getValue(PostModel::class.java)
                    listPosts.add(postModel!!)

                }
                val postAdapters = PostAdapter(this@ThereProfileActivity, listPosts)

                postsRecyclerView.adapter = postAdapters
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ThereProfileActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.principal_menu, menu)
        menu!!.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_users).isVisible = false
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}