package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "MainActivity onCreate 开始")
            setContentView(R.layout.activity_main)
            Log.d(TAG, "setContentView 完成")

            // 查找导航控制器
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            if (navHostFragment == null) {
                Log.e(TAG, "NavHostFragment 未找到")
                return
            }
            
            val navController: NavController = navHostFragment.navController
            Log.d(TAG, "NavController 获取成功")

            // 查找底部导航视图
            val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
            Log.d(TAG, "BottomNavigationView 获取成功")

            // 将导航控制器与底部导航视图关联起来
            // 这样点击底部按钮时，NavHostFragment 中的页面就会自动切换
            navView.setupWithNavController(navController)
            Log.d(TAG, "导航设置完成")
            
            Log.d(TAG, "MainActivity onCreate 完成")
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity onCreate 异常", e)
        }
    }
}