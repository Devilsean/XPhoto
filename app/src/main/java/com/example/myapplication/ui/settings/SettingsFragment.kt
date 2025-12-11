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

        // 1. 主题设置
        findPreference<ListPreference>("theme")?.apply {
            // 显示当前选择项作为 summary
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newValue ->
                when (newValue as String) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                true
            }
        }

        // 2. 语言设置
        findPreference<ListPreference>("language")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newValue ->
                val locale = LocaleListCompat.forLanguageTags(newValue as String)
                AppCompatDelegate.setApplicationLocales(locale)

                context?.let { ctx ->
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle("语言已更改")
                        .setMessage("为了让新的语言设置完全生效，建议立即重启应用。")
                        .setPositiveButton("立即重启") { _, _ ->
                            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                            val componentName = intent?.component
                            if (componentName != null) {
                                val mainIntent = Intent.makeRestartActivityTask(componentName)
                                ctx.startActivity(mainIntent)
                            }
                            Runtime.getRuntime().exit(0)
                        }
                        .setNegativeButton("稍后", null)
                        .setCancelable(false)
                        .show()
                }
                true
            }
        }

        // 3. 通知管理：跳系统设置
        findPreference<Preference>("notifications")?.setOnPreferenceClickListener {
            context?.let { ctx ->
                try {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", ctx.packageName, null)
                        }
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(ctx, "无法打开通知设置", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        // 4. 清理缓存
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener { pref ->
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("清理缓存")
                    .setMessage("这将清除应用的临时缓存文件，可能会释放存储空间。确定要继续吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定") { _, _ ->
                        val deleted = ctx.cacheDir?.deleteRecursively() ?: false
                        pref.summary = if (deleted) {
                            "缓存已清理"
                        } else {
                            "当前无可清理缓存"
                        }
                    }
                    .show()
            }
            true
        }

        // 5. 历史记录深度
        findPreference<EditTextPreference>("history_depth")?.apply {
            // 初始化 summary（第一次打开时）
            if (text.isNullOrBlank()) {
                text = "50"
            }
            summary = "当前：${text} 步"

            setOnPreferenceChangeListener { preference, newValue ->
                val value = (newValue as? String)?.toIntOrNull()
                if (value == null || value <= 0) {
                    Toast.makeText(context, "请输入大于 0 的数字", Toast.LENGTH_SHORT).show()
                    return@setOnPreferenceChangeListener false
                }
                preference.summary = "当前：$value 步"
                true
            }
        }

        // 6. 关于应用
        findPreference<Preference>("about_us")?.setOnPreferenceClickListener {
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("关于本应用")
                    .setMessage("版本 1.0.0\n\n醒图安卓训练营 · 图片编辑 Demo 应用。\n\n主要功能：相册浏览、OpenGL 编辑画布、基础裁剪与导出。\n\n© 2025 Sean（部分功能在 AI 辅助下完成，并由本人理解与整合）")
                    .setPositiveButton("确定", null)
                    .show()
            }
            true
        }

        // 7. 隐私政策
        findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
            val url = "https://policies.google.com/privacy" // 使用google隐私政策占位
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "未找到可以打开此链接的浏览器应用", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}
