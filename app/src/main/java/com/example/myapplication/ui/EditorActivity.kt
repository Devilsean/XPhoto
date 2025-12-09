package com.example.myapplication.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.data.entity.EditedImage
import com.example.myapplication.ui.widget.CropOverlayView
import kotlinx.coroutines.launch
import java.io.OutputStream

class EditorActivity : AppCompatActivity(),ScreenshotListener {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ImageRenderer
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var cropOverlayView: CropOverlayView

    private var lastTouchX:Float=0.0f
    private var lastTouchY:Float=0.0f
    private var exitAfterSave=false
    private var isCropMode=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        
        glSurfaceView=findViewById(R.id.gl_surface_view)
        cropOverlayView=findViewById(R.id.crop_overlay_view)
        
        glSurfaceView.setEGLContextClientVersion(2)
        val imageUriString=intent.getStringExtra("image_uri")
        if(imageUriString!=null){
            val imageUri= Uri.parse(imageUriString)
            renderer=ImageRenderer(this,imageUri)
            renderer.setScreenshotListener(this)
            glSurfaceView.setRenderer(renderer)
            glSurfaceView.renderMode=GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        scaleGestureDetector=ScaleGestureDetector(this,ScaleListener())
        
        // 为GLSurfaceView 设置触摸监听器
        glSurfaceView.setOnTouchListener { _, event ->
            if (isCropMode) {
                //裁剪模式下不处理触摸，让触摸事件穿透
                false
            } else {
                // 非裁剪模式下处理缩放和拖动
                handleImageTouch(event)
                true
            }
        }
        
        setupButtons()
        setupCropRatioButtons()
        
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isCropMode) {
                    exitCropMode()
                } else {
                    showExitConfirmationDialog()
                }
            }
        })
    }
    
    private fun setupButtons() {
        // 顶部工具栏按钮
        findViewById<View>(R.id.back_button)?.setOnClickListener {
            Log.d("EditorActivity", "Back button clicked")
            if (isCropMode) {
                exitCropMode()
            } else {
                showExitConfirmationDialog()
            }
        }
        
        findViewById<View>(R.id.undo_button)?.setOnClickListener {
            Log.d("EditorActivity", "Undo button clicked")
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Toast.makeText(this, "撤销功能待实现", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.redo_button)?.setOnClickListener {
            Log.d("EditorActivity", "Redo button clicked")
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Toast.makeText(this, "重做功能待实现", Toast.LENGTH_SHORT).show()
        }
        
        // 底部编辑按钮
        findViewById<Button>(R.id.crop_button)?.setOnClickListener {
            Log.d("EditorActivity", "Crop button clicked")
            Toast.makeText(this, "裁剪按钮被点击", Toast.LENGTH_SHORT).show()
            enterCropMode()
        }?: Log.e("EditorActivity", "crop_button not found!")
        
        findViewById<Button>(R.id.grayscale_button)?.setOnClickListener{
            Log.d("EditorActivity", "Grayscale button clicked")
            renderer.isGrayscaleEnabled=!renderer.isGrayscaleEnabled
            glSurfaceView.requestRender()
        }
        
        findViewById<Button>(R.id.export_button)?.setOnClickListener {
            Log.d("EditorActivity", "Export button clicked")
            renderer.takeScreenshot()
            glSurfaceView.requestRender()
        }
        
        // 裁剪模式按钮
        findViewById<Button>(R.id.crop_cancel_button)?.setOnClickListener {
            Log.d("EditorActivity", "Cancel button clicked")
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            exitCropMode()
        }
        
        findViewById<Button>(R.id.crop_confirm_button)?.setOnClickListener {
            Log.d("EditorActivity", "Confirm button clicked")
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            applyCrop()
        }
        
        findViewById<Button>(R.id.crop_reset_button)?.setOnClickListener {
            Log.d("EditorActivity", "Reset button clicked")
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            resetCrop()
        }
        
        findViewById<Button>(R.id.crop_rotate_button)?.setOnClickListener {
            Log.d("EditorActivity", "Rotate button clicked")
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            rotateCrop()
        }
    }
    
    private fun setupCropRatioButtons() {
        findViewById<Button>(R.id.ratio_free).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.FREE)
        }
        findViewById<Button>(R.id.ratio_1_1).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_1_1)
        }
        findViewById<Button>(R.id.ratio_3_4).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_3_4)
        }
        findViewById<Button>(R.id.ratio_4_3).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_4_3)
        }
        findViewById<Button>(R.id.ratio_9_16).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_9_16)
        }
        findViewById<Button>(R.id.ratio_16_9).setOnClickListener {
            cropOverlayView.setAspectRatio(CropOverlayView.AspectRatio.RATIO_16_9)
        }
    }
    
    private fun enterCropMode() {
        Log.d("EditorActivity", "Entering crop mode")
        isCropMode = true
        cropOverlayView.isVisible = true
        findViewById<View>(R.id.aspect_ratio_scroll).isVisible = true
        findViewById<LinearLayout>(R.id.crop_action_layout).isVisible = true
        findViewById<LinearLayout>(R.id.editor_button_layout).isVisible = false
        Toast.makeText(this, "进入裁剪模式", Toast.LENGTH_SHORT).show()
    }
    
    private fun exitCropMode() {
        isCropMode = false
        cropOverlayView.isVisible = false
        findViewById<View>(R.id.aspect_ratio_scroll).isVisible = false
        findViewById<LinearLayout>(R.id.crop_action_layout).isVisible = false
        findViewById<LinearLayout>(R.id.editor_button_layout).isVisible = true
    }
    
    private fun applyCrop() {
        val cropRect = cropOverlayView.getCropRect()
        val viewWidth = cropOverlayView.width.toFloat()
        val viewHeight = cropOverlayView.height.toFloat()
        
        // 将裁剪框坐标转换为归一化坐标 (0-1)
        val normalizedCropRect = RectF(
            cropRect.left / viewWidth,
            cropRect.top / viewHeight,
            cropRect.right / viewWidth,
            cropRect.bottom / viewHeight
        )
        
        // 传递给 renderer 进行裁剪
        renderer.setCropRect(normalizedCropRect)
        glSurfaceView.requestRender()
        
        exitCropMode()
        Toast.makeText(this, "裁剪已应用", Toast.LENGTH_SHORT).show()
    }
    
    private fun resetCrop() {
        cropOverlayView.resetCropRect()
        Toast.makeText(this, "已重置裁剪框", Toast.LENGTH_SHORT).show()
    }
    
    private fun rotateCrop() {
        // 90度旋转裁剪框（交换比例）
        val currentRatio = cropOverlayView.getCurrentAspectRatio()
        val newRatio = when (currentRatio) {
            CropOverlayView.AspectRatio.RATIO_3_4 -> CropOverlayView.AspectRatio.RATIO_4_3
            CropOverlayView.AspectRatio.RATIO_4_3 -> CropOverlayView.AspectRatio.RATIO_3_4
            CropOverlayView.AspectRatio.RATIO_9_16 -> CropOverlayView.AspectRatio.RATIO_16_9
            CropOverlayView.AspectRatio.RATIO_16_9 -> CropOverlayView.AspectRatio.RATIO_9_16
            else -> currentRatio // 1:1 和自由模式不变
        }
        cropOverlayView.setAspectRatio(newRatio)
        Toast.makeText(this, "已旋转到 ${newRatio.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出编辑")
            .setMessage("是否要保存当前更改")
            .setPositiveButton("保存"){ _,_->
                exitAfterSave=true
                renderer.takeScreenshot()
                glSurfaceView.requestRender()
            }.setNegativeButton("不保存"){ _,_->
                finish()
            }.setNeutralButton("取消",null)
            .show()
    }

    override fun onResume(){
        super.onResume()
        glSurfaceView.onResume()
    }
    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::renderer.isInitialized) {
            renderer.cleanup()
        }
    }
    
    private fun handleImageTouch(event: MotionEvent) {
        scaleGestureDetector.onTouchEvent(event)
        when(event.action){
            MotionEvent.ACTION_DOWN-> {
                lastTouchX=event.x
                lastTouchY=event.y
            }
            MotionEvent.ACTION_MOVE-> {
                if(!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    renderer.offsetX += dx*2/glSurfaceView.width
                    renderer.offsetY -= dy*2/glSurfaceView.height
                    glSurfaceView.requestRender()
                }
                lastTouchX=event.x
                lastTouchY=event.y
            }
        }
    }
    
    private inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener(){
        override fun onScale(detector:ScaleGestureDetector): Boolean {
            renderer.scaleFactor*=detector.scaleFactor
            renderer.scaleFactor=Math.max(0.1f,Math.min(renderer.scaleFactor,5.0f))
            glSurfaceView.requestRender()
            return true
        }
    }
    override fun onScreenshotTaken(bitmap: Bitmap) {
        val fileName="edited_image_${System.currentTimeMillis()}.png"
        val contentValues= ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME,fileName)
            put(MediaStore.MediaColumns.MIME_TYPE,"image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/MyApplication")
        }
        val resolver=contentResolver
        var stream:OutputStream?=null
        var uri:Uri?=null
        try{
            uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)
            if(uri!=null) {
                stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    runOnUiThread {
                        lifecycleScope.launch {
                            val app = application as MyApplication
                            val editedImage = EditedImage(
                                id = 0,
                                originalImageUri = intent.getStringExtra("image_uri") ?: "",
                                editedImageUri = uri.toString(),
                                createdAt = System.currentTimeMillis()
                            )
                            app.editedImageRepository.saveEditedImage(editedImage)}
                        Toast.makeText(this, "图片已保存至相册", Toast.LENGTH_SHORT).show()
                        if(exitAfterSave){
                            finish()
                        }
                    }
                }
            }
        }catch (e:Exception){
            if(uri!=null){
                resolver.delete(uri, null, null)
            }
            runOnUiThread {
                Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
                if(exitAfterSave){
                    finish()
                }
            }
        }finally{
            stream?.close()
        }

    }
}