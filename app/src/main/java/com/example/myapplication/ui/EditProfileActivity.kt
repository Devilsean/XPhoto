package com.example.myapplication.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.data.entity.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {
    
    private lateinit var avatarImageView: ImageView
    private lateinit var nicknameEditText: EditText
    private lateinit var signatureEditText: EditText
    private lateinit var saveButton: Button
    
    private var selectedAvatarUri: String? = null
    private var currentUser: User? = null
    
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it.toString()
            Glide.with(this).load(it).into(avatarImageView)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        
        avatarImageView = findViewById(R.id.iv_avatar)
        nicknameEditText = findViewById(R.id.et_nickname)
        signatureEditText = findViewById(R.id.et_signature)
        saveButton = findViewById(R.id.btn_save)
        
        val app = application as MyApplication
        val userRepository = app.userRepository
        
        // 加载当前用户信息
        lifecycleScope.launch {
            userRepository.user.collect { user ->
                user?.let {
                    currentUser = it
                    nicknameEditText.setText(it.nickname)
                    signatureEditText.setText(it.signature)
                    it.avatarUri?.let { uri ->
                        selectedAvatarUri = uri
                        Glide.with(this@EditProfileActivity)
                            .load(Uri.parse(uri))
                            .into(avatarImageView)
                    }
                }
            }
        }
        
        // 点击头像选择图片
        avatarImageView.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }
        
        // 保存按钮
        saveButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()
            val signature = signatureEditText.text.toString().trim()
            
            if (nickname.isEmpty()) {
                Toast.makeText(this, R.string.nickname_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            lifecycleScope.launch {
                val updatedUser = User(
                    id = 1,
                    nickname = nickname,
                    signature = signature,
                    avatarUri = selectedAvatarUri,
                    updatedAt = System.currentTimeMillis()
                )
                userRepository.updateUser(updatedUser)
                Toast.makeText(this@EditProfileActivity, R.string.save_success, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
