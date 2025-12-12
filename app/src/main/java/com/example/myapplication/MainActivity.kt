package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.myapplication.ui.home.HomeFragment
import com.example.myapplication.ui.my.MyFragment

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    // Tab 标题资源ID列表
    private val tabTitleResIds = listOf(R.string.tab_edit, R.string.tab_my)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "MainActivity onCreate started")
            setContentView(R.layout.activity_main)
            Log.d(TAG, "setContentView completed")

            // 初始化 ViewPager2
            viewPager = findViewById(R.id.view_pager)
            Log.d(TAG, "ViewPager2 initialized")
            
            // 初始化 TabLayout
            tabLayout = findViewById(R.id.tab_layout)
            Log.d(TAG, "TabLayout initialized")
            
            // 设置 ViewPager2 适配器
            viewPager.adapter = MainPagerAdapter(this)
            Log.d(TAG, "ViewPager2 adapter set")
            
            // 使用 TabLayoutMediator 将 TabLayout 与 ViewPager2 关联
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = getString(tabTitleResIds[position])
                // 不设置图标，仅显示文字
                tab.icon = null
            }.attach()
            Log.d(TAG, "TabLayout and ViewPager2 connected")
            
            Log.d(TAG, "MainActivity onCreate completed")
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity onCreate exception", e)
        }
    }
    
    /**
     * ViewPager2 适配器
     */
    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        
        override fun getItemCount(): Int = tabTitleResIds.size
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> MyFragment()
                else -> HomeFragment()
            }
        }
    }
}