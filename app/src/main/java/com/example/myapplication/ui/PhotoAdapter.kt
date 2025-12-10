package com.example.myapplication.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.utils.MediaStoreHelper

class PhotoAdapter(private val mediaList: List<MediaStoreHelper.MediaItem>) :
    RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImage: ImageView = view.findViewById(R.id.iv_photo)
        val playIcon: ImageView = view.findViewById(R.id.iv_play_icon)
        val durationText: TextView? = view.findViewById(R.id.tv_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaItem = mediaList[position]

        when {
            mediaItem.isVideo -> {
                // 视频：显示视频缩略图和播放图标
                holder.playIcon.visibility = View.VISIBLE
                
                // 显示视频时长
                holder.durationText?.let {
                    it.visibility = View.VISIBLE
                    it.text = formatDuration(mediaItem.duration)
                }
                
                Glide.with(holder.itemView.context)
                    .load(mediaItem.uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.photoImage)
            }
            mediaItem.mimeType == "image/gif" -> {
                // GIF：使用 Glide 加载动画
                holder.playIcon.visibility = View.GONE
                holder.durationText?.visibility = View.GONE
                
                Glide.with(holder.itemView.context)
                    .asGif()
                    .load(mediaItem.uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.photoImage)
            }
            else -> {
                // 静态图片（包括 WebP）
                holder.playIcon.visibility = View.GONE
                holder.durationText?.visibility = View.GONE
                
                Glide.with(holder.itemView.context)
                    .load(mediaItem.uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.photoImage)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val uriString = mediaItem.uri.toString()
            
            android.util.Log.d("PhotoAdapter", "点击媒体项: URI=$uriString, isVideo=${mediaItem.isVideo}")
            
            val intent = if (mediaItem.isVideo) {
                // 视频文件打开视频播放器
                Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("media_uri", uriString)
                    // 授予 URI 读取权限
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                // 图片文件打开编辑器（带裁剪功能）
                Intent(context, EditorActivity::class.java).apply {
                    putExtra("image_uri", uriString)
                    // 授予 URI 读取权限
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            
            try {
                context.startActivity(intent)
                android.util.Log.d("PhotoAdapter", "成功启动Activity")
            } catch (e: Exception) {
                android.util.Log.e("PhotoAdapter", "启动Activity失败", e)
                android.widget.Toast.makeText(context, "打开失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = mediaList.size

    /**
     * 格式化视频时长(毫秒转为mm:ss 格式)
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}
