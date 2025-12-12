package com.example.myapplication.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.data.repository.DraftRepository
import com.example.myapplication.data.repository.EditedImageRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ImageViewerActivity : AppCompatActivity() {
    
    private lateinit var imageView: ImageView
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var editedImageRepository: EditedImageRepository
    private lateinit var draftRepository: DraftRepository
    
    // 收藏相关视图
    private lateinit var favoriteIcon: ImageView
    private lateinit var favoriteText: TextView
    private lateinit var favoriteBadge: ImageView
    
    // 底部操作栏按钮
    private lateinit var favoriteButton: LinearLayout
    private lateinit var exportButton: LinearLayout
    
    // 变换矩阵
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    
    // 触摸模式
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE
    
    // 触摸点
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var oldDist = 1f
    
    // 缩放限制
    private var minScale = 0.1f  // 允许缩小到10%
    private val maxScale = 5f
    private var initialScale = 1f  // 初始缩放比例
    
    // 图片信息
    private var imagePath: String? = null
    private var imageId: Long = -1
    private var isFavorite: Boolean = false
    private var draftId: Long = -1
    private var imageType: String = "work" // "work", "draft", "favorite"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        // 初始化Repository
        val app = application as MyApplication
        editedImageRepository = app.editedImageRepository
        draftRepository = app.draftRepository
        
        imageView = findViewById(R.id.image_view)
        favoriteIcon = findViewById(R.id.favorite_icon)
        favoriteText = findViewById(R.id.favorite_text)
        favoriteBadge = findViewById(R.id.favorite_badge)
        favoriteButton = findViewById(R.id.favorite_button)
        exportButton = findViewById(R.id.export_button)
        
        // 获取图片路径和ID
        imagePath = intent.getStringExtra("image_path")
        imageId = intent.getLongExtra("image_id", -1)
        isFavorite = intent.getBooleanExtra("is_favorite", false)
        draftId = intent.getLongExtra("draft_id", -1)
        imageType = intent.getStringExtra("image_type") ?: "work"
        
        if (imagePath.isNullOrEmpty()) {
            finish()
            return
        }
        
        // 加载图片
        loadImage(imagePath!!)
        
        // 根据图片类型设置UI
        setupUIByImageType()
        
        // 设置缩放手势检测器
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        
        // 设置ImageView的scaleType为MATRIX以支持自定义变换
        imageView.scaleType = ImageView.ScaleType.MATRIX
        
        // 设置触摸监听
        imageView.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
        
        // 关闭按钮
        findViewById<ImageButton>(R.id.close_button).setOnClickListener {
            finish()
        }
        
        // 编辑按钮
        findViewById<LinearLayout>(R.id.edit_button).setOnClickListener {
            openEditor()
        }
        
        // 收藏按钮 - 仅作品可用
        favoriteButton.setOnClickListener {
            if (imageType == "draft") {
                Toast.makeText(this, R.string.draft_cannot_favorite, Toast.LENGTH_SHORT).show()
            } else {
                toggleFavorite()
            }
        }
        
        // 导出按钮 - 仅作品可用，将作品导出到系统相册
        exportButton.setOnClickListener {
            if (imageType == "draft") {
                Toast.makeText(this, R.string.draft_cannot_export, Toast.LENGTH_SHORT).show()
            } else {
                exportToGallery()
            }
        }
        
        // 详情按钮
        findViewById<LinearLayout>(R.id.details_button).setOnClickListener {
            showImageDetails()
        }
    }
    
    private fun setupUIByImageType() {
        when (imageType) {
            "draft" -> {
                // 草稿：隐藏收藏标识，禁用收藏和导出按钮
                favoriteBadge.visibility = View.GONE
                favoriteButton.alpha = 0.5f
                exportButton.alpha = 0.5f
            }
            "work", "favorite" -> {
                // 作品/收藏：正常显示所有按钮
                favoriteButton.alpha = 1.0f
                exportButton.alpha = 1.0f
                updateFavoriteUI()
            }
        }
    }
    
    private fun loadImage(imagePath: String) {
        val imageSource: Any = if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
            File(imagePath.removePrefix("file://"))
        } else {
            android.net.Uri.parse(imagePath)
        }
        
        Glide.with(this)
            .load(imageSource)
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                ) {
                    imageView.setImageDrawable(resource)
                    // 图片加载完成后初始化矩阵
                    imageView.post {
                        initializeMatrix()
                    }
                }
                
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // 清理资源
                }
            })
    }
    
    /**
     * 更新收藏状态UI
     */
    private fun updateFavoriteUI() {
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.ic_heart_full)
            favoriteText.text = getString(R.string.favorited)
            favoriteBadge.visibility = View.VISIBLE
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_heart_null)
            favoriteText.text = getString(R.string.favorite)
            favoriteBadge.visibility = View.GONE
        }
    }
    
    /**
     * 切换收藏状态
     */
    private fun toggleFavorite() {
        if (imageId == -1L) {
            Toast.makeText(this, R.string.cannot_favorite, Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                isFavorite = editedImageRepository.toggleFavorite(imageId)
                runOnUiThread {
                    updateFavoriteUI()
                    val messageRes = if (isFavorite) R.string.added_to_favorites else R.string.removed_from_favorites
                    Toast.makeText(this@ImageViewerActivity, messageRes, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ImageViewerActivity, getString(R.string.operation_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 导出作品到系统相册
     * 仅作品可用，草稿需要先在编辑器中导出为作品
     */
    private fun exportToGallery() {
        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, R.string.cannot_export, Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val file = getImageFile()
                if (file == null || !file.exists()) {
                    runOnUiThread {
                        Toast.makeText(this@ImageViewerActivity, R.string.image_not_exists, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val fileName = "exported_${System.currentTimeMillis()}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyApplication")
                }
                
                val resolver = contentResolver
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
                            if (imageId != -1L) {
                                editedImageRepository.updateExportedUri(imageId, uri.toString())
                            }
                            
                            runOnUiThread {
                                Toast.makeText(this@ImageViewerActivity, R.string.exported_to_gallery, Toast.LENGTH_SHORT).show()
                            }
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
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ImageViewerActivity, getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 打开编辑器
     */
    private fun openEditor() {
        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, R.string.cannot_edit, Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, EditorActivity::class.java)
        
        // 将文件路径转换为正确的URI格式
        val imageUri = if (imagePath!!.startsWith("/")) {
            // 文件路径，转换为file URI
            android.net.Uri.fromFile(File(imagePath!!)).toString()
        } else if (imagePath!!.startsWith("file://")) {
            // 已经是file URI格式
            imagePath
        } else {
            // 其他URI格式（如content://）
            imagePath
        }
        
        intent.putExtra("image_uri", imageUri)
        // 不传递draft_id，这样编辑保存后会创建新作品
        startActivity(intent)
    }
    
    /**
     * 显示图片详情对话框
     */
    private fun showImageDetails() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_details, null)
        
        val tvStoragePath = dialogView.findViewById<TextView>(R.id.tv_storage_path)
        val tvTimeLabel = dialogView.findViewById<TextView>(R.id.tv_time_label)
        val tvTimeValue = dialogView.findViewById<TextView>(R.id.tv_time_value)
        val layoutResolution = dialogView.findViewById<LinearLayout>(R.id.layout_resolution)
        val tvResolution = dialogView.findViewById<TextView>(R.id.tv_resolution)
        val layoutFileSize = dialogView.findViewById<LinearLayout>(R.id.layout_file_size)
        val tvFileSize = dialogView.findViewById<TextView>(R.id.tv_file_size)
        val layoutFormat = dialogView.findViewById<LinearLayout>(R.id.layout_format)
        val tvFormat = dialogView.findViewById<TextView>(R.id.tv_format)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)
        
        // 设置存储位置（绝对路径）
        val absolutePath = getAbsoluteImagePath()
        tvStoragePath.text = absolutePath ?: getString(R.string.unknown)
        
        // 根据图片类型设置时间标签和值
        // 草稿：显示最后编辑时间（草稿通过保存功能保存）
        // 作品/收藏：显示保存时间（作品通过导出功能保存，收藏只是作品的标记）
        lifecycleScope.launch {
            when (imageType) {
                "draft" -> {
                    // 草稿显示最后编辑时间
                    tvTimeLabel.text = getString(R.string.last_edit_time)
                    if (draftId != -1L) {
                        val draft = draftRepository.getDraftById(draftId)
                        draft?.let {
                            tvTimeValue.text = formatTime(it.modifiedAt)
                        } ?: run {
                            tvTimeValue.text = getString(R.string.unknown)
                        }
                    } else {
                        tvTimeValue.text = getString(R.string.unknown)
                    }
                }
                "favorite", "work" -> {
                    // 作品和收藏显示保存时间（收藏只是作品的一个标记，便于查找）
                    tvTimeLabel.text = getString(R.string.save_time)
                    if (imageId != -1L) {
                        val editedImage = editedImageRepository.getEditedImageById(imageId)
                        editedImage?.let {
                            tvTimeValue.text = formatTime(it.createdAt)
                        } ?: run {
                            tvTimeValue.text = getString(R.string.unknown)
                        }
                    } else {
                        tvTimeValue.text = getString(R.string.unknown)
                    }
                }
                else -> {
                    tvTimeLabel.text = getString(R.string.time)
                    tvTimeValue.text = getString(R.string.unknown)
                }
            }
        }
        
        // 尝试获取图片分辨率、大小和格式
        try {
            val file = getImageFile()
            if (file != null && file.exists()) {
                // 获取文件大小
                val fileSize = file.length()
                layoutFileSize.visibility = View.VISIBLE
                tvFileSize.text = formatFileSize(fileSize)
                
                // 获取文件格式
                val extension = file.extension.uppercase()
                if (extension.isNotEmpty()) {
                    layoutFormat.visibility = View.VISIBLE
                    tvFormat.text = extension
                }
                
                // 获取图片分辨率
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    layoutResolution.visibility = View.VISIBLE
                    tvResolution.text = "${options.outWidth} × ${options.outHeight}"
                }
            }
        } catch (e: Exception) {
            // 无法获取额外信息，保持隐藏状态
            android.util.Log.e("ImageViewerActivity", "获取图片信息失败", e)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * 获取图片文件
     */
    private fun getImageFile(): File? {
        if (imagePath.isNullOrEmpty()) return null
        
        return try {
            when {
                imagePath!!.startsWith("/") -> File(imagePath!!)
                imagePath!!.startsWith("file://") -> File(imagePath!!.removePrefix("file://"))
                else -> {
                    // 尝试从content URI获取文件路径
                    val uri = android.net.Uri.parse(imagePath)
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        // 创建临时文件来获取信息
                        val tempFile = File.createTempFile("temp_image", ".tmp", cacheDir)
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        tempFile
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取图片的绝对路径
     * 尝试从各种路径格式中获取绝对路径
     */
    private fun getAbsoluteImagePath(): String? {
        if (imagePath.isNullOrEmpty()) return null
        
        return try {
            when {
                // 已经是绝对路径
                imagePath!!.startsWith("/") -> {
                    File(imagePath!!).absolutePath
                }
                // file:// URI格式
                imagePath!!.startsWith("file://") -> {
                    File(imagePath!!.removePrefix("file://")).absolutePath
                }
                // content:// URI格式，尝试获取真实路径
                imagePath!!.startsWith("content://") -> {
                    val uri = android.net.Uri.parse(imagePath)
                    getPathFromContentUri(uri) ?: imagePath
                }
                else -> imagePath
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "获取绝对路径失败", e)
            imagePath
        }
    }
    
    /**
     * 从content URI获取真实文件路径
     */
    private fun getPathFromContentUri(uri: Uri): String? {
        var filePath: String? = null
        
        try {
            // 尝试通过MediaStore查询
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            }
            
            // 如果上面的方法失败，尝试通过DocumentsContract获取
            if (filePath == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                if (android.provider.DocumentsContract.isDocumentUri(this, uri)) {
                    val docId = android.provider.DocumentsContract.getDocumentId(uri)
                    
                    when {
                        // ExternalStorageProvider
                        uri.authority == "com.android.externalstorage.documents" -> {
                            val split = docId.split(":")
                            val type = split[0]
                            if ("primary".equals(type, ignoreCase = true)) {
                                filePath = "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
                            }
                        }
                        // DownloadsProvider
                        uri.authority == "com.android.providers.downloads.documents" -> {
                            val contentUri = android.content.ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                docId.toLongOrNull() ?: 0L
                            )
                            filePath = getDataColumn(contentUri, null, null)
                        }
                        // MediaProvider
                        uri.authority == "com.android.providers.media.documents" -> {
                            val split = docId.split(":")
                            val type = split[0]
                            val contentUri = when (type) {
                                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }
                            contentUri?.let {
                                filePath = getDataColumn(it, "_id=?", arrayOf(split[1]))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "从content URI获取路径失败", e)
        }
        
        return filePath
    }
    
    /**
     * 从MediaStore获取DATA列
     */
    private fun getDataColumn(uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)
        
        try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "getDataColumn失败", e)
        }
        
        return null
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    private fun handleTouch(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(midPoint, event)
                    mode = ZOOM
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    matrix.postTranslate(dx, dy)
                } else if (mode == ZOOM && event.pointerCount >= 2) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y)
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                // 检查并修正缩放范围
                checkAndFixScale()
            }
        }
        
        imageView.imageMatrix = matrix
        return true
    }
    
    private fun spacing(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }
    
    private fun midPoint(point: PointF, event: MotionEvent) {
        if (event.pointerCount < 2) return
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
    
    /**
     * 初始化矩阵，使图片居中显示并计算合适的初始缩放
     */
    private fun initializeMatrix() {
        val drawable = imageView.drawable ?: return
        
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return
        }
        
        // 计算适合屏幕的缩放比例（fit center）
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        initialScale = minOf(scaleX, scaleY)
        
        // 设置最小缩放为初始缩放的一半，允许用户缩小查看
        minScale = initialScale * 0.5f
        
        // 计算居中偏移
        val dx = (viewWidth - imageWidth * initialScale) / 2f
        val dy = (viewHeight - imageHeight * initialScale) / 2f
        
        // 设置初始矩阵
        matrix.reset()
        matrix.postScale(initialScale, initialScale)
        matrix.postTranslate(dx, dy)
        
        imageView.imageMatrix = matrix
        savedMatrix.set(matrix)
    }
    
    private fun checkAndFixScale() {
        val values = FloatArray(9)
        matrix.getValues(values)
        val currentScale = values[Matrix.MSCALE_X]
        
        // 使用动态计算的最小缩放值
        val effectiveMinScale = minScale.coerceAtMost(initialScale * 0.5f)
        
        if (currentScale < effectiveMinScale) {
            // 缩放太小，重置到最小缩放
            val scaleFactor = effectiveMinScale / currentScale
            matrix.postScale(scaleFactor, scaleFactor, imageView.width / 2f, imageView.height / 2f)
        } else if (currentScale > maxScale) {
            // 缩放太大，限制到最大缩放
            val scaleFactor = maxScale / currentScale
            matrix.postScale(scaleFactor, scaleFactor, imageView.width / 2f, imageView.height / 2f)
        }
        
        imageView.imageMatrix = matrix
    }
    
    /**
     * 双击重置到初始状态
     */
    private fun resetToInitialScale() {
        initializeMatrix()
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return true
        }
    }
}