package com.example.scoutconnections

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.PostAdapter
import com.example.scoutconnections.adapters.UserAdapter
import com.example.scoutconnections.models.PostModel
import com.example.scoutconnections.models.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PostLikedByActivity : AppCompatActivity() {

    private lateinit var postId: String
    private lateinit var listUsers: MutableList<UserModel>
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_liked_by)

        postId = intent.getStringExtra("postId").toString()

        recyclerView = findViewById<RecyclerView>(R.id.users_like_recycler_view)

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.post_liked)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        listUsers = ArrayList()

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Posts").child(postId)

        db.child("Likes").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listUsers.clear()
                snapshot.children.forEach {
                    val id = it.ref.key
                    getUser(id)


                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })


    }

    private fun getUser(id: String?) {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users")
        db.orderByChild("uid").equalTo(id).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val modelUser = it.getValue(UserModel::class.java)
                    if(!listUsers.contains(modelUser)){
                        listUsers.add(modelUser!!)
                    }

                }
                val adapterUser = UserAdapter(this@PostLikedByActivity, listUsers)
                recyclerView.adapter = adapterUser
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}