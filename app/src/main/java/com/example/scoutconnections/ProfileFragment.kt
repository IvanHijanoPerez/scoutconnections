package com.example.scoutconnections

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
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

class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    private lateinit var progressDialog: ProgressDialog

    private val storagePath = "Users/"

    private val CODE_CAMARA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400

    val mAuth = FirebaseAuth.getInstance()
    private val user = mAuth.currentUser

    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()

    private lateinit var image_uri: Uri

    private lateinit var imageOrCover: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Users")

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val imageProfile = view.findViewById<ImageView>(R.id.image_profile)
        val coverProfile = view.findViewById<ImageView>(R.id.cover_profile)
        val nameProfile = view.findViewById<TextView>(R.id.name_profile)
        val emailProfile = view.findViewById<TextView>(R.id.email_profile)
        val phoneProfile = view.findViewById<TextView>(R.id.phone_profile)
        val roleProfile = view.findViewById<TextView>(R.id.role_profile)
        val fabMenu = view.findViewById<FloatingActionButton>(R.id.fab_menu)

        val query = reference.orderByChild("email").equalTo(user?.email)

        progressDialog = ProgressDialog(activity)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val name = ds.child("name").value.toString()
                    val image = ds.child("image").value.toString()
                    val email = ds.child("email").value.toString()
                    val phone = ds.child("phone").value.toString()
                    val role = ds.child("monitor").value
                    val cover = ds.child("cover").value.toString()

                    nameProfile.text = name
                    emailProfile.text = String(Character.toChars(0x1F4EC)) + " " + email
                    phoneProfile.text = String(Character.toChars(0x1F4DE)) + " " + phone
                    if (role != null) {
                        if (role == false) {
                            roleProfile.text = String(Character.toChars(0x1F530)) + " Scout"
                        } else {
                            roleProfile.text = String(Character.toChars(0x1F464)) + " Monitor"
                        }
                    }
                    try {
                        if (image != "") {
                            Picasso.get().load(image).into(imageProfile)
                        }
                    } catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_profile_24).into(imageProfile)
                    }

                    try {
                        Picasso.get().load(cover).into(coverProfile)
                    } catch (e: Exception) {
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, getString(R.string.profile_problem), Toast.LENGTH_SHORT)
                    .show()
            }

        })

        fabMenu.setOnClickListener {
            showEditProfileMenu()
        }

        return view
    }


    private fun checkStoragePermission(): Boolean {
        return activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
    }

    private fun equestStoragePermission() {
        requestPermissions(storagePermissions, CODE_STORAGE)
    }

    private fun checkCameraPermission(): Boolean {
        val result1 = activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.CAMERA
            )
        } == PackageManager.PERMISSION_GRANTED
        val result2 = activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
        return result1 && result2
    }

    private fun requestCameraPermission() {
        requestPermissions(cameraPermissions, CODE_CAMARA)
    }


    private fun showEditProfileMenu() {
        val options = arrayOf(getString(R.string.edit_image), getString(R.string.edit_cover), getString(
                    R.string.edit_name), getString(R.string.edit_phone))
        val constructor = AlertDialog.Builder(activity)
        constructor.setTitle(getString(R.string.choose_action))
        constructor.setItems(options) { _, pos ->
            when (pos) {
                0 -> {
                    progressDialog.setMessage(getString(R.string.updating_image))
                    imageOrCover = "image"
                    showPhotoDialog()

                }
                1 -> {
                    progressDialog.setMessage(getString(R.string.updating_cover))
                    imageOrCover = "cover"
                    showPhotoDialog()
                }
                2 -> {
                    progressDialog.setMessage(getString(R.string.updating_name))
                    showNamePhoneDialog("name")
                }
                else -> {
                    progressDialog.setMessage(getString(R.string.updating_phone))
                    showNamePhoneDialog("phone")
                }
            }
        }
        constructor.create().show()
    }

    private fun showNamePhoneDialog(s: String) {
        val customDialog = AlertDialog.Builder(activity)
        customDialog.setTitle(getString(R.string.update) + " " + s)

        val linearLayout = LinearLayout(activity)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(10, 10, 10, 10)

        val campo = EditText(activity)

        campo.hint = getString(R.string.type_here)
        if (s == "name") {
            campo.inputType = InputType.TYPE_CLASS_TEXT
        } else {
            campo.inputType = InputType.TYPE_CLASS_NUMBER
        }
        linearLayout.addView(campo)

        customDialog.setView(linearLayout)

        customDialog.setPositiveButton(
            getString(R.string.update),
            DialogInterface.OnClickListener { _, _ ->
                val cam = campo.text.toString().trim()
                if (cam.isEmpty()) {

                    if(s == "name"){
                        Toast.makeText(
                            activity,
                            getString(R.string.fill_valid_name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }else{
                        Toast.makeText(
                            activity,
                            getString(R.string.fill_valid_phone),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    showNamePhoneDialog(s)
                } else {
                    progressDialog.show()

                    val value = HashMap<String, String>()
                    value[s] = cam

                    val uid = user?.uid
                    val db =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val reference = db.getReference("Users")
                    if (uid != null) {
                        reference.child(uid).updateChildren(value as Map<String, Any>)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                if(s == "name"){
                                    Toast.makeText(
                                        activity,
                                         getString(R.string.updated_name),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }else{
                                    Toast.makeText(
                                        activity,
                                        getString(R.string.updated_phone),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(
                                activity,
                                getString(R.string.error_ocurred),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })

        customDialog.setNegativeButton(
            getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            })

        customDialog.create().show()
    }

    private fun showPhotoDialog() {
        val optionsPhoto = arrayOf(getString(R.string.camera), getString(R.string.gallery))
        val constructorPhoto = AlertDialog.Builder(activity)
        constructorPhoto.setTitle(getString(R.string.select_image))
        constructorPhoto.setItems(optionsPhoto) { _, pos ->
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
                        equestStoragePermission()

                    } else {
                        selectFromGallery()
                    }
                }
            }
        }
        constructorPhoto.create().show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CODE_CAMARA ->
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted =
                        grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && writeStorageAccepted) {
                        selectFromCamera()
                    } else {
                        Toast.makeText(
                            activity,
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
                            activity,
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
                uploadImageCover(image_uri)
            } else if (requestCode == CODE_SELECT_IMAGE_CAMERA) {
                uploadImageCover(image_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun uploadImageCover(imageUri: Uri) {
        progressDialog.show()
        if (user != null) {
            var storageReference = FirebaseStorage.getInstance().reference
            val pathNameFile = storagePath + "" + imageOrCover + "_" + user.uid
            val aR = storageReference.child(pathNameFile)
            aR.putFile(imageUri).addOnSuccessListener {
                val taskUri = it.storage.downloadUrl
                while (!taskUri.isSuccessful) {
                }
                val downloadUri = taskUri.result
                if (taskUri.isSuccessful) {
                    val results = HashMap<String, String>()
                    results[imageOrCover] = downloadUri.toString()


                    val uid = user?.uid
                    val db =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val reference = db.getReference("Users")
                    if (uid != null) {
                        reference.child(uid).updateChildren(results as Map<String, Any>)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(activity, getString(R.string.updated_photo), Toast.LENGTH_SHORT)
                                    .show()
                            }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(
                                activity,
                                getString(R.string.error_updating_photo),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(activity, getString(R.string.error_ocurred), Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, getString(R.string.temporal_image))
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.temporal_description))
        image_uri = activity?.contentResolver?.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, CODE_SELECT_IMAGE_CAMERA)
    }

    private fun selectFromGallery() {
        var galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, CODE_SELECT_IMAGE_GALLERY)
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user == null) {
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        } else {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.principal_menu, menu)

        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_users).isVisible = false
        menu.findItem(R.id.action_add_post).isVisible = false

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            mAuth.signOut()
            checkUserStatus()
        }

        return super.onOptionsItemSelected(item)
    }

}