package com.example.scoutconnections

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
            FirebaseDatabase.getInstance(getString(R.string.firebase_database_instance))
        val reference = db.getReference(getString(R.string.users_db))

        cameraPermissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val imageProfile = view.findViewById<ImageView>(R.id.image_profile)
        val coverProfile = view.findViewById<ImageView>(R.id.cover_profile)
        val nameProfile = view.findViewById<TextView>(R.id.nombrePerfil)
        val emailProfile = view.findViewById<TextView>(R.id.correoPerfil)
        val phoneProfile = view.findViewById<TextView>(R.id.telefonoPerfil)
        val rolPerfil = view.findViewById<TextView>(R.id.rolPerfil)
        val fabMenu = view.findViewById<FloatingActionButton>(R.id.fabMenu)

        val consulta = reference.orderByChild("correo").equalTo(user?.email)

        progressDialog = ProgressDialog(activity)

        consulta.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val nombre = ds.child("nombre").value.toString()
                    val imagen = ds.child("imagen").value.toString()
                    val correo = ds.child("correo").value.toString()
                    val telefono = ds.child("telefono").value.toString()
                    val rol = ds.child("monitor").value
                    val fondo = ds.child("fondo").value.toString()

                    nameProfile.text = nombre
                    emailProfile.text = String(Character.toChars(0x1F4EC)) + " " + correo
                    phoneProfile.text = String(Character.toChars(0x1F4DE)) + " " + telefono
                    if (rol != null) {
                        if (rol == false) {
                            rolPerfil.text = String(Character.toChars(0x1F530)) + " " + "Educando"
                        } else {
                            rolPerfil.text = String(Character.toChars(0x1F464)) + " " + "Monitor"
                        }
                    }
                    try {
                        if (!imagen.equals("")) {
                            Picasso.get().load(imagen).into(imageProfile)
                        }
                    } catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_foto_perfil).into(imageProfile)
                    }

                    try {
                        Picasso.get().load(fondo).into(coverProfile)
                    } catch (e: Exception) {
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, "Ha habido un problema con el perfil", Toast.LENGTH_SHORT)
                    .show()
            }

        })

        //Boton Menu Fab
        fabMenu.setOnClickListener {
            showMenuEditarPerfil()
        }

        return view
    }


    //Funciones para permisos de almacenamiento y camara
    private fun comprobarPermisoAlmacenamiento(): Boolean {
        val resultado = activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
        return resultado
    }

    private fun solicitarPermisoAlmacenamiento() {
        requestPermissions(storagePermissions, CODE_STORAGE)
    }

    private fun comprobarPermisoCamara(): Boolean {
        val resultado1 = activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.CAMERA
            )
        } == PackageManager.PERMISSION_GRANTED
        val resultado2 = activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
        return resultado1 && resultado2
    }

    private fun solicitarPermisoCamara() {
        requestPermissions(cameraPermissions, CODE_CAMARA)
    }


    //Función que muestra el menu para editar el ususario
    private fun showMenuEditarPerfil() {
        val opciones = arrayOf("Editar imagen", "Editar fondo", "Editar nombre", "Editar teléfono")
        val constructor = AlertDialog.Builder(activity)
        constructor.setTitle("Escoge una acción")
        constructor.setItems(opciones) { _, pos ->
            when (pos) {
                0 -> {
                    progressDialog.setMessage("Actualizando imagen")
                    imageOrCover = "imagen"
                    showFotoDialogo()

                }
                1 -> {
                    progressDialog.setMessage("Actualizando fondo")
                    imageOrCover = "fondo"
                    showFotoDialogo()
                }
                2 -> {
                    progressDialog.setMessage("Actualizando nombre")
                    showNombreTelefonoDialogo("nombre")
                }
                else -> {
                    progressDialog.setMessage("Actualizando teléfono")
                    showNombreTelefonoDialogo("telefono")
                }
            }
        }
        constructor.create().show()
    }

    private fun showNombreTelefonoDialogo(s: String) {
        val customDialog = AlertDialog.Builder(activity)
        customDialog.setTitle("Actualizar $s")

        val linearLayout = LinearLayout(activity)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(10, 10, 10, 10)

        val campo = EditText(activity)
        campo.setHint("Escribe aquí")
        if (s.equals("nombre")) {
            campo.inputType = InputType.TYPE_CLASS_TEXT
        } else {
            campo.inputType = InputType.TYPE_CLASS_NUMBER
        }
        linearLayout.addView(campo)

        customDialog.setView(linearLayout)

        customDialog.setPositiveButton(
            "Actualizar",
            DialogInterface.OnClickListener { dialogInterface, i ->
                var cam = campo.text.toString().trim()
                if (cam.isEmpty()) {
                    Toast.makeText(
                        activity,
                        "Por favor, rellena el campo con un $s válido",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNombreTelefonoDialogo(s)
                } else {
                    progressDialog.show()

                    var valor = HashMap<String, String>()
                    valor.put(s, cam)

                    val uid = user?.uid
                    val baseDatos =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val referencia = baseDatos.getReference("Usuarios")
                    if (uid != null) {
                        referencia.child(uid).updateChildren(valor as Map<String, Any>)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    activity,
                                    "Se ha actualizado el $s",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(
                                activity,
                                "Error actualizando el $s...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })

        customDialog.setNegativeButton(
            "Cancelar",
            DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })

        customDialog.create().show()
    }


    //Función que muestra el menu para editar la foto
    private fun showFotoDialogo() {
        val opcionesFoto = arrayOf("Cámara", "Galería")
        val constructorFoto = AlertDialog.Builder(activity)
        constructorFoto.setTitle("Selecciona una imagen")
        constructorFoto.setItems(opcionesFoto) { _, pos ->
            when (pos) {
                0 -> {
                    if (!comprobarPermisoCamara()) {
                        solicitarPermisoCamara()

                    } else {
                        cogerDeCamara()
                    }
                }
                1 -> {
                    if (!comprobarPermisoAlmacenamiento()) {
                        solicitarPermisoAlmacenamiento()

                    } else {
                        cogerDeGaleria()
                    }
                }
            }
        }
        constructorFoto.create().show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CODE_CAMARA ->
                if (grantResults.size > 0) {
                    val camaraAceptada = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val escribirAlmacenamientoAceptada =
                        grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (camaraAceptada && escribirAlmacenamientoAceptada) {
                        cogerDeCamara()
                    } else {
                        Toast.makeText(
                            activity,
                            "Por favor, habilita los permisos de cámara y almacenamiento",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            CODE_STORAGE ->
                if (grantResults.size > 0) {
                    val escribirAlmacenamientoAceptada =
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (escribirAlmacenamientoAceptada) {
                        cogerDeGaleria()
                    } else {
                        Toast.makeText(
                            activity,
                            "Por favor, habilita los permisos de almacenamiento",
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
                subirImagenFondo(image_uri)
            } else if (requestCode == CODE_SELECT_IMAGE_CAMERA) {
                subirImagenFondo(image_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun subirImagenFondo(imagenUri: Uri) {
        progressDialog.show()
        if (user != null) {
            var almacenamientoReferencia = FirebaseStorage.getInstance().reference
            val direccionYNombreArchivo = storagePath + "" + imageOrCover + "_" + user.uid
            val aR = almacenamientoReferencia.child(direccionYNombreArchivo)
            aR.putFile(imagenUri).addOnSuccessListener {
                val tareaUri = it.storage.downloadUrl
                while (!tareaUri.isSuccessful) {
                }
                val descargaUri = tareaUri.result
                if (tareaUri.isSuccessful) {
                    var resultados = HashMap<String, String>()
                    resultados.put(imageOrCover, descargaUri.toString())


                    val uid = user?.uid
                    val baseDatos =
                        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val referencia = baseDatos.getReference("Usuarios")
                    if (uid != null) {
                        referencia.child(uid).updateChildren(resultados as Map<String, Any>)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(activity, "Foto actualizada...", Toast.LENGTH_SHORT)
                                    .show()
                            }.addOnFailureListener {
                            progressDialog.dismiss()
                            Toast.makeText(
                                activity,
                                "Error actualizando la foto...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(activity, "Ha ocurrido algún error", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cogerDeCamara() {
        var valores = ContentValues()
        valores.put(MediaStore.Images.Media.TITLE, "Foto temporal")
        valores.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion temporal")
        image_uri = activity?.contentResolver?.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            valores
        )!!
        var camaraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        camaraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(camaraIntent, CODE_SELECT_IMAGE_CAMERA)
    }

    private fun cogerDeGaleria() {
        var galeriaIntent = Intent(Intent.ACTION_PICK)
        galeriaIntent.setType("image/*")
        startActivityForResult(galeriaIntent, CODE_SELECT_IMAGE_GALLERY)
    }

    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario() {
        val usuario = mAuth.currentUser
        if (usuario == null) {
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        } else {
            //correoTxt.setText(usuario.email)
        }
    }

    //Función que añade menu al actionBar
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.principal_menu, menu)

        menu.findItem(R.id.action_search).setVisible(false)
        menu.findItem(R.id.action_users).setVisible(false)
        menu.findItem(R.id.action_add_post).setVisible(false)

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    //Función que indica la acción al seleccionar el menu del actionBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            mAuth.signOut()
            comprobarEstadoUsuario()
        }

        return super.onOptionsItemSelected(item)
    }

}