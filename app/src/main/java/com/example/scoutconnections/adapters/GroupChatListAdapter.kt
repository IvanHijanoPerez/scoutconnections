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
import com.example.scoutconnections.GroupChatActivity
import com.example.scoutconnections.R
import com.example.scoutconnections.ThereProfileActivity
import com.example.scoutconnections.models.GroupModel
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class GroupChatListAdapter(var context: Context, var listGroups: List<GroupModel>) :
    RecyclerView.Adapter<GroupChatListAdapter.MyHolder>() {


    var lastMessageMap: HashMap<String, String> = HashMap<String, String>()
    lateinit var mAuth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.row_group_chatlist, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val gId = listGroups[position].gid
        val gImage = listGroups[position].image
        val gTitle = listGroups[position].title


//        val cal = Calendar.getInstance(Locale.ITALY)
         val cal = System.currentTimeMillis()
//        cal.timeInMillis = lMessageTime!!.toLong()
        val timeC = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal)
        holder.cTime.text = timeC

        holder.cTitle.text = gTitle
        try {
            if (!gImage.equals("")) {
                Picasso.get().load(gImage).into(holder.cImageGroup)

            }
        } catch (e: Exception) {
        }


        holder.itemView.setOnClickListener {
            val intent = Intent(context, GroupChatActivity::class.java)
            intent.putExtra("groupId", gId)
            context.startActivity(intent)
        }
    }

    fun setLastMessageMap(userId: String, lastMessage: String, lastMessageTime: String) {
        lastMessageMap[userId] = lastMessage
        lastMessageMap[userId + "_time"] = lastMessageTime
    }

    override fun getItemCount(): Int {
        return listGroups.size
    }


    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var cTitle: TextView
        var cLastMessage: TextView
        var cImageGroup: ImageView
        var cTime: TextView
        var cName: TextView


        init {
            cTitle = itemView.findViewById(R.id.title_group_chatlist)
            cLastMessage = itemView.findViewById(R.id.message_group_chatlist)
            cImageGroup = itemView.findViewById(R.id.image_group_chatlist)
            cTime = itemView.findViewById(R.id.time_group_chatlist)
            cName = itemView.findViewById(R.id.name_group_chatlist)
        }
    }

}