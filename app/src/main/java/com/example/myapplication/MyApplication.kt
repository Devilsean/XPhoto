package com.example.myapplication

import android.app.Application
import android.util.Log
import com.example.myapplication.data.database.AppDataBase
import com.example.myapplication.data.repository.AlbumRepository
import com.example.myapplication.data.repository.DraftRepository
import com.example.myapplication.data.repository.EditedImageRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.utils.GlobalExceptionHandler
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MyApplication: Application() {
    
    companion object {
        private const val TAG = "MyApplication"
    }
    
    // 协程异常处理器
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常", throwable)
    }
    
    // 应用级协程作用域，添加异常处理器
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler
    )
    
    val database: AppDataBase by lazy {
        try {
            AppDataBase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "数据库初始化失败", e)
            throw e
        }
    }
    
    val draftRepository: DraftRepository by lazy {
        try {
            DraftRepository(database.draftDao())
        } catch (e: Exception) {
            Log.e(TAG, "DraftRepository 初始化失败", e)
            throw e
        }
    }
    
    val editedImageRepository: EditedImageRepository by lazy {
        try {
            EditedImageRepository(database.editedImageDao())
        } catch (e: Exception) {
            Log.e(TAG, "EditedImageRepository 初始化失败", e)
            throw e
        }
    }
    
    val albumRepository: AlbumRepository by lazy {
        try {
            AlbumRepository(database.albumDao(), database.albumImageDao())
        } catch (e: Exception) {
            Log.e(TAG, "AlbumRepository 初始化失败", e)
            throw e
        }
    }

    val userRepository: UserRepository by lazy {
        try {
            UserRepository(database.userDao())
        } catch (e: Exception) {
            Log.e(TAG, "UserRepository 初始化失败", e)
            throw e
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application onCreate 开始")

        try {
            // 初始化全局异常处理器
            GlobalExceptionHandler.init(this)
            Log.d(TAG, "全局异常处理器初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "全局异常处理器初始化失败", e)
        }

        try {
            // 应用动态颜色主题
            DynamicColors.applyToActivitiesIfAvailable(this)
            Log.d(TAG, "动态颜色主题应用完成")
        } catch (e: Exception) {
            Log.e(TAG, "动态颜色主题应用失败", e)
        }

        // 初始化用户数据（只检查一次）- 延迟执行，不阻塞启动
        applicationScope.launch {
            try {
                Log.d(TAG, "开始初始化用户数据")
                val user = userRepository.user.firstOrNull()
                if (user == null) {
                    Log.d(TAG, "用户数据为空，插入默认用户")
                    userRepository.insertDefaultUser()
                }
                Log.d(TAG, "用户数据初始化完成")
            } catch (e: Exception) {
                Log.e(TAG, "初始化用户数据失败", e)
                // 初始化失败时记录日志，但不崩溃
            }
        }
        
        Log.d(TAG, "Application onCreate 完成")
    }
}
