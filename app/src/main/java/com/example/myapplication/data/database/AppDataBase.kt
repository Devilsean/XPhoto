package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.dao.AlbumDao
import com.example.myapplication.data.dao.AlbumImageDao
import com.example.myapplication.data.dao.DraftDao
import com.example.myapplication.data.dao.EditedImageDao
import com.example.myapplication.data.dao.UserDao
import com.example.myapplication.data.entity.Album
import com.example.myapplication.data.entity.AlbumImage
import com.example.myapplication.data.entity.Draft
import com.example.myapplication.data.entity.EditedImage
import com.example.myapplication.data.entity.User

@Database(
    entities = [
        EditedImage::class,
        Draft::class,
        Album::class,
        AlbumImage::class,
        User::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun editedImageDao(): EditedImageDao
    abstract fun draftDao(): DraftDao
    abstract fun albumDao(): AlbumDao
    abstract fun albumImageDao(): AlbumImageDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDataBase? = null

        fun getDatabase(context: Context): AppDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDataBase::class.java,
                    "my_application_database"
                )
                .fallbackToDestructiveMigration()  // 添加这一行
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
