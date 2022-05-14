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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.GroupChatAdapter
import com.example.scoutconnections.models.GroupChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.*

class GroupChatActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var groupId: String
    var role = ""
    lateinit var imageChat: ImageView
    lateinit var titleChat: TextView
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    private val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    private val referenceGr = db.getReference("Groups")

    private val CODE_CAMERA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400
    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()
    private var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        var toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = ""


        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true


        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        groupId = intent.getStringExtra("groupId").toString()

        toolbar.setOnClickListener {
            val intent = Intent(this, GroupInfoActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }

        recyclerView = findViewById<RecyclerView>(R.id.chat_recycler_view)
        imageChat = findViewById<ImageView>(R.id.image_chat)
        titleChat = findViewById<TextView>(R.id.title_chat)
        val messageChat = findViewById<EditText>(R.id.message_chat)
        val sendBtn = findViewById<ImageButton>(R.id.send_btn)
        val attachBtn = findViewById<ImageButton>(R.id.attach_btn)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager

        loadGroupInfo()
        loadGroupMessages()
        loadMyRole()
        
        sendBtn.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                val message = messageChat.text.toString().trim()
                if(message.isEmpty()){
                    Toast.makeText(
                        this@GroupChatActivity,
                        getString(R.string.no_empty_message),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    sendMessage(message)
                }
            }

        })

        attachBtn.setOnClickListener {
            showPickImageDialog()
        }
    }

    private fun loadMyRole() {
        val query = referenceGr.child(groupId).child("Participants").orderByChild("uid").equalTo(user!!.uid)
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    role = ds.child("role").value.toString()
                    invalidateOptionsMenu()
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
        hashMap["message"] = message
        hashMap["time"] = time
        hashMap["type"] = "text"

        referenceGr.child(groupId).child("Messages").child(time).setValue(hashMap)

        val messageChat = findViewById<EditText>(R.id.message_chat)
        messageChat.setText("")



    }

    private fun loadGroupInfo() {
        val query = referenceGr.orderByChild("gid").equalTo(groupId)


        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val title = ds.child("title").value.toString()
                    val description = ds.child("description").value.toString()
                    val image = ds.child("image").value.toString()
                    val time = ds.child("time").value.toString()
                    val creator = ds.child("creator").value.toString()

                    titleChat.text = title

                    try {
                        if (image != "") {
                            Picasso.get().load(image).into(imageChat)
                        }
                    } catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_people_24).into(imageChat)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun loadGroupMessages() {
        var listChats = ArrayList<GroupChatModel>()
        referenceGr.child(groupId).child("Messages").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listChats.clear()
                snapshot.children.forEach {
                    val chat = it.getValue(GroupChatModel::class.java)
                    listChats.add(chat!!)

                }
                val adapterGroupChat = GroupChatAdapter(this@GroupChatActivity, listChats, groupId)

                recyclerView.adapter = adapterGroupChat
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
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
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.sending_image))
        progressDialog.show()

        val time = System.currentTimeMillis().toString()
        val filenamePath = "GroupChats/$time"

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

                val results = HashMap<String, Any>()
                results["sender"] = user!!.uid
                results["message"] = downloadUri
                results["time"] = time
                results["type"] = "image"

                referenceGr.child(groupId).child("Messages").child(time).setValue(results)


            }

        }.addOnFailureListener{

        }


    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)


        menu?.findItem(R.id.action_users)?.isVisible = false
        menu?.findItem(R.id.action_search)?.isVisible = false
        menu?.findItem(R.id.action_add_post)?.isVisible = false
        menu?.findItem(R.id.action_logout)?.isVisible = false
        menu?.findItem(R.id.action_create_group)?.isVisible = false
        menu?.findItem(R.id.action_add_participant_group)?.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }


}