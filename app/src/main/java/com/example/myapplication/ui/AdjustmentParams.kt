package com.example.myapplication.ui


data class AdjustmentParams(
    var brightness: Float = 0f,      // 亮度 (-1.0 到 1.0)
    var contrast: Float = 0f,        // 对比度 (-1.0 到 1.0)
    var saturation: Float = 0f,      // 饱和度 (-1.0 到 1.0)
    var highlights: Float = 0f,      // 高光 (-1.0 到 1.0)
    var shadows: Float = 0f,         // 阴影 (-1.0 到 1.0)
    var temperature: Float = 0f,     // 色温 (-1.0 到 1.0, 负值偏冷，正值偏暖)
    var tint: Float = 0f,            // 色调 (-1.0 到 1.0, 负值偏绿，正值偏品红)
    var clarity: Float = 0f,         // 清晰度 (0.0 到 1.0)
    var sharpen: Float = 0f          // 锐化 (0.0 到 1.0)
) {

    fun copy(): AdjustmentParams {
        return AdjustmentParams(
            brightness = this.brightness,
            contrast = this.contrast,
            saturation = this.saturation,
            highlights = this.highlights,
            shadows = this.shadows,
            temperature = this.temperature,
            tint = this.tint,
            clarity = this.clarity,
            sharpen = this.sharpen
        )
    }
    

    fun reset() {
        brightness = 0f
        contrast = 0f
        saturation = 0f
        highlights = 0f
        shadows = 0f
        temperature = 0f
        tint = 0f
        clarity = 0f
        sharpen = 0f
    }
    

    fun hasAnyAdjustment(): Boolean {
        return brightness != 0f || contrast != 0f || saturation != 0f ||
               highlights != 0f || shadows != 0f || temperature != 0f ||
               tint != 0f || clarity != 0f || sharpen != 0f
    }
    
    fun merge(other: AdjustmentParams) {
        brightness = (brightness + other.brightness).coerceIn(-1f, 1f)
        contrast = (contrast + other.contrast).coerceIn(-1f, 1f)
        saturation = (saturation + other.saturation).coerceIn(-1f, 1f)
        highlights = (highlights + other.highlights).coerceIn(-1f, 1f)
        shadows = (shadows + other.shadows).coerceIn(-1f, 1f)
        temperature = (temperature + other.temperature).coerceIn(-1f, 1f)
        tint = (tint + other.tint).coerceIn(-1f, 1f)
        clarity = (clarity + other.clarity).coerceIn(0f, 1f)
        sharpen = (sharpen + other.sharpen).coerceIn(0f, 1f)
    }
}

enum class AdjustmentType(
    val displayName: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float
) {
    BRIGHTNESS("亮度", -1f, 1f, 0f),
    CONTRAST("对比度", -1f, 1f, 0f),
    SATURATION("饱和度", -1f, 1f, 0f),
    HIGHLIGHTS("高光", -1f, 1f, 0f),
    SHADOWS("阴影", -1f, 1f, 0f),
    TEMPERATURE("色温", -1f, 1f, 0f),
    TINT("色调", -1f, 1f, 0f),
    CLARITY("清晰度", 0f, 1f, 0f),
    SHARPEN("锐化", 0f, 1f, 0f);
    
    fun getValue(params: AdjustmentParams): Float {
        return when (this) {
            BRIGHTNESS -> params.brightness
            CONTRAST -> params.contrast
            SATURATION -> params.saturation
            HIGHLIGHTS -> params.highlights
            SHADOWS -> params.shadows
            TEMPERATURE -> params.temperature
            TINT -> params.tint
            CLARITY -> params.clarity
            SHARPEN -> params.sharpen
        }
    }
    
    fun setValue(params: AdjustmentParams, value: Float) {
        when (this) {
            BRIGHTNESS -> params.brightness = value
            CONTRAST -> params.contrast = value
            SATURATION -> params.saturation = value
            HIGHLIGHTS -> params.highlights = value
            SHADOWS -> params.shadows = value
            TEMPERATURE -> params.temperature = value
            TINT -> params.tint = value
            CLARITY -> params.clarity = value
            SHARPEN -> params.sharpen = value
        }
    }
}