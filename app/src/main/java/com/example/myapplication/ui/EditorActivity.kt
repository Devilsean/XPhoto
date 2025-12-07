package com.example.myapplication.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import java.io.OutputStream

class EditorActivity : AppCompatActivity(), ScreenshotListener {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ImageRenderer
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var lastTouchX:Float=0.0f
    private var lastTouchY:Float=0.0f
    private var exitAfterSave=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        glSurfaceView=findViewById(R.id.gl_surface_view)
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
        findViewById<Button>(R.id.grayscale_button).setOnClickListener{
            renderer.isGrayscaleEnabled=!renderer.isGrayscaleEnabled
            glSurfaceView.requestRender()
        }
        findViewById<Button>(R.id.export_button).setOnClickListener {
            renderer.takeScreenshot()
            glSurfaceView.requestRender()
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
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
    override fun onPause(){
        super.onPause()
        glSurfaceView.onPause()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
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
        return true
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