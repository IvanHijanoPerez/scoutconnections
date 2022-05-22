package com.example.scoutconnections.adapters

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.EventAddActivity
import com.example.scoutconnections.PostAddActivity
import com.example.scoutconnections.R
import com.example.scoutconnections.ThereProfileActivity
import com.example.scoutconnections.models.EventModel
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

class EventAdapter(
    var context: Context,
    var listEvents: List<EventModel>
) :
    RecyclerView.Adapter<EventAdapter.MyHolder>() {

    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    val db =
        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    private val referenceUs = db.getReference("Users")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_events, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val eTitle = listEvents[position].title
        val eTime = listEvents[position].time
        val eCreator = listEvents[position].creator
        val eImage = listEvents[position].image
        val eDescription = listEvents[position].description
        val eTEvent = listEvents[position].tEvent
        val eId = listEvents[position].eid

        val cal = Calendar.getInstance(Locale.ITALY)
        cal.timeInMillis = eTEvent!!.toLong()
        val time = SimpleDateFormat("HH:mm dd/MM/yyyy").format(cal.timeInMillis)


        holder.eTitle.text = eTitle
        holder.eDescription.text = eDescription
        holder.eTEvent.text = time

        if(eImage.equals("")){
            holder.eImage.visibility = View.GONE
        }else{
            holder.eImage.visibility = View.VISIBLE
            try {
                Picasso.get().load(eImage).into(holder.eImage)
            } catch (e: Exception) {
            }
        }

        val query = referenceUs.orderByChild("uid").equalTo(eCreator)

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
            showMoreOptions(holder.moreBtn, eCreator!!, eId, eImage)
        }

        holder.pLayout.setOnClickListener {
            val intent = Intent(context, ThereProfileActivity::class.java)
            intent.putExtra("uid", eCreator)
            context.startActivity(intent)
        }
    }

    private fun showMoreOptions(moreBtn: ImageButton, uid: String, eId: String?, eImage: String?) {
        val popUpMenu = PopupMenu(context, moreBtn, Gravity.END)
        if(uid == user!!.uid){
            popUpMenu.menu.add(Menu.NONE, 0, 0, context.getString(R.string.delete))
            popUpMenu.menu.add(Menu.NONE, 1, 0, context.getString(R.string.edit))
        }

        popUpMenu.setOnMenuItemClickListener { p0 ->
            val id = p0!!.itemId

            if (id == 0) {

                val customDialog = AlertDialog.Builder(context)
                customDialog.setTitle(context.getString(R.string.delete_event))
                customDialog.setMessage(context.getString(R.string.sure_delete_event))

                customDialog.setPositiveButton(
                    context.getString(R.string.delete),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            beginDelete(eId, eImage)
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
                val intent = Intent(context, EventAddActivity::class.java)
                intent.putExtra("editId", eId)
                context.startActivity(intent)
            }
            false
        }
        popUpMenu.show()
    }

    private fun beginDelete(eId: String?, eImage: String?) {
        if(eImage.equals("")){
            deleteWithoutImage(eId)
        }else{
            deleteWithImage(eId, eImage)
        }
    }

    private fun deleteWithImage(eId: String?, eImage: String?) {
        val pd = ProgressDialog(context)
        pd.setMessage(context.getString(R.string.deleting))
        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(eImage!!)
        ref.delete().addOnSuccessListener {
            val db =
                FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
            val reference = db.getReference("Events")
            val query = reference.orderByChild("eid").equalTo(eId)
            query.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        it.ref.removeValue()
                    }
                    Toast.makeText(context, context.getString(R.string.deleted_event), Toast.LENGTH_SHORT).show()
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

    private fun deleteWithoutImage(eId: String?) {
        val pd = ProgressDialog(context)
        pd.setMessage(context.getString(R.string.deleting))
        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Events")
        val query = reference.orderByChild("eid").equalTo(eId)
        query.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    it.ref.removeValue()
                    Toast.makeText(context, context.getString(R.string.deleted_event), Toast.LENGTH_SHORT).show()
                    pd.dismiss()

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

    }

    override fun getItemCount(): Int {
        return listEvents.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var eImage: ImageView
        var eTitle: TextView
        var eDescription: TextView

        var eTEvent: TextView

        var uImage: ImageView
        var uName: TextView

        var moreBtn: ImageButton

        var pLayout: LinearLayout

        init {
            eImage = itemView.findViewById(R.id.im_event)
            eTitle = itemView.findViewById(R.id.tit_event)
            eDescription = itemView.findViewById(R.id.desc_event)
            eTEvent = itemView.findViewById(R.id.time_event)
            uImage = itemView.findViewById(R.id.image_creator)
            uName = itemView.findViewById(R.id.name_creator)
            moreBtn = itemView.findViewById(R.id.more_event_btn)
            pLayout = itemView.findViewById(R.id.profile_layout)
        }
    }
}