package com.example.scoutconnections.adapters

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.AddPostActivity
import com.example.scoutconnections.R
import com.example.scoutconnections.ThereProfileActivity
import com.example.scoutconnections.models.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
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
        val pLikes = listPosts[position].likes

        val cal = Calendar.getInstance(Locale.ITALY)

        cal.timeInMillis = pTime!!.toLong()
        val time = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)

        holder.pLikes.text = pLikes.toString() + " Likes"
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
                        if (!pImage.equals("")) {
                            Picasso.get().load(uImage).into(holder.uImage)

                        }
                    } catch (e: Exception) {
                    }


                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })



        if(!pCreator.equals(user!!.uid)){
            holder.moreBtn.visibility = View.GONE
        }

        holder.moreBtn.setOnClickListener {
            showMoreOptions(holder.moreBtn, user!!.uid, pId, pImage)
        }

        holder.likeBtn.setOnClickListener {
            val likes = listPosts[position].likes
            mProcessLike = true
            val id = listPosts[position].pid
            referenceLikes.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(mProcessLike){
                        if(snapshot.child(id!!).hasChild(user.uid)){
                            referencePost.child(id).child("likes").setValue((likes!!-1))
                            referenceLikes.child(id).child(user.uid).removeValue()
                            mProcessLike = false
                        } else {
                            referencePost.child(id!!).child("likes").setValue((likes!!+1))
                            referenceLikes.child(id).child(user.uid).setValue("Liked")
                            mProcessLike = false
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
        holder.commentBtn.setOnClickListener {

        }
        holder.shareBtn.setOnClickListener {

        }
        holder.pLayout.setOnClickListener {
            val intent = Intent(context, ThereProfileActivity::class.java)
            intent.putExtra("uid", pCreator)
            context.startActivity(intent)
        }

    }

    private fun setLikes(holder: PostAdapter.MyHolder, pId: String?) {
        referenceLikes.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(pId!!).hasChild(user!!.uid)){
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
        popUpMenu.menu.add(Menu.NONE, 0, 0, context.getString(R.string.delete))
        popUpMenu.menu.add(Menu.NONE, 1, 0, context.getString(R.string.edit))
        popUpMenu.setOnMenuItemClickListener { p0 ->
            val id = p0!!.itemId
            if (id == 0) {
                beginDelete(pId, pImage)
            } else if (id == 1) {
                val intent = Intent(context, AddPostActivity::class.java)
                intent.putExtra("editId", pId)
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
                    val referenceLikes = db.getReference("Likes")
                    val query = referenceLikes.child(pId!!)
                    query.addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach {
                                it.ref.removeValue()
                            }

                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })
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
                    val referenceLikes = db.getReference("Likes")
                    val query = referenceLikes.child(pId!!)
                    query.addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach {
                                it.ref.removeValue()
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


    override fun getItemCount(): Int {
        return listPosts.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pImage: ImageView
        var pTitle: TextView
        var pDescription: TextView
        var pLikes: TextView
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