package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalImageUri: String,
    val isGrayscaleEnabled: Boolean = false,
    val scaleFactor: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    // 旋转角度
    val rotationAngle: Float = 0f,
    // 裁剪信息
    val cropLeft: Float? = null,
    val cropTop: Float? = null,
    val cropRight: Float? = null,
    val cropBottom: Float? = null,
    val thumbnailPath: String? = null,
    val filterType: String? = null,
    // 调整参数
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val clarity: Float = 0f,
    val sharpen: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
