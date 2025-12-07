package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 查找导航控制器
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        // 查找底部导航视图
        val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)

        // 将导航控制器与底部导航视图关联起来
        // 这样点击底部按钮时，NavHostFragment 中的页面就会自动切换
        navView.setupWithNavController(navController)
    }
}