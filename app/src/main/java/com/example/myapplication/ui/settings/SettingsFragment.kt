package com.example.myapplication.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)

        // “主题设置”功能
        val themePreference: ListPreference? = findPreference("theme")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue as String) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        val clearCachePreference: Preference? = findPreference("clear_cache")
        clearCachePreference?.setOnPreferenceClickListener {
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("清理缓存")
                    .setMessage("这将清除应用的临时缓存文件，可能会释放存储空间。确定要继续吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定") { _, _ ->
                        ctx.cacheDir?.deleteRecursively()
                        clearCachePreference.summary = "缓存已清理"
                    }
                    .show()
            }
            true
        }

        // “语言设置”功能
        val languagePreference: ListPreference? = findPreference("language")
        languagePreference?.setOnPreferenceChangeListener { _, newValue ->
            val locale = LocaleListCompat.forLanguageTags(newValue as String)
            AppCompatDelegate.setApplicationLocales(locale)
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("语言已更改")
                    .setMessage("为了让新的语言设置完全生效，建议立即重启应用。")
                    .setPositiveButton("立即重启") { _, _ ->
                        // 创建一个意图来启动应用的主活动
                        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                        val componentName = intent?.component
                        val mainIntent = Intent.makeRestartActivityTask(componentName)
                        ctx.startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    }
                    .setNegativeButton("稍后", null)
                    .setCancelable(false)
                    .show()
            }
            true
        }

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
                    }
                    .show()
            }
            true
        }


        // “关于我们”功能
        val aboutUsPreference: Preference? = findPreference("about_us")
        aboutUsPreference?.setOnPreferenceClickListener {
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("关于 MyApplication")
                    .setMessage("版本 1.0.0\n\n一个强大的图片编辑应用。\n\n© 2024 Gemini-Code-Assistant")
                    .setPositiveButton("确定", null)
                    .show()
            }
            true
        }

        // “隐私政策”功能
        val privacyPolicyPreference: Preference? = findPreference("privacy_policy")
        privacyPolicyPreference?.setOnPreferenceClickListener {
            val url = "https://policies.google.com/privacy" // 请替换为您的隐私政策链接
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            true
        }

        // “通知管理”功能
        val notificationsPreference: Preference? = findPreference("notifications")
        notificationsPreference?.setOnPreferenceClickListener {
            context?.let { ctx ->
                val intent = Intent()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                } else {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", ctx.packageName, null)
                }
                startActivity(intent)
            }
            true
        }

        // “历史记录深度”功能
        val historyDepthPreference: EditTextPreference? = findPreference("history_depth")
        historyDepthPreference?.summary = "当前: ${historyDepthPreference?.text} 步" // 初始化摘要
        historyDepthPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = "当前: $newValue 步" // 当值改变时更新摘要
            true
        }

        // “本地存储路径”功能
        val storagePathPreference: Preference? = findPreference("storage_path")
        storagePathPreference?.setOnPreferenceClickListener {
            val path = storagePathPreference.summary.toString()
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(path), "resource/folder")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "未找到可以打开此路径的应用", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // 为 ListPreference 设置自动摘要
        val startupActionPreference: ListPreference? = findPreference("startup_action")
        startupActionPreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        val autosaveFrequencyPreference: ListPreference? = findPreference("autosave_frequency")
        autosaveFrequencyPreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }
}