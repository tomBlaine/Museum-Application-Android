package com.example.a306cw

import ImagePagerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage


class ArtefactActivity : AppCompatActivity() {

    lateinit var artefactText : TextView
    lateinit var artefactTitle : TextView
    private lateinit var commentTxtEdit : EditText
    private lateinit var viewPager: ViewPager
    private lateinit var dots: Array<ImageView>
    private lateinit var dotIndicator: LinearLayout

    private lateinit var commentSubmitButton: ImageButton
    private lateinit var likeArtefactButton: ImageButton
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private var storage = FirebaseStorage.getInstance()
    var artefactID: String? = ""
    var numComments: Long = 0
    var userPriv: Int = 0
    private var userUID: String? = null
    private var userAvatarUri: String? = null
    private var imageUriList = ArrayList<String>()
    private var likedThisArtefact: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.artifact)
        artefactID = intent.getStringExtra("artefactID")

        supportActionBar?.show()


        artefactText = findViewById<TextView>(R.id.artefactText)
        artefactTitle = findViewById<TextView>(R.id.artifactName)
        commentTxtEdit = findViewById<EditText>(R.id.commentEditText)
        commentSubmitButton = findViewById<ImageButton>(R.id.commentSubmitButton)
        likeArtefactButton = findViewById<ImageButton>(R.id.likeButton)
        viewPager = findViewById<ViewPager>(R.id.viewPager)
        dotIndicator = findViewById<LinearLayout>(R.id.dotsIndicator)

        likeArtefactButton.setImageResource(R.drawable.heartinacircle)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(getString(R.string.firebase_instance))

        setArtefactText(artefactID)
        setArtefactImage(artefactID)

        val refreshFilter = IntentFilter("refreshActivity")
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                getNumberOfComments(artefactID)
            }
        }, refreshFilter)

        val currentUser = mAuth.currentUser
        userUID = currentUser?.uid



        if(currentUser != null && !currentUser.isAnonymous){

            getUserData(userUID?.toString()) { result, error ->
                if (error == null) {

                    val result = result!!
                    userPriv = result.priv.toInt()

                    userAvatarUri = result.avatar.toString()


                    getNumberOfComments(artefactID)

                } else {
                    userPriv = 0
                    getNumberOfComments(artefactID)
                }
            }

        } else{
            commentSubmitButton.visibility = View.GONE
            likeArtefactButton.visibility = View.GONE
            commentTxtEdit.hint = getString(R.string.signin_to_comment)
            userPriv = 0
            getNumberOfComments(artefactID)
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })




    }



    private fun getNumberOfComments(artefactID: String?) {

        val artefactsRef = database.getReference("Artefact/$artefactID/Comments")

        artefactsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                numComments = dataSnapshot.childrenCount
                Log.d("TAG", "Number of Comments: $numComments")
                populateCommentList(numComments.toInt(), artefactID)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", "Failed to read value: ${error.toException()}")
            }
        })
    }

    data class Comment(val text: String? = null, val user_id: String? = null, val approved: Boolean? = false)

    private fun populateCommentList(numOfComments : Int, artefactID: String?) {

        val list = ArrayList<CommentModel>()
        val commentRef = database.getReference("Artefact/$artefactID/Comments")
        var count: Int = 0
        commentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { commentSnapshot ->
                    Log.e("Flow", "child")

                    val comment = commentSnapshot.child("").getValue(Comment::class.java)
                    var username: String = ""
                    var avatar: String = ""
                    val commentModel = CommentModel()



                    if (comment != null) {

                        commentModel.setComment(comment.text)

                        getUserData(comment.user_id.toString()) { result, error ->
                            if (error == null) {
                                count+=1
                                val result = result!!
                                username = result.user_name.toString()
                                avatar = result.avatar.toString()
                                commentModel.setName(username)
                                commentModel.setImage(avatar)
                                commentModel.setUID(commentSnapshot.key ?: "")
                                commentModel.setUserUID(comment.user_id)
                                commentModel.setApproved(comment.approved)
                                if(comment.approved.toString()=="true"){
                                    list.add(commentModel)
                                } else{
                                    if(userPriv>1){
                                        list.add(commentModel)
                                    }else if(mAuth.currentUser?.uid.toString()==comment.user_id){
                                        list.add(commentModel)
                                    }
                                }


                                Log.d("UserData", "Username: $username, Avatar: $avatar")
                                if(count==numOfComments){
                                    instantiateRecycler(list)
                                }

                            } else {

                                Log.e("UserData", "Error getting user data", error)

                            }
                        }


                    }


                }

            }


            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("LoadComments", "Failed")
            }
        })
        if(numOfComments==0){
            instantiateRecycler(list)
        }
        
    }

    data class User(val user_name: String? = null, val avatar: String? = null, val priv: String = "0")

    private fun getUserData(userId: String?, callback: (User?, Throwable?)->Unit) {
        val userRef = database.getReference("User/$userId")

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userModel = snapshot.getValue(User::class.java)

                callback(userModel, null)

            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase database", "error getting comment user data")
                callback(User(), null)
            }
        })

    }

    private fun instantiateRecycler(list: ArrayList<CommentModel>) {
        val recyclerView = findViewById<View>(R.id.commentsRecyclerView) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        val mAdapter = CommentAdapter(list, userPriv, artefactID, userUID.toString())
        recyclerView.adapter = mAdapter
    }

    private fun setArtefactImage(id : String?) {

        val storageRef1 = storage.reference.child("Images/$id")

        storageRef1.listAll()
            .addOnSuccessListener { listResult ->
                val tasks = listResult.items.map { it.downloadUrl }
                val urlsTask: Task<List<Uri>> = Tasks.whenAllSuccess(tasks)
                urlsTask.addOnSuccessListener { urls ->
                    urls.forEach { url ->
                        imageUriList.add(url.toString())
                        if(url.toString()==userAvatarUri){
                            likedThisArtefact = true
                            likeArtefactButton.setImageResource(R.drawable.liked)
                        }
                        Log.e("images", "image uri: ${url.toString()}")
                    }
                    viewPager.adapter = ImagePagerAdapter(this, imageUriList)
                    setUpDots(imageUriList.size)
                }
                    .addOnFailureListener { e ->
                        Log.d("Error", "Failed to get images: ", e)
                    }
            }
    }

    private fun setUpDots(numOfDots: Int) {
        val num = numOfDots ?: 0
        dots = Array(num) { ImageView(this) }
        val dotSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, resources.displayMetrics).toInt()
        for (i in 0 until num) {
            dots[i] = ImageView(this)
            dots[i].setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.notselected))
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            params.setMargins(8, 0, 8, 0)
            dotIndicator.addView(dots[i], params)
        }
        if (num > 0) {
            dots[0].setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.selected))
        }
    }

    private fun updateDots(position: Int) {
        for (i in dots.indices) {
            if (i == position) {
                dots[i].setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.selected))
            } else {
                dots[i].setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.notselected))
            }
        }
    }




    private fun setArtefactText(id : String?) {
        val artefactRef = database.getReference("Artefact/$id/name")

        artefactRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue<String>()
                Log.d("Name", "Artifact name is: $name")
                artefactTitle.text = name
                supportActionBar?.apply {
                    title = name
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase database", "error getting artefact $id name")
            }
        })

        val artefactRef1 = database.getReference("Artefact/$id/content")

        artefactRef1.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val content = snapshot.getValue<String>()
                artefactText.text = content
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase database", "error getting artefact $id name")
            }
        })

    }

    fun postCommentClick(view: View){
        closeKeyboard()
        if(commentTxtEdit!=null){

            var approvedBool: Boolean = false
            if(userPriv>1){
                approvedBool = true
            }
            val newComment = Comment(commentTxtEdit.text.toString(), mAuth.currentUser?.uid.toString(), approvedBool)
            val uniqueId = database.getReference("Artefact/$artefactID/Comments").push().key
            val userRef = database.getReference("Artefact/$artefactID/Comments").child((uniqueId).toString())
            userRef.setValue(newComment)
                .addOnSuccessListener {
                    Log.d("UploadComment", "Comment Posted")
                    displayMessage(view, getString(R.string.comment_posted))
                    numComments += 1
                    commentTxtEdit.setText("")
                    getNumberOfComments(artefactID)

                }
                .addOnFailureListener { exception ->
                    Log.e("UploadComment", "comment post failed", exception)
                    displayMessage(view, getString(R.string.error_comment))

                }
        } else{
            displayMessage(view, "Please enter a comment")
        }

    }



    private fun displayMessage(view: View, msgText: String)
    {
        val sb = Snackbar.make(view,msgText, Snackbar.LENGTH_SHORT)
        sb.show()
    }

    private fun closeKeyboard()
    {
        val view = this.currentFocus
        if(view!=null)
        {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken,0)
        }
    }

    fun likeArtefactClick(view: View) {
        val currentUser = mAuth.currentUser

        if(currentUser != null && currentUser?.isAnonymous == false){
            var newAvatarString = imageUriList[0]
            if(likedThisArtefact){
                newAvatarString = ""
                likeArtefactButton.setImageResource(R.drawable.heartinacircle)
            }
            val userRef = database.getReference("User/$userUID/avatar")
            userRef.setValue(newAvatarString)
                .addOnSuccessListener {
                    Log.d("Favourite", "Set as favourite")
                    likeArtefactButton.setImageResource(R.drawable.liked)
                    displayMessage(view, getString(R.string.changed_fav_mes))
                    likedThisArtefact = true

                }
                .addOnFailureListener { exception ->
                    Log.e("Favourite", "Failed to set as favourite", exception)
                    displayMessage(view, getString(R.string.error_liking))

                }
        }


    }

    override fun onPause() {
        super.onPause()

        saveCommentOnPause()
    }

    override fun onResume() {
        super.onResume()
        getAndSetComment()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        saveCommentOnPause()

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        getAndSetComment()
    }

    private fun saveCommentOnPause(){

        val commentText = commentTxtEdit.text.toString()
        val sharedPreferences = getSharedPreferences("MuseumApp", Context.MODE_PRIVATE)
        with (sharedPreferences.edit()) {
            putString("commentTextKey", commentText)
            apply()
        }
    }

    private fun getAndSetComment(){
        val sharedPreferences = getSharedPreferences("MuseumApp", Context.MODE_PRIVATE)
        val savedCommentText = sharedPreferences.getString("commentTextKey", "")

        getNumberOfComments(artefactID)
        commentTxtEdit.setText(savedCommentText)
    }






}
