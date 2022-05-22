package com.example.scoutconnections

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.ChatListAdapter
import com.example.scoutconnections.adapters.GroupChatListAdapter
import com.example.scoutconnections.models.ChatListModel
import com.example.scoutconnections.models.ChatModel
import com.example.scoutconnections.models.GroupModel
import com.example.scoutconnections.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SocialFragment(dashboardActivity: DashboardActivity) : Fragment() {


    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    val dashboardActivity = dashboardActivity
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var adapterChatList: ChatListAdapter
    private lateinit var adapterGroupChatList: GroupChatListAdapter
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

        val view = inflater.inflate(R.layout.fragment_social, container, false)



        chatsRecyclerView = view.findViewById(R.id.chatlist_recycler_view)
        chatsRecyclerView.setHasFixedSize(true)
        chatsRecyclerView.layoutManager = LinearLayoutManager(dashboardActivity)

        groupsRecyclerView = view.findViewById(R.id.group_chatlist_recycler_view)
        groupsRecyclerView.setHasFixedSize(true)
        groupsRecyclerView.layoutManager = LinearLayoutManager(dashboardActivity)

        val reference = db.getReference("ChatList").child(user!!.uid)
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    val cL = ds.getValue(ChatListModel::class.java)
                    chatList.add(cL!!)
                }
                loadChats()
                loadGroupChats()
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
                    listUsers.sortBy { it.name }
                    adapterChatList = ChatListAdapter(dashboardActivity, listUsers)
                    chatsRecyclerView.adapter = adapterChatList
                    for (i in 0 until listUsers.size) {
                        lastMessage(listUsers[i].uid)
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun loadGroupChats() {
        val listGroups: MutableList<GroupModel> = ArrayList()
        val reference = db.getReference("Groups")
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listGroups.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    if (ds.child("Participants").child(user!!.uid).exists()){
                        val model = ds.getValue(GroupModel::class.java)
                        listGroups.add(model!!)
                    }
                    listGroups.sortBy { it.title }
                    adapterGroupChatList = GroupChatListAdapter(dashboardActivity, listGroups)
                    groupsRecyclerView.adapter = adapterGroupChatList


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

    private fun searchGroups(query: String) {
        val listGroups: MutableList<GroupModel> = ArrayList()
        val reference = db.getReference("Groups")
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listGroups.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    if (ds.child("Participants").child(user!!.uid).exists()){
                        if(ds.child("title").toString().toLowerCase().contains(query.toLowerCase())){
                            val model = ds.getValue(GroupModel::class.java)
                            listGroups.add(model!!)
                        }

                    }
                    listGroups.sortBy { it.title }
                    adapterGroupChatList = GroupChatListAdapter(dashboardActivity, listGroups)
                    groupsRecyclerView.adapter = adapterGroupChatList


                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun searchUsers(query: String) {
        val listUsers: MutableList<UserModel> = ArrayList()
        val reference = db.getReference("Users")
        reference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listUsers.clear()
                for (ds: DataSnapshot in snapshot.children) {
                    val us = ds.getValue(UserModel::class.java)
                    for (cl: ChatListModel in chatList) {
                        if(us!!.uid != null && us.uid.equals(cl.uid)){
                            if(us.name.toString().toLowerCase().contains(query.toLowerCase()) || us.email.toString().toLowerCase().contains(query.toLowerCase())){
                                listUsers.add(us)
                                break
                            }

                        }
                    }
                    listUsers.sortBy { it.name }
                    adapterChatList = ChatListAdapter(dashboardActivity, listUsers)
                    chatsRecyclerView.adapter = adapterChatList
                    for (i in 0 until listUsers.size) {
                        lastMessage(listUsers[i].uid)
                    }

                }
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
        menuInflater.inflate(R.menu.main_menu, menu)

        val item = menu?.findItem(R.id.action_search)
        val searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (p0 != null) {
                    if (!TextUtils.isEmpty(p0.trim())) {
                        searchUsers(p0)
                        searchGroups(p0)
                    }
                } else {
                    loadChats()
                    loadGroupChats()
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (p0 != null) {
                    if (!TextUtils.isEmpty(p0.trim())) {
                        searchUsers(p0)
                        searchGroups(p0)
                    }
                } else {
                    loadChats()
                    loadGroupChats()
                }
                return false
            }

        })
        searchView.setOnCloseListener(SearchView.OnCloseListener {
            loadChats()
            loadGroupChats()
            false
        })


        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false
        menu.findItem(R.id.action_add_participant_group).isVisible = false
        menu.findItem(R.id.action_add_event).isVisible = false

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
        }  else if (id == R.id.action_create_group) {
            startActivity(Intent(activity, GroupCreateActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }


}