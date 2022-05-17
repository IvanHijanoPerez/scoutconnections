package com.example.scoutconnections

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.CommentAdapter
import com.example.scoutconnections.models.CommentModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class PostDetailActivity : AppCompatActivity() {

    private lateinit var postId: String
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    private lateinit var pImage: ImageView
    private lateinit var pTitle: TextView
    private lateinit var pDescription: TextView
    private lateinit var pLikes: TextView
    private lateinit var pTime: TextView
    private lateinit var pComments: TextView

    private lateinit var uImage: ImageView
    private lateinit var uName: TextView

    private lateinit var likeBtn: Button
    private lateinit var moreBtn: ImageButton

    var mProcessComment = false
    var mProcessLike = false

    private lateinit var pComment: EditText

    private lateinit var progressDialog: ProgressDialog

    private lateinit var likesPost: String
    private lateinit var creatorPost: String
    private lateinit var imagePost: String

    private lateinit var recyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        postId = intent.getStringExtra("postId").toString()

        pImage = findViewById<ImageView>(R.id.im_post)
        pTitle = findViewById<TextView>(R.id.tit_post)
        pDescription = findViewById<TextView>(R.id.desc_post)
        pLikes = findViewById<TextView>(R.id.likes_post)
        pComments = findViewById<TextView>(R.id.comments_post)
        pTime = findViewById<TextView>(R.id.time_post)
        uImage = findViewById<ImageView>(R.id.image_creator)
        uName = findViewById<TextView>(R.id.name_creator)
        moreBtn = findViewById<ImageButton>(R.id.more_btn)
        likeBtn = findViewById<Button>(R.id.like_btn)
        val shareBtn = findViewById<Button>(R.id.share_btn)
        val pLayout = findViewById<LinearLayout>(R.id.profile_layout)

        recyclerView = findViewById<RecyclerView>(R.id.comments_recycler_view)

        pComment = findViewById<EditText>(R.id.comment_post)
        val sendBtn = findViewById<ImageButton>(R.id.send_comment_btn)

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.post_detail)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        loadPostInfo()

        setLike()

        checkUserStatus()

        sendBtn.setOnClickListener {
            postComment()
        }

        likeBtn.setOnClickListener {
            likePost()
        }

        pLayout.setOnClickListener {
            val intent = Intent(this, ThereProfileActivity::class.java)
            intent.putExtra("uid", creatorPost)
            startActivity(intent)
        }

        moreBtn.setOnClickListener {
            showMoreOptions()
        }

        pLikes.setOnClickListener {
            val intent = Intent(this@PostDetailActivity, PostLikedByActivity::class.java)
            intent.putExtra("postId", postId)
            this.startActivity(intent)
        }

        shareBtn.setOnClickListener{
            val title = pTitle.text.toString().trim()
            val description = pDescription.text.toString().trim()
            val bitmapDrawable = pImage.drawable as? BitmapDrawable
            if (bitmapDrawable == null) {
                shareText(title, description)
            } else {
                val bitmap = bitmapDrawable.bitmap
                shareImageText(title, description, bitmap)
            }
        }

        loadComments()

    }

    private fun shareImageText(pTitle: String?, pDescription: String?, bitmap: Bitmap?) {
        val body = pTitle + "\n" + pDescription
        val uri = saveImageShare(bitmap) as? Uri

        val sIntent = Intent(Intent.ACTION_SEND)
        sIntent.type = "image/png"
        sIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_here))
        sIntent.putExtra(Intent.EXTRA_TEXT, body)
        startActivity(Intent.createChooser(sIntent, getString(R.string.share_via)))
    }

    private fun saveImageShare(bitmap: Bitmap?): Any {
        val imageFolder = File(cacheDir, "images")
        var uri = null as? Uri
        try{
            imageFolder.mkdirs()
            val file = File(imageFolder, "shared_image.png")

            val stream = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(this, "com.example.scoutconnections.fileprovider", file)
        }
        catch (e: Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        return uri!!
    }

    private fun shareText(pTitle: String?, pDescription: String?) {
        val body = pTitle + "\n" + pDescription
        val sIntent = Intent(Intent.ACTION_SEND)
        sIntent.type = "text/plain"
        sIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_here))
        sIntent.putExtra(Intent.EXTRA_TEXT, body)
        startActivity(Intent.createChooser(sIntent, getString(R.string.share_via)))
    }



    private fun loadComments() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        recyclerView.layoutManager = layoutManager

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Posts").child(postId).child("Comments")

        var listComments: MutableList<CommentModel> = ArrayList()

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listComments.clear()
                dataSnapshot.children.forEach {
                    val commentModel = it.getValue(CommentModel::class.java)
                    listComments.add(commentModel!!)

                }
                val postAdapters = CommentAdapter(this@PostDetailActivity, listComments, postId)

                recyclerView.adapter = postAdapters
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun showMoreOptions() {
        val popUpMenu = PopupMenu(this, moreBtn, Gravity.END)
        if(creatorPost == user!!.uid){
            popUpMenu.menu.add(Menu.NONE, 0, 0, getString(R.string.delete))
            popUpMenu.menu.add(Menu.NONE, 1, 0, getString(R.string.edit))
        }
        popUpMenu.setOnMenuItemClickListener { p0 ->
            val id = p0!!.itemId
            if (id == 0) {

                val customDialog = AlertDialog.Builder(this)
                customDialog.setTitle(getString(R.string.delete_post))
                customDialog.setMessage(getString(R.string.sure_delete_post))

                customDialog.setPositiveButton(
                    getString(R.string.delete),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            beginDelete()
                        }

                    })

                customDialog.setNegativeButton(
                    getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            p0?.dismiss()
                        }
                    })

                customDialog.create().show()

            } else if (id == 1) {
                val intent = Intent(this, PostAddActivity::class.java)
                intent.putExtra("editId", postId)
                this.startActivity(intent)
            } else if (id == 2) {
                val intent = Intent(this, PostDetailActivity::class.java)
                intent.putExtra("postId", postId)
                this.startActivity(intent)
            }
            false
        }
        popUpMenu.show()
    }

    private fun beginDelete() {
        if(imagePost == ""){
            deleteWithoutImage()
        }else{
            deleteWithImage()
        }
    }

    private fun deleteWithImage() {
        val pd = ProgressDialog(this)
        pd.setMessage(getString(R.string.deleting))
        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imagePost)
        ref.delete().addOnSuccessListener {
            val db =
                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
            val reference = db.getReference("Posts")
            val query = reference.orderByChild("pid").equalTo(postId)
            query.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        it.ref.removeValue()
                    }
                    Toast.makeText(this@PostDetailActivity, getString(R.string.deleted_post), Toast.LENGTH_SHORT).show()
                    pd.dismiss()
                    startActivity(Intent(this@PostDetailActivity, DashboardActivity::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
            .addOnFailureListener {
                pd.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteWithoutImage() {
        val pd = ProgressDialog(this)
        pd.setMessage(getString(R.string.deleting))
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Posts")
        val query = reference.orderByChild("pid").equalTo(postId)
        query.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    it.ref.removeValue()
                    Toast.makeText(this@PostDetailActivity, getString(R.string.deleted_post), Toast.LENGTH_SHORT).show()
                    pd.dismiss()
                    startActivity(Intent(this@PostDetailActivity, DashboardActivity::class.java))
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun setLike() {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referencePost = db.getReference("Posts")
        referencePost.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(postId).child("Likes").hasChild(user!!.uid)){
                    likeBtn.text = getString(R.string.liked)
                } else {
                    likeBtn.text = getString(R.string.like)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }

    private fun likePost() {
        mProcessLike = true
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referencePost = db.getReference("Posts")

        val dbRef = referencePost.child(postId).child("Likes")

        dbRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(mProcessLike){
                    if(snapshot.hasChild(user!!.uid)){
                        referencePost.child(postId).child("nLikes").setValue((likesPost.toInt() - 1))
                        dbRef.child(user.uid).removeValue()
                        mProcessLike = false
                    } else {
                        referencePost.child(postId).child("nLikes").setValue((likesPost.toInt() + 1))
                        dbRef.child(user.uid).setValue("Liked")
                        mProcessLike = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun postComment() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.adding_comment))

        val comment = pComment.text.toString().trim()
        if (comment.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.comment_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Posts").child(postId).child("Comments")
        val time = System.currentTimeMillis().toString()

        var hash = HashMap<String, Any>()
        hash["cid"] = time
        hash["comment"] = comment
        hash["time"] = time
        hash["creator"] = user!!.uid

        db.child(time).setValue(hash).addOnSuccessListener{
            progressDialog.dismiss()
            Toast.makeText(
                this,
                getString(R.string.comment_added),
                Toast.LENGTH_SHORT
            ).show()
            pComment.setText("")
            updateCommentCount()

        }.addOnFailureListener { p0 ->
            progressDialog.dismiss()
            Toast.makeText(
                this@PostDetailActivity,
                p0.message,
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun updateCommentCount() {
        mProcessComment = true
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Posts").child(postId)
        db.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(mProcessComment){
                    val comments = snapshot.child("nComments").value
                    db.child("nComments").setValue(comments.toString().toInt() + 1)
                    mProcessComment = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadPostInfo() {
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val referencePost = db.getReference("Posts")
        val referenceUs = db.getReference("Users")

        val query = referencePost.orderByChild("pid").equalTo(postId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val title = ds.child("title").value.toString()
                    val description = ds.child("description").value.toString()
                    val likes = ds.child("nLikes").value.toString()
                    likesPost = likes
                    val comments = ds.child("nComments").value.toString()
                    val time = ds.child("time").value.toString()
                    val image = ds.child("image").value.toString()
                    imagePost = image
                    val creator = ds.child("creator").value.toString()
                    creatorPost = creator

                    val cal = Calendar.getInstance(Locale.ITALY)
                    cal.timeInMillis = time!!.toLong()
                    val postTime = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

                    pTitle.text = title
                    pDescription.text = description
                    pLikes.text = likes + " " + getString(R.string.likes)
                    pComments.text = comments + " " + getString(R.string.comments)
                    pTime.text = postTime

                    if(image == ""){
                        pImage.visibility = View.GONE
                    }else{
                        pImage.visibility = View.VISIBLE
                        try {
                            Picasso.get().load(image).into(pImage)
                        } catch (e: Exception) {
                        }
                    }

                    val queryUs = referenceUs.orderByChild("uid").equalTo(creator)

                    queryUs.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (ds: DataSnapshot in snapshot.children) {
                                val usName = ds.child("name").value.toString()
                                val usImage = ds.child("image").value.toString()

                                uName.text = usName

                                try {
                                    if (usImage != "") {
                                        Picasso.get().load(usImage).into(uImage)

                                    }
                                } catch (e: Exception) {
                                }


                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu!!.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_users).isVisible = false
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false
        menu.findItem(R.id.action_create_group).isVisible = false
        menu.findItem(R.id.action_add_participant_group).isVisible = false
        menu.findItem(R.id.action_add_event).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}