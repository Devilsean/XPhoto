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
    @Volatile
    var currentFilter: FilterType = FilterType.NONE
    
    // 调整参数
    @Volatile
    var adjustmentParams: AdjustmentParams = AdjustmentParams()

    private var screenshotListener:ScreenshotListener?=null
    private var captureNextFrame=false
    
    // 裁剪相关
    private var cropRect: android.graphics.RectF? = null
    private var croppedBitmap: Bitmap? = null
    private var pendingCrop = false
    private var originalBitmap: Bitmap? = null
    
    // FBO相关（用于离屏渲染导出图片）
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    private var fboInitialized = false

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
     * 获取当前图片宽度
     */
    fun getImageWidth(): Int {
        return imageWidth
    }
    
    /**
     * 获取当前图片高度
     */
    fun getImageHeight(): Int {
        return imageHeight
    }
    
    /**
     * 设置裁剪区域（归一化坐标 0-1）
     */
    fun setCropRect(rect: android.graphics.RectF) {
        cropRect = rect
        pendingCrop = true
    }
    
    /**
     * 获取当前裁剪区域
     */
    fun getCropRect(): android.graphics.RectF? {
        return cropRect
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
                
                // 检查裁剪区域是否有效
                if (validCropWidth <= 0 || validCropHeight <= 0) {
                    android.util.Log.e("ImageRenderer", "无效的裁剪区域")
                    return
                }
                
                // 裁剪图片
                val newCroppedBitmap = Bitmap.createBitmap(
                    sourceBitmap,
                    validCropX,
                    validCropY,
                    validCropWidth,
                    validCropHeight
                )
                
                // 释放旧的裁剪 bitmap（但不释放原始 bitmap）
                if (croppedBitmap != null && croppedBitmap != originalBitmap) {
                    croppedBitmap?.recycle()
                }
                croppedBitmap = newCroppedBitmap
                
                // 更新图片尺寸
                imageWidth = newCroppedBitmap.width
                imageHeight = newCroppedBitmap.height
                
                // 重新加载纹理（在 GL 线程中）
                reloadTexture(newCroppedBitmap)
                
                // 重置变换以适应新的裁剪图片
                scaleFactor = 1.0f
                offsetX = 0.0f
                offsetY = 0.0f
                
                // 清除裁剪矩形，避免重复应用
                cropRect = null
                
                android.util.Log.d("ImageRenderer", "裁剪成功: ${newCroppedBitmap.width}x${newCroppedBitmap.height}")
                
            } catch (e: Exception) {
                android.util.Log.e("ImageRenderer", "裁剪失败", e)
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

    // 3. 定义顶点着色器
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
    
    // 存储所有滤镜的程序ID
    private val filterPrograms = mutableMapOf<FilterType, Int>()
    private var programId:Int=0
    private var textureId:Int=0
    private var transformMatrixHandle:Int=0
    private var grayscaleEnabledHandle:Int=0


    // 4. 在onSurfaceCreated中加载OpenGL程序和纹理
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        android.util.Log.d("ImageRenderer", "onSurfaceCreated 开始")
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f)
        
        // 为每个滤镜创建着色器程序
        val vertexShader=loadShader(GLES20.GL_VERTEX_SHADER,vertexShaderCode)
        if (vertexShader == 0) {
            android.util.Log.e("ImageRenderer", "顶点着色器编译失败")
        }
        
        FilterType.values().forEach { filter ->
            val fragmentShader=loadShader(GLES20.GL_FRAGMENT_SHADER, filter.fragmentShader)
            if (fragmentShader == 0) {
                android.util.Log.e("ImageRenderer", "片段着色器编译失败: ${filter.name}")
            }
            val program = GLES20.glCreateProgram().also{
                GLES20.glAttachShader(it,vertexShader)
                GLES20.glAttachShader(it,fragmentShader)
                GLES20.glLinkProgram(it)
                
                // 检查链接状态
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] == 0) {
                    val error = GLES20.glGetProgramInfoLog(it)
                    android.util.Log.e("ImageRenderer", "程序链接失败 ${filter.name}: $error")
                    GLES20.glDeleteProgram(it)
                }
            }
            filterPrograms[filter] = program
            android.util.Log.d("ImageRenderer", "滤镜程序创建: ${filter.name} -> $program")
        }
        
        // 设置默认程序
        programId = filterPrograms[FilterType.NONE] ?: 0
        android.util.Log.d("ImageRenderer", "默认程序ID: $programId")
        
        try {
            textureId=loadTexture(context,imageUri)
            android.util.Log.d("ImageRenderer", "纹理ID: $textureId, 图片尺寸: ${imageWidth}x${imageHeight}")
            transformMatrixHandle=GLES20.glGetUniformLocation(programId,"u_TransformMatrix")
            
            // 保存原始 bitmap 用于裁剪
            originalBitmap = loadBitmapFromUri(context, imageUri)
            
            if (originalBitmap == null) {
                android.util.Log.e("ImageRenderer", "无法加载原始图片用于裁剪")
            } else {
                android.util.Log.d("ImageRenderer", "原始图片加载成功: ${originalBitmap!!.width}x${originalBitmap!!.height}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRenderer", "初始化渲染器失败", e)
            throw RuntimeException("初始化渲染器失败: ${e.message}", e)
        }
        android.util.Log.d("ImageRenderer", "onSurfaceCreated 完成")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0,width,height)
        if(imageWidth==0||imageHeight==0||width==0||height==0)return
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
        
        // 1. 根据当前滤镜选择程序（兼容旧的灰度模式）
        val activeFilter = if (isGrayscaleEnabled) FilterType.GRAYSCALE else currentFilter
        programId = filterPrograms[activeFilter] ?: filterPrograms[FilterType.NONE] ?: 0
        
        // 2. 使用选定的OpenGL程序
        GLES20.glUseProgram(programId)
        
        // 3. 更新变换矩阵句柄（每次可能使用不同的程序）
        transformMatrixHandle=GLES20.glGetUniformLocation(programId,"u_TransformMatrix")
        
        // 4. 设置变换矩阵
        Matrix.setIdentityM(transformMatrix,0)
        Matrix.scaleM(transformMatrix,0,scaleFactor,scaleFactor,1.0f)
        Matrix.translateM(transformMatrix,0,offsetX,offsetY,0.0f)

        // 2. 获取着色器中变量的句柄
        val positionHandle=GLES20.glGetAttribLocation(programId,"a_Position")
        val texCoordHandle=GLES20.glGetAttribLocation(programId,"a_TexCoord")
        val textureHandle=GLES20.glGetUniformLocation(programId,"u_Texture")

        GLES20.glUniformMatrix4fv(transformMatrixHandle,1,false,transformMatrix,0)
