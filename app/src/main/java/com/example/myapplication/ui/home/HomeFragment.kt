package com.example.myapplication.ui.home

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.ui.AlbumActivity
import com.example.myapplication.ui.EditorActivity
import com.example.myapplication.ui.VideoPlayerActivity
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {
    
    companion object {
        private const val TAG = "HomeFragment"
    }

    private var latestTmpUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                // 图片拍摄成功，跳转到编辑页面
                val intent = Intent(requireActivity(), EditorActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            }
        }
    }

    private val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                val intent = Intent(requireActivity(), VideoPlayerActivity::class.java)
                intent.putExtra("media_uri", uri.toString())
                startActivity(intent)
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        val authority = "${requireActivity().packageName}.provider"
        return androidx.core.content.FileProvider.getUriForFile(requireActivity().applicationContext, authority, tmpFile)
    }

    private fun getTmpVideoUri(): Uri {
        val tmpFile = File.createTempFile("tmp_video_file", ".mp4", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        val authority = "${requireActivity().packageName}.provider"
        return androidx.core.content.FileProvider.getUriForFile(requireActivity().applicationContext, authority, tmpFile)
    }

    // 为方便演示，先在内部创建简单的 Adapter
    class SimpleTextAdapter(private val items: List<String>) : RecyclerView.Adapter<SimpleTextAdapter.ViewHolder>() {
        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.view as android.widget.TextView).text = items[position]
        }
        override fun getItemCount() = items.size
    }

    // --- 轮播图 Adapter ---
    class CarouselAdapter(private val items: List<Int>) : RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {
        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val imageView = android.widget.ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
            return ViewHolder(imageView)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.view as android.widget.ImageView).setImageResource(items[position])
        }
        override fun getItemCount() = items.size
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView 开始")
        
        val view: View
        try {
            view = inflater.inflate(R.layout.fragment_home, container, false)
            Log.d(TAG, "inflate 完成")
        } catch (e: Exception) {
            Log.e(TAG, "inflate 失败", e)
            return null
        }

        try {
            // --- 1. 设置轮播图 ---
            val viewPager: ViewPager2 = view.findViewById(R.id.carousel_view_pager)
            val carouselItems = listOf(
                R.drawable.banner3,
                R.drawable.banner4,
            )
            viewPager.adapter = CarouselAdapter(carouselItems)
            Log.d(TAG, "轮播图设置完成")
        } catch (e: Exception) {
            Log.e(TAG, "轮播图设置失败", e)
        }
        
        // --- 2. 设置核心操作点击事件 ---
        val launchCameraButton: CardView? = view.findViewById(R.id.btn_launch_camera)
        val importGalleryButton: CardView? = view.findViewById(R.id.btn_import_gallery)

        launchCameraButton?.setOnClickListener {
            val options = arrayOf(getString(R.string.take_photo), getString(R.string.record_video))
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_action)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // 拍照
                            try {
                                getTmpFileUri().let { uri ->
                                    latestTmpUri = uri
                                    takePictureLauncher.launch(uri)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.cannot_open_camera, Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "打开相机失败", e)
                            }
                        }
                        1 -> {
                            // 录像
                            try {
                                getTmpVideoUri().let { uri ->
                                    latestTmpUri = uri
                                    takeVideoLauncher.launch(uri)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.cannot_open_video_camera, Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "打开摄像机失败", e)
                            }
                        }
                    }
                }
                .show()
        }

        importGalleryButton?.setOnClickListener {
            try {
                val intent = Intent(activity, AlbumActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "打开相册失败", e)
            }
        }

        // --- 设置相册图标渐变色 ---
        try {
            val albumIcon: ImageView? = view.findViewById(R.id.iv_album_icon)
            albumIcon?.let { imageView ->
                // 获取渐变色的起始和结束颜色
                val startColor = ContextCompat.getColor(requireContext(), R.color.splash_bg_blue)
                val endColor = ContextCompat.getColor(requireContext(), R.color.splash_bg_pink)
                
                // 使用 post 确保视图已经测量完成
                imageView.post {
                    val width = imageView.width.toFloat()
                    val height = imageView.height.toFloat()
                    
                    if (width > 0 && height > 0) {
                        // 创建线性渐变着色器（135度角，从左上到右下）
                        val shader = LinearGradient(
                            0f, 0f,           // 起始点
                            width, height,    // 结束点（对角线方向）
                            startColor,
                            endColor,
                            Shader.TileMode.CLAMP
                        )
                        
                        // 获取 drawable 并应用渐变着色
                        imageView.drawable?.let { drawable ->
                            val mutableDrawable = drawable.mutate()
                            // 使用自定义 Paint 绘制带渐变的图标
                            val paint = android.graphics.Paint().apply {
                                this.shader = shader
                            }
                            
                            // 创建带渐变的 Bitmap
                            val bitmap = android.graphics.Bitmap.createBitmap(
                                width.toInt(), height.toInt(), android.graphics.Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bitmap)
                            
                            // 先绘制原始图标
                            mutableDrawable.setBounds(0, 0, width.toInt(), height.toInt())
                            mutableDrawable.draw(canvas)
                            
                            // 使用 SRC_IN 模式应用渐变
                            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                            canvas.drawRect(0f, 0f, width, height, paint)
                            
                            // 设置新的 Bitmap 作为图标
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置相册图标渐变色失败", e)
        }

        // --- 3. 设置常用功能网格 ---
        try {
            val quickAccessRecyclerView: RecyclerView = view.findViewById(R.id.quick_access_recycler_view)
            quickAccessRecyclerView.layoutManager = GridLayoutManager(context, 4)
            
            // 创建常用功能列表（图标+文字）
            val quickAccessItems = listOf(
                QuickAccessItem(getString(R.string.feature_filter), R.drawable.ic_filter_vector),
                QuickAccessItem(getString(R.string.feature_crop), R.drawable.ic_crop_vector),
                QuickAccessItem(getString(R.string.feature_puzzle), R.drawable.ic_puzzle),
                QuickAccessItem(getString(R.string.feature_rotate), R.drawable.ic_rotate),
                QuickAccessItem(getString(R.string.feature_text), R.drawable.ic_text_vector),
                QuickAccessItem(getString(R.string.feature_sticker), R.drawable.ic_sticker),
                QuickAccessItem(getString(R.string.feature_brush), R.drawable.ic_brush),
                QuickAccessItem(getString(R.string.feature_all), R.drawable.ic_grid)
            )
            
            quickAccessRecyclerView.adapter = QuickAccessAdapter(quickAccessItems) { item ->
                Toast.makeText(context, R.string.placeholder_only, Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "常用功能网格设置完成")
        } catch (e: Exception) {
            Log.e(TAG, "常用功能网格设置失败", e)
        }

        // --- 4. 设置作品/草稿列表（从数据库加载）---
        try {
            val draftsRecyclerView: RecyclerView = view.findViewById(R.id.drafts_recycler_view)
            draftsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            val app = requireActivity().application as? com.example.myapplication.MyApplication
            if (app == null) {
                Log.e(TAG, "无法获取 MyApplication 实例")
                return view
            }
            
            val draftRepository = app.draftRepository

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    draftRepository.allDrafts.collect { drafts ->
                        if (drafts.isEmpty()) {
                            draftsRecyclerView.adapter = SimpleTextAdapter(listOf(getString(R.string.no_drafts)))
                        } else {
                            // 创建草稿适配器，显示预览图
                            draftsRecyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                                    val itemView = LayoutInflater.from(parent.context)
                                        .inflate(R.layout.item_draft, parent, false)

                                    // 设置固定宽度，高度由布局的 9:16 比例自动计算
                                    val itemWidth = 90  // dp (横向滚动时的固定宽度)
                                    val itemMargin = 4  // dp (项目之间的外边距)
                                    val density = parent.context.resources.displayMetrics.density
                                    val widthPx = (itemWidth * density).toInt()
                                    val marginPx = (itemMargin * density).toInt()

                                    val layoutParams = RecyclerView.LayoutParams(widthPx, RecyclerView.LayoutParams.WRAP_CONTENT)
                                    layoutParams.setMargins(marginPx, marginPx, marginPx, marginPx)
                                    itemView.layoutParams = layoutParams

                                    return object : RecyclerView.ViewHolder(itemView) {}
                                }
            
                                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                                    val draft = drafts[position]
                                    val imageView = holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_draft_preview)
                                    
                                    // 加载原始图片作为预览
                                    try {
                                        val uri = Uri.parse(draft.originalImageUri)
                                        Glide.with(holder.itemView.context)
                                            .load(uri)
                                            .placeholder(android.R.color.darker_gray)
                                            .into(imageView)
                                    } catch (e: Exception) {
                                        imageView?.setImageResource(android.R.color.darker_gray)
                                    }
                                    
                                    // 点击进入编辑页面
                                    holder.itemView.setOnClickListener {
                                        try {
                                            val intent = Intent(requireActivity(), EditorActivity::class.java)
                                            intent.putExtra("image_uri", draft.originalImageUri)
                                            intent.putExtra("draft_id", draft.id)
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "打开编辑器失败", e)
                                        }
                                    }
                                }
            
                                override fun getItemCount(): Int = minOf(drafts.size, 10)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "加载草稿列表失败", e)
                }
            }
            Log.d(TAG, "草稿列表设置完成")
        } catch (e: Exception) {
            Log.e(TAG, "草稿列表设置失败", e)
        }

        Log.d(TAG, "onCreateView 完成")
        return view
    }
}
