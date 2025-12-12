package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "album_images",
    foreignKeys = [
        ForeignKey(
            entity = Album::class,        
            parentColumns = ["id"],
            childColumns = ["albumId"],   
            onDelete = ForeignKey.CASCADE 
        )
    ],
    indices = [Index("albumId")]  
)
data class AlbumImage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val albumId: Long,      
    val imageUri: String,   
    val addedAt: Long = System.currentTimeMillis()   
)
