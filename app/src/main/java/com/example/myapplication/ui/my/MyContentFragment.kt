package com.example.myapplication.ui.my

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.data.entity.Draft
import com.example.myapplication.data.entity.EditedImage
import com.example.myapplication.data.repository.DraftRepository
import com.example.myapplication.data.repository.EditedImageRepository
import com.example.myapplication.ui.EditorActivity
import com.example.myapplication.ui.ImageViewerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

class MyContentFragment : Fragment() {
    private var contentType: String? = null
    
    // 批量选择模式
    private var isSelectionMode = false
    private val selectedDraftIds = mutableSetOf<Long>()
    private val selectedImageIds = mutableSetOf<Long>()
    
    // 当前数据
    private var currentDrafts: List<Draft> = emptyList()
    private var currentImages: List<EditedImage> = emptyList()
    
    // UI组件
    private var recyclerView: RecyclerView? = null
    private var batchActionBar: MaterialCardView? = null
    private var tvSelectionCount: TextView? = null
    
    // Repository
    private lateinit var draftRepository: DraftRepository
    private lateinit var editedImageRepository: EditedImageRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contentType = it.getString(ARG_CONTENT_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 创建根布局
        val rootLayout = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // 创建RecyclerView
        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(context, 3)
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
        rootLayout.addView(recyclerView)
        
        // 添加批量操作栏
        val batchActionView = inflater.inflate(R.layout.layout_batch_action_bar, rootLayout, false)
        batchActionBar = batchActionView as MaterialCardView
        rootLayout.addView(batchActionBar)
        
        // 初始化批量操作栏组件
        setupBatchActionBar()
        
        // 获取Repository
        val app = requireActivity().application as MyApplication
        draftRepository = app.draftRepository
        editedImageRepository = app.editedImageRepository

        // 根据contentType显示不同的数据
        when (contentType) {
            "作品" -> setupWorksView()
            "草稿" -> setupDraftsView()
            "收藏" -> setupFavoritesView()
            else -> setupEmptyView()
        }
        
        return rootLayout
    }
    
    private fun setupBatchActionBar() {
        tvSelectionCount = batchActionBar?.findViewById(R.id.tv_selection_count)
        
        // 全选按钮
        batchActionBar?.findViewById<MaterialButton>(R.id.btn_select_all)?.setOnClickListener {
            selectAll()
        }
        
        // 取消选择按钮
        batchActionBar?.findViewById<MaterialButton>(R.id.btn_cancel_selection)?.setOnClickListener {
            exitSelectionMode()
        }
        
        // 删除按钮
        batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_delete)?.setOnClickListener {
            showDeleteConfirmDialog()
        }
        
