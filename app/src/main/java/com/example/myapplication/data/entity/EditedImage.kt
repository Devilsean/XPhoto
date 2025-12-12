package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edited_images")
data class EditedImage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalImageUri: String,         // 原始图片URI
    val editedImageUri: String,           // 编辑后图片URI（应用内部存储）
    val exportedUri: String? = null,      // 导出到系统相册的URI（可选）
    val thumbnailPath: String? = null,    // 缩略图路径
    val isExported: Boolean = false,      // 是否已导出到系统相册
    val isFavorite: Boolean = false,      // 是否收藏（收藏只是作品的一个标识，便于查找）
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