// 传递调整参数到着色器 (仅对NONE滤镜有效)
        if (activeFilter == FilterType.NONE) {
            val brightnessHandle = GLES20.glGetUniformLocation(programId, "u_Brightness")
            val contrastHandle = GLES20.glGetUniformLocation(programId, "u_Contrast")
            val saturationHandle = GLES20.glGetUniformLocation(programId, "u_Saturation")
            val highlightsHandle = GLES20.glGetUniformLocation(programId, "u_Highlights")
            val shadowsHandle = GLES20.glGetUniformLocation(programId, "u_Shadows")
            val temperatureHandle = GLES20.glGetUniformLocation(programId, "u_Temperature")
            val tintHandle = GLES20.glGetUniformLocation(programId, "u_Tint")
            val clarityHandle = GLES20.glGetUniformLocation(programId, "u_Clarity")
            val sharpenHandle = GLES20.glGetUniformLocation(programId, "u_Sharpen")
            
            GLES20.glUniform1f(brightnessHandle, adjustmentParams.brightness)
            GLES20.glUniform1f(contrastHandle, adjustmentParams.contrast)
            GLES20.glUniform1f(saturationHandle, adjustmentParams.saturation)
            GLES20.glUniform1f(highlightsHandle, adjustmentParams.highlights)
            GLES20.glUniform1f(shadowsHandle, adjustmentParams.shadows)
            GLES20.glUniform1f(temperatureHandle, adjustmentParams.temperature)
            GLES20.glUniform1f(tintHandle, adjustmentParams.tint)
            GLES20.glUniform1f(clarityHandle, adjustmentParams.clarity)
            GLES20.glUniform1f(sharpenHandle, adjustmentParams.sharpen)
        }

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
            // 使用离屏渲染导出纯图片内容（不包含画布背景）
            val exportedBitmap = renderToFBO()
            if (exportedBitmap != null) {
                screenshotListener?.onScreenshotTaken(exportedBitmap)
            } else {
                android.util.Log.e("ImageRenderer", "导出图片失败")
            }
        }
        // 8. 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    // 6. 定义辅助函数加载OpenGL程序和纹理
    private fun loadShader(type:Int,shaderCode:String):Int{
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            android.util.Log.e("ImageRenderer", "无法创建着色器")
            return 0
        }
        
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        // 检查编译状态
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            android.util.Log.e("ImageRenderer", "着色器编译失败: $error")
            GLES20.glDeleteShader(shader)
            return 0
        }
        
        return shader
    }

    // 7. 定义辅助函数加载纹理
    private fun loadTexture(context:Context,uri:Uri):Int{
        val textureIds= IntArray(1)
        GLES20.glGenTextures(1,textureIds,0)
        if(textureIds[0]==0){
            throw RuntimeException("无法生成纹理ID")
        }
        
        val bitmap = try {
            loadBitmapFromUri(context, uri)
        } catch (e: Exception) {
            throw RuntimeException("加载图片失败: ${e.message}", e)
        }
        
        if (bitmap == null) {
            throw RuntimeException("图片解码失败")
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
    
    /**
     * 从URI加载Bitmap，支持file URI和content URI
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return when (uri.scheme) {
            "file" -> {
                // file URI，直接从文件路径加载
                val filePath = uri.path
                if (filePath != null) {
                    BitmapFactory.decodeFile(filePath)
                } else {
                    null
                }
            }
            "content" -> {
                // content URI，使用ContentResolver
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
            else -> {
                // 尝试作为文件路径处理
                val path = uri.toString()
                if (path.startsWith("/")) {
                    BitmapFactory.decodeFile(path)
                } else {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            }
        }
    }
    
    /**
     * 使用FBO离屏渲染导出图片
     * 这样可以只导出图片内容本身，不包含画布背景
     */
    private fun renderToFBO(): Bitmap? {
        try {
            // 获取当前图片的实际尺寸
            val exportWidth = imageWidth
            val exportHeight = imageHeight
            
            if (exportWidth <= 0 || exportHeight <= 0) {
                android.util.Log.e("ImageRenderer", "图片尺寸无效: ${exportWidth}x${exportHeight}")
                return null
            }
            
            // 创建FBO
            val fboIds = IntArray(1)
            GLES20.glGenFramebuffers(1, fboIds, 0)
            val fbo = fboIds[0]
            
            // 创建FBO纹理
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            val fboTexture = textureIds[0]
            
            // 配置FBO纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                exportWidth, exportHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
            )
            
            // 绑定FBO
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fboTexture, 0
            )
            
            // 检查FBO状态
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                android.util.Log.e("ImageRenderer", "FBO不完整: $status")
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glDeleteFramebuffers(1, fboIds, 0)
                GLES20.glDeleteTextures(1, textureIds, 0)
                return null
            }
            
            // 设置视口为图片尺寸
            GLES20.glViewport(0, 0, exportWidth, exportHeight)
            
            // 清除背景（透明）
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            
            // 选择滤镜程序
            val activeFilter = if (isGrayscaleEnabled) FilterType.GRAYSCALE else currentFilter
            val exportProgramId = filterPrograms[activeFilter] ?: filterPrograms[FilterType.NONE] ?: 0
            GLES20.glUseProgram(exportProgramId)
            
            // 创建用于导出的顶点数据（填满整个FBO，不需要变换）
            val exportVertexData = floatArrayOf(
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
            )
            val exportVertexBuffer = ByteBuffer.allocateDirect(exportVertexData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(exportVertexData)
            exportVertexBuffer.position(0)
            
            // 设置单位变换矩阵（不应用缩放和平移）
            val identityMatrix = FloatArray(16)
            Matrix.setIdentityM(identityMatrix, 0)
            
            val exportTransformHandle = GLES20.glGetUniformLocation(exportProgramId, "u_TransformMatrix")
            GLES20.glUniformMatrix4fv(exportTransformHandle, 1, false, identityMatrix, 0)
            
            // 传递调整参数
            if (activeFilter == FilterType.NONE) {
                val brightnessHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Brightness")
                val contrastHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Contrast")
                val saturationHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Saturation")
                val highlightsHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Highlights")
                val shadowsHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Shadows")
                val temperatureHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Temperature")
                val tintHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Tint")
                val clarityHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Clarity")
                val sharpenHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Sharpen")
                
                GLES20.glUniform1f(brightnessHandle, adjustmentParams.brightness)
                GLES20.glUniform1f(contrastHandle, adjustmentParams.contrast)
                GLES20.glUniform1f(saturationHandle, adjustmentParams.saturation)
                GLES20.glUniform1f(highlightsHandle, adjustmentParams.highlights)
                GLES20.glUniform1f(shadowsHandle, adjustmentParams.shadows)
                GLES20.glUniform1f(temperatureHandle, adjustmentParams.temperature)
                GLES20.glUniform1f(tintHandle, adjustmentParams.tint)
                GLES20.glUniform1f(clarityHandle, adjustmentParams.clarity)
                GLES20.glUniform1f(sharpenHandle, adjustmentParams.sharpen)
            }
            
            // 获取着色器变量句柄
            val positionHandle = GLES20.glGetAttribLocation(exportProgramId, "a_Position")
            val texCoordHandle = GLES20.glGetAttribLocation(exportProgramId, "a_TexCoord")
            val textureHandle = GLES20.glGetUniformLocation(exportProgramId, "u_Texture")
            
            // 启用顶点属性
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            
            // 传递顶点数据
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, exportVertexBuffer)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
            
            // 绑定原始纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(textureHandle, 0)
            
            // 绘制
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            
            // 读取像素
            val buffer = ByteBuffer.allocateDirect(exportWidth * exportHeight * 4)
            buffer.order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(
                0, 0, exportWidth, exportHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
            )
            buffer.rewind()
            
            // 创建Bitmap
            val bitmap = Bitmap.createBitmap(exportWidth, exportHeight, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            
            // 翻转图片（OpenGL坐标系Y轴与Bitmap相反）
            val matrix = android.graphics.Matrix()
            matrix.preScale(1.0f, -1.0f)
            val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, exportWidth, exportHeight, matrix, true)
            bitmap.recycle()
            
            // 禁用顶点属性
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
            
            // 恢复默认帧缓冲
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            
            // 清理FBO资源
            GLES20.glDeleteFramebuffers(1, fboIds, 0)
            GLES20.glDeleteTextures(1, textureIds, 0)
            
            android.util.Log.d("ImageRenderer", "FBO导出成功: ${exportWidth}x${exportHeight}")
            
            return flippedBitmap
            
        } catch (e: Exception) {
            android.util.Log.e("ImageRenderer", "FBO渲染失败", e)
            e.printStackTrace()
            return null
        }
    }
}