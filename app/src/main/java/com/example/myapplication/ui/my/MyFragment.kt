package com.example.myapplication.ui.my

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.ui.EditProfileActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        val avatarImageView = view.findViewById<ImageView>(R.id.avatar_view)
        val nicknameTextView = view.findViewById<TextView>(R.id.tv_user_name)
        val signatureTextView = view.findViewById<TextView>(R.id.tv_user_desc)
        val statsTextView = view.findViewById<TextView>(R.id.tv_user_stats)
        val infoCard = view.findViewById<View>(R.id.info_card)
    
        viewPager.adapter = MyPagerAdapter(this)
    
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "草稿箱"
                1 -> "已完成"
                2 -> "收藏"
                else -> null
            }
        }.attach()
        
        view.findViewById<View>(R.id.settings_button).setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }
        
        // 加载用户信息
        val app = requireActivity().application as MyApplication
        val userRepository = app.userRepository
        
        viewLifecycleOwner.lifecycleScope.launch {
            userRepository.user.collect { user ->
                user?.let {
                    nicknameTextView.text = it.nickname
                    signatureTextView.text = it.signature
                    // 可以在这里添加统计信息的显示
                    // statsTextView.text = "作品 ${worksCount} · 草稿 ${draftsCount} · 相册 ${albumsCount}"
                    it.avatarUri?.let { uri ->
                        Glide.with(requireContext())
                            .load(Uri.parse(uri))
                            .into(avatarImageView)
                    }
                }
            }
        }
        
        // 点击卡片编辑信息
        infoCard.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
    }

}
