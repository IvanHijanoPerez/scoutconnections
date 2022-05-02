package com.example.scoutconnections.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.ChatActivity
import com.example.scoutconnections.R
import com.example.scoutconnections.ThereProfileActivity
import com.example.scoutconnections.models.CommentModel
import com.example.scoutconnections.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter (
    var context: Context,
    var listComments: List<CommentModel>,
    var postId: String
) :
    RecyclerView.Adapter<CommentAdapter.MyHolder>() {

    val db =
        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    private val referenceUs = db.getReference("Users")
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_comments, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val cidComment = listComments[position].cid
        val commentComment = listComments[position].comment
        val creatorComment = listComments[position].creator
        val timeComment = listComments[position].time

        holder.cComment.text = commentComment

        val cal = Calendar.getInstance(Locale.ITALY)

        cal.timeInMillis = timeComment!!.toLong()
        val timeC = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

        holder.cTime.text = timeC

        val query = referenceUs.orderByChild("uid").equalTo(creatorComment)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val uName = ds.child("name").value.toString()
                    val uImage = ds.child("image").value.toString()

                    holder.cName.text = uName

                    try {
                        if (uImage != "") {
                            Picasso.get().load(uImage).into(holder.cAvatar)

                        }
                    } catch (e: Exception) {
                    }


                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        holder.itemView.setOnClickListener {
            if (user!!.uid == creatorComment){
                val customDialog = AlertDialog.Builder(it.rootView.context)
                customDialog.setTitle(context.getString(R.string.delete_comment))
                customDialog.setMessage(context.getString(R.string.sure_delete_comment))

                customDialog.setPositiveButton(
                    context.getString(R.string.delete),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            deleteComment(cidComment!!)
                        }

                    })

                customDialog.setNegativeButton(
                    context.getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            p0?.dismiss()
                        }
                    })

                customDialog.create().show()
            } else {
                Toast.makeText(context, context.getString(R.string.only_delete_your_comments), Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun deleteComment(cidComment: String) {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Posts").child(postId)
        db.child("Comments").child(cidComment).removeValue()
        db.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val nComments = snapshot.child("nComments").value.toString().toInt()
                db.child("nComments").setValue(nComments - 1)
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }


    override fun getItemCount(): Int {
        return listComments.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cAvatar: ImageView
        var cName: TextView
        var cTime: TextView
        var cComment: TextView

        init {
            cAvatar = itemView.findViewById(R.id.image_comment)
            cName = itemView.findViewById(R.id.name_user_comment)
            cTime = itemView.findViewById(R.id.time_comment)
            cComment = itemView.findViewById(R.id.comment_comment)
        }
    }
}