package com.example.scoutconnections.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.R
import com.example.scoutconnections.models.GroupChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class GroupChatAdapter(
    var context: Context,
    var listGroupChats: List<GroupChatModel>,
    groupId: String
) :
    RecyclerView.Adapter<GroupChatAdapter.MyHolder>() {

    val groupId = groupId
    lateinit var mAuth: FirebaseAuth
    val MSG_TYPE_LEFT = 0
    val MSG_TYPE_RIGHT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return if (viewType == MSG_TYPE_RIGHT) {
            val view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_right, parent, false)
            MyHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_left, parent, false)
            MyHolder(view)
        }
    }

    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        val model = listGroupChats[position]
        val messageChat = listGroupChats[position].message
        val timeChat = listGroupChats[position].time
        val typeChat = listGroupChats[position].type
        val senderChat = listGroupChats[position].sender

        val cal = Calendar.getInstance(Locale.ITALY)

        cal.timeInMillis = timeChat!!.toLong()
        val time = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

        if(typeChat.equals("text")){
            if (messageChat == ""){
                holder.mMessage.text = context.getString(R.string.deleted_message)
            } else {
                holder.mMessage.text = messageChat
            }
            holder.mMessage.visibility = View.VISIBLE
            holder.mMessageImage.visibility = View.GONE
            holder.mTimeMsg.visibility = View.VISIBLE
            holder.mTimeImage.visibility = View.GONE

            holder.mTimeMsg.text = time
        } else {
            holder.mMessage.visibility = View.GONE
            holder.mMessageImage.visibility = View.VISIBLE
            holder.mTimeMsg.visibility = View.GONE
            holder.mTimeImage.visibility = View.VISIBLE
            Picasso.get().load(messageChat).into(holder.mMessageImage)

            holder.mTimeImage.text = time
        }


        setUserName(model, holder)

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


    }

    private fun setUserName(model: GroupChatModel, holder: GroupChatAdapter.MyHolder) {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(
                    "Users"
                )
        db.orderByChild("uid").equalTo(model.sender).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val name = ds.child("name").value.toString()
                    holder.mName.text = name
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun deleteMessage(position: Int) {
        val mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        val messageTime = listGroupChats[position].time
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(
                    "Groups"
                )
        val query = db.child(groupId).child("Messages").orderByChild("time").equalTo(messageTime)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    if (ds.child("sender").value?.equals(user?.uid) == true) {
                        var hashMap = HashMap<String, Any>()

                        if(ds.child("type").value?.equals("text") == true){
                            hashMap["message"] = ""
                        } else {
                            println(listGroupChats[position].message)
                            val picRef = FirebaseStorage.getInstance().getReferenceFromUrl(listGroupChats[position].message!!)
                            picRef.delete()
                            hashMap["message"] = ""
                            hashMap["type"] = "text"
                        }


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
        return listGroupChats.size
    }

    override fun getItemViewType(position: Int): Int {
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        return if (listGroupChats[position].sender.equals(user?.uid)) {
            MSG_TYPE_RIGHT
        } else {
            MSG_TYPE_LEFT

        }
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mName: TextView
        var mMessage: TextView
        var mMessageImage: ImageView
        var mTimeMsg: TextView
        var mTimeImage: TextView
        var lMessage: RelativeLayout


        init {
            mName = itemView.findViewById(R.id.name_row)
            mMessage = itemView.findViewById(R.id.msg_chat)
            mMessageImage = itemView.findViewById(R.id.image_upload_chat)
            mTimeMsg = itemView.findViewById(R.id.time_chat_msg)
            mTimeImage = itemView.findViewById(R.id.time_chat_image)
            lMessage = itemView.findViewById(R.id.message_group_layout)
        }
    }

}