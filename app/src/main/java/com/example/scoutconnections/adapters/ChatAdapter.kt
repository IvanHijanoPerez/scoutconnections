package com.example.scoutconnections.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(var context: Context, var listaChats: List<ChatModel>) : RecyclerView.Adapter<ChatAdapter.MiHolder>(){

    lateinit var mAuth : FirebaseAuth
    val MSG_TYPE_LEFT = 0
    val MSG_TYPE_RIGHT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiHolder {
        if(viewType == MSG_TYPE_RIGHT){
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_derecha, parent, false)
            return MiHolder(view)
        }else{
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_izquierda, parent, false)
            return MiHolder(view)
        }
    }

    override fun onBindViewHolder(holder: MiHolder, position: Int) {
        val mensajeChat = listaChats[position].mensaje
        val tiempoChat = listaChats[position].tiempo

        val cal = Calendar.getInstance(Locale.ITALY)

        cal.timeInMillis = tiempoChat!!.toLong()
        val tiempo = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

        holder.mMensaje.text = mensajeChat
        holder.mTiempo.text = tiempo

        //Mostrar dialogo borrar
        holder.lMensaje.setOnClickListener{
            val customDialog = AlertDialog.Builder(context)
            customDialog.setTitle("Borrar mensaje")
            customDialog.setMessage("¿Estás seguro de que quieres eliminar este mensaje?")

            customDialog.setPositiveButton("Borrar", object: DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    eliminarMensaje(position)
                }

            })

            customDialog.setNegativeButton("Cancelar", object: DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    p0?.dismiss()
                }
            })

            customDialog.create().show()
        }

        //Mostrar estado del mensaje
        if(position == listaChats.size-1){
            if(listaChats[position].leido == true){
                holder.mLeido.text = "Leído"
            }else{
                holder.mLeido.text = "Enviado"
            }
        }else{
            holder.mLeido.visibility = View.GONE
        }

    }

    private fun eliminarMensaje(position: Int) {
        val mAuth =  FirebaseAuth.getInstance()
        val usuario = mAuth.currentUser
        val mensajeTiempo = listaChats[position].tiempo
        val bd = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Chats")
        val consulta = bd.orderByChild("tiempo").equalTo(mensajeTiempo)
        consulta.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(ds: DataSnapshot in snapshot.children){
                    if(ds.child("emisor").value?.equals(usuario?.uid) == true){
                        var hashMap = HashMap<String, Any>()
                        hashMap.put("mensaje","Este mensaje ha sido eliminado")
                        ds.ref.updateChildren(hashMap)
                    }else{
                        Toast.makeText(context, "Solo puedes eliminar tus mensajes", Toast.LENGTH_SHORT).show()
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun getItemCount(): Int {
        return listaChats.size
    }

    override fun getItemViewType(position: Int): Int {
        mAuth = FirebaseAuth.getInstance()
        val usuario = mAuth.currentUser


        if(listaChats.get(position).emisor.equals(usuario?.uid)){
            return MSG_TYPE_RIGHT
        }else{
            return MSG_TYPE_LEFT
        }
    }

    inner class MiHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var mMensaje: TextView
        var mTiempo: TextView
        var mLeido: TextView
        var lMensaje: LinearLayout


        init {
            mMensaje = itemView.findViewById(R.id.msjChat)
            mTiempo = itemView.findViewById(R.id.tiempoChat)
            mLeido = itemView.findViewById(R.id.leidoChat)
            lMensaje = itemView.findViewById(R.id.mensajeLayout)
        }
    }

}