package com.example.scoutconnections

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoutconnections.adapters.EventAdapter
import com.example.scoutconnections.models.EventModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*


class EventsFragment(dashboardActivity: DashboardActivity) : Fragment() {
    val dashboardActivity = dashboardActivity
    lateinit var recyclerView: RecyclerView
    val mAuth = FirebaseAuth.getInstance()
    val user = mAuth.currentUser
    private lateinit var timeEvent: EditText
    private lateinit var titleEvent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_events, container, false)

        recyclerView = view.findViewById(R.id.group_chatlist_recycler_view)
        titleEvent = view.findViewById<TextView>(R.id.events_tw)
        timeEvent = view.findViewById<EditText>(R.id.time_selected_event)
        val addTime = view.findViewById<Button>(R.id.select_date)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(dashboardActivity)

        addTime.setOnClickListener {
            showDateDialog()
        }

        loadNextEvents()

        return view
    }

    private fun loadNextEvents() {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Events")
        titleEvent.text = getString(R.string.next_events)

        val time = System.currentTimeMillis()

        var listEvents: MutableList<EventModel> = ArrayList()

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listEvents.clear()
                dataSnapshot.children.forEach {
                    val eventModel = it.getValue(EventModel::class.java)
                    val t = eventModel!!.tEvent!!.toLong()

                    if(t > time){
                        listEvents.add(eventModel!!)

                    }
                    listEvents.sortBy { it.tEvent }

                    val eventAdapters = EventAdapter(dashboardActivity, listEvents)
                    recyclerView.adapter = eventAdapters


                }


            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun showDateDialog() {
        val calendar = Calendar.getInstance()

        val dateListener = DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
            calendar.set(Calendar.YEAR, i)
            calendar.set(Calendar.MONTH, i2)
            calendar.set(Calendar.DAY_OF_MONTH, i3)

            val cal = Calendar.getInstance(Locale.ITALY)

            cal.timeInMillis = calendar.timeInMillis

            loadEventsDate(cal.timeInMillis)

        }
        val datePicker = activity?.let {
            DatePickerDialog(
                it, dateListener, calendar.get(Calendar.YEAR), calendar.get(
                    Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }

        datePicker!!.datePicker.firstDayOfWeek = Calendar.MONDAY
        datePicker.show()
    }

    private fun loadEventsDate(timeInMillis: Long) {
        val db = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Events")
        val dateSelected = SimpleDateFormat("dd/MM/yyyy").format(timeInMillis)
        timeEvent.setText(dateSelected)
        titleEvent.text = getString(R.string.events) + " " + getString(R.string.on) + " " + dateSelected


        var listEvents: MutableList<EventModel> = ArrayList()

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listEvents.clear()
                dataSnapshot.children.forEach {
                    val eventModel = it.getValue(EventModel::class.java)
                    val t = eventModel!!.tEvent!!.toLong()
                    val dateEM = SimpleDateFormat("dd/MM/yyyy").format(t)

                    if(dateSelected == dateEM){
                        listEvents.add(eventModel!!)

                    }
                    listEvents.sortBy { it.tEvent }

                    val eventAdapters = EventAdapter(dashboardActivity, listEvents)
                    recyclerView.adapter = eventAdapters


                }


            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main_menu, menu)

        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_users).isVisible = false
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_create_group).isVisible = false
        menu.findItem(R.id.action_add_participant_group).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        val db =
            FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app")
        val reference = db.getReference("Users")

        val query = reference.orderByChild("email").equalTo(user?.email)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (ds: DataSnapshot in snapshot.children) {

                        val monitor = ds.child("monitor").value

                        if (monitor == false) {
                            menu.findItem(R.id.action_add_event).isVisible = false
                        }
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_add_event) {
            startActivity(Intent(activity, EventAddActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

}