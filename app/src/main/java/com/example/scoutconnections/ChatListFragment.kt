package com.example.scoutconnections

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.ChatListAdapter
import com.example.scoutconnections.models.ChatListModel
import com.example.scoutconnections.models.ChatModel
import com.example.scoutconnections.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ChatListFragment : Fragment() {

    val mAuth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterChatList: ChatListAdapter
    val db =
        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
    var chatList: MutableList<ChatListModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val user = mAuth.currentUser
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)



        recyclerView = view.findViewById(R.id.chatlist_recycler_view)
        val reference = db.getReference("ChatList").child(user!!.uid)
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    val cL = ds.getValue(ChatListModel::class.java)
                    chatList.add(cL!!)
                }
                loadChats()
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        return view
    }

    private fun loadChats() {
        val listUsers: MutableList<UserModel> = ArrayList()
        val reference = db.getReference("Users")
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listUsers.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    val us = ds.getValue(UserModel::class.java)
                    for (cl: ChatListModel in chatList) {
                        if(us!!.uid != null && us.uid.equals(cl.uid)){
                            listUsers.add(us)
                            break
                        }
                    }
                    adapterChatList = ChatListAdapter(Scouts.getContext()!!, listUsers)
                    recyclerView.adapter = adapterChatList
                    for (i in 0 until listUsers.size) {
                        lastMessage(listUsers[i].uid)
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun lastMessage(uid: String?) {
        val user = mAuth.currentUser
        val reference = db.getReference("Chats")
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var lM = "default"
                var lMt = "default"
                for (ds: DataSnapshot in snapshot.children) {
                    val chat = ds.getValue(ChatModel::class.java)
                    if (chat == null) {
                        continue
                    }
                    val sender = chat.sender
                    val receiver = chat.receiver
                    if (sender == null || receiver == null) {
                        continue
                    }
                    if ((chat.receiver.equals(user!!.uid) && chat.sender.equals(uid)) || (chat.sender.equals(user!!.uid) && chat.receiver.equals(uid))){
                        if(chat.type.equals("image")){
                            lM = Scouts.getContext()!!.getString(R.string.image)
                        } else {
                            lM = chat.message!!
                        }
                        lMt = chat.time!!
                    }
                }
                adapterChatList.setLastMessageMap(uid!!,lM, lMt)
                adapterChatList.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
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
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        super.onCreateOptionsMenu(menu, menuInflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            mAuth.signOut()
            checkUserStatus()
        } else if (id == R.id.action_users) {
            /*var actionBar = (activity as AppCompatActivity?)!!.supportActionBar
            actionBar?.setTitle("Usuarios")
            val fragment = UsuariosFragment()
            val ft = (activity as AppCompatActivity?)!!.supportFragmentManager.beginTransaction()
            ft.replace(R.id.contenido,fragment)
            ft.commit()*/
            startActivity(Intent(activity, UsersActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }


}