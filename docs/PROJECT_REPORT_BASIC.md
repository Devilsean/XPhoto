# 图片编辑应用 - 基础项目报告

## 项目概述

本项目是一款基于 Android 原生开发的图片编辑应用，参考醒图等主流图片编辑软件的设计理念，实现了图片浏览、编辑、滤镜、裁剪等核心功能。项目采用 Kotlin 语言开发，运用 Jetpack 组件和现代 Android 开发最佳实践。

---

## 一、首页实现要点

### 1.1 启动页设计

应用启动时首先展示启动页（Splash Screen），用于品牌展示和应用初始化。

#### 启动页布局 ([`activity_splash.xml`](../app/src/main/res/layout/activity_splash.xml))

```
ConstraintLayout (渐变背景)
├── 标题区域 (ConstraintLayout)
│   ├── X 图标 (FrameLayout)
│   │   ├── 填充背景 (View)
│   │   └── X 矢量图标 (ImageView)
│   └── "Photo" 文字 (TextView)
├── 标语文字 "Capture. Create. Perfect." (TextView)
└── 功能图标装饰 (多个 ImageView)
    ├── 裁剪图标 (ic_crop_vector)
    ├── 调色板图标 (ic_palette_vector)
    ├── 抠图图标 (ic_cutout_vector)
    ├── 橡皮擦图标 (ic_eraser_vector)
    ├── 滤镜图标 (ic_wand_vector)
    ├── 文字图标 (ic_text_vector)
    └── 美颜图标 (ic_beauty_vector)
```

#### 渐变背景实现 ([`splash_gradient_bg.xml`](../app/src/main/res/drawable/splash_gradient_bg.xml))

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="135"
        android:endColor="@color/splash_bg_pink"
        android:startColor="@color/splash_bg_blue"
        android:type="linear" />
