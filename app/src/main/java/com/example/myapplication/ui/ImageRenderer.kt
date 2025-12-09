package com.example.myapplication.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

interface ScreenshotListener{
    fun onScreenshotTaken(bitmap: Bitmap)
}

class ImageRenderer (private val context: Context, private val imageUri: Uri): GLSurfaceView.Renderer {
    // 1. 定义顶点数据和纹理坐标数据
    private val vertexData=floatArrayOf(
        -1.0f,-1.0f,
        1.0f,-1.0f,
        -1.0f,1.0f,
        1.0f,1.0f
    )
    private val vertexBuffer: FloatBuffer
    private var imageWidth:Int=0
    private var imageHeight:Int=0
    private val textureData=floatArrayOf(
        0.0f,1.0f,
        1.0f,1.0f,
        0.0f,0.0f,
        1.0f,0.0f
    )
    private val textureBuffer: FloatBuffer

    private val transformMatrix=FloatArray(16)
    @Volatile
    var scaleFactor=1.0f
    @Volatile
    var offsetX=0.0f
    @Volatile
    var offsetY=0.0f
    @Volatile
    var isGrayscaleEnabled=false

    private var screenshotListener:ScreenshotListener?=null
    private var captureNextFrame=false
    
    // 裁剪相关
    private var cropRect: android.graphics.RectF? = null
    private var croppedBitmap: Bitmap? = null
    private var pendingCrop = false
    private var originalBitmap: Bitmap? = null

