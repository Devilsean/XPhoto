package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.EditedImageDao
import com.example.myapplication.data.entity.EditedImage
import kotlinx.coroutines.flow.Flow


class EditedImageRepository(private val editedImageDao: EditedImageDao) {
    
    val allEditedImages: Flow<List<EditedImage>> = editedImageDao.getAllFlow()
    
    // 收藏作品Flow
    val favoriteImages: Flow<List<EditedImage>> = editedImageDao.getFavoritesFlow()
    
    suspend fun saveEditedImage(editedImage: EditedImage): Long {
        return editedImageDao.insert(editedImage)
    }
    
    suspend fun updateEditedImage(editedImage: EditedImage) {
        editedImageDao.update(editedImage)
    }
    
    suspend fun deleteEditedImage(editedImage: EditedImage) {
        editedImageDao.delete(editedImage)
    }
    
    suspend fun getEditedImageById(id: Long): EditedImage? {
        return editedImageDao.getById(id)
    }
    

    suspend fun getEditedImagesByOriginalUri(uri: String): List<EditedImage> {
        return editedImageDao.getByOriginalUri(uri)
    }

    suspend fun getAllEditedImages(): List<EditedImage> {
        return editedImageDao.getAll()
    }

    suspend fun deleteAllEditedImages() {
        editedImageDao.deleteAll()
    }
    
    // 收藏相关方法
    suspend fun getFavoriteImages(): List<EditedImage> {
        return editedImageDao.getFavorites()
    }
    
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        editedImageDao.updateFavoriteStatus(id, isFavorite)
    }
    
    suspend fun toggleFavorite(id: Long): Boolean {
        val image = editedImageDao.getById(id)
        if (image != null) {
            val newStatus = !image.isFavorite
            editedImageDao.updateFavoriteStatus(id, newStatus)
            return newStatus
        }
        return false
    }
    
    /**
     * 更新导出URI
     * 当作品被导出到系统相册时，记录导出的URI
     */
    suspend fun updateExportedUri(id: Long, exportedUri: String) {
        editedImageDao.updateExportedUri(id, exportedUri)
    }
    
    /**
     * 批量删除作品
     */
    suspend fun deleteEditedImages(ids: List<Long>) {
        editedImageDao.deleteByIds(ids)
    }
}