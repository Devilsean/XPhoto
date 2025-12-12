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
                        .setTitle(R.string.language_changed)
                        .setMessage(R.string.language_changed_message)
                        .setPositiveButton(R.string.restart_now) { _, _ ->
                            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                            val componentName = intent?.component
                            if (componentName != null) {
                                val mainIntent = Intent.makeRestartActivityTask(componentName)
                                ctx.startActivity(mainIntent)
                            }
                            Runtime.getRuntime().exit(0)
                        }
                        .setNegativeButton(R.string.later, null)
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
                    Toast.makeText(ctx, R.string.cannot_open_notification_settings, Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        // 4. 清理缓存
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener { pref ->
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.clear_cache_title)
                    .setMessage(R.string.clear_cache_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        val deleted = ctx.cacheDir?.deleteRecursively() ?: false
                        pref.summary = if (deleted) {
                            getString(R.string.cache_cleared)
                        } else {
                            getString(R.string.no_cache_to_clear)
                        }
                    }
                    .show()
            }
            true
        }

        // 5. 关于应用
        findPreference<Preference>("about_us")?.setOnPreferenceClickListener {
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.about_app_title)
                    .setMessage(R.string.about_app_message)
                    .setPositiveButton(R.string.confirm, null)
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
                Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_SHORT).show()
            }
            true
        }

        // 8. 清除所有数据
        findPreference<Preference>("clear_all_data")?.setOnPreferenceClickListener {
            context?.let { ctx ->
                // 第一次确认弹窗
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.clear_all_data_title)
                    .setMessage(R.string.clear_all_data_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.continue_text) { _, _ ->
                        // 第二次确认弹窗
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.confirm_again)
                            .setMessage(R.string.confirm_clear_all_message)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.confirm_clear) { _, _ ->
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
                    Toast.makeText(ctx, R.string.all_data_cleared, Toast.LENGTH_SHORT).show()
                    
                    // 提示用户重启应用
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.data_cleared_title)
                        .setMessage(R.string.data_cleared_message)
                        .setPositiveButton(R.string.restart_now) { _, _ ->
                            restartApp()
                        }
                        .setNegativeButton(R.string.later, null)
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, getString(R.string.clear_data_failed, e.message), Toast.LENGTH_SHORT).show()
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
