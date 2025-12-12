package com.example.myapplication.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class ShimmerImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var shimmerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f
    private val shimmerWidth = 200f
    
    init {
        // 设置扫光渐变
        shimmerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        startShimmerAnimation()
    }

    private fun startShimmerAnimation() {
        post {  // 等待 View 测量完成
            if (width > 0) {
                shimmerAnimator = ValueAnimator.ofFloat(-shimmerWidth, width + shimmerWidth).apply {
                    duration = 1500
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    interpolator = LinearInterpolator()
                    addUpdateListener { animation ->
                        shimmerTranslate = animation.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制扫光效果
        if (drawable != null) {
            val shader = LinearGradient(
                shimmerTranslate - shimmerWidth / 2,
                0f,
                shimmerTranslate + shimmerWidth / 2,
                0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(80, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            shimmerPaint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        shimmerAnimator?.cancel()
    }
}
