package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.AlbumRepository
import com.example.myapplication.data.repository.DraftRepository
import com.example.myapplication.data.repository.EditedImageRepository
import com.google.android.material.color.DynamicColors

/**
 * 应用程序类
 * 这个类在应用启动时首先被创建，整个应用生命周期内只有一个实例
 * 适合在这里初始化全局单例对象，如数据库
 */
class MyApplication: Application() {
    
    /**
     * 数据库实例
     * 使用 lazy 延迟初始化：只有在第一次访问时才会创建
     * 这样可以避免应用启动时的性能开销
     */
    val database: AppDatabase by lazy { 
        AppDatabase.getDatabase(this) 
    }
    
    /**
     * Repository 实例
     * 这些 Repository 提供了对数据库的访问接口
     * 在Activity 或 Fragment 中可以通过 (application as MyApplication).xxxRepository 访问
     */
    val draftRepository: DraftRepository by lazy {
        DraftRepository(database.draftDao())
    }
    
    val editedImageRepository: EditedImageRepository by lazy {
        EditedImageRepository(database.editedImageDao())
    }
    
    val albumRepository: AlbumRepository by lazy {
        AlbumRepository(database.albumDao(), database.albumImageDao())
    }
    
    override fun onCreate(){
        super.onCreate()
        // 启用Material You 动态颜色主题
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
