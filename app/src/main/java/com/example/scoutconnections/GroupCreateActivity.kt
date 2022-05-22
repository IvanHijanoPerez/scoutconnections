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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class GroupCreateActivity : AppCompatActivity() {

    val mAuth = FirebaseAuth.getInstance()
    private val user = mAuth.currentUser
    private val CODE_CAMERA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400
    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()
    private var image_uri: Uri? = null

    private lateinit var groupId: String

    private lateinit var progressDialog: ProgressDialog

    private lateinit var titleGroup: EditText
    private lateinit var descriptionGroup: EditText
    private lateinit var imageGroup: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_create)

        progressDialog = ProgressDialog(this)

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.create_group)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        imageGroup = findViewById<ImageView>(R.id.image_group)
        titleGroup = findViewById<EditText>(R.id.title_group)
        descriptionGroup = findViewById<EditText>(R.id.description_group)
        val createGroup = findViewById<FloatingActionButton>(R.id.create_group_btn)

        imageGroup.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                showPickImageDialog()
            }

        })

        createGroup.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                val title = titleGroup.text.toString().trim()
                val description = descriptionGroup.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(
                        this@GroupCreateActivity,
                        getString(R.string.enter_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                
                if (image_uri == null) {
                    createGroup(title, description, "")
                } else {
                    createGroup(title, description, image_uri.toString())
                }

            
            }

        })

        checkUserStatus()
    }

    private fun createGroup(title: String, description: String, uri: String) {
        progressDialog.setMessage(getString(R.string.creating_group))
        progressDialog.show()
        val time = System.currentTimeMillis().toString()

        groupId = time

        val pathNameFile = "Groups/image_$time"

        if (uri != "") {

            val bd = FirebaseStorage.getInstance().reference.child(pathNameFile)
            bd.putFile(Uri.parse(uri)).addOnSuccessListener {
                val uriTask = it.storage.downloadUrl
                while (!uriTask.isSuccessful) {
                }
                val uriDownload = uriTask.result
                if (uriTask.isSuccessful) {
                    var results = HashMap<String, Any>()
                    results["gid"] = time
                    results["title"] = title
                    results["description"] = description
                    results["time"] = time
                    results["image"] = uriDownload.toString()
                    results["creator"] = user!!.uid

                    val db =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val reference = db.getReference("Groups")

                    reference.child(time).setValue(results).addOnSuccessListener {
                        val results1 = HashMap<String, Any>()
                        results1["uid"] = user.uid
                        results1["role"] = "creator"
                        results1["time"] = time

                        reference.child(time).child("Participants").child(user.uid).setValue(results1).addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.group_created), Toast.LENGTH_SHORT).show()

                            finish()
                            val intent = Intent(this, GroupChatActivity::class.java)
                            intent.putExtra("groupId", groupId)

                            startActivity(intent)

                        }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.error_ocurred), Toast.LENGTH_SHORT)
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

            val results = HashMap<String, Any>()
            results["gid"] = time
            results["title"] = title
            results["description"] = description
            results["time"] = time
            results["image"] = ""
            results["creator"] = user!!.uid

            val db =
                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
            val reference = db.getReference("Groups")

            reference.child(time).setValue(results).addOnSuccessListener {

                val results1 = HashMap<String, Any>()
                results1["uid"] = user.uid
                results1["role"] = "creator"
                results1["time"] = time

                reference.child(time).child("Participants").child(user.uid).setValue(results1).addOnSuccessListener {
                    progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.group_created), Toast.LENGTH_SHORT).show()

                    finish()
                    val intent = Intent(this, GroupChatActivity::class.java)
                    intent.putExtra("groupId", groupId)

                    startActivity(intent)

                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
        }
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