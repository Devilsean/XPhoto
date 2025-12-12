package com.example.myapplication.ui

data class EditState(
    val adjustmentParams: AdjustmentParams,
    val scaleFactor: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotationAngle: Float,
    val cropRect: android.graphics.RectF?,
    val isGrayscaleEnabled: Boolean,
    val filterType: FilterType,
    val bitmapHistoryIndex: Int = -1,  // bitmap历史索引，用于裁剪撤销
    val imageWidth: Int = 0,           // 当前图片宽度
    val imageHeight: Int = 0,          // 当前图片高度
    val timestamp: Long = System.currentTimeMillis()
) {
    fun copy(): EditState {
        return EditState(
            adjustmentParams = adjustmentParams.copy(),
            scaleFactor = scaleFactor,
            offsetX = offsetX,
            offsetY = offsetY,
            rotationAngle = rotationAngle,
            cropRect = cropRect?.let { android.graphics.RectF(it) },
            isGrayscaleEnabled = isGrayscaleEnabled,
            filterType = filterType,
            bitmapHistoryIndex = bitmapHistoryIndex,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            timestamp = timestamp
        )
    }
}

class EditHistory(private val maxHistorySize: Int = 50) {
    private val history = mutableListOf<EditState>()
    private var currentIndex = -1
    
    fun addState(state: EditState) {
        // 如果当前不在历史末尾，删除当前位置之后的所有状态
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        
        // 添加新状态
        history.add(state.copy())
        currentIndex = history.size - 1
        
        // 如果超过最大历史记录数，删除最旧的记录
        if (history.size > maxHistorySize) {
            history.removeAt(0)
            currentIndex--
        }
    }
    
    fun undo(): EditState? {
        if (!canUndo()) return null
        currentIndex--
        return history[currentIndex].copy()
    }
    
    fun redo(): EditState? {
        if (!canRedo()) return null
        currentIndex++
        return history[currentIndex].copy()
    }
    
    fun canUndo(): Boolean {
        return currentIndex > 0
    }
    
    fun canRedo(): Boolean {
        return currentIndex < history.size - 1
    }

    fun getCurrentState(): EditState? {
        return if (currentIndex >= 0 && currentIndex < history.size) {
            history[currentIndex].copy()
        } else {
            null
        }
    }

    fun clear() {
        history.clear()
        currentIndex = -1
    }

    fun getHistorySize(): Int {
        return history.size
    }

    fun getCurrentIndex(): Int {
        return currentIndex
    }
}