</shape>
```

**技术要点：**

- 使用 `LinearGradient` 实现 135° 角度的蓝粉渐变背景
- 功能图标散布在标题周围，展示应用的核心功能
- 品牌标识采用 "X" + "Photo" 的组合设计

#### 启动页逻辑 ([`SplashActivity.kt`](../app/src/main/java/com/example/myapplication/ui/SplashActivity.kt))

```kotlin
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "SplashActivity onCreate 开始")
            setContentView(R.layout.activity_splash)
            
            // 延迟 1 秒后跳转到主界面
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d(TAG, "准备跳转到 MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "跳转到 MainActivity 失败", e)
                }
            }, 1000)
        } catch (e: Exception) {
            Log.e(TAG, "SplashActivity onCreate 异常", e)
            // 异常时尝试直接跳转到主界面
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
```

**实现要点：**

| 特性 | 实现方式 |
|------|----------|
| **延迟跳转** | 使用 `Handler.postDelayed()` 延迟 1 秒 |
| **异常处理** | try-catch 包裹，异常时直接跳转主界面 |
| **日志记录** | 关键节点添加 Log 便于调试 |
| **页面销毁** | 跳转后调用 `finish()` 防止返回 |

### 1.2 UI 搭建

首页采用 `ScrollView` + `ConstraintLayout` 的布局方式，主要包含以下模块：

#### 布局结构 ([`fragment_home.xml`](../app/src/main/res/layout/fragment_home.xml))

```
ScrollView
└── ConstraintLayout
    ├── 顶部轮播图 Banner (ViewPager2)
    ├── 快速开始区块 (相机/相册入口)
    ├── 常用功能网格 (RecyclerView + GridLayoutManager)
    └── 最近编辑/草稿列表 (RecyclerView + LinearLayoutManager)
```

#### 核心控件使用

| 控件 | 用途 | 位置 |
|------|------|------|
| `ViewPager2` | 轮播图展示 | 顶部 Banner |
| `RecyclerView` | 功能网格、草稿列表 | 中部/底部 |
| `MaterialCardView` | 卡片式布局容器 | 各功能区块 |
| `ImageView` | 图标展示 | 功能入口 |
| `Glide` | 图片加载 | 草稿预览 |

#### 首页核心代码 ([`HomeFragment.kt`](../app/src/main/java/com/example/myapplication/ui/home/HomeFragment.kt))

```kotlin
// 轮播图设置
val viewPager: ViewPager2 = view.findViewById(R.id.carousel_view_pager)
val carouselItems = listOf(R.drawable.banner3, R.drawable.banner4)
viewPager.adapter = CarouselAdapter(carouselItems)

// 常用功能网格
val quickAccessRecyclerView: RecyclerView = view.findViewById(R.id.quick_access_recycler_view)
quickAccessRecyclerView.layoutManager = GridLayoutManager(context, 4)
quickAccessRecyclerView.adapter = QuickAccessAdapter(quickAccessItems) { item ->
    // 功能点击处理
}
```

### 1.2 自定义 View 实现

项目实现了两个自定义 View，展示"炫酷"的视觉效果：

#### 1.2.1 ShimmerImageView - 动态扫光效果 ([`ShimmerImageView.kt`](../app/src/main/java/com/example/myapplication/ui/widget/ShimmerImageView.kt))

**实现原理：** 使用 `LinearGradient` 创建渐变着色器，通过 `ValueAnimator` 动态更新渐变位置，实现扫光动画效果。

```kotlin
class ShimmerImageView : AppCompatImageView {
    private var shimmerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f
    private val shimmerWidth = 200f
    
    init {
        // 设置扫光渐变混合模式
        shimmerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        startShimmerAnimation()
    }

    private fun startShimmerAnimation() {
        post {
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
        if (drawable != null) {
            // 创建动态位置的线性渐变
            val shader = LinearGradient(
                shimmerTranslate - shimmerWidth / 2, 0f,
                shimmerTranslate + shimmerWidth / 2, 0f,
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
}
```

**技术要点：**

- 使用 `LinearGradient` 创建透明-白色-透明的渐变效果
- `PorterDuff.Mode.SRC_ATOP` 混合模式确保扫光只在图片内容上显示
- `ValueAnimator` 实现平滑的位置动画

#### 1.2.2 RainbowBorderImageView - 彩虹旋转边框 ([`RainbowBorderImageView.kt`](../app/src/main/java/com/example/myapplication/ui/widget/RainbowBorderImageView.kt))

**实现原理：** 使用 `SweepGradient` 创建环形渐变，通过 `Matrix` 旋转实现动态彩虹边框效果。

```kotlin
class RainbowBorderImageView : AppCompatImageView {
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val contentPath = Path()
    private var rotationAngle = 0f
    private var animator: ValueAnimator? = null
    private val borderWidth = 8f

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

    override fun onDraw(canvas: Canvas) {
        // 1. 裁剪并绘制圆形图片内容
        canvas.save()
        canvas.clipPath(contentPath)
        super.onDraw(canvas)
        canvas.restore()

        // 2. 绘制旋转的彩虹边框
        val centerX = width / 2f
        val centerY = height / 2f
        val borderRadius = min(width, height) / 2f - (borderWidth / 2f)

        val gradient = SweepGradient(
            centerX, centerY,
            intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, 
                       Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED),
            null
        )

        // 旋转渐变效果
        val matrix = Matrix()
        matrix.postRotate(rotationAngle, centerX, centerY)
        gradient.setLocalMatrix(matrix)

        borderPaint.shader = gradient
        canvas.drawCircle(centerX, centerY, borderRadius, borderPaint)
    }
}
```

**技术要点：**

- `SweepGradient` 创建 360° 环形彩虹渐变
- `Path.clipPath()` 实现圆形图片裁剪
- `Matrix.postRotate()` 实现渐变旋转动画

#### 1.2.3 首页图标渐变效果

首页相册图标使用 `LinearGradient` + `PorterDuff.Mode.SRC_IN` 实现渐变着色：

```kotlin
// 创建线性渐变着色器（135度角，从左上到右下）
val shader = LinearGradient(
    0f, 0f, width, height,
    startColor, endColor,
    Shader.TileMode.CLAMP
)

// 使用 SRC_IN 模式应用渐变
val paint = Paint().apply { this.shader = shader }
paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
canvas.drawRect(0f, 0f, width, height, paint)
```

### 1.3 多媒体预览

项目支持加载并预览多种格式的媒体文件：

#### 支持的格式

| 格式 | 实现方式 | 说明 |
|------|----------|------|
| **WebP** | Glide 原生支持 | 静态/动态 WebP 均支持 |
| **GIF** | Glide `asGif()` | 动画 GIF 自动播放 |
| **MP4** | ExoPlayer (Media3) | 视频播放器 |

#### 媒体加载代码 ([`PhotoAdapter.kt`](../app/src/main/java/com/example/myapplication/ui/PhotoAdapter.kt))

```kotlin
when {
    mediaItem.isVideo -> {
        // 视频：显示缩略图和播放图标
        holder.playIcon.visibility = View.VISIBLE
        holder.durationText?.text = formatDuration(mediaItem.duration)
        Glide.with(context)
            .load(mediaItem.uri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.photoImage)
    }
    mediaItem.mimeType == "image/gif" -> {
        // GIF：使用 Glide 加载动画
        Glide.with(context)
            .asGif()
            .load(mediaItem.uri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.photoImage)
    }
    else -> {
        // 静态图片（包括 WebP）
        Glide.with(context)
            .load(mediaItem.uri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.photoImage)
    }
}
```

---

## 二、相册页实现要点

### 2.1 媒体库访问

使用 `MediaStore API` 异步拉取设备上的图片和视频：

#### MediaStoreHelper 实现 ([`MediaStoreHelper.kt`](../app/src/main/java/com/example/myapplication/utils/MediaStoreHelper.kt))

```kotlin
class MediaStoreHelper(private val context: Context) {

    data class MediaItem(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val mimeType: String,
        val dateAdded: Long,
        val size: Long,
        val width: Int = 0,
        val height: Int = 0,
        val duration: Long = 0  // 视频时长（毫秒）
    ) {
        val isVideo: Boolean get() = mimeType.startsWith("video/")
        val isImage: Boolean get() = mimeType.startsWith("image/")
    }

    /**
     * 加载所有媒体（图片+视频）- 支持分页
     */
    suspend fun loadAllMedia(pageParams: PageParams): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        
        // 加载图片
        mediaList.addAll(loadImages(PageParams(pageSize = Int.MAX_VALUE, offset = 0)))
        // 加载视频
        mediaList.addAll(loadVideos(PageParams(pageSize = Int.MAX_VALUE, offset = 0)))
        
        // 按日期排序并应用分页
        return@withContext mediaList.sortedByDescending { it.dateAdded }
            .drop(pageParams.offset)
            .take(pageParams.pageSize)
    }

    /**
     * 加载图片
     */
    suspend fun loadImages(pageParams: PageParams): List<MediaItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            // 遍历 cursor 构建 MediaItem 列表
            // ...
        }
    }
}
```

### 2.2 网格展示

使用 `RecyclerView` + `GridLayoutManager` 实现网格布局：

#### AlbumActivity 实现 ([`AlbumActivity.kt`](../app/src/main/java/com/example/myapplication/ui/AlbumActivity.kt))

```kotlin
class AlbumActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private val mediaList = mutableListOf<MediaStoreHelper.MediaItem>()
    
    // 分页参数
    private var currentPage = 0
    private val pageSize = 50
    private var isLoading = false
    private var hasMoreData = true

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_album)
        photoAdapter = PhotoAdapter(mediaList)
        
        // 3列网格布局
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = photoAdapter
        
        // 滚动监听实现分页加载
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                
                // 当滚动到倒数第10个item时，加载下一页
                if (!isLoading && hasMoreData && lastVisibleItem >= totalItemCount - 10) {
                    loadMoreMedia()
                }
            }
        })
    }

    private fun loadMedia() {
        if (isLoading) return
        isLoading = true
        
        lifecycleScope.launch {
            val pageParams = MediaStoreHelper.PageParams(
                pageSize = pageSize,
                offset = currentPage * pageSize
            )
            val newItems = mediaStoreHelper.loadAllMedia(pageParams)
            
            if (newItems.isEmpty()) {
                hasMoreData = false
            } else {
                mediaList.addAll(newItems)
                photoAdapter.notifyDataSetChanged()
                currentPage++
            }
            isLoading = false
        }
    }
}
```

### 2.3 厂商适配与权限处理

#### 权限适配 ([`PermissionHelper.kt`](../app/src/main/java/com/example/myapplication/utils/PermissionHelper.kt))

针对不同 Android 版本的权限差异进行适配：

```kotlin
object PermissionHelper {