    // 2. 定义OpenGL程序
    init{
        vertexBuffer= ByteBuffer.allocateDirect(vertexData.size*4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
        vertexBuffer.position(0)

        textureBuffer=ByteBuffer.allocateDirect(textureData.size*4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureData)
        textureBuffer.position(0)
    }

    fun setScreenshotListener(listener:ScreenshotListener){
        screenshotListener=listener
    }
    
    fun takeScreenshot(){
        captureNextFrame=true
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        originalBitmap?.recycle()
        originalBitmap = null
        croppedBitmap?.recycle()
        croppedBitmap = null
    }
    
    /**
     * 设置裁剪区域（归一化坐标 0-1）
     */
    fun setCropRect(rect: android.graphics.RectF) {
        cropRect = rect
        pendingCrop = true
    }
    
    /**
     * 应用裁剪（在 GL 线程中调用）
     */
    private fun applyCrop() {
        cropRect?.let { rect ->
            try {
                // 获取当前使用的 bitmap（裁剪后的或原始的）
                val sourceBitmap = croppedBitmap ?: originalBitmap ?: return
                
                // 计算裁剪区域的实际像素坐标
                val cropX = (rect.left * sourceBitmap.width).toInt()
                val cropY = (rect.top * sourceBitmap.height).toInt()
                val cropWidth = ((rect.right - rect.left) * sourceBitmap.width).toInt()
                val cropHeight = ((rect.bottom - rect.top) * sourceBitmap.height).toInt()
                
                // 确保裁剪区域有效
                val validCropX = cropX.coerceIn(0, sourceBitmap.width - 1)
                val validCropY = cropY.coerceIn(0, sourceBitmap.height - 1)
                val validCropWidth = cropWidth.coerceIn(1, sourceBitmap.width - validCropX)
                val validCropHeight = cropHeight.coerceIn(1, sourceBitmap.height - validCropY)
                
                // 裁剪图片
                val newCroppedBitmap = Bitmap.createBitmap(
                    sourceBitmap,
                    validCropX,
                    validCropY,
                    validCropWidth,
                    validCropHeight
                )
                
                // 释放旧的裁剪 bitmap
                if (croppedBitmap != null && croppedBitmap != sourceBitmap) {
                    croppedBitmap?.recycle()
                }
                croppedBitmap = newCroppedBitmap
                
                // 更新图片尺寸
                imageWidth = croppedBitmap!!.width
                imageHeight = croppedBitmap!!.height
                
                // 重新加载纹理（在 GL 线程中）
                reloadTexture(croppedBitmap!!)
                
                // 重置变换
                scaleFactor = 1.0f
                offsetX = 0.0f
                offsetY = 0.0f
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 重新加载纹理
     */
    private fun reloadTexture(bitmap: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    // 3. 定义顶点着色器和片段着色器
    private val vertexShaderCode="""
        uniform mat4 u_TransformMatrix;
        attribute vec4 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;
        void main(){
            gl_Position=u_TransformMatrix*a_Position;
            v_TexCoord=a_TexCoord;
        }
    """
    private val fragmentShaderCode="""
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform int u_GrayscaleEnabled;
        void main(){
            vec4 color =texture2D(u_Texture,v_TexCoord);
            if(u_GrayscaleEnabled==1){
                float gray=0.299*color.r+0.587*color.g+0.114*color.b;
                gl_FragColor=vec4(gray,gray,gray,1.0);
            }else{
                gl_FragColor=color;
            }
        }
    """
    private var programId:Int=0
    private var textureId:Int=0
    private var transformMatrixHandle:Int=0
    private var grayscaleEnabledHandle:Int=0


    // 4. 在onSurfaceCreated中加载OpenGL程序和纹理
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f)
        val vertexShader=loadShader(GLES20.GL_VERTEX_SHADER,vertexShaderCode)
        val fragmentShader=loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode)
        programId=GLES20.glCreateProgram().also{
            GLES20.glAttachShader(it,vertexShader)
            GLES20.glAttachShader(it,fragmentShader)
            GLES20.glLinkProgram(it)
        }
        textureId=loadTexture(context,imageUri)
        transformMatrixHandle=GLES20.glGetUniformLocation(programId,"u_TransformMatrix")
        grayscaleEnabledHandle=GLES20.glGetUniformLocation(programId,"u_GrayscaleEnabled")
        
        // 保存原始 bitmap 用于裁剪
        originalBitmap = context.contentResolver.openInputStream(imageUri).use {
            BitmapFactory.decodeStream(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0,width,height)
        if(imageHeight==0||imageHeight==0||width==0||height==0)return
        val screenRatio=width.toFloat()/height.toFloat()
        val imageRatio=imageWidth.toFloat()/imageHeight.toFloat()
        var left=-1.0f
        var right=1.0f
        var bottom=-1.0f
        var top=1.0f
        if(screenRatio>imageRatio){
            // 视口更宽，根据高度缩放
            val newWidth=imageRatio/screenRatio
            left=-newWidth
            right=newWidth
        }else{
            // 视口更高，根据宽度缩放
            val newHeight=screenRatio/imageRatio
            bottom=-newHeight
            top=newHeight
        }
        val newVertexData=floatArrayOf(
            left,bottom,
            right,bottom,
            left,top,
            right,top
        )

        vertexBuffer.clear()
        vertexBuffer.put(newVertexData)
        vertexBuffer.position(0)
    }

    // 5. 在onDrawFrame中绘制矩形
    override fun onDrawFrame(gl: GL10?) {
        // 处理待处理的裁剪操作
        if (pendingCrop) {
            pendingCrop = false
            applyCrop()
        }
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // 1. 使用我们的OpenGL程序
        GLES20.glUseProgram(programId)
        GLES20.glUniform1i(grayscaleEnabledHandle,if(isGrayscaleEnabled)1 else 0)
        // 2. 设置变换矩阵
        Matrix.setIdentityM(transformMatrix,0)
        Matrix.scaleM(transformMatrix,0,scaleFactor,scaleFactor,1.0f)
        Matrix.translateM(transformMatrix,0,offsetX,offsetY,0.0f)

        // 2. 获取着色器中变量的句柄
        val positionHandle=GLES20.glGetAttribLocation(programId,"a_Position")
        val texCoordHandle=GLES20.glGetAttribLocation(programId,"a_TexCoord")
        val textureHandle=GLES20.glGetUniformLocation(programId,"u_Texture")

        GLES20.glUniformMatrix4fv(transformMatrixHandle,1,false,transformMatrix,0)

        // 3. 启用顶点属性数组
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        // 4. 将顶点数据传递给着色器
        GLES20.glVertexAttribPointer(positionHandle,2,GLES20.GL_FLOAT,false,0,vertexBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle,2,GLES20.GL_FLOAT,false,0,textureBuffer)
        // 5. 激活并绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId)
        // 6. 将纹理单元传递给着色器
        GLES20.glUniform1i(textureHandle,0)
        // 7. 绘制矩形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        if(captureNextFrame) {
            captureNextFrame = false
            val viewport = IntArray(4)
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
            val width = viewport[2]
            val height = viewport[3]

            val buffer = ByteBuffer.allocateDirect(width * height * 4)
            buffer.order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(
                0,
                0,
                width,
                height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                buffer
            )
            buffer.rewind()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            val matrix = android.graphics.Matrix()
            matrix.preScale(1.0f, -1.0f)
            val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
            bitmap.recycle()
            screenshotListener?.onScreenshotTaken(flippedBitmap)
        }
        // 8. 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    // 6. 定义辅助函数加载OpenGL程序和纹理
    private fun loadShader(type:Int,shaderCode:String):Int{
        return GLES20.glCreateShader(type).also{shader->
            GLES20.glShaderSource(shader,shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    // 7. 定义辅助函数加载纹理
    private fun loadTexture(context:Context,uri:Uri):Int{
        val textureIds= IntArray(1)
        GLES20.glGenTextures(1,textureIds,0)
        if(textureIds[0]==0){
            return 0
        }
        val bitmap=context.contentResolver.openInputStream(uri).use{
            BitmapFactory.decodeStream(it)
        }
        imageWidth=bitmap.width
        imageHeight=bitmap.height

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureIds[0])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)

        bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        return textureIds[0]
    }
}