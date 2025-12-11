package com.example.myapplication.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide 

class MyContentFragment : Fragment() {
    private var contentType: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contentType = it.getString(ARG_CONTENT_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val recyclerView = RecyclerView(requireContext())
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.setHasFixedSize(true) 
        recyclerView.setItemViewCacheSize(20)  

        // 获取Repository
        val app = requireActivity().application as com.example.myapplication.MyApplication

        // 根据contentType显示不同的数据
        when (contentType) {
            "作品" -> {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
                val editedImageRepository = app.editedImageRepository
                viewLifecycleOwner.lifecycleScope.launch {
                    editedImageRepository.allEditedImages.collect { images ->
                        recyclerView.adapter =
                            object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                override fun onCreateViewHolder(
                                    parent: ViewGroup,
                                    viewType: Int
                                ): RecyclerView.ViewHolder {
                                    val itemView = LayoutInflater.from(parent.context)
                                        .inflate(R.layout.item_draft, parent, false)
                                    
                                    // 设置固定4:3比例的高度
                                    val displayMetrics = parent.context.resources.displayMetrics
                                    val screenWidth = displayMetrics.widthPixels
                                    val itemWidth = screenWidth / 3
                                    val itemHeight = (itemWidth * 4 / 3f).toInt()
                                    
                                    val imageView = itemView.findViewById<android.widget.ImageView>(R.id.iv_draft_preview)
                                    imageView.layoutParams.height = itemHeight
                                    
                                    return object : RecyclerView.ViewHolder(itemView) {}
                                }

                                override fun onBindViewHolder(
                                    holder: RecyclerView.ViewHolder,
                                    position: Int
                                ) {
                                    val image = images[position]
                                    val imageView =
                                        holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_draft_preview)

                                    // 加载已编辑的图片
                                    try {
                                        val uri = android.net.Uri.parse(image.editedImageUri)
                                        Glide.with(holder.itemView.context)
                                            .load(uri)
                                            .placeholder(android.R.color.darker_gray)
                                            .into(imageView)
                                    } catch (e: Exception) {
                                        imageView.setImageResource(android.R.color.darker_gray)
                                    }

                                    // 点击查看大图（可选功能）
                                    holder.itemView.setOnClickListener {
                                        // TODO: 打开图片查看器
                                    }
                                }

                                override fun getItemCount(): Int = images.size
                            }
                    }
                }
            }

            "草稿" -> {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
                val draftRepository = app.draftRepository
                viewLifecycleOwner.lifecycleScope.launch {
                    draftRepository.allDrafts.collect { drafts ->
                        recyclerView.adapter =
                            object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                override fun onCreateViewHolder(
                                    parent: ViewGroup,
                                    viewType: Int
                                ): RecyclerView.ViewHolder {
                                    val itemView = LayoutInflater.from(parent.context)
                                        .inflate(R.layout.item_draft, parent, false)
                                    
                                    // 设置固定4:3比例的高度
                                    val displayMetrics = parent.context.resources.displayMetrics
                                    val screenWidth = displayMetrics.widthPixels
                                    val itemWidth = screenWidth / 3
                                    val itemHeight = (itemWidth * 4 / 3f).toInt()

                                    val imageView = itemView.findViewById<android.widget.ImageView>(R.id.iv_draft_preview)
                                    imageView.layoutParams.height = itemHeight
                                    
                                    return object : RecyclerView.ViewHolder(itemView) {}
                                }

                                override fun onBindViewHolder(
                                    holder: RecyclerView.ViewHolder,
                                    position: Int
                                ) {
                                    val draft = drafts[position]
                                    val imageView =
                                        holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_draft_preview)

                                    // 加载原始图片作为预览
                                    try {
                                        val uri = android.net.Uri.parse(draft.originalImageUri)
                                        Glide.with(holder.itemView.context)
                                            .load(uri)
                                            .placeholder(android.R.color.darker_gray)
                                            .into(imageView)
                                    } catch (e: Exception) {
                                        imageView.setImageResource(android.R.color.darker_gray)
                                    }

                                    // 点击进入编辑页面
                                    holder.itemView.setOnClickListener {
                                        val intent = android.content.Intent(
                                            requireActivity(),
                                            com.example.myapplication.ui.EditorActivity::class.java
                                        )
                                        intent.putExtra("image_uri", draft.originalImageUri)
                                        intent.putExtra("draft_id", draft.id)
                                        startActivity(intent)
                                    }
                                }

                                override fun getItemCount(): Int = drafts.size
                            }
                    }
                }
            }

            "收藏" -> {
                // 显示相册
                val albumRepository = app.albumRepository
                viewLifecycleOwner.lifecycleScope.launch {
                    albumRepository.allAlbums.collect { albums ->
                        recyclerView.adapter =
                            object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                override fun onCreateViewHolder(
                                    parent: ViewGroup,
                                    viewType: Int
                                ): RecyclerView.ViewHolder {
                                    val textView = TextView(parent.context).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            300
                                        )
                                        gravity = android.view.Gravity.CENTER
                                    }
                                    return object : RecyclerView.ViewHolder(textView) {}
                                }

                                override fun onBindViewHolder(
                                    holder: RecyclerView.ViewHolder,
                                    position: Int
                                ) {
                                    val album = albums[position]
                                    (holder.itemView as TextView).text = "${album.name}\n${
                                        java.text.SimpleDateFormat(
                                            "MM-dd",
                                            java.util.Locale.getDefault()
                                        )
                                            .format(java.util.Date(album.createdAt))
                                    }"
                                }

                                override fun getItemCount(): Int = albums.size
                            }
                    }
                }
            }

            else -> {
                // 默认显示空列表
                recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): RecyclerView.ViewHolder {
                        val textView = TextView(parent.context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                300
                            )
                            gravity = android.view.Gravity.CENTER
                        }
                        return object : RecyclerView.ViewHolder(textView) {}
                    }

                    override fun onBindViewHolder(
                        holder: RecyclerView.ViewHolder,
                        position: Int
                    ) {
                        (holder.itemView as TextView).text = "$contentType ${position + 1}"
                    }

                    override fun getItemCount(): Int = 0
                }
            }
        }
        return recyclerView
    }

    companion object {
        private const val ARG_CONTENT_TYPE = "content_type"

        fun newInstance(contentType: String): MyContentFragment {
            return MyContentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_TYPE, contentType)
                }
            }
        }
    }
}
