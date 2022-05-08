package com.example.scoutconnections.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.ChatActivity
import com.example.scoutconnections.R
import com.example.scoutconnections.models.GroupModel
import com.example.scoutconnections.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ChatListAdapter(var context: Context, var listUsers: List<UserModel>) :
    RecyclerView.Adapter<ChatListAdapter.MyHolder>() {


    var lastMessageMap: HashMap<String, String> = HashMap<String, String>()
    lateinit var mAuth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val uUid = listUsers[position].uid
        val uImage = listUsers[position].image
        val uName = listUsers[position].name
        val lMessage = lastMessageMap[uUid]
        val lMessageTime = lastMessageMap[uUid + "_time"]

        holder.cName.text = uName
        if (lMessage == null || lMessage == "default") {
            holder.cLastMessage.visibility = View.GONE
        } else {
            holder.cLastMessage.visibility = View.VISIBLE
            holder.cLastMessage.text = lMessage

            val cal = Calendar.getInstance(Locale.ITALY)

            cal.timeInMillis = lMessageTime!!.toLong()
            val timeC = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

            holder.cTime.text = timeC
        }

        try {
            if (!uImage.equals("")) {
                Picasso.get().load(uImage).into(holder.cImageUser)

            }
        } catch (e: Exception) {
        }


        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("uidUser", uUid)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            context.startActivity(intent)
        }
    }

    fun setLastMessageMap(userId: String, lastMessage: String, lastMessageTime: String) {
        lastMessageMap[userId] = lastMessage
        lastMessageMap[userId + "_time"] = lastMessageTime
    }

    override fun getItemCount(): Int {
        return listUsers.size
    }


    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var cName: TextView
        var cLastMessage: TextView
        var cImageUser: ImageView
        var cTime: TextView


        init {
            cName = itemView.findViewById(R.id.name_chatlist)
            cLastMessage = itemView.findViewById(R.id.last_message_chatlist)
            cImageUser = itemView.findViewById(R.id.image_chatlist)
            cTime = itemView.findViewById(R.id.time_chatlist)
        }
    }

}