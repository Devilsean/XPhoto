package com.example.myapplication.ui.my

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyPagerAdapter(private val fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        // 使用固定常量而不是本地化字符串，避免语言切换后数据混乱
        val contentType = when (position) {
            0 -> MyContentFragment.TYPE_DRAFTS
            1 -> MyContentFragment.TYPE_WORKS
            2 -> MyContentFragment.TYPE_FAVORITES
            else -> ""
        }
        return MyContentFragment.newInstance(contentType)
    }
}