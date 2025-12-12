package com.example.myapplication.utils

import android.content.Context
import android.util.Log
import kotlin.system.exitProcess

/**
 * 全局异常处理器
 * 捕获未处理的异常，记录日志并优雅地关闭应用
 */
class GlobalExceptionHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 记录异常信息
            Log.e(TAG, "未捕获的异常发生在线程: ${thread.name}", throwable)
            
            // 记录详细的堆栈信息
            val stackTrace = throwable.stackTraceToString()
            Log.e(TAG, "堆栈跟踪:\n$stackTrace")
            
            // 可以在这里添加崩溃报告上传逻辑
            // 例如：上传到Firebase Crashlytics或其他崩溃分析服务
            
        } catch (e: Exception) {
            Log.e(TAG, "异常处理器本身发生异常", e)
        } finally {
            // 调用默认的异常处理器
            defaultHandler?.uncaughtException(thread, throwable)
            
            // 退出应用
            exitProcess(1)
        }
    }

    companion object {
        private const val TAG = "GlobalExceptionHandler"
        
        @Volatile
        private var instance: GlobalExceptionHandler? = null
        
        /**
         * 初始化全局异常处理器
         */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                        instance = GlobalExceptionHandler(
                            context.applicationContext,
                            defaultHandler
                        )
                        Thread.setDefaultUncaughtExceptionHandler(instance)
                        Log.d(TAG, "全局异常处理器已初始化")
                    }
                }
            }
        }
    }
}