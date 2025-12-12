package com.example.myapplication.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "SplashActivity onCreate 开始")
            setContentView(R.layout.activity_splash)
            Log.d(TAG, "setContentView 完成")

            // 延迟 1 秒后跳转到主界面
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d(TAG, "准备跳转到 MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "跳转到 MainActivity 失败", e)
                }
            }, 1000)
            
            Log.d(TAG, "SplashActivity onCreate 完成")
        } catch (e: Exception) {
            Log.e(TAG, "SplashActivity onCreate 异常", e)
            // 尝试直接跳转到主界面
            try {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } catch (e2: Exception) {
                Log.e(TAG, "紧急跳转也失败", e2)
            }
        }
    }
}
