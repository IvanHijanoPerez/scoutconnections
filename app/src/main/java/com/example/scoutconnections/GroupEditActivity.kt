package com.example.scoutconnections

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class GroupEditActivity : AppCompatActivity() {
    lateinit var groupId: String
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    lateinit var actionBar: ActionBar

    private val CODE_CAMERA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400
    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()
    private var image_uri: Uri? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var editImagePath: String
    private lateinit var titleGroup: EditText
    private lateinit var descriptionGroup: EditText
    private lateinit var imageGroup: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_edit)

        groupId = intent.getStringExtra("groupId").toString()

        actionBar = supportActionBar!!
        actionBar.title = getString(R.string.edit_group)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        imageGroup = findViewById<ImageView>(R.id.image_group)
        titleGroup = findViewById<EditText>(R.id.title_group)
        descriptionGroup = findViewById<EditText>(R.id.description_group)
        val updateGroup = findViewById<FloatingActionButton>(R.id.update_group_btn)

        loadGroupInfo()

        imageGroup.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                showPickImageDialog()
            }

        })

        updateGroup.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                val title = titleGroup.text.toString().trim()
                val description = descriptionGroup.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(
                        this@GroupEditActivity,
                        getString(R.string.enter_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                if(editImagePath != "") {
                    if (image_uri == null) {
                        editGroup(title, description, "", true)
                    } else {
                        editGroup(title, description, image_uri.toString(), true)
                    }
                } else {
                    if (image_uri == null) {
                        editGroup(title, description, "", false)
                    } else {
                        editGroup(title, description, image_uri.toString(), false)
                    }
                }


            }

        })

        checkUserStatus()
    }

    private fun editGroup(title: String, description: String, uri: String, hadImage: Boolean) {
        progressDialog.setMessage(getString(R.string.editing_post))
        progressDialog.show()
        val time = System.currentTimeMillis().toString()
        val pathNameFile = "Groups/image_$time"

        if(hadImage){
            val picRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImagePath!!)
            picRef.delete().addOnSuccessListener {



                if (uri != "") {

                    val bd = FirebaseStorage.getInstance().reference.child(pathNameFile)
                    bd.putFile(Uri.parse(uri)).addOnSuccessListener {
                        val uriTask = it.storage.downloadUrl
                        while (!uriTask.isSuccessful) {
                        }
                        val uriDownload = uriTask.result
                        if (uriTask.isSuccessful) {
                            var results = HashMap<String, String>()
                            results["title"] = title
                            results["description"] = description
                            results["image"] = uriDownload.toString()

                            val change =
                                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Groups")

                            change.child(groupId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, getString(R.string.group_edited), Toast.LENGTH_SHORT).show()

                                startActivity(Intent(this, DashboardActivity::class.java))

                                val gIntent = Intent(this, GroupChatActivity::class.java)
                                gIntent.putExtra("groupId", groupId)
                                startActivity(gIntent)

                                val intent = Intent(this, GroupInfoActivity::class.java)
                                intent.putExtra("groupId", groupId)
                                startActivity(intent)
                            }.addOnFailureListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, getString(R.string.editing_post_error), Toast.LENGTH_SHORT)
                                    .show()
                            }

                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
                } else {

                    val results = HashMap<String, String>()
                    results["title"] = title
                    results["description"] = description
                    results["image"] = ""

                    val change =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Groups")

                    change.child(groupId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.group_edited), Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, DashboardActivity::class.java))

                        val gIntent = Intent(this, GroupChatActivity::class.java)
                        gIntent.putExtra("groupId", groupId)
                        startActivity(gIntent)

                        val intent = Intent(this, GroupInfoActivity::class.java)
                        intent.putExtra("groupId", groupId)
                        startActivity(intent)
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.editing_post_error), Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            }
                .addOnFailureListener {

                }

        } else {

            if (uri != "") {

                val bd = FirebaseStorage.getInstance().reference.child(pathNameFile)
                bd.putFile(Uri.parse(uri)).addOnSuccessListener {
                    val uriTask = it.storage.downloadUrl
                    while (!uriTask.isSuccessful) {
                    }
                    val uriDownload = uriTask.result
                    if (uriTask.isSuccessful) {
                        var results = HashMap<String, String>()
                        results["title"] = title
                        results["description"] = description
                        results["image"] = uriDownload.toString()

                        val change =
                            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Groups")

                        change.child(groupId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.group_edited), Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this, DashboardActivity::class.java))

                            val gIntent = Intent(this, GroupChatActivity::class.java)
                            gIntent.putExtra("groupId", groupId)
                            startActivity(gIntent)

                            val intent = Intent(this, GroupInfoActivity::class.java)
                            intent.putExtra("groupId", groupId)
                            startActivity(intent)
                        }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.editing_post_error), Toast.LENGTH_SHORT)
                                .show()
                        }

                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            } else {

                val results = HashMap<String, String>()
                results["title"] = title
                results["description"] = description
                results["image"] = ""

                val change =
                    FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Posts")

                change.child(groupId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.group_edited), Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, DashboardActivity::class.java))

                    val gIntent = Intent(this, GroupChatActivity::class.java)
                    gIntent.putExtra("groupId", groupId)
                    startActivity(gIntent)

                    val intent = Intent(this, GroupInfoActivity::class.java)
                    intent.putExtra("groupId", groupId)
                    startActivity(intent)
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.editing_post_error), Toast.LENGTH_SHORT)
                        .show()
                }
            }

        }
    }


    private fun loadGroupInfo() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referenceGr = db.getReference("Groups")
        val query = referenceGr.orderByChild("gid").equalTo(groupId)


        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val title = ds.child("title").value.toString()
                    val description = ds.child("description").value.toString()
                    val image = ds.child("image").value.toString()
                    editImagePath = image
                    val time = ds.child("time").value.toString()
                    val creator = ds.child("creator").value.toString()

                    titleGroup.setText(title)
                    descriptionGroup.setText(description)

                    try {
                        Picasso.get().load(image).into(imageGroup)
                    } catch (e: Exception) {
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun showPickImageDialog() {
        val photoOptions = arrayOf(getString(R.string.camera), getString(R.string.gallery), getString(R.string.delete_photo))
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
                2 -> {
                    image_uri = null
                    imageGroup.setImageResource(R.drawable.ic_people_24)
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
                imageGroup.setImageURI(image_uri)
            } else if (requestCode == CODE_SELECT_IMAGE_CAMERA) {
                imageGroup.setImageURI(image_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}