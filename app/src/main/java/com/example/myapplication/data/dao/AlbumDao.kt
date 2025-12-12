package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album): Long
    
    @Update
    suspend fun update(album: Album)
    
    @Delete
    suspend fun delete(album: Album)
    
    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: Long): Album?
    
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Album>>
    
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    suspend fun getAll(): List<Album>
    
    @Query("SELECT * FROM albums WHERE name LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    suspend fun searchByName(keyword: String): List<Album>
    
    @Query("DELETE FROM albums")
    suspend fun deleteAll()
}