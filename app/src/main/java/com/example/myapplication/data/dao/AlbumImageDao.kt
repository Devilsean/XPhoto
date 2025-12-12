package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entity.AlbumImage
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(albumImage: AlbumImage): Long
    
    @Delete
    suspend fun delete(albumImage: AlbumImage)
    
    @Query("SELECT * FROM album_images WHERE id = :id")
    suspend fun getById(id: Long): AlbumImage?
    
    @Query("SELECT * FROM album_images WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun getImagesByAlbumFlow(albumId: Long): Flow<List<AlbumImage>>
    
    @Query("SELECT * FROM album_images WHERE albumId = :albumId ORDER BY addedAt DESC")
    suspend fun getImagesByAlbum(albumId: Long): List<AlbumImage>
    
    @Query("SELECT * FROM album_images WHERE imageUri = :imageUri")
    suspend fun getAlbumsByImage(imageUri: String): List<AlbumImage>
    
    @Query("SELECT COUNT(*) FROM album_images WHERE albumId = :albumId AND imageUri = :imageUri")
    suspend fun isImageInAlbum(albumId: Long, imageUri: String): Int

    @Query("SELECT COUNT(*) FROM album_images WHERE albumId = :albumId")
    suspend fun getImageCount(albumId: Long): Int
    
    @Query("DELETE FROM album_images WHERE albumId = :albumId AND imageUri = :imageUri")
    suspend fun removeImageFromAlbum(albumId: Long, imageUri: String)
    
    @Query("DELETE FROM album_images WHERE albumId = :albumId")
    suspend fun deleteAllByAlbum(albumId: Long)
    
    @Query("DELETE FROM album_images")
    suspend fun deleteAll()
}