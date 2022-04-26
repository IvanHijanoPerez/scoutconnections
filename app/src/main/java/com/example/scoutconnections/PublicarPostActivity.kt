package com.example.scoutconnections

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

class PublicarPostActivity : AppCompatActivity() {

    val mAuth =  FirebaseAuth.getInstance()
    private val CODIGO_CAMARA = 100
    private val CODIGO_ALMACENAMIENTO = 200
    private val CODIGO_COGER_IMAGEN_GALERIA = 300
    private val CODIGO_COGER_IMAGEN_CAMARA = 400
    var permisosCamara = arrayOf<String>()
    var permisosAlmacenamiento = arrayOf<String>()
    private lateinit var imagen_uri: Uri
    lateinit var postImagen: ImageView
    val usuario = mAuth.currentUser
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicar_post)

        val actionBar = getSupportActionBar()
        actionBar!!.setTitle("Publicar post")
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)

        val postTitulo = findViewById<EditText>(R.id.postTitulo)
        val postDescripcion = findViewById<EditText>(R.id.postDescripcion)
        postImagen = findViewById<ImageView>(R.id.postImagen)
        val postPublicar = findViewById<Button>(R.id.postPublicar)

        //Inicializar arrays de permisos
        permisosCamara = arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permisosAlmacenamiento = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        postImagen.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                mostrarDialogoCogerImagen()
            }
        })

        postPublicar.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                val titulo = postTitulo.text.toString().trim()
                val descripcion = postDescripcion.text.toString().trim()
                if(titulo.isEmpty()){
                    Toast.makeText(applicationContext, "Introduce un título", Toast.LENGTH_SHORT).show()
                    return
                }
                if(descripcion.isEmpty()){
                    Toast.makeText(applicationContext, "Introduce una descripción", Toast.LENGTH_SHORT).show()
                    return
                }

                if(imagen_uri == null){
                    subirPost(titulo, descripcion, "noImagen")
                }else{
                    subirPost(titulo, descripcion, imagen_uri.toString())
                }
            }

        })
    }

    private fun subirPost(titulo: String, descripcion: String, uri: String) {
        progressDialog.setMessage("Publicando post...")
        progressDialog.show()
        val tiempo = System.currentTimeMillis().toString()
        val direccionYNombreArchivo = "Posts/" + "post_" + tiempo

        if(!uri.equals("noImagen")){

            val bd = FirebaseStorage.getInstance().reference.child(direccionYNombreArchivo)
            bd.putFile(Uri.parse(uri)).addOnSuccessListener{
                val tareaUri = it.storage.downloadUrl
                while(!tareaUri.isSuccessful){}
                val descargaUri = tareaUri.result
                if(tareaUri.isSuccessful){
                    var resultados = HashMap<String, String>()
                    resultados.put("creador", usuario!!.uid)
                    resultados.put("titulo", titulo)
                    resultados.put("descripcion", descripcion)
                    resultados.put("tiempo", tiempo)
                    resultados.put("imagen", descargaUri.toString())

                    val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                    val referencia = baseDatos.getReference("Posts")

                    referencia.child(tiempo).setValue(resultados).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this , "Post publicado", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener{
                        progressDialog.dismiss()
                        Toast.makeText(this , "Error publicando el post...", Toast.LENGTH_SHORT).show()
                    }

                }else{
                    progressDialog.dismiss()
                    Toast.makeText(this , "Ha ocurrido algún error", Toast.LENGTH_SHORT).show()
                }
                }.addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this , it.message, Toast.LENGTH_SHORT).show()
                }
            }else{

                var resultados = HashMap<String, String>()
                resultados.put("creador", usuario!!.uid)
                resultados.put("titulo", titulo)
                resultados.put("descripcion", descripcion)
                resultados.put("tiempo", tiempo)
                resultados.put("imagen", "noImagen")

                val baseDatos = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                val referencia = baseDatos.getReference("Posts")

                referencia.child(tiempo).setValue(resultados).addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this , "Post publicado...", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this , "Error publicando el post...", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun mostrarDialogoCogerImagen() {
        val opcionesFoto = arrayOf("Cámara","Galería")
        val constructorFoto = AlertDialog.Builder(this)
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

    private fun cogerDeCamara() {
        var valores = ContentValues()
        valores.put(MediaStore.Images.Media.TITLE, "Foto temporal")
        valores.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion temporal")
        imagen_uri = contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,valores)!!
        var camaraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        camaraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imagen_uri)
        startActivityForResult(camaraIntent, CODIGO_COGER_IMAGEN_CAMARA)
    }

    private fun cogerDeGaleria() {
        var galeriaIntent = Intent(Intent.ACTION_PICK)
        galeriaIntent.setType("image/*")
        startActivityForResult(galeriaIntent, CODIGO_COGER_IMAGEN_GALERIA)
    }

    private fun comprobarPermisoAlmacenamiento(): Boolean{
        val resultado = this?.let { ContextCompat.checkSelfPermission(it,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED
        return resultado
    }

    private fun solicitarPermisoAlmacenamiento(){
        ActivityCompat.requestPermissions(this,permisosAlmacenamiento,CODIGO_ALMACENAMIENTO)
    }

    private fun comprobarPermisoCamara(): Boolean{
        val resultado1 = this?.let { ContextCompat.checkSelfPermission(it,android.Manifest.permission.CAMERA) } == PackageManager.PERMISSION_GRANTED
        val resultado2 = this?.let { ContextCompat.checkSelfPermission(it,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED
        return resultado1 && resultado2
    }

    private fun solicitarPermisoCamara(){
        ActivityCompat.requestPermissions(this,permisosCamara,CODIGO_CAMARA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CODIGO_CAMARA ->
                if(grantResults.size > 0){
                    val camaraAceptada = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val escribirAlmacenamientoAceptada = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if(camaraAceptada && escribirAlmacenamientoAceptada){
                        cogerDeCamara()
                    }else{
                        Toast.makeText(this , "Por favor, habilita los permisos de cámara y almacenamiento", Toast.LENGTH_SHORT).show()
                    }
                }
            CODIGO_ALMACENAMIENTO ->
                if(grantResults.size > 0){
                    val escribirAlmacenamientoAceptada = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if(escribirAlmacenamientoAceptada){
                        cogerDeGaleria()
                    }else{
                        Toast.makeText(this , "Por favor, habilita los permisos de almacenamiento", Toast.LENGTH_SHORT).show()
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
                postImagen.setImageURI(imagen_uri)
            }
            else if(requestCode == CODIGO_COGER_IMAGEN_CAMARA){
                postImagen.setImageURI(imagen_uri)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    //Función que comprueba el estado del usuario
    private fun comprobarEstadoUsuario(){
        val usuario = mAuth.currentUser
        if(usuario == null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }else{
            //correoTxt.setText(usuario.email)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        comprobarEstadoUsuario()
        super.onResume()
    }

    override fun onStart() {
        comprobarEstadoUsuario()
        super.onStart()
    }
}