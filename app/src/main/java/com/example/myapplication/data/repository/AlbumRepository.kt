package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.AlbumDao
import com.example.myapplication.data.dao.AlbumImageDao
import com.example.myapplication.data.entity.Album
import com.example.myapplication.data.entity.AlbumImage
import kotlinx.coroutines.flow.Flow

class AlbumRepository(
    private val albumDao: AlbumDao,
    private val albumImageDao: AlbumImageDao
) {
    
    val allAlbums: Flow<List<Album>> = albumDao.getAllFlow()
    
    // 相册操作 
    suspend fun createAlbum(album: Album): Long {
        return albumDao.insert(album)
    }
    
    suspend fun updateAlbum(album: Album) {
        albumDao.update(album)
    }
    
    suspend fun deleteAlbum(album: Album) {
        albumDao.delete(album)
    }
    
    suspend fun getAlbumById(id: Long): Album? {
        return albumDao.getById(id)
    }
    
    suspend fun getAllAlbums(): List<Album> {
        return albumDao.getAll()
    }
    
    suspend fun searchAlbums(keyword: String): List<Album> {
        return albumDao.searchByName(keyword)
    }
    
    // 相册图片操作 
    suspend fun addImageToAlbum(albumId: Long, imageUri: String): Long {
        val count = albumImageDao.isImageInAlbum(albumId, imageUri)
        if (count > 0) {
            return -1
        }
        
        val albumImage = AlbumImage(
            albumId = albumId,
            imageUri = imageUri
        )
        return albumImageDao.insert(albumImage)
    }
    
    suspend fun removeImageFromAlbum(albumId: Long, imageUri: String) {
        albumImageDao.removeImageFromAlbum(albumId, imageUri)
    }
    
    fun getAlbumImagesFlow(albumId: Long): Flow<List<AlbumImage>> {
        return albumImageDao.getImagesByAlbumFlow(albumId)
    }
    
    suspend fun getAlbumImages(albumId: Long): List<AlbumImage> {
        return albumImageDao.getImagesByAlbum(albumId)
    }
    
    suspend fun getAlbumsContainingImage(imageUri: String): List<AlbumImage> {
        return albumImageDao.getAlbumsByImage(imageUri)
    }
    
    suspend fun getAlbumImageCount(albumId: Long): Int {
        return albumImageDao.getImageCount(albumId)
    }
    
    suspend fun isImageInAlbum(albumId: Long, imageUri: String): Boolean {
        return albumImageDao.isImageInAlbum(albumId, imageUri) > 0
    }
    
    // 组合操作
    suspend fun createAlbumWithImages(album: Album, imageUris: List<String>): Long {
        val albumId = albumDao.insert(album)
        imageUris.forEach { uri ->
            val albumImage = AlbumImage(
                albumId = albumId,
                imageUri = uri
            )
            albumImageDao.insert(albumImage)
        }
        
        return albumId
    }
    
    // 删除所有相册和相册图片
    suspend fun deleteAllAlbums() {
        albumImageDao.deleteAll()
        albumDao.deleteAll()
    }
}