package com.example.scoutconnections.adapters

import android.content.Context
import android.content.Intent
import com.example.scoutconnections.models.UserModel
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import android.widget.TextView
import com.example.scoutconnections.ChatActivity
import com.example.scoutconnections.R
import java.lang.Exception

class UserAdapter(var context: Context, var listUsers: List<UserModel>) :
    RecyclerView.Adapter<UserAdapter.MyHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
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
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("uidUser", uidUser)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return listUsers.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mAvatar: ImageView
        var mName: TextView
        var mEmail: TextView

        init {
            mAvatar = itemView.findViewById(R.id.avatar_row)
            mName = itemView.findViewById(R.id.name_row)
            mEmail = itemView.findViewById(R.id.email_row)
        }
    }
}