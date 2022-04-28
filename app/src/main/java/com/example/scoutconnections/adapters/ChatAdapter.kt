package com.example.scoutconnections.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.R
import com.example.scoutconnections.models.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(var context: Context, var listChats: List<ChatModel>) :
    RecyclerView.Adapter<ChatAdapter.MyHolder>() {

    lateinit var mAuth: FirebaseAuth
    val MSG_TYPE_LEFT = 0
    val MSG_TYPE_RIGHT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return if (viewType == MSG_TYPE_RIGHT) {
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false)
            MyHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false)
            MyHolder(view)
        }
    }

    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        val messageChat = listChats[position].message
        val timeChat = listChats[position].time

        val cal = Calendar.getInstance(Locale.ITALY)

        cal.timeInMillis = timeChat!!.toLong()
        val time = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

        holder.mMessage.text = messageChat
        holder.mTime.text = time

        holder.lMessage.setOnClickListener {
            val customDialog = AlertDialog.Builder(context)
            customDialog.setTitle(context.getString(R.string.delete_message))
            customDialog.setMessage(context.getString(R.string.sure_delete_message))

            customDialog.setPositiveButton(
                context.getString(R.string.delete),
                object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        deleteMessage(position)
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
        }

        if (position == listChats.size - 1) {
            if (listChats[position].seen == true) {
                holder.mSeen.text = context.getString(R.string.seen)
            } else {
                holder.mSeen.text = context.getString(R.string.sent)
            }
        } else {
            holder.mSeen.visibility = View.GONE
        }

    }

    private fun deleteMessage(position: Int) {
        val mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        val messageTime = listChats[position].time
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(
                    "Chats"
                )
        val query = db.orderByChild("time").equalTo(messageTime)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    if (ds.child("sender").value?.equals(user?.uid) == true) {
                        var hashMap = HashMap<String, Any>()
                        hashMap["message"] =
                            context.getString(R.string.deleted_message)
                        ds.ref.updateChildren(hashMap)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.only_delete_your_messages),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    override fun getItemCount(): Int {
        return listChats.size
    }

    override fun getItemViewType(position: Int): Int {
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        return if (listChats[position].sender.equals(user?.uid)) {
            MSG_TYPE_RIGHT
        } else {
            MSG_TYPE_LEFT

        }
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var mMessage: TextView
        var mTime: TextView
        var mSeen: TextView
        var lMessage: LinearLayout


        init {
            mMessage = itemView.findViewById(R.id.msg_chat)
            mTime = itemView.findViewById(R.id.time_chat)
            mSeen = itemView.findViewById(R.id.seen_chat)
            lMessage = itemView.findViewById(R.id.messageLayout)
        }
    }

}