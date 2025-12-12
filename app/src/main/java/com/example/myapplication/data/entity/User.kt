package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: Int = 1,  // 只有一个用户，固定ID为1
    val nickname: String = "用户昵称",
    val signature: String = "这个人很懒，什么都没留下",
    val avatarUri: String? = null,  // 头像URI
    val updatedAt: Long = System.currentTimeMillis()
)
