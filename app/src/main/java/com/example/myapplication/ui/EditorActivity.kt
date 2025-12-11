package com.example.myapplication.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.data.entity.Draft
import com.example.myapplication.data.entity.EditedImage
import com.example.myapplication.data.repository.DraftRepository
import com.example.myapplication.data.repository.EditedImageRepository
import com.example.myapplication.ui.widget.CropOverlayView
import kotlinx.coroutines.launch
import java.io.OutputStream


class EditorActivity : AppCompatActivity(), ScreenshotListener {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ImageRenderer
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
    // 数据库相关
    private lateinit var draftRepository: DraftRepository
    private lateinit var editedImageRepository: EditedImageRepository
    
    private var lastTouchX: Float = 0.0f
    private var lastTouchY: Float = 0.0f
    private var exitAfterSave = false
    
    // 当前草稿ID，如果是新建则为null
    private var currentDraftId: Long? = null
    // 原始图片URI
    private var originalImageUri: String? = null
    
    // 裁剪相关
    private lateinit var cropOverlayView: CropOverlayView
    private var isCropMode = false
    private lateinit var editorButtonCard: MaterialCardView
    private lateinit var cropActionCard: MaterialCardView
    private lateinit var aspectRatioScroll: HorizontalScrollView
    
    // 图片显示区域（用于裁剪坐标转换）
    private var imageDisplayRect = android.graphics.RectF()
    
    // 滤镜相关
    private lateinit var filterScroll: HorizontalScrollView
    private lateinit var filterRecyclerView: RecyclerView
    private lateinit var filterAdapter: FilterAdapter
    private var isFilterMode = false
    
    // 调整相关
    private lateinit var adjustmentPanelCard: MaterialCardView
    private lateinit var adjustmentRecyclerView: RecyclerView
    private lateinit var adjustmentAdapter: AdjustmentAdapter
    private var isAdjustmentMode = false
    private lateinit var editHistory: EditHistory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        
        // 初始化Repository
        val app = application as MyApplication
        draftRepository = app.draftRepository
        editedImageRepository = app.editedImageRepository
        
        // 获取图片URI和草稿ID（如果有）
        originalImageUri = intent.getStringExtra("image_uri")
        currentDraftId = intent.getLongExtra("draft_id", -1).takeIf { it != -1L }
        
        // 验证图片URI
        if (originalImageUri == null || originalImageUri!!.isEmpty()) {
            Toast.makeText(this, "未提供图片URI", Toast.LENGTH_LONG).show()
            android.util.Log.e("EditorActivity", "图片URI为空或null")
            finish()
            return
        }
        
        android.util.Log.d("EditorActivity", "接收到图片URI: $originalImageUri")
        
        // 初始化视图
        glSurfaceView = findViewById(R.id.gl_surface_view)
        cropOverlayView = findViewById(R.id.crop_overlay_view)
        editorButtonCard = findViewById(R.id.bottom_toolbar_card)
        cropActionCard = findViewById(R.id.crop_action_card)
        aspectRatioScroll = findViewById(R.id.aspect_ratio_scroll)
        filterScroll = findViewById(R.id.filter_scroll)
        adjustmentPanelCard = findViewById(R.id.adjustment_panel_card)
        adjustmentRecyclerView = findViewById(R.id.adjustment_recycler_view)
        
        // 初始化编辑历史
        editHistory = EditHistory()
        filterRecyclerView = findViewById(R.id.filter_recycler_view)
        
