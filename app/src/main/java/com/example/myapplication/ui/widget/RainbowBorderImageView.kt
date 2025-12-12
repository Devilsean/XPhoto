package com.example.myapplication.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class RainbowBorderImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val contentPath = Path() // 用于裁剪图片内容
    private var contentRectF = RectF() // 用于计算图片内容的矩形区域
    private var rotationAngle = 0f
    private var animator: ValueAnimator? = null
    private val borderWidth = 8f // 边框宽度

    init {
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth
        scaleType = ScaleType.CENTER_CROP
        startRotationAnimation()
    }

    private fun startRotationAnimation() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 确保在尺寸变化时重新计算裁剪区域
        val halfBorder = borderWidth / 2f

        // 裁剪区域 (图片内容) 的矩形，要留出边框的空间
        contentRectF.set(halfBorder, halfBorder, w - halfBorder, h - halfBorder)

        // 创建圆形的裁剪路径
        contentPath.reset()
        contentPath.addCircle(
            w / 2f,
            h / 2f,
            min(contentRectF.width(), contentRectF.height()) / 2f,
            Path.Direction.CW
        )
    }

    override fun onDraw(canvas: Canvas) {
        // 1. 裁剪并绘制图片内容 (Image View 的默认绘制)
        canvas.save()
        canvas.clipPath(contentPath) // 将画布裁剪成圆形
        super.onDraw(canvas) // 绘制裁剪后的图片（头像）
        canvas.restore() // 恢复画布，以便绘制完整的边框

        // 2. 绘制旋转的彩虹边框
        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制边框的半径，使其居中于视图边缘和裁剪边缘之间
        val borderRadius = min(width, height) / 2f - (borderWidth / 2f)

        val gradient = SweepGradient(
            centerX, centerY,
            intArrayOf(
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.RED
            ),
            null
        )

        // 旋转渐变效果
        val matrix = Matrix()
        matrix.postRotate(rotationAngle, centerX, centerY)
        gradient.setLocalMatrix(matrix)

        borderPaint.shader = gradient
        canvas.drawCircle(centerX, centerY, borderRadius, borderPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}