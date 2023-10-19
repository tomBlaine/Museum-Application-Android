package com.example.a306cw

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase

class CommentAdapter (private val imageModelArrayList: MutableList<CommentModel>, private val userPriv: Int, private val artefactID: String?, private val userUID: String) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgView = itemView.findViewById<View>(R.id.icon) as ImageView
        var commentTxtView = itemView.findViewById<View>(R.id.commentText) as TextView
        var nameTxtView = itemView.findViewById<View>(R.id.userText) as TextView
        var deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton)
        var acceptButton = itemView.findViewById<ImageButton>(R.id.acceptButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.comment_row_layout, parent, false)

        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return imageModelArrayList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = imageModelArrayList[position]
        val uri = info.getImage()
        val uid = info.getUID()

        if(uri==null || uri==""){
            Glide.with(holder.itemView.context)
                .load(R.drawable.placeholder_avatar)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.imgView)
        }else{
            Glide.with(holder.itemView.context)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.imgView)
        }


        holder.commentTxtView.text = info.getComment()
        holder.nameTxtView.text = info.getName()

        val database = FirebaseDatabase.getInstance("https://cw-fc5f2-default-rtdb.europe-west1.firebasedatabase.app/")

        var approved = info.getApproved()
        if(approved!=null){
            if(approved){
                holder.acceptButton.visibility = View.GONE
            }else{
                holder.acceptButton.setOnClickListener {

                    val myRef = database.getReference("Artefact/$artefactID/Comments/$uid/approved")

                    myRef.setValue(true)
                        .addOnSuccessListener {
                            Log.d("UploadCommentData", "comment now approved")
                            holder.acceptButton.visibility = View.GONE
                            sendRefreshBroadcast(holder.itemView.context)

                        }
                        .addOnFailureListener { exception ->
                            Log.e("UploadCommentData", "Data upload failed", exception)

                        }
                }
            }
        }


        holder.deleteButton.setOnClickListener {

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Delete Confirmation")
            builder.setMessage("Are you sure you want to delete comment: '${info.getComment()}' permanently?")

            builder.setPositiveButton("Yes") { dialog, which ->
                val myRef = database.getReference("Artefact/$artefactID/Comments/$uid")

                myRef.removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.e("DeleteComment", "success")
                        sendRefreshBroadcast(holder.itemView.context)
                    } else {
                        Log.e("DeleteComment", "failed")
                    }
                }
            }

            builder.setNegativeButton("No") { dialog, which ->

            }

            val dialog = builder.create()
            dialog.show()


        }


        if(userPriv<2){
            Log.e("UIDvsUID", "uid of comment = ${info.getUserUID()}, uid of user= $userUID")
            if(info.getUserUID() != userUID) {
                holder.deleteButton.visibility = View.GONE
            }
            holder.acceptButton.visibility = View.GONE
        }


    }

    private fun sendRefreshBroadcast(context: Context) {
        val intent = Intent("refreshActivity")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}