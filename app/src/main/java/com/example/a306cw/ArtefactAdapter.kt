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
import com.google.firebase.storage.FirebaseStorage


class ArtefactAdapter(private val imageModelArrayList: MutableList<ArtefactModel>, private val isCurator: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_ITEM = 0
    private val TYPE_FOOTER = 1
    private var storage = FirebaseStorage.getInstance()

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgView = itemView.findViewById<View>(R.id.icon) as ImageView
        var txtView = itemView.findViewById<View>(R.id.firstLine) as TextView
        var yearTxtView = itemView.findViewById<View>(R.id.secondLine) as TextView
        var editButton = itemView.findViewById<ImageButton>(R.id.editButton) as ImageButton
        var deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton) as ImageButton
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ITEM) {
            val v = inflater.inflate(R.layout.artifact_row_layout, parent, false)
            ItemViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.new_artefact_row, parent, false)
            FooterViewHolder(v)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == imageModelArrayList.size && isCurator) TYPE_FOOTER else TYPE_ITEM
    }

    override fun getItemCount(): Int {
        return if (isCurator) imageModelArrayList.size + 1 else imageModelArrayList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val info = imageModelArrayList[position]
            val uri = info.getImage()

            Log.e("uri instantiation", uri.toString())
            Glide.with(holder.itemView.context)
                .load(uri)
                .apply(RequestOptions().placeholder(R.drawable.placeholder))
                .into(holder.imgView)

            holder.txtView.text = info.getName()

            if(info.getYear()!=null){
                holder.yearTxtView.text = info.getYear()
            } else{
                holder.yearTxtView.text = ""
            }


            val database = FirebaseDatabase.getInstance("https://cw-fc5f2-default-rtdb.europe-west1.firebasedatabase.app/")
            val storageRef = storage.reference.child("Images/${info.getUID()}")
            if(isCurator){
                holder.editButton.setOnClickListener{
                    val intent = Intent(holder.itemView.context, EditArtefactActivity::class.java)
                    intent.putExtra("artefactID", info.getUID())
                    holder.itemView.context.startActivity(intent)
                }
                holder.deleteButton.setOnClickListener{

                    val builder = AlertDialog.Builder(holder.itemView.context)
                    builder.setTitle("Delete Confirmation")
                    builder.setMessage("Are you sure you want to delete '${info.getName()}' permanently?")

                    builder.setPositiveButton("Yes") { _, _ ->
                        val myRef = database.getReference("Artefact/${info.getUID()}")

                        val dbTask = myRef.removeValue()
                        val storageTask = storageRef.delete()

                        Tasks.whenAllComplete(dbTask, storageTask).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.e("DeleteArtefact", "success")
                                sendRefreshBroadcast(holder.itemView.context)

                            } else {
                                Log.e("UploadArtefact", "artefact post failed")

                            }
                        }
                    }

                    builder.setNegativeButton("No") { _, _ ->

                    }

                    val dialog = builder.create()
                    dialog.show()

                }
            }else{
                holder.deleteButton.visibility = View.GONE
                holder.editButton.visibility = View.GONE
            }


            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, ArtefactActivity::class.java)
                intent.putExtra("artefactID", info.getUID())
                holder.itemView.context.startActivity(intent)
            }

        } else if (holder is FooterViewHolder) {
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, EditArtefactActivity::class.java)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    private fun sendRefreshBroadcast(context: Context) {
        val intent = Intent("refreshActivity1")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}