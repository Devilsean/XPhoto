package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.Draft
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: Draft): Long
    
    @Update
    suspend fun update(draft: Draft)
    
    @Delete
    suspend fun delete(draft: Draft)
    
    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getById(id: Long): Draft?
    
    @Query("SELECT * FROM drafts ORDER BY modifiedAt DESC")
    fun getAllFlow(): Flow<List<Draft>>
    
    @Query("SELECT * FROM drafts ORDER BY modifiedAt DESC")
    suspend fun getAll(): List<Draft>
    
    @Query("SELECT * FROM drafts WHERE originalImageUri = :uri ORDER BY modifiedAt DESC")
    suspend fun getByOriginalUri(uri: String): List<Draft>
    
    @Query("SELECT * FROM drafts ORDER BY modifiedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<Draft>
    
    @Query("DELETE FROM drafts")
    suspend fun deleteAll()
    
    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM drafts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}