    /**
     * 获取需要的媒体权限列表
     */
    fun getRequiredMediaPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12 (API 29-32)
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // Android 9 及以下 (API 28-)
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * 检查是否已授予所有媒体权限
     */
    fun hasMediaPermissions(context: Context): Boolean {
        return getRequiredMediaPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED
        }
    }
}
```

#### 厂商适配注意事项

| 厂商 | 问题 | 解决方案 |
|------|------|----------|
| **小米** | MIUI 权限弹窗样式不同 | 使用系统标准 API，避免自定义权限弹窗 |
| **华为** | EMUI 媒体库索引延迟 | 添加加载状态提示，使用分页加载 |
| **OPPO/Vivo** | ColorOS/FuntouchOS 权限管理严格 | 提供清晰的权限说明，引导用户手动授权 |
| **通用** | Android 10+ 分区存储 | 使用 MediaStore API 而非直接文件路径 |

---

## 三、编辑器与导出功能要点

### 3.1 OpenGL ES 渲染画布

使用 `GLSurfaceView` + `OpenGL ES 2.0` 实现图片渲染：

#### ImageRenderer 实现 ([`ImageRenderer.kt`](../app/src/main/java/com/example/myapplication/ui/ImageRenderer.kt))

```kotlin
class ImageRenderer(private val context: Context, private val imageUri: Uri) : 
    GLSurfaceView.Renderer {
    
    // 顶点着色器
    private val vertexShaderCode = """
        uniform mat4 u_TransformMatrix;
        attribute vec4 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;
        void main(){
            gl_Position = u_TransformMatrix * a_Position;
            v_TexCoord = a_TexCoord;
        }
    """
    
    // 变换参数
    @Volatile var scaleFactor = 1.0f
    @Volatile var offsetX = 0.0f
    @Volatile var offsetY = 0.0f
    @Volatile var rotationAngle: Float = 0f
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 为每个滤镜创建着色器程序
        FilterType.values().forEach { filter ->
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, filter.fragmentShader)
            val program = GLES20.glCreateProgram().also {
                GLES20.glAttachShader(it, vertexShader)
                GLES20.glAttachShader(it, fragmentShader)
                GLES20.glLinkProgram(it)
            }
            filterPrograms[filter] = program
        }
        
        // 加载纹理
        textureId = loadTexture(context, imageUri)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 选择滤镜程序
        val activeFilter = if (isGrayscaleEnabled) FilterType.GRAYSCALE else currentFilter
        programId = filterPrograms[activeFilter] ?: filterPrograms[FilterType.NONE] ?: 0
        GLES20.glUseProgram(programId)
        
        // 设置变换矩阵（缩放、平移）
        Matrix.setIdentityM(transformMatrix, 0)
        Matrix.translateM(transformMatrix, 0, offsetX * scaleFactor, offsetY * scaleFactor, 0.0f)
        Matrix.scaleM(transformMatrix, 0, scaleFactor, scaleFactor, 1.0f)
        
        // 传递调整参数到着色器
        if (activeFilter == FilterType.NONE) {
            GLES20.glUniform1f(brightnessHandle, adjustmentParams.brightness)
            GLES20.glUniform1f(contrastHandle, adjustmentParams.contrast)
            GLES20.glUniform1f(saturationHandle, adjustmentParams.saturation)
            // ... 其他参数
        }
        
        // 绑定纹理并绘制
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}
```

### 3.2 编辑操作实现

#### 手势处理 ([`EditorActivity.kt`](../app/src/main/java/com/example/myapplication/ui/EditorActivity.kt))

```kotlin
class EditorActivity : AppCompatActivity() {
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 处理缩放手势
        scaleGestureDetector.onTouchEvent(event)
        // 处理惯性滑动
        gestureDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress && !isScaling) {
                    // 单指平移
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    applyTranslation(dx, dy)
                }
            }
        }
        return true
    }
    
    /**
     * 缩放手势监听器 - 支持焦点缩放
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            var newScale = renderer.scaleFactor * scaleFactor
            newScale = newScale.coerceIn(0.1f, 5.0f)
            
            // 计算焦点位置，实现以手指为中心的缩放
            val focusX = detector.focusX
            val focusY = detector.focusY
            // ... 焦点缩放计算
            
            renderer.scaleFactor = newScale
            glSurfaceView.requestRender()
            return true
        }
    }
}
```

#### 裁剪功能 ([`CropOverlayView.kt`](../app/src/main/java/com/example/myapplication/ui/widget/CropOverlayView.kt))

```kotlin
class CropOverlayView : View {
    enum class AspectRatio(val ratio: Float, val displayName: String) {
        FREE(0f, "自由"),
        RATIO_1_1(1f, "1:1"),
        RATIO_3_4(3f / 4f, "3:4"),
        RATIO_4_3(4f / 3f, "4:3"),
        RATIO_9_16(9f / 16f, "9:16"),
        RATIO_16_9(16f / 9f, "16:9")
    }
    
    override fun onDraw(canvas: Canvas) {
        // 绘制半透明遮罩
        canvas.drawRect(0f, 0f, w, cropRect.top, overlayPaint)
        // ... 其他遮罩区域
        
        // 绘制裁剪框边框
        canvas.drawRect(cropRect, borderPaint)
        
        // 绘制九宫格线
        val gridWidth = cropRect.width() / 3
        canvas.drawLine(cropRect.left + gridWidth, cropRect.top,
            cropRect.left + gridWidth, cropRect.bottom, gridPaint)
        // ... 其他网格线
        
        // 绘制四角标记
        canvas.drawLine(cropRect.left, cropRect.top, 
            cropRect.left + cornerSize, cropRect.top, cornerPaint)
        // ... 其他角标记
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchMode = getTouchMode(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                when (touchMode) {
                    TouchMode.DRAG -> moveCropRect(dx, dy)
                    TouchMode.TOP_LEFT -> resizeCropRect(dx, dy, true, true, false, false)
                    // ... 其他调整模式
                }
            }
        }
        return true
    }
}
```

### 3.3 结果合成与导出

#### FBO 离屏渲染导出

```kotlin
/**
 * 使用 FBO 离屏渲染导出图片
 * 只导出图片内容本身，不包含画布背景
 */
