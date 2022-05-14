package com.example.scoutconnections.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.R
import com.example.scoutconnections.models.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.lang.Exception

class AddParticipantAdapter(
    var context: Context,
    var listUsers: List<UserModel>,
    var groupId: String,
    var groupRole: String
) :
    RecyclerView.Adapter<AddParticipantAdapter.MyHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_add_participants, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val modelUser = listUsers[position]
        val uidUser = listUsers[position].uid
        val imageUser = listUsers[position].image
        val nameUser = listUsers[position].name
        val emailUser = listUsers[position].email
        holder.mName.text = nameUser
        holder.mEmail.text = emailUser
        try {
            if (!imageUser.equals("")) {
                Picasso.get().load(imageUser).into(holder.mAvatar)

            }
        } catch (e: Exception) {
        }
        
        checkParticipation(modelUser,holder)
        
        holder.itemView.setOnClickListener {
            val ref = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Groups")
            ref.child(groupId).child("Participants").child(uidUser!!).addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val prevRole = snapshot.child("role").value.toString()

                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(context.getString(R.string.choose_option))
                        if(groupRole == "creator"){
                            if(prevRole == "admin"){
                                val options = arrayOf(context.getString(R.string.remove_admin), context.getString(
                                                                    R.string.remove_participant))
                                builder.setItems(options) { _, pos ->
                                    when (pos) {
                                        0 -> {
                                            removeAdmin(modelUser)
                                        }
                                        1 -> {
                                            removeParticipant(modelUser)
                                        }
                                    }
                                }
                                builder.create().show()
                            }
                            else if (prevRole == "participant") {
                                val options = arrayOf(context.getString(R.string.make_admin), context.getString(
                                    R.string.remove_participant))
                                builder.setItems(options) { _, pos ->
                                    when (pos) {
                                        0 -> {
                                            makeAdmin(modelUser)
                                        }
                                        1 -> {
                                            removeParticipant(modelUser)
                                        }
                                    }
                                }
                                builder.create().show()
                            }
                        }
                        else if (groupRole == "admin"){
                            if (prevRole == "creator"){
                                Toast.makeText(context, context.getString(R.string.creator_group), Toast.LENGTH_SHORT).show()
                            }
                            else if (prevRole == "admin") {
                                val options = arrayOf(context.getString(R.string.remove_admin), context.getString(
                                    R.string.remove_participant))
                                builder.setItems(options) { _, pos ->
                                    when (pos) {
                                        0 -> {
                                            removeAdmin(modelUser)
                                        }
                                        1 -> {
                                            removeParticipant(modelUser)
                                        }
                                    }
                                }
                                builder.create().show()
                            }
                            else if (prevRole == "participant") {
                                val options = arrayOf(context.getString(R.string.make_admin), context.getString(
                                    R.string.remove_participant))
                                builder.setItems(options) { _, pos ->
                                    when (pos) {
                                        0 -> {
                                            makeAdmin(modelUser)
                                        }
                                        1 -> {
                                            removeParticipant(modelUser)
                                        }
                                    }
                                }
                                builder.create().show()
                            }
                        }

                    } else {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(context.getString(R.string.add_participant)).setMessage(context.getString(
                                                    R.string.add_user_group))
                        builder.setPositiveButton(
                            context.getString(R.string.add),
                            DialogInterface.OnClickListener { _, _ ->
                                addParticipant(modelUser)
                            })

                        builder.setNegativeButton(
                            context.getString(R.string.cancel),
                            DialogInterface.OnClickListener { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            })

                        builder.create().show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

        }
    }

    private fun addParticipant(modelUser: UserModel) {
        val time = System.currentTimeMillis().toString()
        var hashMap = HashMap<String, Any>()
        hashMap["uid"] = modelUser.uid!!
        hashMap["role"] = "participant"
        hashMap["time"] = time

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Groups")

        reference.child(groupId).child("Participants").child(modelUser.uid!!).setValue(hashMap).addOnSuccessListener {
            Toast.makeText(context, context.getString(R.string.participant_added), Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
        }

    }

    private fun makeAdmin(modelUser: UserModel) {
        var hashMap = HashMap<String, Any>()
        hashMap["role"] = "admin"

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Groups")

        reference.child(groupId).child("Participants").child(modelUser.uid!!).updateChildren(hashMap).addOnSuccessListener {
            Toast.makeText(context, context.getString(R.string.participant_is_admin), Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeParticipant(modelUser: UserModel) {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Groups")

        reference.child(groupId).child("Participants").child(modelUser.uid!!).removeValue().addOnSuccessListener {
            Toast.makeText(context, context.getString(R.string.participant_removed), Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeAdmin(modelUser: UserModel) {
        var hashMap = HashMap<String, Any>()
        hashMap["role"] = "participant"

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Groups")

        reference.child(groupId).child("Participants").child(modelUser.uid!!).updateChildren(hashMap).addOnSuccessListener {
            Toast.makeText(context, context.getString(R.string.admin_is_participant), Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkParticipation(modelUser: UserModel, holder: AddParticipantAdapter.MyHolder) {
        val ref = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Groups")
        ref.child(groupId).child("Participants").child(modelUser.uid!!).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){

                    when (snapshot.child("role").value.toString()) {
                        "creator" -> {
                            holder.mStatus.text = context.getString(R.string.creator)

                        }
                        "admin" -> {
                            holder.mStatus.text = context.getString(R.string.administrator)

                        }
                        else -> {
                            holder.mStatus.text = context.getString(R.string.participant)

                        }
                    }
                } else {
                    holder.mStatus.text = ""

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    override fun getItemCount(): Int {
        return listUsers.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mAvatar: ImageView
        var mName: TextView
        var mEmail: TextView
        var mStatus: TextView

        init {
            mAvatar = itemView.findViewById(R.id.avatar_row)
            mName = itemView.findViewById(R.id.name_row)
            mEmail = itemView.findViewById(R.id.email_row)
            mStatus = itemView.findViewById(R.id.status_row)
        }
    }
}