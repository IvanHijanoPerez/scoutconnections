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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AddPostActivity : AppCompatActivity() {

    val mAuth = FirebaseAuth.getInstance()
    private val CODE_CAMERA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400
    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()
    private var image_uri: Uri? = null
    lateinit var imagePost: ImageView
    private val user = mAuth.currentUser
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.add_post)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)

        val titlePost = findViewById<EditText>(R.id.title_post)
        val descriptionPost = findViewById<EditText>(R.id.description_post)
        imagePost = findViewById<ImageView>(R.id.image_post)
        val addPost = findViewById<Button>(R.id.add_post)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        imagePost.setOnClickListener { showPickImageDialog() }

        addPost.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val title = titlePost.text.toString().trim()
                val description = descriptionPost.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.enter_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (description.isEmpty()) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.enter_description),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (image_uri == null) {
                    addPost(title, description, getString(R.string.no_image_post_db))
                } else {
                    addPost(title, description, image_uri.toString())
                }
            }

        })
    }

    private fun addPost(title: String, description: String, uri: String) {
        progressDialog.setMessage(getString(R.string.adding_post))
        progressDialog.show()
        val time = System.currentTimeMillis().toString()
        val pathNameFile = "Posts/post_$time"

        if (uri != getString(R.string.no_image_post_db)) {

            val bd = FirebaseStorage.getInstance().reference.child(pathNameFile)
            bd.putFile(Uri.parse(uri)).addOnSuccessListener {
                val uriTask = it.storage.downloadUrl
                while (!uriTask.isSuccessful) {
                }
                val uriDownload = uriTask.result
                if (uriTask.isSuccessful) {
                    var results = HashMap<String, String>()
                    results[getString(R.string.creator_post_db)] = user!!.uid
                    results[getString(R.string.title_post_db)] = title
                    results[getString(R.string.description_post_db)] = description
                    results[getString(R.string.time_post_db)] = time
                    results[getString(R.string.image_post_db)] = uriDownload.toString()

                    val db =
                        FirebaseDatabase.getInstance(getString(R.string.firebase_database_instance))
                    val reference = db.getReference(getString(R.string.posts_db))

                    reference.child(time).setValue(results).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.post_added), Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.adding_post_error), Toast.LENGTH_SHORT)
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
            results[getString(R.string.creator_post_db)] = user!!.uid
            results[getString(R.string.title_post_db)] = title
            results[getString(R.string.description_post_db)] = description
            results[getString(R.string.time_post_db)] = time
            results[getString(R.string.image_post_db)] = getString(R.string.no_image_post_db)

            val db =
                FirebaseDatabase.getInstance(getString(R.string.firebase_database_instance))
            val reference = db.getReference(getString(R.string.posts_db))

            reference.child(time).setValue(results).addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.post_added), Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.adding_post_error), Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showPickImageDialog() {
        val photoOptions = arrayOf(getString(R.string.camera), getString(R.string.gallery))
        val constructorPhoto = AlertDialog.Builder(this)
        constructorPhoto.setTitle(getString(R.string.select_image))
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
                imagePost.setImageURI(image_uri)
            } else if (requestCode == CODE_SELECT_IMAGE_CAMERA) {
                imagePost.setImageURI(image_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        checkUserStatus()
        super.onResume()
    }

    override fun onStart() {
        checkUserStatus()
        super.onStart()
    }
}