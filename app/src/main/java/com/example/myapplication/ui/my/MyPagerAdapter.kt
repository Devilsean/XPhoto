package com.example.myapplication.ui.my

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        val contentType=when (position) {
            0 -> "Drafts"
            1 -> "Completed"
            2 -> "Favorites"
            else -> ""
        }
        return MyContentFragment.newInstance(contentType)
    }
}