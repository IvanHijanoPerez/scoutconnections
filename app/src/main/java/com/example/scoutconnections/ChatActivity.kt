package com.example.scoutconnections

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.scoutconnections.adapters.ChatAdapter
import com.example.scoutconnections.models.ChatModel
import com.example.scoutconnections.notifications.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.ByteArrayOutputStream

import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ChatActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var userId: String
    lateinit var seenListener: ValueEventListener
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    private val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    private val referenceUs = db.getReference("Users")
    private val referenceCh = db.getReference("Chats")
    private var notify = false
    lateinit var requestQueue: RequestQueue
    lateinit var nameUser: String
    lateinit var statusUser: String

    private val CODE_CAMERA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400
    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()
    private var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        userId = intent.getStringExtra("uidUser").toString()

        var toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = ""
        toolbar.setOnClickListener {
            val intent = Intent(this, ThereProfileActivity::class.java)
            intent.putExtra("uid", userId)
            startActivity(intent)
        }

        recyclerView = findViewById<RecyclerView>(R.id.chat_recycler_view)
        val imageChat = findViewById<ImageView>(R.id.image_chat)
        val statusChat = findViewById<TextView>(R.id.status_chat)
        val nameChat = findViewById<TextView>(R.id.name_chat)
        val seenChat = findViewById<TextView>(R.id.seen_chat)
        val messageChat = findViewById<EditText>(R.id.message_chat)
        val msgChat = findViewById<TextView>(R.id.msg_chat)
        val sendBtn = findViewById<ImageButton>(R.id.send_btn)
        val attachBtn = findViewById<ImageButton>(R.id.attach_btn)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true

        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager

        requestQueue = Volley.newRequestQueue(this)



        val query = referenceUs.orderByChild("uid").equalTo(userId)


        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val name = ds.child("name").value.toString()
                    nameUser = name
                    val image = ds.child("image").value.toString()
                    val status = ds.child("status").value.toString()
                    statusUser = status
                    val typing = ds.child("typingTo").value.toString()


                    if (typing == user!!.uid) {
                        statusChat.text = getString(R.string.typing)
                    } else {

                        if (status == getString(R.string.online)) {
                            statusChat.text = status
                        } else {
                            val cal = Calendar.getInstance(Locale.ITALY)

                            cal.timeInMillis = status!!.toLong()
                            val time = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)
                            statusChat.text = getString(R.string.last_connection) + ": " + time
                        }
                    }

                    nameChat.text = name
                    try {
                        if (image != "") {
                            Picasso.get().load(image).into(imageChat)
                        }
                    } catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_profile_24).into(imageChat)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        sendBtn.setOnClickListener {
            notify = true
            val message = messageChat.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_empty_message), Toast.LENGTH_SHORT)
                    .show()

            } else {
                sendMessage(message)
            }
            messageChat.setText("")
        }

        messageChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().trim().isEmpty()) {
                    checkTypingUser("noOne")
                } else {
                    checkTypingUser(userId)
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        attachBtn.setOnClickListener {
            showPickImageDialog()
        }

        readMessages()
        seenMessage()
    }

    private fun showPickImageDialog() {
        val photoOptions = arrayOf(getString(R.string.camera), getString(R.string.gallery))
        val constructorPhoto = AlertDialog.Builder(this)
        constructorPhoto.setItems(photoOptions) { _, pos ->
            when (pos) {
                0 -> {
                    if (!checkCameraPermission()) {
                        requestCameraPermission()

                    } else {
                        selectFromCamera()
                    }
                }
                1 -> {
                    if (!checkStoragePermission()) {
                        requestStoragePermission()

                    } else {
                        selectFromGallery()
                    }
                }
            }
        }
        constructorPhoto.create().show()
    }

    private fun selectFromCamera() {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, getString(R.string.temporal_image))
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.temporal_description))
        image_uri = contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        val camaraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        camaraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(camaraIntent, CODE_SELECT_IMAGE_CAMERA)
    }

    private fun selectFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, CODE_SELECT_IMAGE_GALLERY)
    }

    private fun checkStoragePermission(): Boolean {
        return this?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, CODE_STORAGE)
    }

    private fun checkCameraPermission(): Boolean {
        val result1 = this.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.CAMERA
            )
        } == PackageManager.PERMISSION_GRANTED
        val result2 = this.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
        return result1 && result2
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CODE_CAMERA)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CODE_CAMERA ->
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted =
                        grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && writeStorageAccepted) {
                        selectFromCamera()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.enable_camera_storage_permissions),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            CODE_STORAGE ->
                if (grantResults.isNotEmpty()) {
                    val writeStorageAccepted =
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (writeStorageAccepted) {
                        selectFromGallery()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.enable_storage_permissions),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_SELECT_IMAGE_GALLERY) {
                if (data != null) {
                    image_uri = data.data!!
                }
                sendImageMessage(image_uri)
            } else if (requestCode == CODE_SELECT_IMAGE_CAMERA) {
                sendImageMessage(image_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun sendImageMessage(imageUri: Uri?) {
        notify = true
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.sending_image))
        progressDialog.show()

        val time = System.currentTimeMillis().toString()
        val filenamePath = "ChatImages/post_$time"

        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val ref = FirebaseStorage.getInstance().reference.child(filenamePath)
        ref.putBytes(data).addOnSuccessListener {
            progressDialog.dismiss()
            val uriTask = it.storage.downloadUrl
            while(!uriTask.isSuccessful){
            }
            val downloadUri = uriTask.result.toString()
            if(uriTask.isSuccessful){
                val db =
                    FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").reference
                val results = HashMap<String, Any>()
                results["sender"] = user!!.uid
                results["receiver"] = userId
                results["message"] = downloadUri
                results["time"] = time
                results["type"] = "image"
                results["seen"] = false

                db.child("Chats").push().setValue(results)

                if (notify && statusUser != getString(R.string.online)) {

                    sendNotification(userId, nameUser, getString(R.string.image))
                }
                notify = false
            }

        }.addOnFailureListener{

        }


    }


    private fun seenMessage() {
        seenListener = referenceCh.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chat = it.getValue(ChatModel::class.java)
                    if (chat?.receiver.equals(user?.uid) && chat?.sender.equals(userId)) {
                        var hashMap = HashMap<String, Any>()
                        hashMap["seen"] = true
                        it.ref.updateChildren(hashMap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun readMessages() {
        var listChats = ArrayList<ChatModel>()
        referenceCh.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listChats.clear()
                snapshot.children.forEach {
                    val chat = it.getValue(ChatModel::class.java)
                    if ((chat?.receiver.equals(user?.uid) && chat?.sender.equals(userId)) || (chat?.receiver.equals(
                            userId
                        ) && chat?.sender.equals(user?.uid))
                    ) {
                        listChats.add(chat!!)
                    }
                    val adapterChat = ChatAdapter(this@ChatActivity, listChats)
                    adapterChat.notifyDataSetChanged()

                    recyclerView.adapter = adapterChat
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun sendMessage(message: String) {

        var hashMap = HashMap<String, Any>()

        val time = System.currentTimeMillis().toString()

        if (user != null) {
            hashMap["sender"] = user.uid
        }
        hashMap["receiver"] = userId
        hashMap["message"] = message
        hashMap["time"] = time
        hashMap["seen"] = false
        hashMap["type"] = "text"

        referenceCh.push().setValue(hashMap)

        val messageChat = findViewById<EditText>(R.id.message_chat)
        messageChat.setText("")


        if (notify && statusUser != getString(R.string.online)) {

            sendNotification(userId, nameUser, message)
        }
        notify = false

        val refChatList1 = db.getReference("ChatList").child(user!!.uid).child(userId)
        refChatList1.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()){
                    refChatList1.child("uid").setValue(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        val refChatList2 = db.getReference("ChatList").child(userId).child(user!!.uid)
        refChatList2.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()){
                    refChatList2.child("uid").setValue(user!!.uid)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun sendNotification(userId: String, name: String?, message: String) {
        val tokens = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("Tokens")
        val query = tokens.orderByKey().equalTo(userId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val token = ds.getValue(Token::class.java)
                    val data = Data(user!!.uid, message, name, userId, R.drawable.ic_chat_24)
                    val sender = Sender(data, token!!.token)

                    try {
                        val senderJsonObj = JSONObject(Gson().toJson(sender))

                        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
                            Method.POST,
                            "https://fcm.googleapis.com/fcm/send",
                            senderJsonObj,
                            Response.Listener { response ->
                                Log.d(
                                    "JSON_RESPONSE",
                                    "onResponse:$response"
                                )
                            },
                            Response.ErrorListener { error ->
                                Log.d(
                                    "JSON_RESPONSE",
                                    "onResponse:$error"
                                )
                            }) {
                            @Throws(AuthFailureError::class)
                            override fun getHeaders(): Map<String, String> {
                                val headers = HashMap<String, String>()
                                //headers.put("Content-Type","application/json")
                                headers["Authorization"] =
                                    "key=AAAARBtw77g:APA91bEwJEkAUKhRbhb8yQKo6F9E__FdoeWb01Zptq4RBhkMoHZrifNxEerskLpQjuZOKZtjTiUbX5VOFOJHNwiMme93QFVlKj4bttK1rUng6mSvBW4QouSqvux_9uMyN9DcUuNh24Gx"
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

    private fun checkUserStatus() {

        if (user == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
        }
    }

    private fun checkOnlineUser(status: String) {
        val dbRefer = referenceUs.child(user!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["status"] = status
        dbRefer.updateChildren(hashMap)
    }

    private fun checkTypingUser(typing: String) {
        val dbRefer = referenceUs.child(user!!.uid)
        var hashMap = HashMap<String, Any>()
        hashMap["typingTo"] = typing
        dbRefer.updateChildren(hashMap)
    }

    override fun onStart() {
        checkUserStatus()
        checkOnlineUser(getString(R.string.online))
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        val time = System.currentTimeMillis().toString()
        checkOnlineUser(time)
        checkTypingUser("noOne")
        referenceCh.removeEventListener(seenListener)
    }

    override fun onResume() {
        checkOnlineUser(getString(R.string.online))
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}