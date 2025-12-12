package com.example.myapplication.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    //裁剪比例枚举
    enum class AspectRatio(val ratio: Float, val displayName: String) {
        FREE(0f, "自由"),
        RATIO_1_1(1f, "1:1"),
        RATIO_3_4(3f / 4f, "3:4"),
        RATIO_4_3(4f / 3f, "4:3"),
        RATIO_9_16(9f / 16f, "9:16"),
        RATIO_16_9(16f / 9f, "16:9")
    }

    // 画笔
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val gridPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 150
    }

    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 120}

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    // 裁剪框矩形
    private val cropRect = RectF()
    private val initialCropRect = RectF()// 保存初始裁剪框状态
    // 当前裁剪比例
    private var currentAspectRatio = AspectRatio.FREE
    
    // 图片显示区域（用于限制裁剪框）
    private var imageDisplayRect = RectF()
    private var hasImageDisplayRect = false
    
    // 触摸相关
    private var activePointerId = -1
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchMode = TouchMode.NONE
    
    // 最小裁剪框尺寸（像素）
    private val minCropSize = 100f
    
    // 角点触摸区域大小
    private val cornerTouchSize = 80f
    
    private enum class TouchMode {
        NONE,
        DRAG,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    init {
        // 初始化裁剪框（延迟到设置图片显示区域后）
        post {
            if (!hasImageDisplayRect) {
                // 如果没有设置图片显示区域，使用默认初始化
                initializeDefaultCropRect()
            }
        }
    }
    
    /**
     * 默认初始化裁剪框
     */
    private fun initializeDefaultCropRect() {
        val w = width.toFloat()
        val h = height.toFloat()
        val size = min(w, h) * 0.8f
        val left = (w - size) / 2
        val top = (h - size) / 2
        cropRect.set(left, top, left + size, top + size)
        initialCropRect.set(cropRect)
        invalidate()
    }
    
    /**
     * 设置图片显示区域
     */
    fun setImageDisplayRect(rect: RectF) {
        imageDisplayRect.set(rect)
        hasImageDisplayRect = true
        
        // 初始化裁剪框为图片显示区域的90%
        val padding = min(rect.width(), rect.height()) * 0.05f
        cropRect.set(
            rect.left + padding,
            rect.top + padding,
            rect.right - padding,
            rect.bottom - padding
        )
        initialCropRect.set(cropRect)
        invalidate()
    }

    /**
     * 设置裁剪比例
     */
    fun setAspectRatio(ratio: AspectRatio) {
        currentAspectRatio = ratio
        if (ratio != AspectRatio.FREE) {
            adjustCropRectToAspectRatio()
        }
        invalidate()
    }

    /**
     * 获取当前裁剪比例
     */
    fun getCurrentAspectRatio(): AspectRatio {
        return currentAspectRatio
    }

    /**
     * 获取当前裁剪框
     */
    fun getCropRect(): RectF {
        return RectF(cropRect)
    }

    /**
     * 重置裁剪框到初始状态
     */
    fun resetCropRect() {
        cropRect.set(initialCropRect)
        currentAspectRatio = AspectRatio.FREE
        invalidate()
    }

    /**
     * 根据比例调整裁剪框
     */
    private fun adjustCropRectToAspectRatio() {
        if (currentAspectRatio == AspectRatio.FREE) return
        
        val centerX = cropRect.centerX()
        val centerY = cropRect.centerY()
        val currentWidth = cropRect.width()
        val currentHeight = cropRect.height()
        
        val targetRatio = currentAspectRatio.ratio
        val newWidth: Float
        val newHeight: Float
        
        if (currentWidth / currentHeight > targetRatio) {
            // 当前太宽，固定高度调整宽度
            newHeight = currentHeight
            newWidth = newHeight * targetRatio
        } else {
            // 当前太高，固定宽度调整高度
            newWidth = currentWidth
            newHeight = newWidth / targetRatio
        }
        
        cropRect.set(
            centerX - newWidth / 2,
            centerY - newHeight / 2,
            centerX + newWidth / 2,
            centerY + newHeight / 2
        )
        
        constrainCropRect()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()
        
        // 绘制半透明遮罩（裁剪框外的区域）
        canvas.drawRect(0f, 0f, w, cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, w, h, overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, overlayPaint)
        
        // 绘制裁剪框边框
        canvas.drawRect(cropRect, borderPaint)
        
        // 绘制九宫格线
        val gridWidth = cropRect.width() / 3
        val gridHeight = cropRect.height() / 3
        
        //垂直线
        canvas.drawLine(cropRect.left + gridWidth, cropRect.top,cropRect.left + gridWidth, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + gridWidth * 2, cropRect.top,cropRect.left + gridWidth * 2, cropRect.bottom, gridPaint)
        
        // 水平线
        canvas.drawLine(cropRect.left, cropRect.top + gridHeight,
            cropRect.right, cropRect.top + gridHeight, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + gridHeight * 2, 
            cropRect.right, cropRect.top + gridHeight * 2, gridPaint)
        
        // 绘制四个角的标记
        val cornerSize = 30f
        
        // 左上角
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top, cornerPaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + cornerSize, cornerPaint)
        
        // 右上角
        canvas.drawLine(cropRect.right - cornerSize, cropRect.top, cropRect.right, cropRect.top, cornerPaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + cornerSize, cornerPaint)
        
        // 左下角
        canvas.drawLine(cropRect.left, cropRect.bottom - cornerSize, cropRect.left, cropRect.bottom, cornerPaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + cornerSize, cropRect.bottom, cornerPaint)
        
        // 右下角
        canvas.drawLine(cropRect.right - cornerSize, cropRect.bottom, cropRect.right, cropRect.bottom, cornerPaint)
        canvas.drawLine(cropRect.right, cropRect.bottom - cornerSize, cropRect.right, cropRect.bottom, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y
                touchMode = getTouchMode(event.x, event.y)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == -1) return false
                
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return false
                
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                
                when (touchMode) {
                    TouchMode.DRAG -> moveCropRect(dx, dy)
                    TouchMode.TOP_LEFT -> resizeCropRect(dx, dy, true, true, false, false)
                    TouchMode.TOP_RIGHT -> resizeCropRect(dx, dy, false, true, true, false)
                    TouchMode.BOTTOM_LEFT -> resizeCropRect(dx, dy, true, false, false, true)
                    TouchMode.BOTTOM_RIGHT -> resizeCropRect(dx, dy, false, false, true, true)
                    TouchMode.LEFT -> resizeCropRect(dx, 0f, true, false, false, false)
                    TouchMode.RIGHT -> resizeCropRect(dx, 0f, false, false, true, false)
                    TouchMode.TOP -> resizeCropRect(0f, dy, false, true, false, false)
                    TouchMode.BOTTOM -> resizeCropRect(0f, dy, false, false, false, true)
                    else -> {}
                }
                
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
                touchMode = TouchMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 判断触摸模式
     */
    private fun getTouchMode(x: Float, y: Float): TouchMode {
        // 检查四个角
        if (isInCorner(x, y, cropRect.left, cropRect.top)) return TouchMode.TOP_LEFT
        if (isInCorner(x, y, cropRect.right, cropRect.top)) return TouchMode.TOP_RIGHT
        if (isInCorner(x, y, cropRect.left, cropRect.bottom)) return TouchMode.BOTTOM_LEFT
        if (isInCorner(x, y, cropRect.right, cropRect.bottom)) return TouchMode.BOTTOM_RIGHT
        
        // 检查四条边
        if (isOnEdge(x, y, cropRect.left, cropRect.top, cropRect.left, cropRect.bottom)) return TouchMode.LEFT
        if (isOnEdge(x, y, cropRect.right, cropRect.top, cropRect.right, cropRect.bottom)) return TouchMode.RIGHT
        if (isOnEdge(x, y, cropRect.left, cropRect.top, cropRect.right, cropRect.top)) return TouchMode.TOP
        if (isOnEdge(x, y, cropRect.left, cropRect.bottom, cropRect.right, cropRect.bottom)) return TouchMode.BOTTOM
        
        // 检查是否在裁剪框内
        if (cropRect.contains(x, y)) return TouchMode.DRAG
        
        return TouchMode.NONE
    }

    /**
     * 判断是否在角点区域
     */
    private fun isInCorner(x: Float, y: Float, cornerX: Float, cornerY: Float): Boolean {
        return abs(x - cornerX) <= cornerTouchSize && abs(y - cornerY) <= cornerTouchSize
    }

    /**
     * 判断是否在边缘
     */
    private fun isOnEdge(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val touchThreshold = 40f
        return if (x1 == x2) { // 垂直边
            abs(x - x1) <= touchThreshold && y >= min(y1, y2) && y <= max(y1, y2)
        } else { // 水平边
            abs(y - y1) <= touchThreshold && x >= min(x1, x2) && x <= max(x1, x2)
        }
    }

    /**
     * 移动裁剪框
     */
    private fun moveCropRect(dx: Float, dy: Float) {
        cropRect.offset(dx, dy)
        constrainCropRect()
    }

    /**
     * 调整裁剪框大小
     */
    private fun resizeCropRect(dx: Float, dy: Float, left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
        val newRect = RectF(cropRect)
        
        if (currentAspectRatio != AspectRatio.FREE) {
            // 固定比例调整
            resizeWithAspectRatio(newRect, dx, dy, left, top, right, bottom)
        } else {
            // 自由调整
            if (left) newRect.left += dx
            if (right) newRect.right += dx
            if (top) newRect.top += dy
            if (bottom) newRect.bottom += dy
        }
        
        // 确保最小尺寸
        if (newRect.width() >= minCropSize && newRect.height() >= minCropSize) {
            cropRect.set(newRect)
            constrainCropRect()
        }
    }

    /**
     * 按比例调整大小 - 改进版
     */
    private fun resizeWithAspectRatio(rect: RectF, dx: Float, dy: Float, left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
        val ratio = currentAspectRatio.ratio
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        
        when {
            // 四个角点- 对角线调整，保持中心点
            (left && top) || (right && bottom) -> {
                val effectiveDelta = maxOf(abs(dx), abs(dy) * ratio)
                val direction = if ((left && top && dx < 0) || (right && bottom && dx > 0)) -1f else 1f
                val newWidth = rect.width() + effectiveDelta * direction * 2
                val newHeight = newWidth / ratio
                
                rect.set(
                    centerX - newWidth / 2,
                    centerY - newHeight / 2,
                    centerX + newWidth / 2,
                    centerY + newHeight / 2
                )
            }
            (left && bottom) || (right && top) -> {
                val effectiveDelta = maxOf(abs(dx), abs(dy) * ratio)
                val direction = if ((left && bottom && dx < 0) || (right && top && dx > 0)) -1f else 1f
                val newWidth = rect.width() + effectiveDelta * direction * 2
                val newHeight = newWidth / ratio
                
                rect.set(
                    centerX - newWidth / 2,
                    centerY - newHeight / 2,
                    centerX + newWidth / 2,
                    centerY + newHeight / 2
                )
            }
            // 边缘拖动 - 从中心等比例缩放
            left || right -> {
                val widthDelta = if (left) -dx *2 else dx * 2
                val newWidth = rect.width() + widthDelta
                val newHeight = newWidth / ratio
                
                rect.set(
                    centerX - newWidth / 2,
                    centerY - newHeight / 2,
                    centerX + newWidth / 2,
                    centerY + newHeight / 2
                )
            }
            top || bottom -> {
                val heightDelta = if (top) -dy * 2 else dy * 2
                val newHeight = rect.height() + heightDelta
                val newWidth = newHeight * ratio
                
                rect.set(
                    centerX - newWidth / 2,
                    centerY - newHeight / 2,
                    centerX + newWidth / 2,
                    centerY + newHeight / 2
                )
            }
        }
    }

    /**
     * 约束裁剪框在图片显示区域内 - 改进版
     */
    private fun constrainCropRect() {
        // 如果设置了图片显示区域，限制在图片区域内；否则限制在视图范围内
        val boundLeft = if (hasImageDisplayRect) imageDisplayRect.left else 0f
        val boundTop = if (hasImageDisplayRect) imageDisplayRect.top else 0f
        val boundRight = if (hasImageDisplayRect) imageDisplayRect.right else width.toFloat()
        val boundBottom = if (hasImageDisplayRect) imageDisplayRect.bottom else height.toFloat()
        
        // 确保裁剪框不超出边界
        if (cropRect.left < boundLeft) {
            val offset = boundLeft - cropRect.left
            cropRect.left = boundLeft
            cropRect.right = min(cropRect.right + offset, boundRight)
        }
        if (cropRect.top < boundTop) {
            val offset = boundTop - cropRect.top
            cropRect.top = boundTop
            cropRect.bottom = min(cropRect.bottom + offset, boundBottom)
        }
        if (cropRect.right > boundRight) {
            val offset = cropRect.right - boundRight
            cropRect.right = boundRight
            cropRect.left = max(cropRect.left - offset, boundLeft)
        }
        if (cropRect.bottom > boundBottom) {
            val offset = cropRect.bottom - boundBottom
            cropRect.bottom = boundBottom
            cropRect.top = max(cropRect.top - offset, boundTop)
        }
        
        // 双重检查确保完全在边界内
        cropRect.left = cropRect.left.coerceIn(boundLeft, boundRight)
        cropRect.top = cropRect.top.coerceIn(boundTop, boundBottom)
        cropRect.right = cropRect.right.coerceIn(cropRect.left, boundRight)
        cropRect.bottom = cropRect.bottom.coerceIn(cropRect.top, boundBottom)
    }
}