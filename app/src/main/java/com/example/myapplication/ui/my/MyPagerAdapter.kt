package com.example.myapplication.ui.my

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        val contentType=when (position) {
            0 -> "草稿"
            1 -> "作品"
            2 -> "收藏"
            else -> ""
        }
        return MyContentFragment.newInstance(contentType)
    }
}