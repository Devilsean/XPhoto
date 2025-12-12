package com.example.myapplication.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
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
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs


class EditorActivity : AppCompatActivity(), ScreenshotListener {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ImageRenderer
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    
    // 数据库相关
    private lateinit var draftRepository: DraftRepository
    private lateinit var editedImageRepository: EditedImageRepository
    
    // 手势相关变量
    private var lastTouchX: Float = 0.0f
    private var lastTouchY: Float = 0.0f
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var isScaling: Boolean = false
    private var flingAnimator: ValueAnimator? = null
    
    // 缩放焦点相关
    private var lastFocusX: Float = 0f
    private var lastFocusY: Float = 0f
    
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
            
            // 延迟保存初始状态，确保renderer已完全初始化
            glSurfaceView.post {
                saveInitialState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        gestureDetector = GestureDetector(this, GestureListener())
        
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
        
        // 调整按钮
        findViewById<LinearLayout>(R.id.adjust_button).setOnClickListener {
            enterAdjustmentMode()
        }
        
        // 旋转按钮（逆时针90度）
        findViewById<LinearLayout>(R.id.rotate_left_button).setOnClickListener {
            rotateImage(-90f)
        }
        
        // 旋转按钮（顺时针90度）
        findViewById<LinearLayout>(R.id.rotate_right_button).setOnClickListener {
            rotateImage(90f)
        }
        
        // 灰度按钮（隐藏）
        findViewById<Button>(R.id.grayscale_button).setOnClickListener {
            renderer.isGrayscaleEnabled = !renderer.isGrayscaleEnabled
            glSurfaceView.requestRender()
            //状态改变时自动保存草稿
            autoSaveDraft()
        }
        
        // 保存按钮 - 保存为作品（生成最终图片并跳转到作品查看界面）
        // 草稿会自动保存，点击保存按钮时将草稿转化为作品
        findViewById<LinearLayout>(R.id.save_button).setOnClickListener {
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
        // 保存当前状态到历史记录（在修改之前）
        saveCurrentStateToHistory()
        
        renderer.currentFilter = filter
        glSurfaceView.requestRender()
        autoSaveDraft()
        
        // 应用滤镜后退出滤镜模式
        exitFilterMode()
        Toast.makeText(this, "已应用${filter.displayName}滤镜", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 旋转图片（增量旋转）
     * @param degrees 旋转角度，正值为顺时针，负值为逆时针
     */
    private fun rotateImage(degrees: Float) {
        // 保存当前状态到历史记录
        saveCurrentStateToHistory()
        
        // 获取当前角度并增加旋转
        val currentAngle = renderer.getRotation()
        var newAngle = currentAngle + degrees
        
        // 将角度规范化到 0-360 范围内
        while (newAngle >= 360f) newAngle -= 360f
        while (newAngle < 0f) newAngle += 360f
        
        // 只保留 0, 90, 180, 270 四个角度
        newAngle = when {
            newAngle < 45f -> 0f
            newAngle < 135f -> 90f
            newAngle < 225f -> 180f
            newAngle < 315f -> 270f
            else -> 0f
        }
        
        renderer.setRotation(newAngle)
        glSurfaceView.requestRender()
        autoSaveDraft()
        
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
        // 注意：不在进入模式时保存状态，而是在实际修改参数时保存
    }
    
    /**
     * 退出调整模式
     */
    private fun exitAdjustmentMode() {
        isAdjustmentMode = false
        adjustmentPanelCard.visibility = View.GONE
        editorButtonCard.visibility = View.VISIBLE
        
        // 重置防抖状态
        pendingHistorySave = false
        lastAdjustmentSaveTime = System.currentTimeMillis()
    }
    
    // 用于防抖的变量
    private var lastAdjustmentSaveTime = 0L
    private var pendingHistorySave = false
    
    /**
     * 调整参数改变时的回调
     */
    private fun onAdjustmentChanged(adjustmentType: AdjustmentType, value: Float) {
        // 在第一次修改时保存历史状态（防抖处理）
        val currentTime = System.currentTimeMillis()
        if (!pendingHistorySave && currentTime - lastAdjustmentSaveTime > 500) {
            saveCurrentStateToHistory()
            pendingHistorySave = true
        }
        
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
     * 保存初始状态到历史记录
     */
    private fun saveInitialState() {
        val initialState = EditState(
            adjustmentParams = renderer.adjustmentParams.copy(),
            scaleFactor = renderer.scaleFactor,
            offsetX = renderer.offsetX,
            offsetY = renderer.offsetY,
            rotationAngle = renderer.getRotation(),
            cropRect = renderer.getCropRect(),
            isGrayscaleEnabled = renderer.isGrayscaleEnabled,
            filterType = renderer.currentFilter,
            bitmapHistoryIndex = renderer.getBitmapHistoryIndex(),
            imageWidth = renderer.getImageWidth(),
            imageHeight = renderer.getImageHeight()
        )
        editHistory.addState(initialState)
        updateUndoRedoButtons()
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
            rotationAngle = renderer.getRotation(),
            cropRect = renderer.getCropRect(),
            isGrayscaleEnabled = renderer.isGrayscaleEnabled,
            filterType = renderer.currentFilter,
            bitmapHistoryIndex = renderer.getBitmapHistoryIndex(),
            imageWidth = renderer.getImageWidth(),
            imageHeight = renderer.getImageHeight()
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
        // 恢复bitmap历史（用于裁剪撤销）
        if (state.bitmapHistoryIndex >= 0) {
            val currentBitmapIndex = renderer.getBitmapHistoryIndex()
            if (currentBitmapIndex != state.bitmapHistoryIndex) {
                // bitmap索引不同，需要恢复bitmap
                renderer.restoreBitmapHistory(state.bitmapHistoryIndex)
            }
        }
        
        // 恢复调整参数
        renderer.adjustmentParams = state.adjustmentParams.copy()
        adjustmentAdapter.updateValues()
        
        // 恢复变换参数
        renderer.scaleFactor = state.scaleFactor
        renderer.offsetX = state.offsetX
        renderer.offsetY = state.offsetY
        
        // 恢复旋转角度
        renderer.setRotation(state.rotationAngle)
        
        // 清除裁剪区域（裁剪已经通过bitmap历史恢复了）
        renderer.clearCropRect()
        
        // 恢复滤镜
        renderer.isGrayscaleEnabled = state.isGrayscaleEnabled
        renderer.currentFilter = state.filterType
        
        // 更新滤镜选择状态
        filterAdapter.setSelectedFilter(state.filterType)
        
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
        
        // 保存当前状态到历史记录（在修改之前）
        saveCurrentStateToHistory()
        
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
        
        // 延迟保存裁剪后的状态，确保裁剪操作已在GL线程中完成
        // 这样可以正确记录裁剪后的bitmapHistoryIndex
        glSurfaceView.queueEvent {
            // 在GL线程中等待裁剪完成后，回到主线程保存状态
            glSurfaceView.post {
                saveCropStateToHistory()
                autoSaveDraft()
            }
        }
        
        exitCropMode()
        Toast.makeText(this, "裁剪已应用", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 保存裁剪后的状态到历史记录
     * 这个方法在裁剪操作完成后调用，确保bitmapHistoryIndex是正确的
     */
    private fun saveCropStateToHistory() {
        val currentState = EditState(
            adjustmentParams = renderer.adjustmentParams.copy(),
            scaleFactor = renderer.scaleFactor,
            offsetX = renderer.offsetX,
            offsetY = renderer.offsetY,
            rotationAngle = renderer.getRotation(),
            cropRect = null,  // 裁剪已应用，cropRect应为null
            isGrayscaleEnabled = renderer.isGrayscaleEnabled,
            filterType = renderer.currentFilter,
            bitmapHistoryIndex = renderer.getBitmapHistoryIndex(),
            imageWidth = renderer.getImageWidth(),
            imageHeight = renderer.getImageHeight()
        )
        editHistory.addState(currentState)
        updateUndoRedoButtons()
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
     * 考虑旋转角度：当旋转90度或270度时，图片的宽高会互换
     */
    private fun calculateImageDisplayRect() {
        val viewWidth = glSurfaceView.width.toFloat()
        val viewHeight = glSurfaceView.height.toFloat()
        
        // 获取图片尺寸
        val originalImageWidth = renderer.getImageWidth().toFloat()
        val originalImageHeight = renderer.getImageHeight().toFloat()
        
        if (originalImageWidth <= 0 || originalImageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            // 如果尺寸无效，使用整个视图
            imageDisplayRect.set(0f, 0f, viewWidth, viewHeight)
            return
        }
        
        // 检查当前旋转角度是否为90度或270度
        val rotationAngle = renderer.getRotation()
        val normalizedAngle = ((rotationAngle % 360) + 360) % 360
        val isRotated90or270 = normalizedAngle == 90f || normalizedAngle == 270f
        
        // 根据旋转角度确定有效的图片宽高
        val effectiveImageWidth = if (isRotated90or270) originalImageHeight else originalImageWidth
        val effectiveImageHeight = if (isRotated90or270) originalImageWidth else originalImageHeight
        
        val screenRatio = viewWidth / viewHeight
        val imageRatio = effectiveImageWidth / effectiveImageHeight
        
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
                    renderer.offsetY = draft.offsetY
                    
                    // 恢复旋转角度
                    renderer.setRotation(draft.rotationAngle)
                    
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
                    rotationAngle = renderer.getRotation(),
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
                    rotationAngle = renderer.getRotation(),
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
            .setMessage("当前编辑状态已自动保存为草稿。\n是否要保存为作品？")
            .setPositiveButton("保存作品") { _, _ ->
                exitAfterSave = true
                renderer.takeScreenshot()
                glSurfaceView.requestRender()
            }
            .setNegativeButton("仅保留草稿") { _, _ ->
                // 草稿已自动保存，直接退出
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
        // 裁剪模式或滤镜模式下不处理图片的缩放和移动
        if (isCropMode || isFilterMode) {
            return super.onTouchEvent(event)
        }
        
        // 先处理缩放手势
        scaleGestureDetector.onTouchEvent(event)
        // 处理惯性滑动
        gestureDetector.onTouchEvent(event)
        
        val action = event.actionMasked
        
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // 取消正在进行的惯性动画
                cancelFlingAnimation()
                
                // 记录第一个触摸点
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y
                isScaling = false
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 多指触摸开始，标记为缩放状态
                isScaling = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // 只有单指且不在缩放时才处理平移
                if (!scaleGestureDetector.isInProgress && !isScaling && event.pointerCount == 1) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        
                        // 应用平移，考虑当前缩放因子
                        applyTranslation(dx, dy)
                        
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                // 处理多指抬起
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                
                if (pointerId == activePointerId) {
                    // 当前跟踪的手指抬起，切换到另一个手指
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        lastTouchX = event.getX(newPointerIndex)
                        lastTouchY = event.getY(newPointerIndex)
                        activePointerId = event.getPointerId(newPointerIndex)
                    }
                }
                
                // 如果只剩一个手指，重置缩放状态
                if (event.pointerCount <= 2) {
                    isScaling = false
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isScaling = false
                // 触摸结束时自动保存草稿
                autoSaveDraft()
            }
        }
        return true
    }
    
    /**
     * 应用平移变换
     */
    private fun applyTranslation(dx: Float, dy: Float) {
        // 将屏幕像素转换为 OpenGL 坐标系的偏移量
        // 考虑当前缩放因子，使平移速度与缩放级别匹配
        val scaleAdjustedDx = dx * 2f / glSurfaceView.width / renderer.scaleFactor
        val scaleAdjustedDy = -dy * 2f / glSurfaceView.height / renderer.scaleFactor
        
        renderer.offsetX += scaleAdjustedDx
        renderer.offsetY += scaleAdjustedDy
        
        // 应用边界限制（可选，防止图片完全移出视野）
        applyBoundaryConstraints()
        
        glSurfaceView.requestRender()
    }
    
    /**
     * 应用边界约束，防止图片完全移出视野
     */
    private fun applyBoundaryConstraints() {
        // 计算最大允许偏移量（基于缩放因子）
        val maxOffset = 2f / renderer.scaleFactor
        
        // 限制偏移量在合理范围内
        renderer.offsetX = renderer.offsetX.coerceIn(-maxOffset, maxOffset)
        renderer.offsetY = renderer.offsetY.coerceIn(-maxOffset, maxOffset)
    }
    
    /**
     * 取消惯性动画
     */
    private fun cancelFlingAnimation() {
        flingAnimator?.cancel()
        flingAnimator = null
    }
    
    /**
     * 执行惯性滑动动画
     */
    private fun performFling(velocityX: Float, velocityY: Float) {
        cancelFlingAnimation()
        
        // 计算惯性滑动的初始速度（转换为 OpenGL 坐标系）
        val initialVelocityX = velocityX / glSurfaceView.width / renderer.scaleFactor
        val initialVelocityY = -velocityY / glSurfaceView.height / renderer.scaleFactor
        
        // 惯性动画持续时间
        val duration = 500L
        
        flingAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(2f)
            
            var lastFraction = 1f
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val delta = lastFraction - fraction
                lastFraction = fraction
                
                // 应用衰减的速度
                renderer.offsetX += initialVelocityX * delta * duration / 1000f
                renderer.offsetY += initialVelocityY * delta * duration / 1000f
                
                applyBoundaryConstraints()
                glSurfaceView.requestRender()
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    autoSaveDraft()
                }
            })
            
            start()
        }
    }
    
    /**
     * 手势监听器 - 处理惯性滑动
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // 只有在非缩放状态下才处理惯性滑动
            if (!isScaling && !scaleGestureDetector.isInProgress) {
                // 速度阈值，避免过小的滑动触发惯性
                val minVelocity = 500f
                if (abs(velocityX) > minVelocity || abs(velocityY) > minVelocity) {
                    performFling(velocityX, velocityY)
                    return true
                }
            }
            return false
        }
    }

    /**
     * 缩放手势监听器 - 支持焦点缩放
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            // 记录缩放开始时的焦点位置
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            isScaling = true
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val oldScale = renderer.scaleFactor
            
            // 计算新的缩放因子
            var newScale = oldScale * scaleFactor
            newScale = newScale.coerceIn(0.1f, 5.0f)
            
            // 计算焦点在 OpenGL 坐标系中的位置
            val focusX = detector.focusX
            val focusY = detector.focusY
            
            // 将屏幕焦点转换为归一化坐标 (-1 到 1)
            val normalizedFocusX = (focusX / glSurfaceView.width) * 2f - 1f
            val normalizedFocusY = 1f - (focusY / glSurfaceView.height) * 2f
            
            // 计算缩放前焦点在图片坐标系中的位置
            val imageX = (normalizedFocusX - renderer.offsetX * oldScale) / oldScale
            val imageY = (normalizedFocusY - renderer.offsetY * oldScale) / oldScale
            
            // 应用新的缩放因子
            renderer.scaleFactor = newScale
            
            // 调整偏移量，使焦点位置保持不变
            renderer.offsetX = (normalizedFocusX - imageX * newScale) / newScale
            renderer.offsetY = (normalizedFocusY - imageY * newScale) / newScale
            
            // 同时处理双指平移
            val dx = focusX - lastFocusX
            val dy = focusY - lastFocusY
            if (abs(dx) > 1 || abs(dy) > 1) {
                renderer.offsetX += dx * 2f / glSurfaceView.width / newScale
                renderer.offsetY -= dy * 2f / glSurfaceView.height / newScale
            }
            
            lastFocusX = focusX
            lastFocusY = focusY
            
            applyBoundaryConstraints()
            glSurfaceView.requestRender()
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
            // 缩放结束时自动保存草稿
            autoSaveDraft()
        }
    }

    override fun onScreenshotTaken(bitmap: Bitmap) {
        saveToWorks(bitmap)
    }
    
    /**
     * 保存为作品（保存最终编辑后的图片到应用内部存储，并跳转到作品查看界面）
     *
     * 流程说明：
     * 1. 草稿在编辑过程中自动保存编辑状态
     * 2. 点击"保存"按钮时，将当前编辑结果渲染为最终图片
     * 3. 保存图片到应用内部存储，创建作品记录
     * 4. 删除对应的草稿（草稿已转化为作品）
     * 5. 跳转到作品查看界面（ImageViewerActivity）
     *
     * 作品可以被收藏（收藏只是作品的一个标识，便于查找）
     * 作品可以在查看界面导出到系统相册
     */
    private fun saveToWorks(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val fileName = "work_${System.currentTimeMillis()}.png"
                // 保存到应用私有目录
                val worksDir = File(filesDir, "works")
                if (!worksDir.exists()) {
                    worksDir.mkdirs()
                }
                val file = File(worksDir, fileName)
                
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                
                // 保存记录到数据库
                // 作品默认不收藏，用户可以在查看时手动收藏
                val editedImage = EditedImage(
                    originalImageUri = originalImageUri ?: "",
                    editedImageUri = file.absolutePath,
                    isExported = false, // 未导出到系统相册
                    isFavorite = false, // 默认不收藏
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )
                val imageId = editedImageRepository.saveEditedImage(editedImage)
                
                // 保存作品后，删除对应的草稿（草稿已转化为作品）
                currentDraftId?.let { draftId ->
                    draftRepository.deleteDraft(draftId)
                    currentDraftId = null
                }
                
                runOnUiThread {
                    Toast.makeText(this@EditorActivity, "已保存为作品", Toast.LENGTH_SHORT).show()
                    
                    if (exitAfterSave) {
                        // 如果是退出时保存，直接结束
                        finish()
                    } else {
                        // 跳转到作品查看界面
                        val intent = Intent(this@EditorActivity, ImageViewerActivity::class.java)
                        intent.putExtra("image_path", file.absolutePath)
                        intent.putExtra("image_id", imageId)
                        intent.putExtra("is_favorite", false)
                        intent.putExtra("image_type", "work")
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@EditorActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    if (exitAfterSave) {
                        finish()
                    }
                }
            }
        }
    }
}