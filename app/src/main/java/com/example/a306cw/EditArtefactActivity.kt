package com.example.a306cw

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class EditArtefactActivity : AppCompatActivity() {

    var artefactID: String? = null
    lateinit var nameTxtEdit : EditText
    lateinit var contentTxtEdit : EditText
    lateinit var yearTxtEdit : EditText
    lateinit var imageView: ImageView
    private lateinit var database : FirebaseDatabase
    val PICK_IMAGE = 1
    private var imageUris = mutableListOf<Uri?>()
    var storage = FirebaseStorage.getInstance()
    var imageChanged: Boolean = false
    lateinit var imageButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.artefact_edit)

        database = FirebaseDatabase.getInstance(getString(R.string.firebase_instance))

        imageButton = findViewById<Button>(R.id.imageButton)
        nameTxtEdit = findViewById<EditText>(R.id.artefactName)
        contentTxtEdit = findViewById<EditText>(R.id.artefactText)
        yearTxtEdit = findViewById<EditText>(R.id.artefactYear)

        artefactID = intent.getStringExtra("artefactID")
        imageChanged = false

        imageView = findViewById<ImageView>(R.id.artifactImage)
        imageButton.setOnClickListener {
            pickImageClick()
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if(artefactID!=null){
            completeFields()
        } else{
            supportActionBar?.apply {
                title = getString(R.string.create_new_artefact_text)
            }
        }
    }

    private fun completeFields() {
        val artefactRef = database.getReference("Artefact/$artefactID")

        artefactRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val artefact = snapshot.getValue<Artefact>()
                if(artefact!=null){
                    nameTxtEdit.setText(artefact.name)
                    contentTxtEdit.setText(artefact.content)
                    yearTxtEdit.setText(artefact.year.toString())
                    supportActionBar?.apply {
                        title = "Editing: ${artefact.name}"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase database", "error getting artefact")
            }
        })
        fetchImage()
    }
    private fun fetchImage() {

        val storageRef = storage.reference.child("Images/${artefactID}/0")
        storageRef.getDownloadUrl().addOnSuccessListener { uri ->
            Log.e("Firebase download", "got uri")

            Glide.with(this)
                .load(uri)
                .apply(RequestOptions().placeholder(R.drawable.placeholder))
                .into(imageView)
        }.addOnFailureListener {
            Log.e("Firebase download", "error getting image")

        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_menu_bar, menu)
        return true
    }

    data class Artefact(val name: String? = null, val content: String? = null, val year: Long? = null)

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_save -> {
            var year = yearTxtEdit.text.toString().toLongOrNull()
            closeKeyboard()
            if(nameTxtEdit!=null && contentTxtEdit!=null){
                if(artefactID==null){

                    val newArtefact = EditArtefactActivity.Artefact(
                        nameTxtEdit.text.toString(),
                        contentTxtEdit.text.toString(),
                        year
                    )
                    artefactID = database.getReference("Artefact").push().key
                    val artefactRef = database.getReference("Artefact").child((artefactID).toString())

                    artefactRef.setValue(newArtefact)
                        .addOnSuccessListener {
                            Log.d("UploadArtefact", "Artefact Posted")
                            saveImage(artefactID.toString())


                        }
                        .addOnFailureListener { exception ->
                            Log.e("UploadArtefact", "post failed", exception)


                        }


                } else{
                    val artefactRef = database.getReference("Artefact/$artefactID")
                    val contentTask = artefactRef.child("content").setValue(contentTxtEdit.text.toString())
                    val nameTask = artefactRef.child("name").setValue(nameTxtEdit.text.toString())
                    val yearTask = artefactRef.child("year").setValue(year)
                    Tasks.whenAllComplete(contentTask, nameTask, yearTask).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            saveImage(artefactID.toString())

                        } else {
                            Log.e("UploadArtefact", "artefact post failed")
                        }
                    }



                }
            }else{
                Log.e("editContent", "Complete necessary fields")
            }

            true
        }
        R.id.action_cancel -> {
            val myIntent = Intent(this, MainActivity::class.java)
            startActivity(myIntent)
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    fun saveImage(artefactUID: String){
        if(imageChanged){
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                uploadImageToFirebase(scope)
            }

        }else{
            backToMainMenu()
        }
    }

    fun backToMainMenu(){
        val myIntent = Intent(this, MainActivity::class.java)
        startActivity(myIntent)
    }


    fun pickImageClick() {
        imageUris.clear()
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Select Images"),
            PICK_IMAGE
        )



    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageChanged = true
            if (data?.clipData != null) {
                val clipData = data.clipData
                for (i in 0 until clipData!!.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    imageUris.add(imageUri)
                    Log.e("got image", "Imgage: $imageUri")
                    imageButton.text = getString(R.string.change_image_text)
                }
            } else if (data?.data != null) {
                val imageUri = data.data
                imageUris.add(imageUri)
                imageChanged = true
                imageButton.text = getString(R.string.change_image_text)
                Log.e("got image", "Image: $imageUri")
            }
            Glide.with(this)
                .load(imageUris[0])
                .apply(RequestOptions().placeholder(R.drawable.placeholder))
                .into(imageView)

        }
    }

    suspend fun uploadImageToFirebase(scope: CoroutineScope){
        val storageRef = storage.reference

        val uploadFutures = mutableListOf<Deferred<Boolean>>()

        for (i in imageUris.indices) {
            val fileUri = imageUris[i]
            val fileName = i.toString()
            Log.e("upload image", "filename: $fileName, artefact id: $artefactID, file uri: $fileUri")
            val fileRef = storageRef.child("Images/$artefactID/$fileName")
            val uploadTask = fileRef.putFile(fileUri!!)

            val uploadFuture = scope.async(Dispatchers.IO) {
                try {
                    uploadTask.await()
                    Log.e("Upload", "Success")
                    true
                } catch (exception: Exception) {
                    Log.e("Upload", "Failure", exception)
                    false
                }
            }

            uploadFutures.add(uploadFuture)
        }

        val results = uploadFutures.awaitAll()

        if (results.all { it }) {
            deleteOldImages(imageUris.size)
        }
    }

    private fun deleteOldImages(fileName: Int){
        val storageRef = storage.reference
        val fileRef = storageRef.child("Images/$artefactID/$fileName")

        fileRef.delete()
            .addOnSuccessListener {
                deleteOldImages(fileName+1)
                Log.d("Images delete", "File deleted successfully")
            }
            .addOnFailureListener { exception ->
                backToMainMenu()
                Log.d("images delete", "Failed to delete file", exception)
            }

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

    override fun onPause() {
        super.onPause()

        saveFieldsOnPause()
    }

    override fun onResume() {
        super.onResume()
        getAndSetComment()

    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        saveFieldsOnPause()

    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        getAndSetComment()
    }
    private fun saveFieldsOnPause(){
        val idText = artefactID
        val nameText = nameTxtEdit.text.toString()
        val yearText = yearTxtEdit.text.toString()
        val contentText = contentTxtEdit.text.toString()
        val uriStrings = imageUris.map { it.toString() }
        val combined = uriStrings.joinToString(separator = ",")

        val sharedPreferences = getSharedPreferences("MuseumApp", Context.MODE_PRIVATE)
        with (sharedPreferences.edit()) {
            putString("idTextKey", idText)
            putString("nameTextKey", nameText)
            putString("yearTextKey", yearText)
            putString("contentTextKey", contentText)
            putString("imageUriKey", combined)
            apply()
        }
    }

    private fun getAndSetComment(){
        val sharedPreferences = getSharedPreferences("MuseumApp", Context.MODE_PRIVATE)
        val idText = sharedPreferences.getString("idTextKey", "")
        if(idText == artefactID){
            val savedNameText = sharedPreferences.getString("nameTextKey", "")
            val savedYearText = sharedPreferences.getString("yearTextKey", "")
            val savedContentText = sharedPreferences.getString("contentTextKey", "")

            val combined = sharedPreferences.getString("imageUris", null)
            if (combined != null) {
                val uriStrings = combined.split(",")
                imageUris = uriStrings.map { Uri.parse(it) }.toMutableList()
            }

            nameTxtEdit.setText(savedNameText)
            yearTxtEdit.setText(savedYearText)
            contentTxtEdit.setText(savedContentText)
        }

    }
}