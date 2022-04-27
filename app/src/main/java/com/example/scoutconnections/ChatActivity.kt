package com.example.scoutconnections

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.scoutconnections.adapters.ChatAdapter
import com.example.scoutconnections.models.ChatModel
import com.example.scoutconnections.models.UsuarioModel
import com.example.scoutconnections.notifications.*
import com.google.android.gms.common.api.Api
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.json.JSONObject

import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ChatActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var usuarioId : String
    lateinit var leidoListener : ValueEventListener
    val mAuth =  FirebaseAuth.getInstance()
    val usuario = mAuth.currentUser
    val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    val referenciaUs = baseDatos.getReference("Usuarios")
    val referenciaCh = baseDatos.getReference("Chats")
    var notificar = false
    lateinit var requestQueue : RequestQueue
    lateinit var msg : String
    lateinit var nombreUsuario: String
    lateinit var estadoUsuario: String

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        var toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle("")


        recyclerView = findViewById<RecyclerView>(R.id.chat_recyclerView)
        val imagenChat = findViewById<ImageView>(R.id.imagenChat)
        val estadoChat = findViewById<TextView>(R.id.estadoChat)
        val nombreChat = findViewById<TextView>(R.id.nombreChat)
        val leidoChat = findViewById<TextView>(R.id.leidoChat)
        val mensajeChat = findViewById<EditText>(R.id.mensajeChat)
        val msjChat = findViewById<TextView>(R.id.msjChat)
        val enviarBtn = findViewById<ImageButton>(R.id.enviarBtn)

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true

        val actionBar = getSupportActionBar()
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager

        requestQueue = Volley.newRequestQueue(this)

        usuarioId = intent.getStringExtra("uidUsuario").toString()

        val consulta = referenciaUs.orderByChild("uid").equalTo(usuarioId)


        consulta.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(ds: DataSnapshot in snapshot.children){
                    val nombre = ds.child("nombre").value.toString()
                    nombreUsuario = nombre
                    val imagen = ds.child("imagen").value.toString()
                    val estado = ds.child("estado").value.toString()
                    estadoUsuario = estado
                    val escribiendo = ds.child("escribiendo").value.toString()


                    //Comprueba el estado del usuario(escribiendo,en linea o desconectado)
                    if(escribiendo.equals(usuario!!.uid)){
                        estadoChat.text = "escribiendo..."
                    }else{

                        if(estado.equals("en línea")){
                            estadoChat.text = estado
                        }else{
                            val cal = Calendar.getInstance(Locale.ITALY)

                            cal.timeInMillis = estado!!.toLong()
                            val tiempo = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)
                            estadoChat.text = "Última conexión: " + tiempo
                        }
                    }

                    nombreChat.text = nombre
                    try {
                        if (!imagen.equals("")){
                            Picasso.get().load(imagen).into(imagenChat)
                        }
                    }catch (e: Exception){
                        Picasso.get().load(R.drawable.ic_foto_perfil).into(imagenChat)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }

        })

        enviarBtn.setOnClickListener {
            notificar = true
            var mensaje = mensajeChat.text.toString().trim()
            if(mensaje.isEmpty()){
                Toast.makeText(this, "No se puede enviar un mensaje vacío", Toast.LENGTH_SHORT).show()

            }else{
                enviarMensaje(mensaje)
            }
            mensajeChat.setText("")
        }

        //Comprobar si se edita el texto
        mensajeChat.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p0.toString().trim().length == 0){
                    comprobarEscribiendoUsuario("nadie")
                }else{
                    comprobarEscribiendoUsuario(usuarioId)
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        leerMensajes()
        leidoMensaje()
    }

    private fun leidoMensaje() {
        leidoListener = referenciaCh.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chat = it.getValue(ChatModel::class.java)
                    if(chat?.receptor.equals(usuario?.uid) && chat?.emisor.equals(usuarioId)){
                        var hashMap = HashMap<String, Any>()
                        hashMap.put("leido",true)
                        it.ref.updateChildren(hashMap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun leerMensajes() {
        var listaChats = ArrayList<ChatModel>()
        referenciaCh.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listaChats.clear()
                snapshot.children.forEach {
                    val chat = it.getValue(ChatModel::class.java)
                    if((chat?.receptor.equals(usuario?.uid) && chat?.emisor.equals(usuarioId))|| (chat?.receptor.equals(usuarioId) && chat?.emisor.equals(usuario?.uid))){
                        listaChats.add(chat!!)
                    }
                    val adapterChat = ChatAdapter(this@ChatActivity, listaChats)
                    adapterChat.notifyDataSetChanged()

                    recyclerView.adapter = adapterChat
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun enviarMensaje(mensaje: String) {

        var hashMap = HashMap<String, Any>()

        val tiempo = System.currentTimeMillis().toString()

        if (usuario != null) {
            hashMap.put("emisor",usuario.uid)
        }
        hashMap.put("receptor",usuarioId)
        hashMap.put("mensaje",mensaje)
        hashMap.put("tiempo",tiempo)
        hashMap.put("leido",false)


        referenciaCh.push().setValue(hashMap)

        val mensajeChat = findViewById<EditText>(R.id.mensajeChat)
        mensajeChat.setText("")



        if(notificar && !estadoUsuario.equals("en línea")){

            enviarNotificacion(usuarioId, nombreUsuario, mensaje)
        }
        notificar = false

    }

    private fun enviarNotificacion(usuarioId: String, nombre: String?, mensaje: String) {
        val tokens = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Tokens")
        val query = tokens.orderByKey().equalTo(usuarioId)
        query.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(ds: DataSnapshot in snapshot.children){
                    val token = ds.getValue(Token::class.java)
                    val datos = Datos(usuario!!.uid, mensaje, nombre, usuarioId, R.drawable.ic_chat_24)
                    val emis = Emisor(datos, token!!.token)

                    try {
                        val emisorJsonObj = JSONObject(Gson().toJson(emis))

                        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
                            Method.POST,
                            "https://fcm.googleapis.com/fcm/send",
                            emisorJsonObj,
                            Response.Listener { response -> Log.d("JSON_RESPONSE", "onResponse:$response") },
                            Response.ErrorListener { error -> Log.d("JSON_RESPONSE", "onResponse:$error") }) {
                            @Throws(AuthFailureError::class)
                            override fun getHeaders(): Map<String, String> {
                                var headers = HashMap<String, String>()
                                //headers.put("Content-Type","application/json")
                                headers.put("Authorization", "key=AAAARBtw77g:APA91bEwJEkAUKhRbhb8yQKo6F9E__FdoeWb01Zptq4RBhkMoHZrifNxEerskLpQjuZOKZtjTiUbX5VOFOJHNwiMme93QFVlKj4bttK1rUng6mSvBW4QouSqvux_9uMyN9DcUuNh24Gx")
                                return headers
                            }
                            override fun getBodyContentType(): String? {
                                return "application/json"
                            }
                        }


                        requestQueue.add(jsonObjectRequest)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario(){

        if(usuario == null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }else{
            //correoTxt.setText(usuario.email)
        }
    }

    //Función que comprueba el estado de conexión del usuario
    private fun comprobarEnlineaUsuario(estado: String){
        val dbRefer = referenciaUs.child(usuario!!.uid)
        var hashMap = HashMap<String, Any>()
        hashMap.put("estado",estado)
        dbRefer.updateChildren(hashMap)
    }

    //Función que comprueba el estado de escritura del usuario
    private fun comprobarEscribiendoUsuario(escribiendo: String){
        val dbRefer = referenciaUs.child(usuario!!.uid)
        var hashMap = HashMap<String, Any>()
        hashMap.put("escribiendo",escribiendo)
        dbRefer.updateChildren(hashMap)
    }

    //Función que indica que hacer al iniciar la app
    override fun onStart() {
        comprobarEstadoUsuario()
        comprobarEnlineaUsuario("en línea")
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        val tiempo = System.currentTimeMillis().toString()
        comprobarEnlineaUsuario(tiempo)
        comprobarEscribiendoUsuario("nadie")
        referenciaCh.removeEventListener(leidoListener)
    }

    override fun onResume() {
        comprobarEnlineaUsuario("en línea")
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}