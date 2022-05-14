package com.example.scoutconnections

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.AddParticipantAdapter
import com.example.scoutconnections.adapters.ChatListAdapter
import com.example.scoutconnections.models.ChatListModel
import com.example.scoutconnections.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupAddParticipantActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var actionBar: ActionBar
    private lateinit var addParticipantAdapter: AddParticipantAdapter
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    lateinit var groupId: String
    lateinit var roleUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_add_participant)

        recyclerView = findViewById<RecyclerView>(R.id.participants_recycler_view)

        actionBar = supportActionBar!!
        actionBar.title = getString(R.string.add_participant)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        groupId = intent.getStringExtra("groupId").toString()

        loadGroupInfo()

    }

    private fun getAllUsers() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Users")
        val listUsers: MutableList<UserModel> = ArrayList()

        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listUsers.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    val us = ds.getValue(UserModel::class.java)

                    val referenceGr = db.getReference("Groups").child(groupId).child("Participants")
                    referenceGr.orderByChild("uid").equalTo(us!!.uid).addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()){
                                listUsers.add(us)
                            }
                            listUsers.sortBy { it.name }
                            addParticipantAdapter = AddParticipantAdapter(this@GroupAddParticipantActivity, listUsers, groupId, roleUser)
                            recyclerView.adapter = addParticipantAdapter

                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })

                }

            }

            override fun onCancelled(error: DatabaseError) {
            }

        })





    }

    private fun loadGroupInfo() {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Groups")
        reference.orderByChild("gid").equalTo(groupId).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val gId = ds.child("gid").value.toString()
                    val gTitle = ds.child("title").value.toString()
                    val gDescription = ds.child("description").value.toString()
                    val gImage = ds.child("image").value.toString()
                    val gCreator = ds.child("creator").value.toString()
                    val gTime = ds.child("time").value.toString()

                    reference.child(groupId).child("Participants").child(user!!.uid).addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            roleUser = snapshot.child("role").value.toString()

                            getAllUsers()
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })

                }
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