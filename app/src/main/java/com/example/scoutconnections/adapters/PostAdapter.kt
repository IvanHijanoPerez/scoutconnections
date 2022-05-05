package com.example.scoutconnections.adapters

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.*
import com.example.scoutconnections.R
import com.example.scoutconnections.models.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(var context: Context, var listPosts: List<PostModel>) :
    RecyclerView.Adapter<PostAdapter.MyHolder>() {

    var mProcessLike = false
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser

    val db =
        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    private val referenceUs = db.getReference("Users")
    val referencePost = db.getReference("Posts")
    val referenceLikes = db.getReference("Likes")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val pTitle = listPosts[position].title
        val pTime = listPosts[position].time
        val pImage = listPosts[position].image
        val pCreator = listPosts[position].creator
        val pDescription = listPosts[position].description
        val pId = listPosts[position].pid
        val pLikes = listPosts[position].nLikes
        val pComments = listPosts[position].nComments

        val cal = Calendar.getInstance(Locale.ITALY)

        cal.timeInMillis = pTime!!.toLong()
        val time = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

        holder.pLikes.text = pLikes.toString() + " " + context.getString(R.string.likes)
        holder.pComments.text = pComments.toString() + " " + context.getString(R.string.comments)
        holder.pTitle.text = pTitle
        holder.pDescription.text = pDescription
        holder.pTime.text = time

        if(pImage.equals("")){
            holder.pImage.visibility = View.GONE
        }else{
            holder.pImage.visibility = View.VISIBLE
            try {
                    Picasso.get().load(pImage).into(holder.pImage)
            } catch (e: Exception) {
            }
        }

        setLikes(holder, pId)



        val query = referenceUs.orderByChild("uid").equalTo(pCreator)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds: DataSnapshot in snapshot.children) {
                    val uName = ds.child("name").value.toString()
                    val uImage = ds.child("image").value.toString()

                    holder.uName.text = uName

                    try {
                        if (uImage != "") {
                            Picasso.get().load(uImage).into(holder.uImage)

                        }
                    } catch (e: Exception) {
                    }


                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
        

        holder.moreBtn.setOnClickListener {
            showMoreOptions(holder.moreBtn, pCreator!!, pId, pImage)
        }

        holder.likeBtn.setOnClickListener {
            val likes = listPosts[position].nLikes
            mProcessLike = true
            val id = listPosts[position].pid

            val dbRef = referencePost.child(id!!).child("Likes")

            dbRef.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(mProcessLike){
                        if(snapshot.hasChild(user!!.uid)){
                            referencePost.child(id).child("nLikes").setValue((likes!!-1))
                            dbRef.child(user.uid).removeValue()
                            mProcessLike = false
                        } else {
                            referencePost.child(id!!).child("nLikes").setValue((likes!!+1))
                            dbRef.child(user.uid).setValue("Liked")
                            mProcessLike = false
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
        holder.commentBtn.setOnClickListener {
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("postId", pId)
            context.startActivity(intent)
        }
        holder.shareBtn.setOnClickListener {
            val bitmapDrawable = holder.pImage.drawable as? BitmapDrawable
            if (bitmapDrawable == null) {
                shareText(pTitle, pDescription)
            } else {
                val bitmap = bitmapDrawable.bitmap
                shareImageText(pTitle, pDescription, bitmap)
            }
        }
        holder.pLayout.setOnClickListener {
            val intent = Intent(context, ThereProfileActivity::class.java)
            intent.putExtra("uid", pCreator)
            context.startActivity(intent)
        }

        holder.pLikes.setOnClickListener {
            val intent = Intent(context, PostLikedByActivity::class.java)
            intent.putExtra("postId", pId)
            context.startActivity(intent)
        }

    }

    private fun shareImageText(pTitle: String?, pDescription: String?, bitmap: Bitmap?) {
        val body = pTitle + "\n" + pDescription
        val uri = saveImageShare(bitmap) as? Uri

        val sIntent = Intent(Intent.ACTION_SEND)
        sIntent.type = "image/png"
        sIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.subject_here))
        sIntent.putExtra(Intent.EXTRA_TEXT, body)
        context.startActivity(Intent.createChooser(sIntent, context.getString(R.string.share_via)))
    }

    private fun saveImageShare(bitmap: Bitmap?): Any {
        val imageFolder = File(context.cacheDir, "images")
        var uri = null as? Uri
        try{
            imageFolder.mkdirs()
            val file = File(imageFolder, "shared_image.png")

            val stream = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(context, "com.example.scoutconnections.fileprovider", file)
        }
        catch (e: Exception){
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
        return uri!!
    }

    private fun shareText(pTitle: String?, pDescription: String?) {
        val body = pTitle + "\n" + pDescription
        val sIntent = Intent(Intent.ACTION_SEND)
        sIntent.type = "text/plain"
        sIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.subject_here))
        sIntent.putExtra(Intent.EXTRA_TEXT, body)
        context.startActivity(Intent.createChooser(sIntent, context.getString(R.string.share_via)))
    }

    private fun setLikes(holder: PostAdapter.MyHolder, pId: String?) {
        referencePost.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(pId!!).child("Likes").hasChild(user!!.uid)){
                    holder.likeBtn.text = context.getString(R.string.liked)
                } else {
                    holder.likeBtn.text = context.getString(R.string.like)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun showMoreOptions(moreBtn: ImageButton, uid: String, pId: String?, pImage: String?) {
        val popUpMenu = PopupMenu(context, moreBtn, Gravity.END)
        if(uid == user!!.uid){
            popUpMenu.menu.add(Menu.NONE, 0, 0, context.getString(R.string.delete))
            popUpMenu.menu.add(Menu.NONE, 1, 0, context.getString(R.string.edit))
        }
        popUpMenu.menu.add(Menu.NONE, 2, 0, context.getString(R.string.view_details))
        popUpMenu.setOnMenuItemClickListener { p0 ->
            val id = p0!!.itemId

            if (id == 0) {

                val customDialog = AlertDialog.Builder(context)
                customDialog.setTitle(context.getString(R.string.delete_comment))
                customDialog.setMessage(context.getString(R.string.sure_delete_comment))

                customDialog.setPositiveButton(
                    context.getString(R.string.delete),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            beginDelete(pId, pImage)
                        }

                    })

                customDialog.setNegativeButton(
                    context.getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            p0?.dismiss()
                        }
                    })

                customDialog.create().show()

            } else if (id == 1) {
                val intent = Intent(context, AddPostActivity::class.java)
                intent.putExtra("editId", pId)
                context.startActivity(intent)
            } else if (id == 2) {
                val intent = Intent(context, PostDetailActivity::class.java)
                intent.putExtra("postId", pId)
                context.startActivity(intent)
            }
            false
        }
        popUpMenu.show()
    }

    private fun beginDelete(pId: String?, pImage: String?) {
        if(pImage.equals("")){
            deleteWithoutImage(pId)
        }else{
            deleteWithImage(pId, pImage)
        }
    }

    private fun deleteWithImage(pId: String?, pImage: String?) {
        val pd = ProgressDialog(context)
        pd.setMessage(context.getString(R.string.deleting))
        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pImage!!)
        ref.delete().addOnSuccessListener {
            val db =
                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
            val reference = db.getReference("Posts")
            val query = reference.orderByChild("pid").equalTo(pId)
            query.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        it.ref.removeValue()
                    }
                    Toast.makeText(context, context.getString(R.string.deleted_post), Toast.LENGTH_SHORT).show()
                    pd.dismiss()
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
        .addOnFailureListener {
            pd.dismiss()
            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun deleteWithoutImage(pId: String?) {
        val pd = ProgressDialog(context)
        pd.setMessage(context.getString(R.string.deleting))
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Posts")
        val query = reference.orderByChild("pid").equalTo(pId)
        query.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    it.ref.removeValue()
                    Toast.makeText(context, context.getString(R.string.deleted_post), Toast.LENGTH_SHORT).show()
                    pd.dismiss()

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

    }


    override fun getItemCount(): Int {
        return listPosts.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pImage: ImageView
        var pTitle: TextView
        var pDescription: TextView
        var pLikes: TextView
        var pComments: TextView
        var pTime: TextView

        var uImage: ImageView
        var uName: TextView

        var moreBtn: ImageButton
        var likeBtn: Button
        var commentBtn: Button
        var shareBtn: Button
        var pLayout: LinearLayout

        init {
            pImage = itemView.findViewById(R.id.im_post)
            pTitle = itemView.findViewById(R.id.tit_post)
            pDescription = itemView.findViewById(R.id.desc_post)
            pLikes = itemView.findViewById(R.id.likes_post)
            pComments = itemView.findViewById(R.id.comments_post)
            pTime = itemView.findViewById(R.id.time_post)
            uImage = itemView.findViewById(R.id.image_creator)
            uName = itemView.findViewById(R.id.name_creator)
            moreBtn = itemView.findViewById(R.id.more_btn)
            likeBtn = itemView.findViewById(R.id.like_btn)
            commentBtn = itemView.findViewById(R.id.comment_btn)
            shareBtn = itemView.findViewById(R.id.share_btn)
            pLayout = itemView.findViewById(R.id.profile_layout)
        }
    }
}