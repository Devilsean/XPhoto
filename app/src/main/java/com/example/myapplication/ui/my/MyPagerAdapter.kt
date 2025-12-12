package com.example.myapplication.ui.my

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.R

class MyPagerAdapter(private val fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        val context = fragment.requireContext()
        val contentType = when (position) {
            0 -> context.getString(R.string.content_type_drafts)
            1 -> context.getString(R.string.content_type_works)
            2 -> context.getString(R.string.content_type_favorites)
            else -> ""
        }
        return MyContentFragment.newInstance(contentType)
    }
}