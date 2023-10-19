package com.example.a306cw

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase


class LoginActivity : AppCompatActivity() {

    private var auth = Firebase.auth
    private var currentUser = auth.currentUser
    private lateinit var database : FirebaseDatabase

    lateinit var emailText : EditText
    lateinit var pwText : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        emailText = findViewById<EditText>(R.id.emailInput)
        pwText = findViewById<EditText>(R.id.pwInput)

        supportActionBar?.apply {
            title = getString(R.string.welcome_bar_text)
        }

        update()
    }

    fun update()
    {
        currentUser = auth.currentUser
        val currentEmail = currentUser?.email

        if(currentEmail != null) {
            val myIntent = Intent(this, MainActivity::class.java)
            startActivity(myIntent)
        }

    }

    override fun onStart() {
        super.onStart()
        update()
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    fun loginClick(view: View)
    {

        auth.signInWithEmailAndPassword(
            emailText.text.toString(),
            pwText.text.toString()
        ) .addOnCompleteListener(this){
                task->
            if(task.isSuccessful){
                closeKeyboard()
                displayMessage(view, getString(R.string.login_email))
                update()
            }else{
                closeKeyboard()
                displayMessage(view, task.exception?.message.toString()) //error?
            }
        }
    }


    fun registerUser(view: View, username: String)
    {
            if(currentUser!=null)
            {
                displayMessage(view, getString(R.string.register_but_signed_in))
                Log.d("logged in", "Already logged in!")
            }else{
                auth.createUserWithEmailAndPassword(
                    emailText.text.toString(),
                    pwText.text.toString()
                ).addOnCompleteListener(this){
                        task->
                    if(task.isSuccessful)
                    {
                        saveUserName(username)
                        closeKeyboard()
                    }else
                    {
                        closeKeyboard()
                        displayMessage(view, task.exception?.message.toString())
                    }
                }
            }
    }



    fun showUsernameDialog(view: View) {

        val input = EditText(this)
        input.setHint(getString(R.string.email_hint))

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            resources.getDimensionPixelSize(R.dimen.dialog_margin),
            0,
            resources.getDimensionPixelSize(R.dimen.dialog_margin),
            0
        )
        input.layoutParams = params
        container.addView(input)


        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.username))
            .setMessage(getString(R.string.alert_username_msg))
            .setView(container)
            .setPositiveButton(getString(R.string.altert_ok)) { _, _ ->
                val userInput = input.text.toString()
                if(userInput!="null"){
                    registerUser(view, userInput)
                }else{
                    displayMessage(view, getString(R.string.enter_username_msg))
                }
            }
            .setNegativeButton(R.string.altert_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    data class User(val user_name: String, val avatar: String, val priv: String)

    private fun saveUserName(enteredUsername: String) {
        currentUser = auth.currentUser
        val userUID = currentUser?.uid
        database = FirebaseDatabase.getInstance(getString(R.string.firebase_instance))

        val userModel = User(enteredUsername,"", "1")

        if(userUID!=null){
            val userRef = database.getReference("User").child(userUID)
            userRef.setValue(userModel)
                .addOnSuccessListener {
                    Log.d("UploadUserData", "Data uploaded successfully")
                    val myIntent = Intent(this, MainActivity::class.java)
                    startActivity(myIntent)

                }
                .addOnFailureListener { exception ->
                    Log.e("UploadUserData", "Data upload failed", exception)

                }
        } else{
            Log.e("UploadUserData", "Data upload failed no UID")
        }


    }

    fun guestClick(view: View) {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.e("Firebase auth", "Signed in anonymously: ${user?.uid}")
                    val myIntent = Intent(this, MainActivity::class.java)
                    startActivity(myIntent)
                } else {
                    Log.e("Firebase auth", "Signed in failed")
                }
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


    }