        // 保存按钮（草稿专用）
        batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_save)?.setOnClickListener {
            batchSaveDraftsAsWorks()
        }
        
        // 导出按钮（作品专用）
        batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_export)?.setOnClickListener {
            batchExportWorks()
        }
        
        // 根据内容类型显示/隐藏按钮
        when (contentType) {
            "草稿" -> {
                batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_save)?.visibility = View.VISIBLE
                batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_export)?.visibility = View.GONE
            }
            "作品", "收藏" -> {
                batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_save)?.visibility = View.GONE
                batchActionBar?.findViewById<LinearLayout>(R.id.btn_batch_export)?.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupWorksView() {
        viewLifecycleOwner.lifecycleScope.launch {
            editedImageRepository.allEditedImages.collectLatest { images ->
                currentImages = images
                recyclerView?.adapter = createImageAdapter(images, showFavoriteBadge = true, imageType = "work")
            }
        }
    }
    
    private fun setupDraftsView() {
        viewLifecycleOwner.lifecycleScope.launch {
            draftRepository.allDrafts.collectLatest { drafts ->
                currentDrafts = drafts
                recyclerView?.adapter = createDraftAdapter(drafts)
            }
        }
    }
    
    private fun setupFavoritesView() {
        viewLifecycleOwner.lifecycleScope.launch {
            editedImageRepository.favoriteImages.collectLatest { images ->
                currentImages = images
                recyclerView?.adapter = createImageAdapter(images, showFavoriteBadge = true, imageType = "favorite")
            }
        }
    }
    
    private fun setupEmptyView() {
        recyclerView?.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val textView = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        300
                    )
                    gravity = android.view.Gravity.CENTER
                }
                return object : RecyclerView.ViewHolder(textView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = "$contentType ${position + 1}"
            }

            override fun getItemCount(): Int = 0
        }
    }
    
    /**
     * 创建草稿适配器
     */
    private fun createDraftAdapter(drafts: List<Draft>): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_draft, parent, false)
                return object : RecyclerView.ViewHolder(itemView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val draft = drafts[position]
                val imageView = holder.itemView.findViewById<ImageView>(R.id.iv_draft_preview)
                val selectionOverlay = holder.itemView.findViewById<View>(R.id.selection_overlay)
                val selectionCheck = holder.itemView.findViewById<ImageView>(R.id.iv_selection_check)

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
                
                // 更新选择状态UI
                val isSelected = selectedDraftIds.contains(draft.id)
                selectionOverlay?.visibility = if (isSelected) View.VISIBLE else View.GONE
                selectionCheck?.visibility = if (isSelectionMode) {
                    if (isSelected) View.VISIBLE else View.INVISIBLE
                } else View.GONE

                // 点击事件
                holder.itemView.setOnClickListener {
                    if (isSelectionMode) {
                        toggleDraftSelection(draft.id)
                        notifyItemChanged(position)
                    } else {
                        // 进入编辑页面
                        val intent = Intent(requireActivity(), EditorActivity::class.java)
                        intent.putExtra("image_uri", draft.originalImageUri)
                        intent.putExtra("draft_id", draft.id)
                        startActivity(intent)
                    }
                }

                // 长按进入选择模式或查看详情
                holder.itemView.setOnLongClickListener {
                    if (!isSelectionMode) {
                        enterSelectionMode()
                        toggleDraftSelection(draft.id)
                        notifyDataSetChanged()
                    }
                    true
                }
            }

            override fun getItemCount(): Int = drafts.size
        }
    }
    
    /**
     * 创建图片适配器（作品/收藏）
     */
    private fun createImageAdapter(
        images: List<EditedImage>, 
        showFavoriteBadge: Boolean,
        imageType: String
    ): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_draft, parent, false)
                return object : RecyclerView.ViewHolder(itemView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val image = images[position]
                val imageView = holder.itemView.findViewById<ImageView>(R.id.iv_draft_preview)
                val favoriteBadge = holder.itemView.findViewById<ImageView>(R.id.iv_favorite_badge)
                val selectionOverlay = holder.itemView.findViewById<View>(R.id.selection_overlay)
                val selectionCheck = holder.itemView.findViewById<ImageView>(R.id.iv_selection_check)

                // 显示收藏标识
                if (showFavoriteBadge && image.isFavorite) {
                    favoriteBadge?.visibility = View.VISIBLE
                } else {
                    favoriteBadge?.visibility = View.GONE
                }

                // 加载已编辑的图片
                try {
                    val imagePath = image.editedImageUri
                    val imageSource: Any = if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
                        File(imagePath.removePrefix("file://"))
                    } else {
                        android.net.Uri.parse(imagePath)
                    }

                    Glide.with(holder.itemView.context)
                        .load(imageSource)
                        .placeholder(android.R.color.darker_gray)
                        .error(android.R.color.darker_gray)
                        .into(imageView)
                } catch (e: Exception) {
                    android.util.Log.e("MyContentFragment", "加载图片失败: ${image.editedImageUri}", e)
                    imageView.setImageResource(android.R.color.darker_gray)
                }
                
                // 更新选择状态UI
                val isSelected = selectedImageIds.contains(image.id)
                selectionOverlay?.visibility = if (isSelected) View.VISIBLE else View.GONE
                selectionCheck?.visibility = if (isSelectionMode) {
                    if (isSelected) View.VISIBLE else View.INVISIBLE
                } else View.GONE

                // 点击事件
                holder.itemView.setOnClickListener {
                    if (isSelectionMode) {
                        toggleImageSelection(image.id)
                        notifyItemChanged(position)
                    } else {
                        // 查看大图
                        val intent = Intent(requireActivity(), ImageViewerActivity::class.java)
                        intent.putExtra("image_path", image.editedImageUri)
                        intent.putExtra("image_id", image.id)
                        intent.putExtra("is_favorite", image.isFavorite)
                        intent.putExtra("image_type", if (image.isFavorite) "favorite" else "work")
                        startActivity(intent)
                    }
                }

                // 长按进入选择模式
                holder.itemView.setOnLongClickListener {
                    if (!isSelectionMode) {
                        enterSelectionMode()
                        toggleImageSelection(image.id)
                        notifyDataSetChanged()
                    }
                    true
                }
            }

            override fun getItemCount(): Int = images.size
        }
    }
    
    // ==================== 选择模式相关方法 ====================
    
    private fun enterSelectionMode() {
        isSelectionMode = true
        batchActionBar?.visibility = View.VISIBLE
        updateSelectionCount()
    }
    
    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedDraftIds.clear()
        selectedImageIds.clear()
        batchActionBar?.visibility = View.GONE
        recyclerView?.adapter?.notifyDataSetChanged()
    }
    
    private fun toggleDraftSelection(id: Long) {
        if (selectedDraftIds.contains(id)) {
            selectedDraftIds.remove(id)
        } else {
            selectedDraftIds.add(id)
        }
        updateSelectionCount()
    }
    
    private fun toggleImageSelection(id: Long) {
        if (selectedImageIds.contains(id)) {
            selectedImageIds.remove(id)
        } else {
            selectedImageIds.add(id)
        }
        updateSelectionCount()
    }
    
    private fun selectAll() {
        when (contentType) {
            "草稿" -> {
                selectedDraftIds.clear()
                selectedDraftIds.addAll(currentDrafts.map { it.id })
            }
            "作品", "收藏" -> {
                selectedImageIds.clear()
                selectedImageIds.addAll(currentImages.map { it.id })
            }
        }
        updateSelectionCount()
        recyclerView?.adapter?.notifyDataSetChanged()
    }
    
    private fun updateSelectionCount() {
        val count = when (contentType) {
            "草稿" -> selectedDraftIds.size
            else -> selectedImageIds.size
        }
        tvSelectionCount?.text = "已选择 $count 项"
    }
    
    // ==================== 批量操作方法 ====================
    
    private fun showDeleteConfirmDialog() {
        val count = when (contentType) {
            "草稿" -> selectedDraftIds.size
            else -> selectedImageIds.size
        }
        
        if (count == 0) {
            Toast.makeText(requireContext(), "请先选择要删除的项目", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 $count 个项目吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                performBatchDelete()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performBatchDelete() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (contentType) {
                    "草稿" -> {
                        draftRepository.deleteDrafts(selectedDraftIds.toList())
                        Toast.makeText(requireContext(), "已删除 ${selectedDraftIds.size} 个草稿", Toast.LENGTH_SHORT).show()
                    }
                    "作品", "收藏" -> {
                        // 删除文件
                        selectedImageIds.forEach { id ->
                            currentImages.find { it.id == id }?.let { image ->
                                try {
                                    val file = File(image.editedImageUri.removePrefix("file://"))
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                } catch (e: Exception) {
                                    // 忽略文件删除错误
                                }
                            }
                        }
                        editedImageRepository.deleteEditedImages(selectedImageIds.toList())
                        Toast.makeText(requireContext(), "已删除 ${selectedImageIds.size} 个作品", Toast.LENGTH_SHORT).show()
                    }
                }
                exitSelectionMode()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 批量保存草稿为作品
     */
    private fun batchSaveDraftsAsWorks() {
        if (selectedDraftIds.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择要保存的草稿", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("保存为作品")
            .setMessage("将选中的 ${selectedDraftIds.size} 个草稿保存为作品？保存后草稿将被删除。")
            .setPositiveButton("保存") { _, _ ->
                performBatchSave()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performBatchSave() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var successCount = 0
                val draftsToSave = currentDrafts.filter { selectedDraftIds.contains(it.id) }
                
                for (draft in draftsToSave) {
                    try {
                        // 创建作品记录
                        val editedImage = com.example.myapplication.data.entity.EditedImage(
                            originalImageUri = draft.originalImageUri,
                            editedImageUri = draft.originalImageUri, // 使用原图作为编辑后的图片
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis()
                        )
                        editedImageRepository.saveEditedImage(editedImage)
                        
                        // 删除草稿
                        draftRepository.deleteDraft(draft.id)
                        successCount++
                    } catch (e: Exception) {
                        android.util.Log.e("MyContentFragment", "保存草稿失败: ${draft.id}", e)
                    }
                }
                
                Toast.makeText(requireContext(), "已保存 $successCount 个作品", Toast.LENGTH_SHORT).show()
                exitSelectionMode()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 批量导出作品到系统相册
     */
    private fun batchExportWorks() {
        if (selectedImageIds.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择要导出的作品", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("导出到相册")
            .setMessage("将选中的 ${selectedImageIds.size} 个作品导出到系统相册？")
            .setPositiveButton("导出") { _, _ ->
                performBatchExport()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performBatchExport() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var successCount = 0
                val imagesToExport = currentImages.filter { selectedImageIds.contains(it.id) }
                
                for (image in imagesToExport) {
                    try {
                        val file = getImageFile(image.editedImageUri)
                        if (file != null && file.exists()) {
                            val fileName = "exported_${System.currentTimeMillis()}_${image.id}.png"
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyApplication")
                            }
                            
                            val resolver = requireContext().contentResolver
                            var outputStream: OutputStream? = null
                            var uri: Uri? = null
                            
                            try {
                                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                if (uri != null) {
                                    outputStream = resolver.openOutputStream(uri)
                                    if (outputStream != null) {
                                        FileInputStream(file).use { inputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                        
                                        // 更新数据库中的导出状态
                                        editedImageRepository.updateExportedUri(image.id, uri.toString())
                                        successCount++
                                    }
                                }
                            } catch (e: Exception) {
                                if (uri != null) {
                                    resolver.delete(uri, null, null)
                                }
                                throw e
                            } finally {
                                outputStream?.close()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MyContentFragment", "导出作品失败: ${image.id}", e)
                    }
                }
                
                Toast.makeText(requireContext(), "已导出 $successCount 个作品到相册", Toast.LENGTH_SHORT).show()
                exitSelectionMode()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getImageFile(imagePath: String): File? {
        return try {
            when {
                imagePath.startsWith("/") -> File(imagePath)
                imagePath.startsWith("file://") -> File(imagePath.removePrefix("file://"))
                else -> null
            }
        } catch (e: Exception) {
            null
        }
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