private fun renderToFBO(): Bitmap? {
    // 根据旋转角度确定导出尺寸
    val isRotated = isRotated90or270()
    val exportWidth = if (isRotated) imageHeight else imageWidth
    val exportHeight = if (isRotated) imageWidth else imageHeight
    
    // 创建 FBO
    val fboIds = IntArray(1)
    GLES20.glGenFramebuffers(1, fboIds, 0)
    
    // 创建 FBO 纹理
    val textureIds = IntArray(1)
    GLES20.glGenTextures(1, textureIds, 0)
    
    // 配置 FBO 纹理
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture)
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
        exportWidth, exportHeight, 0,
        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
    
    // 绑定 FBO 并渲染
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, 
        GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTexture, 0)
    
    // 设置视口并绘制
    GLES20.glViewport(0, 0, exportWidth, exportHeight)
    // ... 绑定着色器、传递参数、绘制
    
    // 读取像素
    val buffer = ByteBuffer.allocateDirect(exportWidth * exportHeight * 4)
    GLES20.glReadPixels(0, 0, exportWidth, exportHeight,
        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
    
    // 创建 Bitmap
    val bitmap = Bitmap.createBitmap(exportWidth, exportHeight, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    
    // 翻转图片（OpenGL 坐标系 Y 轴与 Bitmap 相反）
    val matrix = android.graphics.Matrix()
    matrix.preScale(1.0f, -1.0f)
    return Bitmap.createBitmap(bitmap, 0, 0, exportWidth, exportHeight, matrix, true)
}
```

#### 保存到相册

```kotlin
private fun saveToWorks(bitmap: Bitmap) {
    lifecycleScope.launch {
        // 保存到应用私有目录
        val worksDir = File(filesDir, "works")
        if (!worksDir.exists()) worksDir.mkdirs()
        val file = File(worksDir, "work_${System.currentTimeMillis()}.png")
        
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        
        // 保存记录到数据库
        val editedImage = EditedImage(
            originalImageUri = originalImageUri ?: "",
            editedImageUri = file.absolutePath,
            isExported = false,
            isFavorite = false,
            createdAt = System.currentTimeMillis()
        )
        editedImageRepository.saveEditedImage(editedImage)
        
        // 删除对应草稿
        currentDraftId?.let { draftId ->
            draftRepository.deleteDraft(draftId)
        }
    }
}
```

---

## 四、技术栈与代码规范要点

### 4.1 编程语言

项目主要使用 **Kotlin** 语言开发，充分利用 Kotlin 的语言特性：

| 特性 | 应用场景 |
|------|----------|
| `data class` | 实体类定义（Draft, EditedImage, MediaItem 等） |
| `sealed class` | 状态管理 |
| `extension function` | 工具函数扩展 |
| `coroutines` | 异步操作（数据库、文件 IO） |
| `Flow` | 响应式数据流 |
| `lazy` | 延迟初始化 |
| `by` 委托 | 属性委托 |

### 4.2 架构与组件

项目采用 **MVVM** 架构模式，运用 Jetpack 组件：

#### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Activity   │  │  Fragment   │  │   Custom View       │  │
│  │  (Editor)   │  │  (Home/My)  │  │   (CropOverlay)     │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
└─────────┼────────────────┼───────────────────┼──────────────┘
          │                │                   │
          ▼                ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                      Repository Layer                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │ DraftRepository │  │EditedImageRepo  │  │ AlbumRepo   │  │
│  └────────┬────────┘  └────────┬────────┘  └──────┬──────┘  │
└───────────┼────────────────────┼─────────────────┼──────────┘
            │                    │                 │
            ▼                    ▼                 ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    Room Database                         ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐  ││
│  │  │ DraftDao │  │EditedDao │  │ AlbumDao │  │ UserDao │  ││
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

#### Room 数据库配置 ([`AppDataBase.kt`](../app/src/main/java/com/example/myapplication/data/database/AppDataBase.kt))

```kotlin
@Database(
    entities = [
        EditedImage::class,
        Draft::class,
        Album::class,
        AlbumImage::class,
        User::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun editedImageDao(): EditedImageDao
    abstract fun draftDao(): DraftDao
    abstract fun albumDao(): AlbumDao
    abstract fun albumImageDao(): AlbumImageDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDataBase? = null

        fun getDatabase(context: Context): AppDataBase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDataBase::class.java,
                    "my_application_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
```

#### 实体类设计

**草稿实体** ([`Draft.kt`](../app/src/main/java/com/example/myapplication/data/entity/Draft.kt))：

```kotlin
@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalImageUri: String,
    val isGrayscaleEnabled: Boolean = false,
    val scaleFactor: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationAngle: Float = 0f,
    // 裁剪信息
    val cropLeft: Float? = null,
    val cropTop: Float? = null,
    val cropRight: Float? = null,
    val cropBottom: Float? = null,
    val filterType: String? = null,
    // 调整参数
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    // ... 其他参数
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
```

### 4.3 异步处理

使用 **Kotlin 协程** 处理异步任务：

#### 协程作用域管理 ([`MyApplication.kt`](../app/src/main/java/com/example/myapplication/MyApplication.kt))

```kotlin
class MyApplication : Application() {
    // 协程异常处理器
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常", throwable)
    }
    
    // 应用级协程作用域
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler
    )
    
    // 延迟初始化数据库
    val database: AppDataBase by lazy {
        AppDataBase.getDatabase(this)
    }
    
    // 延迟初始化 Repository
    val draftRepository: DraftRepository by lazy {
        DraftRepository(database.draftDao())
    }
}
```

#### 异步数据加载示例

```kotlin
// 使用 lifecycleScope 进行异步操作
lifecycleScope.launch {
    try {
        val pageParams = MediaStoreHelper.PageParams(
            pageSize = pageSize,
            offset = currentPage * pageSize
        )
        // 在 IO 线程执行媒体查询
        val newItems = mediaStoreHelper.loadAllMedia(pageParams)
        
        // 回到主线程更新 UI
        mediaList.addAll(newItems)
        photoAdapter.notifyDataSetChanged()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

#### Flow 响应式数据流

```kotlin
// Repository 中使用 Flow
class DraftRepository(private val draftDao: DraftDao) {
    val allDrafts: Flow<List<Draft>> = draftDao.getAllDrafts()
}

// UI 中收集 Flow
viewLifecycleOwner.lifecycleScope.launch {
    draftRepository.allDrafts.collect { drafts ->
        // 更新 UI
        updateDraftsList(drafts)
    }
}
```

---

## 五、调试与性能排查要点

### 5.1 问题定位

#### Android Studio Debugger 使用

项目中关键位置添加了调试日志：

```kotlin
// 使用 TAG 标识日志来源
companion object {
    private const val TAG = "EditorActivity"
}

// 关键操作日志
android.util.Log.d(TAG, "接收到图片URI: $originalImageUri")
android.util.Log.e(TAG, "加载图片失败", e)
```

#### 断点调试技巧

| 场景 | 断点位置 | 检查内容 |
|------|----------|----------|
| 图片加载失败 | `loadTexture()` | URI 有效性、Bitmap 是否为 null |
| 裁剪坐标错误 | `applyCrop()` | cropRect 归一化坐标范围 |
| 滤镜不生效 | `onDrawFrame()` | programId 是否正确、uniform 传递 |
| 手势响应异常 | `onTouchEvent()` | 触摸模式、坐标计算 |

### 5.2 性能定位

#### 内存泄漏排查

```kotlin
// Bitmap 资源释放
fun cleanup() {
    originalBitmap?.recycle()
    originalBitmap = null
    croppedBitmap?.recycle()
    croppedBitmap = null
    
    // 清理 bitmap 历史
    for (bitmap in bitmapHistory) {
        if (bitmap != originalBitmap) {
            bitmap.recycle()
        }
    }
    bitmapHistory.clear()
}

// Activity 销毁时清理
override fun onDestroy() {
    super.onDestroy()
    if (::renderer.isInitialized) {
        renderer.cleanup()
    }
}
```

#### CPU 峰值优化

```kotlin
// 使用防抖处理频繁的调整操作
private var lastAdjustmentSaveTime = 0L
private var pendingHistorySave = false

private fun onAdjustmentChanged(adjustmentType: AdjustmentType, value: Float) {
    val currentTime = System.currentTimeMillis()
    // 500ms 防抖
    if (!pendingHistorySave && currentTime - lastAdjustmentSaveTime > 500) {
        saveCurrentStateToHistory()
        pendingHistorySave = true
    }
    
    // 更新参数并渲染
    adjustmentType.setValue(renderer.adjustmentParams, value)
    glSurfaceView.requestRender()
}
```

#### 卡顿优化

```kotlin
// 分页加载避免一次性加载大量数据
data class PageParams(
    val pageSize: Int = 50,
    val offset: Int = 0
)

// 使用 DiskCacheStrategy 缓存图片
Glide.with(context)
    .load(mediaItem.uri)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .centerCrop()
    .into(holder.photoImage)
```

### 5.3 排查日志

项目中关键代码部分添加了详细的排查日志：

```kotlin
// 初始化日志
Log.d(TAG, "onSurfaceCreated 开始")
Log.d(TAG, "纹理ID: $textureId, 图片尺寸: ${imageWidth}x${imageHeight}")
Log.d(TAG, "滤镜程序创建: ${filter.name} -> $program")

// 错误日志
Log.e(TAG, "着色器编译失败: $error")
Log.e(TAG, "FBO不完整: $status")
Log.e(TAG, "裁剪失败", e)

// 操作日志
Log.d(TAG, "裁剪成功: ${newCroppedBitmap.width}x${newCroppedBitmap.height}")
Log.d(TAG, "FBO导出成功: ${exportWidth}x${exportHeight}")
```

---

## 六、设备适配与打包要点

### 6.1 设备要求

App 已在以下设备上测试稳定运行：

| 品牌 | 型号 | Android 版本 | 测试结果 |
|------|------|--------------|----------|
| 一加 | ACE 5 | ColorOS 16.0 | ✅ 通过 |
| OPPO | A1 pro | ColorOS 14.0 | ✅ 通过 |
| 华为 | Mate 30 | HarmonyOS 4.2.0 | ✅ 通过 |

#### 适配要点

1. **屏幕适配**：使用 `ConstraintLayout` 和百分比布局
2. **刘海屏适配**：使用 `WindowInsets` API
3. **深色模式**：提供 `values-night` 主题资源

### 6.2 代码混淆

#### build.gradle.kts 配置 ([`app/build.gradle.kts`](../app/build.gradle.kts))

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### ProGuard 规则 ([`proguard-rules.pro`](../app/proguard-rules.pro))

```proguard
# 保留行号信息用于调试
-keepattributes SourceFile,LineNumberTable

# 保留 Room 实体类
-keep class com.example.myapplication.data.entity.** { *; }

# 保留 Glide 相关类
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }

# 保留 Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

---

## 七、开发遇到的困难及解决思路

### 7.1 导出图片错误包含背景画布

**问题描述：** 直接从 GLSurfaceView 截图导出时，导出的图片包含了黑色背景画布，而不是纯粹的图片内容。

**原因分析：** GLSurfaceView 的渲染区域是整个视图大小，直接读取像素会包含图片周围的背景区域。图片在画布中居中显示时，四周会有空白/黑色区域。

**解决方案：** 使用 FBO（Framebuffer Object）离屏渲染技术，创建一个与原始图片尺寸相同的离屏缓冲区，只渲染图片内容本身：

```kotlin
/**
 * 使用 FBO 离屏渲染导出图片
 * 只导出图片内容本身，不包含画布背景
 */
private fun renderToFBO(): Bitmap? {
    // 根据旋转角度确定导出尺寸（使用原始图片尺寸，而非视图尺寸）
    val isRotated = isRotated90or270()
    val exportWidth = if (isRotated) imageHeight else imageWidth
    val exportHeight = if (isRotated) imageWidth else imageHeight
    
    // 创建 FBO
    val fboIds = IntArray(1)
    GLES20.glGenFramebuffers(1, fboIds, 0)
    val fbo = fboIds[0]
    
    // 创建 FBO 纹理（尺寸为图片原始尺寸）
    val textureIds = IntArray(1)
    GLES20.glGenTextures(1, textureIds, 0)
    val fboTexture = textureIds[0]
    
    // 配置 FBO 纹理
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTexture)
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
        exportWidth, exportHeight, 0,
        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
    
    // 绑定 FBO 并渲染
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
        GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTexture, 0)
    
    // 关键：设置视口为图片原始尺寸，而非视图尺寸
    GLES20.glViewport(0, 0, exportWidth, exportHeight)
    
    // 使用单位矩阵渲染（不包含缩放和平移变换）
    // ... 绑定着色器、传递参数、绘制
    
    // 读取像素
    val buffer = ByteBuffer.allocateDirect(exportWidth * exportHeight * 4)
    GLES20.glReadPixels(0, 0, exportWidth, exportHeight,
        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
    
    // 清理 FBO 资源
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    GLES20.glDeleteFramebuffers(1, fboIds, 0)
    GLES20.glDeleteTextures(1, textureIds, 0)
    
    // 创建 Bitmap 并翻转（OpenGL Y轴与Bitmap相反）
    val bitmap = Bitmap.createBitmap(exportWidth, exportHeight, Bitmap.Config.ARGB_8888)
    buffer.rewind()
    bitmap.copyPixelsFromBuffer(buffer)
    
    val matrix = android.graphics.Matrix()
    matrix.preScale(1.0f, -1.0f)
    return Bitmap.createBitmap(bitmap, 0, 0, exportWidth, exportHeight, matrix, true)
}
```

**技术要点：**

- FBO 允许渲染到离屏纹理而非屏幕
- 视口尺寸设置为图片原始尺寸，确保导出图片不包含背景
- 渲染时使用单位变换矩阵，只应用滤镜和调整参数

### 7.2 裁剪功能（破坏性操作）无法回退

**问题描述：** 裁剪是破坏性操作，一旦执行就会修改原始图片数据，用户无法撤销裁剪操作恢复到之前的状态。

**原因分析：** 裁剪操作直接修改了 `croppedBitmap`，没有保存裁剪前的状态。与滤镜、亮度等非破坏性调整不同，裁剪会永久改变图片的像素数据。

**解决方案：** 实现 Bitmap 历史记录栈，在每次破坏性操作前保存当前状态：

```kotlin
// 历史记录管理
private val bitmapHistory = mutableListOf<Bitmap>()
private var currentBitmapIndex = -1
private val maxBitmapHistory = 10  // 限制历史数量，防止内存溢出

/**
 * 保存当前 Bitmap 状态到历史记录
 */
private fun saveBitmapToHistory(bitmap: Bitmap) {
    // 删除当前位置之后的历史（用户撤销后又做了新操作）
    if (currentBitmapIndex < bitmapHistory.size - 1) {
        for (i in bitmapHistory.size - 1 downTo currentBitmapIndex + 1) {
            val oldBitmap = bitmapHistory.removeAt(i)
            if (oldBitmap != originalBitmap) {
                oldBitmap.recycle()  // 回收内存
            }
        }
    }
    
    // 超过最大数量时删除最旧的
    if (bitmapHistory.size >= maxBitmapHistory) {
        val oldBitmap = bitmapHistory.removeAt(0)
        if (oldBitmap != originalBitmap) {
            oldBitmap.recycle()
        }
        currentBitmapIndex--
    }
    
    // 添加新的 bitmap 副本
    val bitmapCopy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
    bitmapHistory.add(bitmapCopy)
    currentBitmapIndex = bitmapHistory.size - 1
}

/**
 * 撤销操作 - 恢复到上一个 Bitmap 状态
 */
private fun undoBitmap(): Boolean {
    if (currentBitmapIndex > 0) {
        currentBitmapIndex--
        val previousBitmap = bitmapHistory[currentBitmapIndex]
        // 重新加载纹理
        reloadTexture(previousBitmap)
        return true
    }
    return false
}

/**
 * 恢复操作 - 前进到下一个 Bitmap 状态
 */
private fun redoBitmap(): Boolean {
    if (currentBitmapIndex < bitmapHistory.size - 1) {
        currentBitmapIndex++
        val nextBitmap = bitmapHistory[currentBitmapIndex]
        reloadTexture(nextBitmap)
        return true
    }
    return false
}

// 在裁剪前保存状态
private fun applyCrop(cropRect: RectF) {
    // 保存裁剪前的状态
    croppedBitmap?.let { saveBitmapToHistory(it) }
    
    // 执行裁剪操作
    // ...
}
```

**内存管理要点：**

- 限制历史记录数量（maxBitmapHistory = 10）
- 及时回收不再需要的 Bitmap
- 使用 `bitmap.copy()` 创建独立副本，避免引用同一对象

### 7.3 Java 25 版本导致 KSP 缓存注册冲突

**问题描述：** 系统使用了 Java 25（早期访问版本），该版本不被 Gradle 和 Kotlin 工具链支持，导致 KSP（Kotlin Symbol Processing）缓存注册冲突，编译时出现错误。

**错误信息：**

```
Caused by: java.lang.IllegalArgumentException:
Unsupported class file major version 69
```

**原因分析：**

- Java 25 是早期访问版本，其 class 文件版本号（69）尚未被 Gradle、Kotlin 编译器和 KSP 支持
- KSP 在处理注解时需要解析 class 文件，遇到不支持的版本会抛出异常
- Room 数据库使用 KSP 进行编译时代码生成，因此受到影响

**解决方案：**

1. **降级 Java 版本**：将系统 Java 版本降级到 LTS 版本（推荐 Java 17 或 Java 21）

```bash
# 检查当前 Java 版本
java -version

# 设置 JAVA_HOME 环境变量指向支持的版本
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17

# 或在 gradle.properties 中指定
org.gradle.java.home=C:/Program Files/Java/jdk-17
```

2. **在 build.gradle.kts 中指定 JVM 目标版本**：

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

3. **使用 Gradle Toolchain 自动管理 Java 版本**：

```kotlin
// build.gradle.kts
kotlin {
    jvmToolchain(17)
}
```

**经验教训：** 开发环境应使用 LTS（长期支持）版本的 Java，避免使用早期访问版本，以确保与构建工具链的兼容性。

### 7.4 协程在 onCreate() 中使用 collect() 导致无限挂起

**问题描述：** 在 `MyApplication.kt` 的 `onCreate()` 方法中使用 `collect()` 收集 Flow 数据，导致应用启动时无限挂起，界面无法显示。

**错误代码：**

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 错误：在 onCreate 中直接 collect 会阻塞
        applicationScope.launch {
            draftRepository.allDrafts.collect { drafts ->
                // 处理草稿数据
            }
        }
    }
}
```

**原因分析：**

- `Flow.collect()` 是一个挂起函数，会持续监听数据变化，永远不会完成
- 在 `onCreate()` 中启动的协程如果包含 `collect()`，会导致该协程永远运行
- 虽然协程本身不会阻塞主线程，但如果业务逻辑依赖 collect 完成后的操作，会导致逻辑问题

**解决方案：**

1. **使用 `first()` 或 `take(1)` 获取单次数据**：

```kotlin
override fun onCreate() {
    super.onCreate()
    
    applicationScope.launch {
        // 只获取第一次数据，不持续监听
        val drafts = draftRepository.allDrafts.first()
        // 处理数据
    }
}
```

2. **将 collect 移到 Activity/Fragment 的生命周期中**：

```kotlin
// 在 Activity 或 Fragment 中收集
class HomeFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                draftRepository.allDrafts.collect { drafts ->
                    updateUI(drafts)
                }
            }
        }
    }
}
```

3. **使用 `launchIn` 配合生命周期作用域**：

```kotlin
draftRepository.allDrafts
    .onEach { drafts -> updateUI(drafts) }
    .launchIn(viewLifecycleOwner.lifecycleScope)
```

**最佳实践：**

- Application 的 `onCreate()` 中只做初始化工作，不要启动长时间运行的协程
- Flow 的 collect 应该在有明确生命周期的组件中进行（Activity、Fragment、ViewModel）
- 使用 `repeatOnLifecycle` 确保在组件不可见时自动取消收集

### 7.5 点击裁剪按钮无反应（GLSurfaceView 事件拦截问题）

**问题描述：** 点击裁剪按钮没有任何反应，Toast 提示不显示，Logcat 也没有相关输出。按钮的点击事件完全没有被触发。

**初步排查：**

- 检查了按钮的 `setOnClickListener` 是否正确设置 ✓
- 检查了按钮是否被其他 View 遮挡 ✓
- 检查了按钮的 `visibility` 和 `isEnabled` 属性 ✓

**原因分析：**
问题出在 `EditorActivity.kt` 中的 `onTouchEvent()` 方法返回 `true`，但这不是根本原因。真正的问题是 **GLSurfaceView 覆盖了整个屏幕**，它会消费所有触摸事件，导致底部工具栏的按钮无法接收点击事件。

```kotlin
// 问题代码：GLSurfaceView 在布局中覆盖整个屏幕
<android.opengl.GLSurfaceView
    android:id="@+id/gl_surface_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />  <!-- 覆盖了整个屏幕 -->

<LinearLayout
    android:id="@+id/bottom_toolbar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_alignParentBottom="true">
    <!-- 按钮被 GLSurfaceView 遮挡 -->
</LinearLayout>
```

**解决方案：**

1. **调整布局层级**：确保按钮在 GLSurfaceView 之上，并且 GLSurfaceView 不覆盖按钮区域

```xml
<RelativeLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- GLSurfaceView 只占用除工具栏外的区域 -->
    <android.opengl.GLSurfaceView
        android:id="@+id/gl_surface_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_above="@id/bottom_toolbar" />
    
    <!-- 底部工具栏 -->
    <LinearLayout
        android:id="@+id/bottom_toolbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true">
        <Button android:id="@+id/btn_crop" ... />
    </LinearLayout>
</RelativeLayout>
```

2. **或使用 ConstraintLayout 约束**：

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <android.opengl.GLSurfaceView
        android:id="@+id/gl_surface_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottom_toolbar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
    
    <LinearLayout
        android:id="@+id/bottom_toolbar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">
        <!-- 按钮 -->
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

3. **修改 onTouchEvent 逻辑**：只处理 GLSurfaceView 区域内的触摸事件

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    // 检查触摸点是否在 GLSurfaceView 区域内
    val glRect = Rect()
    glSurfaceView.getGlobalVisibleRect(glRect)
    
    if (!glRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
        // 触摸点不在 GLSurfaceView 内，交给其他 View 处理
        return super.onTouchEvent(event)
    }
    
    // 处理图片的缩放、平移手势
    scaleGestureDetector.onTouchEvent(event)
    gestureDetector.onTouchEvent(event)
    // ...
    return true
}
```

**调试技巧：**

- 使用 Layout Inspector 查看 View 层级和边界
- 在 `onTouchEvent` 中添加日志，确认触摸事件的分发路径
- 临时将 GLSurfaceView 设置为 `android:clickable="false"` 测试

### 7.6 Android 13 权限适配问题

**问题描述：** 在 Android 13 设备上，使用 `READ_EXTERNAL_STORAGE` 权限无法读取媒体文件。

**原因分析：** Android 13 引入了细粒度媒体权限，`READ_EXTERNAL_STORAGE` 被拆分为 `READ_MEDIA_IMAGES`、`READ_MEDIA_VIDEO`、`READ_MEDIA_AUDIO`。

**解决方案：** 根据 Android 版本动态请求对应权限：

```kotlin
fun getRequiredMediaPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            // Android 13+
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            // Android 10-12
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else -> {
            // Android 9 及以下
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}
```

---

## 八、项目结构总览

```
app/src/main/java/com/example/myapplication/
├── MyApplication.kt                 # Application 类，初始化全局组件
├── MainActivity.kt                  # 主 Activity，TabLayout + ViewPager2
├── data/
│   ├── database/
│   │   └── AppDataBase.kt          # Room 数据库配置
│   ├── dao/
│   │   ├── DraftDao.kt             # 草稿 DAO
│   │   ├── EditedImageDao.kt       # 作品 DAO
│   │   ├── AlbumDao.kt             # 相册 DAO
│   │   └── UserDao.kt              # 用户 DAO
│   ├── entity/
│   │   ├── Draft.kt                # 草稿实体
│   │   ├── EditedImage.kt          # 作品实体
│   │   ├── Album.kt                # 相册实体
│   │   └── User.kt                 # 用户实体
│   └── repository/
│       ├── DraftRepository.kt      # 草稿仓库
│       ├── EditedImageRepository.kt # 作品仓库
│       └── AlbumRepository.kt      # 相册仓库
├── ui/
│   ├── home/
│   │   ├── HomeFragment.kt         # 首页 Fragment
│   │   └── QuickAccessAdapter.kt   # 快捷功能适配器
│   ├── my/
│   │   └── MyFragment.kt           # 我的页面 Fragment
│   ├── widget/
│   │   ├── ShimmerImageView.kt     # 扫光效果自定义 View
│   │   ├── RainbowBorderImageView.kt # 彩虹边框自定义 View
│   │   └── CropOverlayView.kt      # 裁剪覆盖层自定义 View
│   ├── AlbumActivity.kt            # 相册页面
│   ├── EditorActivity.kt           # 编辑器页面
│   ├── ImageRenderer.kt            # OpenGL 渲染器
│   ├── FilterType.kt               # 滤镜类型枚举
│   ├── FilterAdapter.kt            # 滤镜选择适配器
│   ├── AdjustmentAdapter.kt        # 调整参数适配器
│   ├── PhotoAdapter.kt             # 照片网格适配器
│   └── VideoPlayerActivity.kt      # 视频播放器
└── utils/
    ├── MediaStoreHelper.kt         # 媒体库访问工具
    ├── PermissionHelper.kt         # 权限处理工具
    └── GlobalExceptionHandler.kt   # 全局异常处理
```

---

## 九、总结

本项目实现了一个功能完整的图片编辑应用，涵盖了 Android 开发的多个核心技术点：

1. **UI 开发**：使用原生控件和自定义 View 实现丰富的视觉效果
2. **媒体处理**：通过 MediaStore API 访问设备媒体库，支持多种格式
3. **图形渲染**：使用 OpenGL ES 2.0 实现高性能图片渲染和滤镜效果
4. **数据持久化**：使用 Room 数据库管理草稿和作品数据
5. **异步编程**：使用 Kotlin 协程和 Flow 处理异步操作
6. **设备适配**：针对不同 Android 版本和厂商进行适配

通过本项目的开发，深入理解了 Android 应用开发的最佳实践，积累了解决实际问题的经验。
