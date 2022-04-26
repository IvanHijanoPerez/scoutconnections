package com.example.scoutconnections.adapters

import android.content.Context
import android.content.Intent
import com.example.scoutconnections.models.UsuarioModel
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.UsuarioAdapter.MiHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import android.widget.Toast
import android.widget.TextView
import com.example.scoutconnections.ChatActivity
import com.example.scoutconnections.R
import java.lang.Exception

class UsuarioAdapter(var context: Context, var listaUsuarios: List<UsuarioModel>) :
    RecyclerView.Adapter<UsuarioAdapter.MiHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiHolder {
        val vista = LayoutInflater.from(context).inflate(R.layout.row_usuarios, parent, false)
        return MiHolder(vista)
    }

    override fun onBindViewHolder(holder: MiHolder, position: Int) {
        val uidUsuario = listaUsuarios[position].uid
        val imagenUsuario = listaUsuarios[position].imagen
        val nombreUsuario = listaUsuarios[position].nombre
        val correoUsuario = listaUsuarios[position].correo
        holder.mNombre.text = nombreUsuario
        holder.mCorreo.text = correoUsuario
        try {
            if (!imagenUsuario.equals("")){
                Picasso.get().load(imagenUsuario).into(holder.mAvatar)
            }
        } catch (e: Exception) {
        }
        holder.itemView.setOnClickListener {
            val intent = Intent(context,ChatActivity::class.java)
            intent.putExtra("uidUsuario",uidUsuario)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return listaUsuarios.size
    }

    inner class MiHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mAvatar: ImageView
        var mNombre: TextView
        var mCorreo: TextView

        init {
            mAvatar = itemView.findViewById(R.id.avatarRow)
            mNombre = itemView.findViewById(R.id.nombreRow)
            mCorreo = itemView.findViewById(R.id.correoRow)
        }
    }
}