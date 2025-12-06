package com.example.myapplication.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.ui.AlbumActivity.PhotoItem


class PhotoAdapter (private val photoList:List<PhotoItem>):
    RecyclerView.Adapter<PhotoAdapter.ViewHolder>(){
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val photoImage: ImageView =view.findViewById(R.id.iv_photo)
    }
    override fun onCreateViewHolder(parent:ViewGroup,viewType:Int):ViewHolder{
        val view= LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_photo,parent,false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder:ViewHolder,position:Int){
        val photoItem=photoList[position]
        Glide.with(holder.itemView.context)
            .load(photoItem.uri)
            .into(holder.photoImage)
        holder.itemView.setOnClickListener {
            val intent= Intent(holder.itemView.context, EditorActivity::class.java).apply{
                putExtra("image_uri",photoItem.uri.toString())
            }
            holder.itemView.context.startActivity(intent)
        }
    }
    override fun getItemCount()=photoList.size
}