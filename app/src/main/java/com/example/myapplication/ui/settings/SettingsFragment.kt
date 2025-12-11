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
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

        // 8. 清除所有数据
        findPreference<Preference>("clear_all_data")?.setOnPreferenceClickListener {
            context?.let { ctx ->
                // 第一次确认弹窗
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("清除所有数据")
                    .setMessage("此操作将清除以下所有数据：\n\n• 所有草稿\n• 所有作品\n• 个人信息\n• 设置偏好\n\n此操作不可撤销，确定要继续吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("继续") { _, _ ->
                        // 第二次确认弹窗
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle("再次确认")
                            .setMessage("您确定要清除所有数据吗？\n\n这将删除您的所有草稿、作品、个人信息和设置偏好，且无法恢复！")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("确定清除") { _, _ ->
                                clearAllData()
                            }
                            .show()
                    }
                    .show()
            }
            true
        }
    }

    /**
     * 清除所有数据
     * 包括：草稿、作品、个人信息、设置偏好
     */
    private fun clearAllData() {
        val ctx = context ?: return
        
        lifecycleScope.launch {
            try {
                val app = requireActivity().application as MyApplication
                
                withContext(Dispatchers.IO) {
                    // 1. 清除草稿数据
                    app.draftRepository.deleteAllDrafts()
                    
                    // 2. 清除作品数据
                    app.editedImageRepository.deleteAllEditedImages()
                    
                    // 3. 清除相册数据
                    app.albumRepository.deleteAllAlbums()
                    
                    // 4. 清除用户数据
                    app.userRepository.deleteAllUsers()
                    
                    // 5. 清除设置偏好
                    PreferenceManager.getDefaultSharedPreferences(ctx).edit().clear().apply()
                    
                    // 6. 清除应用内部存储的文件（草稿和作品图片）
                    clearAppInternalFiles(ctx)
                    
                    // 7. 清除缓存
                    ctx.cacheDir?.deleteRecursively()
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "所有数据已清除", Toast.LENGTH_SHORT).show()
                    
                    // 提示用户重启应用
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle("数据已清除")
                        .setMessage("所有数据已成功清除。建议重启应用以确保所有更改生效。")
                        .setPositiveButton("立即重启") { _, _ ->
                            restartApp()
                        }
                        .setNegativeButton("稍后", null)
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "清除数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 清除应用内部存储的文件
     */
    private fun clearAppInternalFiles(ctx: android.content.Context) {
        try {
            // 清除草稿目录
            val draftsDir = File(ctx.filesDir, "drafts")
            if (draftsDir.exists()) {
                draftsDir.deleteRecursively()
            }
            
            // 清除作品目录
            val worksDir = File(ctx.filesDir, "works")
            if (worksDir.exists()) {
                worksDir.deleteRecursively()
            }
            
            // 清除编辑图片目录
            val editedDir = File(ctx.filesDir, "edited_images")
            if (editedDir.exists()) {
                editedDir.deleteRecursively()
            }
            
            // 清除其他可能的图片目录
            val imagesDir = File(ctx.filesDir, "images")
            if (imagesDir.exists()) {
                imagesDir.deleteRecursively()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "清除应用文件失败", e)
        }
    }

    /**
     * 重启应用
     */
    private fun restartApp() {
        context?.let { ctx ->
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            val componentName = intent?.component
            if (componentName != null) {
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                ctx.startActivity(mainIntent)
            }
            Runtime.getRuntime().exit(0)
        }
    }
}