        // 初始化 OpenGL
        try {
            glSurfaceView.setEGLContextClientVersion(2)
            val imageUri = Uri.parse(originalImageUri)
            renderer = ImageRenderer(this, imageUri)
            renderer.setScreenshotListener(this)
            glSurfaceView.setRenderer(renderer)
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            
            // 如果是加载草稿，恢复草稿状态
            if (currentDraftId != null) {
                loadDraft(currentDraftId!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        
        setupButtons()
        setupFilterRecyclerView()
        
        // 手动保存草稿按钮（可选）
        // findViewById<Button>(R.id.save_draft_button).setOnClickListener {
        setupAdjustmentRecyclerView()
        //     saveDraft()
        // }
        
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }
    
    private fun setupButtons() {
        // 顶部工具栏按钮
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            showExitConfirmationDialog()
        }
        
        findViewById<ImageButton>(R.id.undo_button).setOnClickListener {
            performUndo()
        }
        
        findViewById<ImageButton>(R.id.redo_button).setOnClickListener {
            performRedo()
        }
        
        // 裁剪按钮（LinearLayout）
        findViewById<LinearLayout>(R.id.crop_button).setOnClickListener {
            enterCropMode()
        }
        
        // 滤镜按钮
        findViewById<LinearLayout>(R.id.filter_button).setOnClickListener {
            enterFilterMode()
        }
        
        // 灰度按钮
        // 调整按钮
        findViewById<LinearLayout>(R.id.adjust_button).setOnClickListener {
            enterAdjustmentMode()
        }
        findViewById<Button>(R.id.grayscale_button).setOnClickListener {
            renderer.isGrayscaleEnabled = !renderer.isGrayscaleEnabled
            glSurfaceView.requestRender()
            //状态改变时自动保存草稿
            autoSaveDraft()
        }
        
        // 导出按钮
        findViewById<Button>(R.id.export_button).setOnClickListener {
            renderer.takeScreenshot()
            glSurfaceView.requestRender()
        }
        
        // 裁剪确认按钮
        findViewById<Button>(R.id.crop_confirm_button).setOnClickListener {
            applyCrop()
        }
        
        // 裁剪取消按钮
        findViewById<Button>(R.id.crop_cancel_button).setOnClickListener {
            exitCropMode()
        }
        // 裁剪重置按钮
        findViewById<ImageButton>(R.id.crop_reset_button).setOnClickListener {
            cropOverlayView.resetCropRect()
        }
        
        // 裁剪旋转按钮
        findViewById<ImageButton>(R.id.crop_rotate_button).setOnClickListener {
            rotateCropRect()
        }
        
        // 比例按钮（Chip）
        findViewById<com.google.android.material.chip.Chip>(R.id.ratio_free).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.FREE)
        }
        findViewById<com.google.android.material.chip.Chip>(R.id.ratio_1_1).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_1_1)
        }
        findViewById<com.google.android.material.chip.Chip>(R.id.ratio_3_4).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_3_4)
        }
        findViewById<com.google.android.material.chip.Chip>(R.id.ratio_4_3).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_4_3)
        }
        findViewById<com.google.android.material.chip.Chip>(R.id.ratio_9_16).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_9_16)
        }
        findViewById<com.google.android.material.chip.Chip>(R.id.ratio_16_9).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_16_9)
        }
    }
    
    /**
     * 设置滤镜RecyclerView
     */
    private fun setupFilterRecyclerView() {
        filterRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        
        // 创建滤镜列表
        val filters = FilterType.values().toList()
        
        // 创建适配器
        filterAdapter = FilterAdapter(filters, null) { selectedFilter ->
            applyFilter(selectedFilter)
        }
        
        filterRecyclerView.adapter = filterAdapter
    }
    
    /**
     * 进入滤镜模式
     */
    private fun enterFilterMode() {
        isFilterMode = true
        filterScroll.visibility = View.VISIBLE
        editorButtonCard.visibility = View.GONE
    }
    
    /**
     * 退出滤镜模式
     */
    private fun exitFilterMode() {
        isFilterMode = false
        filterScroll.visibility = View.GONE
        editorButtonCard.visibility = View.VISIBLE
    }
    
    /**
     * 应用滤镜
     */
    private fun applyFilter(filter: FilterType) {
        renderer.currentFilter = filter
        glSurfaceView.requestRender()
        autoSaveDraft()
        
        // 应用滤镜后退出滤镜模式
        exitFilterMode()
        Toast.makeText(this, "已应用${filter.displayName}滤镜", Toast.LENGTH_SHORT).show()
    }
    /**
     * 设置调整RecyclerView
     */
    private fun setupAdjustmentRecyclerView() {
        adjustmentRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // 创建所有调整类型列表
        val adjustmentTypes = AdjustmentType.values().toList()
        
        // 创建适配器
        adjustmentAdapter = AdjustmentAdapter(
            adjustmentTypes,
            renderer.adjustmentParams
        ) { adjustmentType, value ->
            onAdjustmentChanged(adjustmentType, value)
        }
        
        adjustmentRecyclerView.adapter = adjustmentAdapter
        
        // 设置调整面板按钮
        findViewById<ImageButton>(R.id.adjustment_close_button).setOnClickListener {
            exitAdjustmentMode()
        }
        
        findViewById<Button>(R.id.adjustment_reset_button).setOnClickListener {
            resetAllAdjustments()
        }
    }
    
    /**
     * 进入调整模式
     */
    private fun enterAdjustmentMode() {
        isAdjustmentMode = true
        adjustmentPanelCard.visibility = View.VISIBLE
        editorButtonCard.visibility = View.GONE
        
        // 保存当前状态到历史记录
        saveCurrentStateToHistory()
    }
    
    /**
     * 退出调整模式
     */
    private fun exitAdjustmentMode() {
        isAdjustmentMode = false
        adjustmentPanelCard.visibility = View.GONE
        editorButtonCard.visibility = View.VISIBLE
    }
    
    /**
     * 调整参数改变时的回调
     */
    private fun onAdjustmentChanged(adjustmentType: AdjustmentType, value: Float) {
        // 更新参数
        adjustmentType.setValue(renderer.adjustmentParams, value)
        
        // 实时渲染
        glSurfaceView.requestRender()
        
        // 自动保存草稿
        autoSaveDraft()
    }
    
    /**
     * 重置所有调整
     */
    private fun resetAllAdjustments() {
        renderer.adjustmentParams.reset()
        adjustmentAdapter.updateValues()
        glSurfaceView.requestRender()
        autoSaveDraft()
        Toast.makeText(this, "已重置所有调整", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 保存当前状态到历史记录
     */
    private fun saveCurrentStateToHistory() {
        val currentState = EditState(
            adjustmentParams = renderer.adjustmentParams.copy(),
            scaleFactor = renderer.scaleFactor,
            offsetX = renderer.offsetX,
            offsetY = renderer.offsetY,
            cropRect = renderer.getCropRect(),
            isGrayscaleEnabled = renderer.isGrayscaleEnabled,
            filterType = renderer.currentFilter
        )
        editHistory.addState(currentState)
        updateUndoRedoButtons()
    }
    
    /**
     * 更新撤销/恢复按钮状态
     */
    private fun updateUndoRedoButtons() {
        findViewById<ImageButton>(R.id.undo_button).isEnabled = editHistory.canUndo()
        findViewById<ImageButton>(R.id.redo_button).isEnabled = editHistory.canRedo()
    }
    
    /**
     * 执行撤销操作
     */
    private fun performUndo() {
        val previousState = editHistory.undo()
        if (previousState != null) {
            restoreEditState(previousState)
            Toast.makeText(this, "已撤销", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 执行恢复操作
     */
    private fun performRedo() {
        val nextState = editHistory.redo()
        if (nextState != null) {
            restoreEditState(nextState)
            Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 恢复编辑状态
     */
    private fun restoreEditState(state: EditState) {
        // 恢复调整参数
        renderer.adjustmentParams = state.adjustmentParams.copy()
        adjustmentAdapter.updateValues()
        
        // 恢复变换参数
        renderer.scaleFactor = state.scaleFactor
        renderer.offsetX = state.offsetX
        renderer.offsetY = state.offsetY
        
        // 恢复裁剪
        state.cropRect?.let { renderer.setCropRect(it) }
        
        // 恢复滤镜
        renderer.isGrayscaleEnabled = state.isGrayscaleEnabled
        renderer.currentFilter = state.filterType
        
        // 重新渲染
        glSurfaceView.requestRender()
        
        // 更新按钮状态
        updateUndoRedoButtons()
        
        // 自动保存
        autoSaveDraft()
    }
    
    /**
     * 进入裁剪模式
     */
    private fun enterCropMode() {
        isCropMode = true
        
        // 计算图片在屏幕上的实际显示区域
        calculateImageDisplayRect()
        
        // 将显示区域传递给裁剪覆盖层
        cropOverlayView.setImageDisplayRect(imageDisplayRect)
        
        cropOverlayView.visibility = View.VISIBLE
        editorButtonCard.visibility = View.GONE
        cropActionCard.visibility = View.VISIBLE
        aspectRatioScroll.visibility = View.VISIBLE
    }
    
    /**
     * 退出裁剪模式
     */
    private fun exitCropMode() {
        isCropMode = false
        cropOverlayView.visibility = View.GONE
        editorButtonCard.visibility = View.VISIBLE
        cropActionCard.visibility = View.GONE
        aspectRatioScroll.visibility = View.GONE
        cropOverlayView.resetCropRect()
    }
    
    /**
     * 应用裁剪
     */
    private fun applyCrop() {
        val cropRect = cropOverlayView.getCropRect()
        
        // 检查图片显示区域是否有效
        if (imageDisplayRect.width() <= 0 || imageDisplayRect.height() <= 0) {
            Toast.makeText(this, "裁剪区域无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 将裁剪框坐标转换为相对于图片显示区域的归一化坐标(0-1)
        val normalizedRect = android.graphics.RectF(
            (cropRect.left - imageDisplayRect.left) / imageDisplayRect.width(),
            (cropRect.top - imageDisplayRect.top) / imageDisplayRect.height(),
            (cropRect.right - imageDisplayRect.left) / imageDisplayRect.width(),
            (cropRect.bottom - imageDisplayRect.top) / imageDisplayRect.height()
        )
        
        // 确保坐标在有效范围内
        normalizedRect.left = normalizedRect.left.coerceIn(0f, 1f)
        normalizedRect.top = normalizedRect.top.coerceIn(0f, 1f)
        normalizedRect.right = normalizedRect.right.coerceIn(0f, 1f)
        normalizedRect.bottom = normalizedRect.bottom.coerceIn(0f, 1f)
        
        // 应用裁剪
        renderer.setCropRect(normalizedRect)
        glSurfaceView.requestRender()
        
        exitCropMode()
        Toast.makeText(this, "裁剪已应用", Toast.LENGTH_SHORT).show()
        
        // 裁剪后自动保存草稿
        autoSaveDraft()
    }
    
    /**
     * 旋转裁剪框（切换横竖比例）
     */
    private fun rotateCropRect() {
        val currentRatio = cropOverlayView.getCurrentAspectRatio()
        val newRatio = when (currentRatio) {
            CropOverlayView.AspectRatio.RATIO_3_4 -> CropOverlayView.AspectRatio.RATIO_4_3
            CropOverlayView.AspectRatio.RATIO_4_3 -> CropOverlayView.AspectRatio.RATIO_3_4
            CropOverlayView.AspectRatio.RATIO_9_16 -> CropOverlayView.AspectRatio.RATIO_16_9
            CropOverlayView.AspectRatio.RATIO_16_9 -> CropOverlayView.AspectRatio.RATIO_9_16
            else -> currentRatio
        }
        cropOverlayView.setAspectRatio(newRatio)
    }
    
    /**
     * 计算图片在GLSurfaceView中的实际显示区域
     */
    private fun calculateImageDisplayRect() {
        val viewWidth = glSurfaceView.width.toFloat()
        val viewHeight = glSurfaceView.height.toFloat()
        
        // 获取图片尺寸
        val imageWidth = renderer.getImageWidth().toFloat()
        val imageHeight = renderer.getImageHeight().toFloat()
        
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            // 如果尺寸无效，使用整个视图
            imageDisplayRect.set(0f, 0f, viewWidth, viewHeight)
            return
        }
        
        val screenRatio = viewWidth / viewHeight
        val imageRatio = imageWidth / imageHeight
        
        val displayWidth: Float
        val displayHeight: Float
        
        if (screenRatio > imageRatio) {
            // 视口更宽，图片按高度适配
            displayHeight = viewHeight
            displayWidth = displayHeight * imageRatio
        } else {
            // 视口更高，图片按宽度适配
            displayWidth = viewWidth
            displayHeight = displayWidth / imageRatio
        }
        
        // 计算居中显示的位置
        val left = (viewWidth - displayWidth) / 2
        val top = (viewHeight - displayHeight) / 2
        
        imageDisplayRect.set(left, top, left + displayWidth, top + displayHeight)
    }

    private fun loadDraft(draftId: Long) {
        lifecycleScope.launch {
            try {
                val draft = draftRepository.getDraftById(draftId)
            if (draft != null) {
                //恢复编辑状态
                renderer.isGrayscaleEnabled = draft.isGrayscaleEnabled
                
                // 恢复滤镜
                draft.filterType?.let { filterTypeName ->
                    try {
                        val filterType = FilterType.valueOf(filterTypeName)
                        renderer.currentFilter = filterType
                        filterAdapter.setSelectedFilter(filterType)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                renderer.scaleFactor = draft.scaleFactor
                    renderer.offsetX = draft.offsetX
                    // 恢复调整参数
                    renderer.adjustmentParams.brightness = draft.brightness
                    renderer.adjustmentParams.contrast = draft.contrast
                    renderer.adjustmentParams.saturation = draft.saturation
                    renderer.adjustmentParams.highlights = draft.highlights
                    renderer.adjustmentParams.shadows = draft.shadows
                    renderer.adjustmentParams.temperature = draft.temperature
                    renderer.adjustmentParams.tint = draft.tint
                    renderer.adjustmentParams.clarity = draft.clarity
                    renderer.adjustmentParams.sharpen = draft.sharpen
                    renderer.offsetY = draft.offsetY
                    
                    // 恢复裁剪状态
                    if (draft.cropLeft != null && draft.cropTop != null &&
                        draft.cropRight != null && draft.cropBottom != null) {
                        val cropRect = android.graphics.RectF(
                            draft.cropLeft,
                            draft.cropTop,
                            draft.cropRight,
                            draft.cropBottom
                        )
                        renderer.setCropRect(cropRect)
                    }
                    
                    glSurfaceView.requestRender()
                    
                    Toast.makeText(this@EditorActivity, "草稿已加载", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditorActivity, "加载草稿失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun autoSaveDraft() {
        if (originalImageUri == null) return
        
        lifecycleScope.launch {
            try {
                // 获取裁剪信息
                val cropRect = renderer.getCropRect()
                
                val draft = Draft(
                    id = currentDraftId ?: 0,  // 如果是新草稿，id为0
                    originalImageUri = originalImageUri!!,
                    isGrayscaleEnabled = renderer.isGrayscaleEnabled,
                    filterType = renderer.currentFilter.name,
                    scaleFactor = renderer.scaleFactor,
                    offsetX = renderer.offsetX,
                    offsetY = renderer.offsetY,
                    cropLeft = cropRect?.left,
                    cropTop = cropRect?.top,
                    cropRight = cropRect?.right,
                    cropBottom = cropRect?.bottom,
                    // 保存调整参数
                    brightness = renderer.adjustmentParams.brightness,
                    contrast = renderer.adjustmentParams.contrast,
                    saturation = renderer.adjustmentParams.saturation,
                    highlights = renderer.adjustmentParams.highlights,
                    shadows = renderer.adjustmentParams.shadows,
                    temperature = renderer.adjustmentParams.temperature,
                    tint = renderer.adjustmentParams.tint,
                    clarity = renderer.adjustmentParams.clarity,
                    sharpen = renderer.adjustmentParams.sharpen,
                    modifiedAt = System.currentTimeMillis()
                )
                
                // 保存或更新草稿
                val draftId = draftRepository.saveOrUpdateDraft(draft)
                if (currentDraftId == null) {
                    currentDraftId = draftId
                    // 第一次保存时显示提示
                    Toast.makeText(this@EditorActivity, "草稿已自动保存", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 静默失败，不影响用户体验
                e.printStackTrace()
                android.util.Log.e("EditorActivity", "自动保存草稿失败", e)
            }
        }
    }

    private fun saveDraft() {
        if (originalImageUri == null) return
        
        lifecycleScope.launch {
            try {
                // 获取裁剪信息
                val cropRect = renderer.getCropRect()
                
                val draft = Draft(
                    id = currentDraftId ?: 0,
                    originalImageUri = originalImageUri!!,
                    isGrayscaleEnabled = renderer.isGrayscaleEnabled,
                    filterType = renderer.currentFilter.name,
                    scaleFactor = renderer.scaleFactor,
                    offsetX = renderer.offsetX,
                    offsetY = renderer.offsetY,
                    cropLeft = cropRect?.left,
                    cropTop = cropRect?.top,
                    cropRight = cropRect?.right,
                    cropBottom = cropRect?.bottom,
                    // 保存调整参数
                    brightness = renderer.adjustmentParams.brightness,
                    contrast = renderer.adjustmentParams.contrast,
                    saturation = renderer.adjustmentParams.saturation,
                    highlights = renderer.adjustmentParams.highlights,
                    shadows = renderer.adjustmentParams.shadows,
                    temperature = renderer.adjustmentParams.temperature,
                    tint = renderer.adjustmentParams.tint,
                    clarity = renderer.adjustmentParams.clarity,
                    sharpen = renderer.adjustmentParams.sharpen,
                    modifiedAt = System.currentTimeMillis()
                )
                
                val draftId = draftRepository.saveOrUpdateDraft(draft)
                currentDraftId = draftId
                Toast.makeText(this@EditorActivity, "草稿保存成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EditorActivity, "保存草稿失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出编辑")
            .setMessage("是否要保存当前更改？")
            .setPositiveButton("保存") { _, _ ->
                exitAfterSave = true
                renderer.takeScreenshot()
                glSurfaceView.requestRender()
            }
            .setNegativeButton("不保存") { _, _ ->
                finish()
            }
            .setNeutralButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        // 暂停时自动保存草稿
        autoSaveDraft()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::renderer.isInitialized) {
            renderer.cleanup()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //裁剪模式或滤镜模式下不处理图片的缩放和移动
        if (isCropMode || isFilterMode) {
            return super.onTouchEvent(event)
        }
        
        scaleGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    renderer.offsetX += dx * 2/ glSurfaceView.width
                    renderer.offsetY -= dy * 2 / glSurfaceView.height
                    glSurfaceView.requestRender()
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                // 触摸结束时自动保存草稿
                autoSaveDraft()
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.scaleFactor *= detector.scaleFactor
            renderer.scaleFactor = Math.max(0.1f, Math.min(renderer.scaleFactor, 5.0f))
            glSurfaceView.requestRender()
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // 缩放结束时自动保存草稿
            autoSaveDraft()
        }
    }

    override fun onScreenshotTaken(bitmap: Bitmap) {
        val fileName = "edited_image_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyApplication")
        }
        val resolver = contentResolver
        var stream: OutputStream? = null
        var uri: Uri? = null
        
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    // 保存编辑记录到数据库
                    saveEditedImageRecord(uri.toString())
                    runOnUiThread {
                        Toast.makeText(this, "图片已保存至相册", Toast.LENGTH_SHORT).show()
                        if (exitAfterSave) {
                            finish()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
            runOnUiThread {
                Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
                if (exitAfterSave) {
                    finish()
                }
            }
        } finally {
            stream?.close()
        }
    }

    private fun saveEditedImageRecord(editedUri: String) {
        if (originalImageUri == null) return
        
        lifecycleScope.launch {
            try {
                val editedImage = EditedImage(
                    originalImageUri = originalImageUri!!,
                    editedImageUri = editedUri,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )
                editedImageRepository.saveEditedImage(editedImage)
                
                // 导出成功后，可以选择删除草稿
                // if (currentDraftId != null) {
                //     val draft = draftRepository.getDraftById(currentDraftId!!)
                //     if (draft != null) {
                //         draftRepository.deleteDraft(draft)
                //     }
                // }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}