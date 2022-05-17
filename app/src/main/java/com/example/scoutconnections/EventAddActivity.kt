package com.example.scoutconnections

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlin.collections.HashMap

class EventAddActivity : AppCompatActivity() {

    val mAuth = FirebaseAuth.getInstance()
    private val CODE_CAMERA = 100
    private val CODE_STORAGE = 200
    private val CODE_SELECT_IMAGE_GALLERY = 300
    private val CODE_SELECT_IMAGE_CAMERA = 400
    var cameraPermissions = arrayOf<String>()
    var storagePermissions = arrayOf<String>()
    private var image_uri: Uri? = null
    private val user = mAuth.currentUser
    private lateinit var progressDialog: ProgressDialog
    private lateinit var titleEvent: EditText
    private lateinit var descriptionEvent: EditText
    private lateinit var imageEvent: ImageView
    private lateinit var timeEvent: EditText
    private lateinit var editImagePath: String


    private lateinit var timeEventEdit: String
    private  var editId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_add)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)

        titleEvent = findViewById<EditText>(R.id.title_event)
        descriptionEvent = findViewById<EditText>(R.id.description_event)
        imageEvent = findViewById<ImageView>(R.id.image_event)
        timeEvent = findViewById<EditText>(R.id.time_selected_event)
        val addTime = findViewById<Button>(R.id.select_date)
        val addEvent = findViewById<Button>(R.id.add_event)




        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        imageEvent.setOnClickListener { showPickImageDialog() }

        val action = intent.action
        val type = intent.type
        if(Intent.ACTION_SEND == action && type != null){
            if ("text/plain" == type){
                handleSendText(intent)
            }
            else if (type.startsWith("image")){
                handleSendImage(intent)
            }
        }

        editId = intent.getStringExtra("editId")
        if(editId == null){
            actionBar.title = getString(R.string.add_event)
            addEvent.text = getString(R.string.upload)
            timeEvent.visibility = View.GONE
        } else {
            actionBar.title = getString(R.string.edit_event)
            addEvent.text = getString(R.string.edit)
            timeEvent.visibility = View.VISIBLE
            loadEventData(editId!!)
        }

        addTime.setOnClickListener {
            showDateDialog()
        }

        addEvent.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val title = titleEvent.text.toString().trim()
                val description = descriptionEvent.text.toString().trim()
                val time = timeEvent.text.toString().trim()



                if (title.isEmpty()) {
                    Toast.makeText(
                        this@EventAddActivity,
                        getString(R.string.enter_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (description.isEmpty()) {
                    Toast.makeText(
                        this@EventAddActivity,
                        getString(R.string.enter_description),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (time.isEmpty()) {
                    Toast.makeText(
                        this@EventAddActivity,
                        getString(R.string.choose_date),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val t = SimpleDateFormat("HH:mm dd/MM/yyyy").parse(time)
                val tEvent = t.time.toString()

                if(editId == null){
                    if (image_uri == null) {
                        addEvent(title, description, tEvent, "")
                    } else {
                        addEvent(title, description, tEvent, image_uri.toString())
                    }
                } else {
                    if(editImagePath != "") {
                        if (image_uri == null) {
                            editEvent(title, description, tEvent, "", editId!!, true)
                        } else {
                            editEvent(title, description, tEvent, image_uri.toString(), editId!!, true)
                        }
                    } else {
                        if (image_uri == null) {
                            editEvent(title, description, tEvent,"", editId!!, false)
                        } else {
                            editEvent(title, description, tEvent, image_uri.toString(), editId!!, false)
                        }
                    }

                }

            }

        })

        checkUserStatus()



    }

    private fun showDateDialog() {
        val calendar = Calendar.getInstance()

        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        var day = calendar.get(Calendar.DAY_OF_MONTH)
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        var minute = calendar.get(Calendar.MINUTE)

        if (editId != null) {
            val date = Date(timeEventEdit.toLong())
            val cal = Calendar.getInstance()
            cal.time = date
            year = cal.get(Calendar.YEAR)
            month = cal.get(Calendar.MONTH)
            day = cal.get(Calendar.DAY_OF_MONTH)
            hour = cal.get(Calendar.HOUR)
            minute = cal.get(Calendar.MINUTE)
        }

        val dateListener = DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
            calendar.set(Calendar.YEAR, i)
            calendar.set(Calendar.MONTH, i2)
            calendar.set(Calendar.DAY_OF_MONTH, i3)

            val timeListener = TimePickerDialog.OnTimeSetListener { timePicker, i, i2 ->
                calendar.set(Calendar.HOUR_OF_DAY, i)
                calendar.set(Calendar.MINUTE, i2)

                val cal = Calendar.getInstance(Locale.ITALY)

                cal.timeInMillis = calendar.timeInMillis
                val timeC = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)
                timeEvent.visibility = View.VISIBLE
                timeEvent.setText(timeC)
            }
            val timePicker = TimePickerDialog(this@EventAddActivity, timeListener, hour, minute, true).show()

        }
        val datePicker = DatePickerDialog(this@EventAddActivity, dateListener, year, month, day)
        datePicker.datePicker.firstDayOfWeek = Calendar.MONDAY
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun handleSendImage(intent: Intent?) {
        val imageUri = intent!!.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            image_uri = imageUri
            imageEvent.setImageURI(image_uri)
        }
    }

    private fun handleSendText(intent: Intent?) {
        val sharedText = intent!!.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            descriptionEvent.setText(sharedText)
        }
    }

    private fun editEvent(title: String, description: String, tEvent: String, uri: String, editId: String, hadImage: Boolean) {
        progressDialog.setMessage(getString(R.string.editing_event))
        progressDialog.show()
        val time = System.currentTimeMillis().toString()
        val pathNameFile = "Events/event_$time"

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
                            results["tEvent"] = tEvent
                            results["image"] = uriDownload.toString()

                            val change =
                                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Events")

                            change.child(editId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, getString(R.string.event_edited), Toast.LENGTH_SHORT).show()
                                finish()
                            }.addOnFailureListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, getString(R.string.editing_event_error), Toast.LENGTH_SHORT)
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
                    results["tEvent"] = tEvent
                    results["image"] = ""

                    val change =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Events")

                    change.child(editId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.event_edited), Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.editing_event_error), Toast.LENGTH_SHORT)
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
                        results["tEvent"] = tEvent
                        results["image"] = uriDownload.toString()

                        val change =
                            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Events")

                        change.child(editId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.event_edited), Toast.LENGTH_SHORT).show()
                            finish()
                        }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.editing_event_error), Toast.LENGTH_SHORT)
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
                results["tEvent"] = tEvent
                results["image"] = ""

                val change =
                    FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Events")

                change.child(editId).updateChildren(results as Map<String, Any>).addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.event_edited), Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.editing_event_error), Toast.LENGTH_SHORT)
                        .show()
                }
            }

        }

    }


    private fun loadEventData(editId: String) {

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Events")

        val query = reference.orderByChild("eid").equalTo(editId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    titleEvent.setText(ds.child("title").value.toString())
                    descriptionEvent.setText(ds.child("description").value.toString())
                    val time = ds.child("tEvent").value.toString()
                    timeEventEdit = time
                    val cal = Calendar.getInstance(Locale.ITALY)

                    cal.timeInMillis = time!!.toLong()
                    val timeC = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

                    timeEvent.setText(timeC)
                    editImagePath = ds.child("image").value.toString()
                    try {
                        if (editImagePath != "") {
                            Picasso.get().load(editImagePath).into(imageEvent)

                        }
                    } catch (e: Exception) {
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun addEvent(title: String, description: String, tEvent: String, uri: String) {
        progressDialog.setMessage(getString(R.string.adding_event))
        progressDialog.show()
        val time = System.currentTimeMillis().toString()
        val pathNameFile = "Events/event_$time"

        if (uri != "") {

            val bd = FirebaseStorage.getInstance().reference.child(pathNameFile)
            bd.putFile(Uri.parse(uri)).addOnSuccessListener {
                val uriTask = it.storage.downloadUrl
                while (!uriTask.isSuccessful) {
                }
                val uriDownload = uriTask.result
                if (uriTask.isSuccessful) {
                    var results = HashMap<String, Any>()
                    results["creator"] = user!!.uid
                    results["title"] = title
                    results["description"] = description
                    results["eid"] = time
                    results["tEvent"] = tEvent
                    results["time"] = time
                    results["image"] = uriDownload.toString()

                    val db =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val reference = db.getReference("Events")

                    reference.child(time).setValue(results).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.event_added), Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, getString(R.string.adding_event_error), Toast.LENGTH_SHORT)
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
            results["creator"] = user!!.uid
            results["title"] = title
            results["description"] = description
            results["eid"] = time
            results["tEvent"] = tEvent
            results["time"] = time
            results["image"] = ""

            val db =
                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
            val reference = db.getReference("Events")

            reference.child(time).setValue(results).addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.event_added), Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, getString(R.string.adding_event_error), Toast.LENGTH_SHORT).show()
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
                    imageEvent.setImageResource(R.drawable.ic_add_photo_24)
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
                imageEvent.setImageURI(image_uri)
            } else if (requestCode == CODE_SELECT_IMAGE_CAMERA) {
                imageEvent.setImageURI(image_uri)
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