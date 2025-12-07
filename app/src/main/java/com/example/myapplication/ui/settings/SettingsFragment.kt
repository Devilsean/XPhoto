package com.example.myapplication.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)

        // 为“退出登录”添加点击事件和确认对话框
        val logoutPreference: Preference? = findPreference("logout")
        logoutPreference?.setOnPreferenceClickListener {
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("确认退出")
                    .setMessage("您确定要退出登录吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定") { _, _ ->
                        // 在这里处理实际的退出登录逻辑
                        // 例如：清除 token、跳转到登录页等
                    }
                    .show()
            }
            true
        }
    }
}