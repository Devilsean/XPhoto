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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.ui.EditProfileActivity
import com.example.myapplication.ui.SettingsActivity
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
        val infoCard = view.findViewById<View>(R.id.info_card)
        val settingsButton = view.findViewById<View>(R.id.settings_button)

        // Tab + ViewPager
        viewPager.adapter = MyPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_drafts)
                1 -> getString(R.string.tab_completed)
                2 -> getString(R.string.tab_favorites)
                else -> null
            }
        }.attach()

        settingsButton.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        // 加载用户信息
        val app = requireActivity().application as MyApplication
        val userRepository = app.userRepository

        viewLifecycleOwner.lifecycleScope.launch {
            userRepository.user.collect { user ->
                user?.let {
                    // 如果昵称是默认值或为空，显示本地化的默认昵称
                    nicknameTextView.text = if (it.nickname.isEmpty() || it.nickname == "用户昵称") {
                        getString(R.string.default_nickname)
                    } else {
                        it.nickname
                    }
                    
                    // 如果签名是默认值或为空，显示本地化的默认签名
                    signatureTextView.text = if (it.signature.isEmpty() || it.signature == "这个人很懒，什么都没留下") {
                        getString(R.string.default_user_signature)
                    } else {
                        it.signature
                    }

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
