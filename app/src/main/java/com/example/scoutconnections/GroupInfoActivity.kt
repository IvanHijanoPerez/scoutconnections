package com.example.scoutconnections

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.AddParticipantAdapter
import com.example.scoutconnections.adapters.ChatListAdapter
import com.example.scoutconnections.models.ChatListModel
import com.example.scoutconnections.models.UserModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class GroupInfoActivity : AppCompatActivity() {

    lateinit var groupId: String
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    lateinit var actionBar: ActionBar
    lateinit var role: String
    lateinit var descriptionGroupInfo: TextView
    lateinit var createdByInfo: TextView
    lateinit var editGroupInfo: TextView
    lateinit var addParticipantInfo: TextView
    lateinit var leaveGroupInfo: TextView
    private lateinit var imageGroupInfo: ImageView
    private lateinit var addParticipantAdapter: AddParticipantAdapter
    private lateinit var participantsGroupInfo: TextView
    private lateinit var participantsRvGroupInfo: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)

        groupId = intent.getStringExtra("groupId").toString()

        actionBar = supportActionBar!!
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        imageGroupInfo = findViewById<ImageView>(R.id.image_groupinfo)
        descriptionGroupInfo = findViewById<TextView>(R.id.description_groupinfo)
        createdByInfo = findViewById<TextView>(R.id.created_by_groupinfo)
        editGroupInfo = findViewById<TextView>(R.id.edit_group_info)
        addParticipantInfo = findViewById<TextView>(R.id.add_participant_group_info)
        leaveGroupInfo = findViewById<TextView>(R.id.leave_group_info)
        participantsGroupInfo = findViewById<TextView>(R.id.participants_group_info)
        participantsRvGroupInfo = findViewById<RecyclerView>(R.id.participants_rv_group_info)

        loadGroupInfo()
        loadMyRole()

        addParticipantInfo.setOnClickListener {
            val intent = Intent(this, GroupAddParticipantActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }

        leaveGroupInfo.setOnClickListener {
            var title = ""
            var description = ""
            var positiveButton = ""
            if (role == "creator") {
                title = getString(R.string.delete_group)
                description =  getString(R.string.sure_delete_group)
                positiveButton = getString(R.string.delete)

            } else {
                title = getString(R.string.leave_group)
                description =  getString(R.string.sure_leave_group)
                positiveButton = getString(R.string.leave)
            }
            val builder = AlertDialog.Builder(this@GroupInfoActivity)
            builder.setTitle(title).setMessage(description)
            builder.setPositiveButton(
                positiveButton,
                DialogInterface.OnClickListener { _, _ ->
                    if (role == "creator") {
                        deleteGroup()
                    } else {
                        leaveGroup()
                    }
                })
            builder.setNegativeButton(
                getString(R.string.cancel),
                DialogInterface.OnClickListener { dialogInterface, _ ->
                    dialogInterface.dismiss()
                })
            builder.create().show()
        }

        editGroupInfo.setOnClickListener {
            val intent = Intent(this, GroupEditActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }
    }

    private fun deleteGroup() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referenceGr = db.getReference("Groups")
        val query = referenceGr.child(groupId).child("Messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds: DataSnapshot in snapshot.children) {
                        val type = ds.child("type").value.toString()
                        val message = ds.child("message").value.toString()

                        if(type == "image"){
                            val picRef = FirebaseStorage.getInstance().getReferenceFromUrl(message)
                            picRef.delete()
                        }
                    }
                    referenceGr.orderByChild("gid").equalTo(groupId)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (ds: DataSnapshot in snapshot.children) {
                                    val image = ds.child("image").value.toString()
                                    if(image != ""){
                                        val picRef = FirebaseStorage.getInstance().getReferenceFromUrl(image)
                                        picRef.delete()
                                    }
                                }
                                referenceGr.child(groupId).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this@GroupInfoActivity,
                                            getString(R.string.group_deleted),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startActivity(Intent(this@GroupInfoActivity, DashboardActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this@GroupInfoActivity,
                                            it.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun leaveGroup() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referenceGr = db.getReference("Groups")
        referenceGr.child(groupId).child("Participants").child(user!!.uid).removeValue()
            .addOnSuccessListener{
                Toast.makeText(
                    this@GroupInfoActivity,
                    getString(R.string.left_group),
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@GroupInfoActivity, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this@GroupInfoActivity,
                    it.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadMyRole() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referenceGr = db.getReference("Groups")
        val query = referenceGr.child(groupId).child("Participants").orderByChild("uid").equalTo(user!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds: DataSnapshot in snapshot.children) {
                        role = ds.child("role").value.toString()

                        when (role) {
                            "creator" -> {
                                editGroupInfo.visibility = View.VISIBLE
                                addParticipantInfo.visibility = View.VISIBLE
                                leaveGroupInfo.text = getString(R.string.delete_group)
                            }
                            "admin" -> {
                                editGroupInfo.visibility = View.GONE
                                addParticipantInfo.visibility = View.VISIBLE
                                leaveGroupInfo.text = getString(R.string.leave_group)
                            }
                            else -> {
                                editGroupInfo.visibility = View.GONE
                                addParticipantInfo.visibility = View.GONE
                                leaveGroupInfo.text = getString(R.string.leave_group)
                            }
                        }

                        loadParticipants()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun loadParticipants() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val listUsers: MutableList<UserModel> = ArrayList()
        val reference = db.getReference("Groups").child(groupId).child("Participants")
        .addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listUsers.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    val idUser = ds.child("uid").value.toString()
                    val referenceUs = db.getReference("Users")
                    referenceUs.orderByChild("uid").equalTo(idUser).addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (ds: DataSnapshot in snapshot.children) {
                                val us = ds.getValue(UserModel::class.java)
                                listUsers.add(us!!)
                            }
                            listUsers.sortBy { it.name }
                            addParticipantAdapter = AddParticipantAdapter(this@GroupInfoActivity, listUsers, groupId, role)
                            participantsRvGroupInfo.adapter = addParticipantAdapter
                            participantsGroupInfo.text = getString(R.string.participants) + " (" + listUsers.size + ")"

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
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referenceGr = db.getReference("Groups")
        val query = referenceGr.orderByChild("gid").equalTo(groupId)


        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val title = ds.child("title").value.toString()
                    val description = ds.child("description").value.toString()
                    val image = ds.child("image").value.toString()
                    val time = ds.child("time").value.toString()
                    val creator = ds.child("creator").value.toString()

                    actionBar.title = title
                    descriptionGroupInfo.text = description

                    val cal = Calendar.getInstance(Locale.ITALY)
                    cal.timeInMillis = time!!.toLong()
                    val timeC = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

                    loadCreatorInfo(timeC, creator)

                    try {
                        Picasso.get().load(image).into(imageGroupInfo)
                    } catch (e: Exception) {
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun loadCreatorInfo(time: String?, creator: String) {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referenceUs = db.getReference("Users")
        val query = referenceUs.orderByChild("uid").equalTo(creator)
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val name = ds.child("name").value.toString()
                    createdByInfo.text = getString(R.string.created_by) + " " + name + " " + getString(R.string.on) + " " + time


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