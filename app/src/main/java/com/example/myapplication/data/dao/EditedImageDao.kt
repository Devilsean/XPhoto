package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.EditedImage
import kotlinx.coroutines.flow.Flow

@Dao
interface EditedImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(editedImage: EditedImage): Long
    
    @Update
    suspend fun update(editedImage: EditedImage)
    
    @Delete
    suspend fun delete(editedImage: EditedImage)
    
    @Query("SELECT * FROM edited_images WHERE id = :id")
    suspend fun getById(id: Long): EditedImage?
    
    @Query("SELECT * FROM edited_images ORDER BY modifiedAt DESC")
    fun getAllFlow(): Flow<List<EditedImage>>
    
    @Query("SELECT * FROM edited_images ORDER BY modifiedAt DESC")
    suspend fun getAll(): List<EditedImage>
    
    @Query("SELECT * FROM edited_images WHERE originalImageUri = :uri ORDER BY modifiedAt DESC")
    suspend fun getByOriginalUri(uri: String): List<EditedImage>
    
    @Query("DELETE FROM edited_images")
    suspend fun deleteAll()
    
    @Query("DELETE FROM edited_images WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    // 收藏相关查询
    @Query("SELECT * FROM edited_images WHERE isFavorite = 1 ORDER BY modifiedAt DESC")
    fun getFavoritesFlow(): Flow<List<EditedImage>>
    
    @Query("SELECT * FROM edited_images WHERE isFavorite = 1 ORDER BY modifiedAt DESC")
    suspend fun getFavorites(): List<EditedImage>
    
    @Query("UPDATE edited_images SET isFavorite = :isFavorite, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean, modifiedAt: Long = System.currentTimeMillis())
    
    /**
     * 更新导出URI
     * 当作品被导出到系统相册时，记录导出的URI
     */
    @Query("UPDATE edited_images SET exportedUri = :exportedUri, isExported = 1, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun updateExportedUri(id: Long, exportedUri: String, modifiedAt: Long = System.currentTimeMillis())
}