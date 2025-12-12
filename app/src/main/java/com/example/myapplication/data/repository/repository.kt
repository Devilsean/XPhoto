package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.UserDao
import com.example.myapplication.data.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val user: Flow<User?> = userDao.getUser()
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
    
    suspend fun insertDefaultUser() {
        userDao.insertUser(User())
    }
    
    suspend fun deleteAllUsers() {
        userDao.deleteAll()
    }
}
