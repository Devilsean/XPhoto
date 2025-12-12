package com.example.myapplication.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val duration: Long = 0 // 视频时长（毫秒）
    ) {
        val isVideo: Boolean get() = mimeType.startsWith("video/")
        val isImage: Boolean get() = mimeType.startsWith("image/")
    }

    /**
     * 分页参数
     */
    data class PageParams(
        val pageSize: Int = 50,
        val offset: Int = 0
    )

    /**
     * 加载所有媒体（图片+视频）
     */
    suspend fun loadAllMedia(pageParams: PageParams = PageParams()): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val mediaList = mutableListOf<MediaItem>()
            
            // 加载图片
            mediaList.addAll(loadImages(PageParams(pageSize = Int.MAX_VALUE, offset = 0)))
            
            // 加载视频
            mediaList.addAll(loadVideos(PageParams(pageSize = Int.MAX_VALUE, offset = 0)))
            
            // 按日期排序并应用分页 - 修复：需要返回排序后的结果
            return@withContext mediaList.sortedByDescending { it.dateAdded }
                .drop(pageParams.offset)
                .take(pageParams.pageSize)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MediaStoreHelper", "加载媒体失败", e)
            return@withContext emptyList()
        }
    }

    /**
     * 加载图片
     */
    suspend fun loadImages(pageParams: PageParams = PageParams()): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            var count = 0
            // 跳过offset数量的项
            if (pageParams.offset > 0) {
                cursor.move(pageParams.offset)
            }
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            
            while (cursor.moveToNext() && count < pageParams.pageSize) {
                count++
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameColumn),
                        mimeType = cursor.getString(mimeTypeColumn),
                        dateAdded = cursor.getLong(dateColumn),
                        size = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn)
                    )
                )
            }
        }
        
        mediaList
    }

    /**
     * 加载视频
     */
    suspend fun loadVideos(pageParams: PageParams = PageParams()): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )
        
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            var count = 0
            // 跳过offset数量的项
            if (pageParams.offset > 0) {
                cursor.move(pageParams.offset)
            }
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            
            while (cursor.moveToNext() && count < pageParams.pageSize) {
                count++
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameColumn),
                        mimeType = cursor.getString(mimeTypeColumn),
                        dateAdded = cursor.getLong(dateColumn),
                        size = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        duration = cursor.getLong(durationColumn)
                    )
                )
            }
        }
        
        mediaList
    }
}