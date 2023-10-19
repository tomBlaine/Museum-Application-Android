package com.example.a306cw

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage


class MainActivity : AppCompatActivity() {

    var storage = FirebaseStorage.getInstance()
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var searchView: SearchView
    var isCurator: Boolean = false
    var list = ArrayList<ArtefactModel>()
    var searchList = ArrayList<ArtefactModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(getString(R.string.firebase_instance))
        searchView = findViewById<SearchView>(R.id.searchBar)
        val currentUser = mAuth.currentUser

        if(currentUser != null && currentUser?.isAnonymous == false){
            getUserPriv(currentUser.uid)
        }

        supportActionBar?.apply {
            title = "Artefact collection"
        }

        val refreshFilter = IntentFilter("refreshActivity1")
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                getNumOfArtefacts()
            }
        }, refreshFilter)


        getNumOfArtefacts()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_logout -> {
            mAuth.signOut()
            val myIntent = Intent(this, LoginActivity::class.java)
            startActivity(myIntent)
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    fun filter(text: String) {
        searchList.clear()

        if (text.isEmpty()) {
            searchList.addAll(list)
            searchList.sortBy { list -> list.modelYear}
        } else {
            for (item in list) {
                if (item.getName().lowercase().contains(text.lowercase())) {
                    searchList.add(item)
                }
            }
        }


        //sendRefreshBroadcast(context)
    }

    private fun getNumOfArtefacts() {
        val artefactsRef = database.getReference("Artefact")

        artefactsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val numArtefacts = dataSnapshot.childrenCount
                Log.d("TAG", "Number of artefacts: $numArtefacts")
                populateList(numArtefacts.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", "Failed to read value: ${error.toException()}")
            }
        })
    }

    private fun instantiateRecycler(imageModelArrayList : ArrayList<ArtefactModel>)
    {
        val recyclerView = findViewById<View>(R.id.menuRecyclerView) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        var mAdapter = ArtefactAdapter(imageModelArrayList, isCurator)
        recyclerView.adapter = mAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                mAdapter = ArtefactAdapter(list, isCurator)
                recyclerView.adapter = mAdapter
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                filter(newText)
                mAdapter = ArtefactAdapter(searchList, isCurator)
                recyclerView.adapter = mAdapter
                return false
            }
        })


    }



    private fun getUserPriv(userID: String) {
        val artefactRef = database.getReference("User/$userID/priv")

        artefactRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val priv = snapshot.getValue<String>()
                if(priv!=null){
                    if(priv.toInt()>1){
                        isCurator = true
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase database", "error getting user priv")
            }
        })
    }

    private fun populateList(numOfArtefacts : Int) {
        list.clear()

        val artefactRef = database.getReference("Artefact")
        var count: Int = 0
        artefactRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { artefactSnapshot ->
                    count+=1
                    val imageModel = ArtefactModel()
                    val storageRef = storage.reference.child("Images/${artefactSnapshot.key}/0")
                    imageModel.setName(artefactSnapshot.child("name").getValue(String::class.java).toString())
                    imageModel.setYear(artefactSnapshot.child("year").getValue(Long::class.java).toString())
                    imageModel.setUID(artefactSnapshot.key)

                    storageRef.getDownloadUrl().addOnSuccessListener { uri ->
                        Log.e("Firebase download", "got uri")
                        Log.e("Firebase download", uri.toString())
                        imageModel.setImage(uri.toString())
                        list.add(imageModel)
                        Log.e("test","count = $count, num of artefacts= $numOfArtefacts")
                        if(count==numOfArtefacts){
                            list.sortBy { list -> list.modelYear}
                            instantiateRecycler(list)
                        }

                    }.addOnFailureListener {
                        Log.e("Firebase download", "error getting image")
                        imageModel.setImage("")
                        list.add(imageModel)
                        if(count==numOfArtefacts){
                            list.sortBy { list -> list.modelYear}
                            instantiateRecycler(list)
                        }

                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                println("Failed to read value: ${error.toException()}")
            }
        })

    }




}