package com.example.myapplication.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class AlbumActivity : AppCompatActivity() {
    private val requestPermissionCode=101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_album)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkPermissionAndLoadPhotos()
    }
    private fun checkPermissionAndLoadPhotos(){
        if(ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_IMAGES
        )== PackageManager.PERMISSION_GRANTED
            ){
            loadPhotos()
        }else{
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                requestPermissionCode
            )
        }
    }
    private fun loadPhotos(){
        Toast.makeText(this,"权限已获取，准备加载照片！",Toast.LENGTH_SHORT).show()
        val photoList=mutableListOf<PhotoItem>()
        val projection=arrayOf(MediaStore.Images.Media._ID)
        val sortOrder="${MediaStore.Images.Media.DATE_TAKEN} DESC"
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use{cursor->
            val idColumn=cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while(cursor.moveToNext()){
                val id =cursor.getLong(idColumn)
                val contentUri= ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                photoList.add(PhotoItem(contentUri))
            }
        }
        val recyclerView: RecyclerView =findViewById(R.id.rv_album)
        recyclerView.layoutManager= GridLayoutManager(this, 3)
        recyclerView.adapter=PhotoAdapter(photoList)
    }
    override fun onRequestPermissionsResult(
        requestCode:Int,
        permissions:Array<out String>,
        grantResults:IntArray
    ){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==requestPermissionCode){
            if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                loadPhotos()
            }else{
                Toast.makeText(this,"权限被拒绝,无法访问相册", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    }
    data class PhotoItem(val uri: Uri)

}