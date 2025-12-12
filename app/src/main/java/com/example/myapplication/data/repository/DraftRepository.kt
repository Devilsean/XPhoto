package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.DraftDao
import com.example.myapplication.data.entity.Draft
import kotlinx.coroutines.flow.Flow

class DraftRepository(private val draftDao: DraftDao) {
    
    val allDrafts: Flow<List<Draft>> = draftDao.getAllFlow()
    
    suspend fun saveDraft(draft: Draft): Long {
        return draftDao.insert(draft)
    }

    suspend fun updateDraft(draft: Draft) {
        draftDao.update(draft)
    }
    
    suspend fun deleteDraft(draft: Draft) {
        draftDao.delete(draft)
    }
    
    /**
     * 通过ID删除草稿
     * 用于导出作品后删除对应的草稿
     */
    suspend fun deleteDraft(id: Long) {
        draftDao.deleteById(id)
    }
    
    suspend fun getDraftById(id: Long): Draft? {
        return draftDao.getById(id)
    }
    
    suspend fun getDraftsByImageUri(uri: String): List<Draft> {
        return draftDao.getByOriginalUri(uri)
    }
    
    suspend fun getRecentDrafts(limit: Int = 5): List<Draft> {
        return draftDao.getRecent(limit)
    }

    suspend fun saveOrUpdateDraft(draft: Draft): Long {
        return if (draft.id == 0L) {
            draftDao.insert(draft)
        } else {
            draftDao.update(draft)
            draft.id
        }
    }
    
    suspend fun deleteAllDrafts() {
        draftDao.deleteAll()
    }
    
    /**
     * 批量删除草稿
     */
    suspend fun deleteDrafts(ids: List<Long>) {
        draftDao.deleteByIds(ids)
    }
}