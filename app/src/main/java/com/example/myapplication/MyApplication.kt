package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.AlbumRepository
import com.example.myapplication.data.repository.DraftRepository
import com.example.myapplication.data.repository.EditedImageRepository
import com.example.myapplication.data.repository.UserRepository
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyApplication: Application() {
    
    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
    
    val draftRepository: DraftRepository by lazy {
        DraftRepository(database.draftDao())
    }
    
    val editedImageRepository: EditedImageRepository by lazy {
        EditedImageRepository(database.editedImageDao())
    }
    
    val albumRepository: AlbumRepository by lazy {
        AlbumRepository(database.albumDao(), database.albumImageDao())
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }
    
    override fun onCreate() {
        super.onCreate()

        // 初始化用户数据（只检查一次）
        applicationScope.launch {
            val user = userRepository.user.first()
            if (user == null) {
                userRepository.insertDefaultUser()
            }
        }
    }
}
