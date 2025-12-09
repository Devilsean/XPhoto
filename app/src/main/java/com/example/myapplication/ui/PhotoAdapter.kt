package com.example.myapplication.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.ui.AlbumActivity.PhotoItem

class PhotoAdapter(private val photoList: List<PhotoItem>) :
    RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImage: ImageView = view.findViewById(R.id.iv_photo)
        val playIcon: ImageView = view.findViewById(R.id.iv_play_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photoItem = photoList[position]
        val mimeType = getMimeType(photoItem.uri, holder.itemView.context)

        when {
            mimeType?.startsWith("video/") == true -> {
                // 视频：显示视频缩略图和播放图标
                holder.playIcon.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(photoItem.uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.photoImage)
            }
            mimeType == "image/gif" -> {
                // GIF：使用 Glide 加载动画
                holder.playIcon.visibility = View.GONE
                Glide.with(holder.itemView.context)
                    .asGif()
                    .load(photoItem.uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.photoImage)
            }
            else -> {
                // 静态图片（包括 WebP）
                holder.playIcon.visibility = View.GONE
                Glide.with(holder.itemView.context)
                    .load(photoItem.uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.photoImage)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = if (mimeType?.startsWith("video/") == true) {
                // 视频文件打开视频播放器
                Intent(holder.itemView.context, VideoPlayerActivity::class.java).apply {
                    putExtra("media_uri", photoItem.uri.toString())
                }
            } else {
                // 图片文件打开编辑器
                Intent(holder.itemView.context, EditorActivityExample::class.java).apply {
                    putExtra("image_uri", photoItem.uri.toString())
                }
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = photoList.size

    private fun getMimeType(uri: Uri, context: android.content.Context): String? {
        return context.contentResolver.getType(uri)
    }
}
