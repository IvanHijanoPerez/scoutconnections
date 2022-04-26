package com.example.scoutconnections

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.util.jar.Manifest

class PerfilFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    private lateinit var progressDialog: ProgressDialog

    val direccionAlmacenamiento = "Usuarios/"

    private val CODIGO_CAMARA = 100
    private val CODIGO_ALMACENAMIENTO = 200
    private val CODIGO_COGER_IMAGEN_GALERIA = 300
    private val CODIGO_COGER_IMAGEN_CAMARA = 400

    val mAuth = FirebaseAuth.getInstance()
    val usuario = mAuth.currentUser

    var permisosCamara = arrayOf<String>()
    var permisosAlmacenamiento = arrayOf<String>()

    private lateinit var imagen_uri: Uri

    private lateinit var imagenOfondo: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val vista = inflater.inflate(R.layout.fragment_perfil, container, false)

        //Inicializar informacion de BD Firebase


        val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referencia = baseDatos.getReference("Usuarios")

        //Inicializar arrays de permisos
        permisosCamara = arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permisosAlmacenamiento = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //Inicializar variables de los objetos visibles
        val imagenPerfil = vista.findViewById<ImageView>(R.id.imagenPerfil)
        val fondoPerfil = vista.findViewById<ImageView>(R.id.fondoPerfil)
        val nombrePerfil = vista.findViewById<TextView>(R.id.nombrePerfil)
        val correoPerfil = vista.findViewById<TextView>(R.id.correoPerfil)
        val telefonoPerfil = vista.findViewById<TextView>(R.id.telefonoPerfil)
        val rolPerfil = vista.findViewById<TextView>(R.id.rolPerfil)
        val fabMenu = vista.findViewById<FloatingActionButton>(R.id.fabMenu)

        val consulta = referencia.orderByChild("correo").equalTo(usuario?.email)

        progressDialog = ProgressDialog(activity)

        consulta.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(ds: DataSnapshot in snapshot.children){
                    val nombre = ds.child("nombre").value.toString()
                    val imagen = ds.child("imagen").value.toString()
                    val correo = ds.child("correo").value.toString()
                    val telefono = ds.child("telefono").value.toString()
                    val rol = ds.child("monitor").value
                    val fondo = ds.child("fondo").value.toString()

                    nombrePerfil.text = nombre
                    correoPerfil.text = String(Character.toChars(0x1F4EC)) + " " + correo
                    telefonoPerfil.text = String(Character.toChars(0x1F4DE)) + " " + telefono
                    if (rol != null) {
                        if(rol == false){
                            rolPerfil.text = String(Character.toChars(0x1F530)) + " " + "Educando"
                        }else{
                            rolPerfil.text = String(Character.toChars(0x1F464)) + " " + "Monitor"
                        }
                    }
                    try {
                        if (!imagen.equals("")){
                            Picasso.get().load(imagen).into(imagenPerfil)
                        }
                    }catch (e: Exception){
                        Picasso.get().load(R.drawable.ic_foto_perfil).into(imagenPerfil)
                    }

                    try {
                        Picasso.get().load(fondo).into(fondoPerfil)
                    }catch (e: Exception){
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity , "Ha habido un problema con el perfil", Toast.LENGTH_SHORT).show()
            }

        })

        //Boton Menu Fab
        fabMenu.setOnClickListener {
            showMenuEditarPerfil()        }

        return vista
    }


    //Funciones para permisos de almacenamiento y camara
    private fun comprobarPermisoAlmacenamiento(): Boolean{
        val resultado = activity?.let { ContextCompat.checkSelfPermission(it,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED
        return resultado
    }

    private fun solicitarPermisoAlmacenamiento(){
        requestPermissions(permisosAlmacenamiento,CODIGO_ALMACENAMIENTO)
    }

    private fun comprobarPermisoCamara(): Boolean{
        val resultado1 = activity?.let { ContextCompat.checkSelfPermission(it,android.Manifest.permission.CAMERA) } == PackageManager.PERMISSION_GRANTED
        val resultado2 = activity?.let { ContextCompat.checkSelfPermission(it,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED
        return resultado1 && resultado2
    }

    private fun solicitarPermisoCamara(){
        requestPermissions(permisosCamara,CODIGO_CAMARA)
    }


    //Función que muestra el menu para editar el ususario
    private fun showMenuEditarPerfil(){
        val opciones = arrayOf("Editar imagen","Editar fondo","Editar nombre","Editar teléfono")
        val constructor = AlertDialog.Builder(activity)
        constructor.setTitle("Escoge una acción")
        constructor.setItems(opciones){ _, pos ->
            when (pos) {
                0 -> {
                    progressDialog.setMessage("Actualizando imagen")
                    imagenOfondo = "imagen"
                    showFotoDialogo()

                }
                1 -> {
                    progressDialog.setMessage("Actualizando fondo")
                    imagenOfondo = "fondo"
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
        linearLayout.setPadding(10,10,10,10)

        val campo = EditText(activity)
        campo.setHint("Escribe aquí")
        if(s.equals("nombre")){
            campo.inputType = InputType.TYPE_CLASS_TEXT
        }else {
            campo.inputType = InputType.TYPE_CLASS_NUMBER
        }
        linearLayout.addView(campo)

        customDialog.setView(linearLayout)

        customDialog.setPositiveButton("Actualizar",DialogInterface.OnClickListener { dialogInterface, i ->
            var cam = campo.text.toString().trim()
            if(cam.isEmpty()){
                Toast.makeText(activity, "Por favor, rellena el campo con un $s válido", Toast.LENGTH_SHORT).show()
                showNombreTelefonoDialogo(s)
            }else{
                progressDialog.show()

                var valor = HashMap<String, String>()
                valor.put(s, cam)

                val uid = usuario?.uid
                val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                val referencia = baseDatos.getReference("Usuarios")
                if (uid != null) {
                    referencia.child(uid).updateChildren(valor as Map<String, Any>).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(activity , "Se ha actualizado el $s", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener{
                        progressDialog.dismiss()
                        Toast.makeText(activity , "Error actualizando el $s...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        customDialog.setNegativeButton("Cancelar",DialogInterface.OnClickListener { dialogInterface, i ->
            dialogInterface.dismiss()
        })

        customDialog.create().show()
    }


    //Función que muestra el menu para editar la foto
    private fun showFotoDialogo() {
        val opcionesFoto = arrayOf("Cámara","Galería")
        val constructorFoto = AlertDialog.Builder(activity)
        constructorFoto.setTitle("Selecciona una imagen")
        constructorFoto.setItems(opcionesFoto){ _, pos ->
            when (pos) {
                0 -> {
                    if(!comprobarPermisoCamara()){
                        solicitarPermisoCamara()

                    }else{
                        cogerDeCamara()
                    }
                }
                1 -> {
                    if(!comprobarPermisoAlmacenamiento()){
                        solicitarPermisoAlmacenamiento()

                    }else{
                        cogerDeGaleria()
                    }
                }
            }
        }
        constructorFoto.create().show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CODIGO_CAMARA ->
                if(grantResults.size > 0){
                    val camaraAceptada = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val escribirAlmacenamientoAceptada = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if(camaraAceptada && escribirAlmacenamientoAceptada){
                        cogerDeCamara()
                    }else{
                        Toast.makeText(activity , "Por favor, habilita los permisos de cámara y almacenamiento", Toast.LENGTH_SHORT).show()
                    }
                }
            CODIGO_ALMACENAMIENTO ->
                if(grantResults.size > 0){
                    val escribirAlmacenamientoAceptada = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if(escribirAlmacenamientoAceptada){
                        cogerDeGaleria()
                    }else{
                        Toast.makeText(activity , "Por favor, habilita los permisos de almacenamiento", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(resultCode == RESULT_OK){
            if(requestCode == CODIGO_COGER_IMAGEN_GALERIA){
                if (data != null) {
                    imagen_uri = data.data!!
                }
                subirImagenFondo(imagen_uri)
            }
            else if(requestCode == CODIGO_COGER_IMAGEN_CAMARA){
                subirImagenFondo(imagen_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun subirImagenFondo(imagenUri: Uri) {
        progressDialog.show()
        if (usuario != null) {
            var almacenamientoReferencia = FirebaseStorage.getInstance().reference
            val direccionYNombreArchivo = direccionAlmacenamiento + "" + imagenOfondo + "_" + usuario.uid
            val aR = almacenamientoReferencia.child(direccionYNombreArchivo)
            aR.putFile(imagenUri).addOnSuccessListener{
                val tareaUri = it.storage.downloadUrl
                while(!tareaUri.isSuccessful){}
                val descargaUri = tareaUri.result
                if(tareaUri.isSuccessful){
                    var resultados = HashMap<String, String>()
                    resultados.put(imagenOfondo, descargaUri.toString())


                    val uid = usuario?.uid
                    val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val referencia = baseDatos.getReference("Usuarios")
                    if (uid != null) {
                        referencia.child(uid).updateChildren(resultados as Map<String, Any>).addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(activity , "Foto actualizada...", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener{
                            progressDialog.dismiss()
                            Toast.makeText(activity , "Error actualizando la foto...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    progressDialog.dismiss()
                    Toast.makeText(activity , "Ha ocurrido algún error", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener{
                progressDialog.dismiss()
                Toast.makeText(activity , it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cogerDeCamara() {
        var valores = ContentValues()
        valores.put(MediaStore.Images.Media.TITLE, "Foto temporal")
        valores.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion temporal")
        imagen_uri = activity?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,valores)!!
        var camaraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        camaraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imagen_uri)
        startActivityForResult(camaraIntent, CODIGO_COGER_IMAGEN_CAMARA)
    }

    private fun cogerDeGaleria() {
        var galeriaIntent = Intent(Intent.ACTION_PICK)
        galeriaIntent.setType("image/*")
        startActivityForResult(galeriaIntent, CODIGO_COGER_IMAGEN_GALERIA)
    }

    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario(){
        val usuario = mAuth.currentUser
        if(usuario == null){
            startActivity(Intent(activity,MainActivity::class.java))
            activity?.finish()
        }else{
            //correoTxt.setText(usuario.email)
        }
    }

    //Función que añade menu al actionBar
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_principal, menu)

        menu.findItem(R.id.accion_buscar).setVisible(false)
        menu.findItem(R.id.accion_usuarios).setVisible(false)
        menu.findItem(R.id.accion_anadir_post).setVisible(false)

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    //Función que indica la acción al seleccionar el menu del actionBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == R.id.accion_logout){
            mAuth.signOut()
            comprobarEstadoUsuario()
        }

        return super.onOptionsItemSelected(item)
    }

}