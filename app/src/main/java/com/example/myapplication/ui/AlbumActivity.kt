package com.example.myapplication.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.utils.MediaStoreHelper
import com.example.myapplication.utils.PermissionHelper
import kotlinx.coroutines.launch

class AlbumActivity : AppCompatActivity() {
    private val requestPermissionCode = 101
    private lateinit var mediaStoreHelper: MediaStoreHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var photoAdapter: PhotoAdapter
    
    private val mediaList = mutableListOf<MediaStoreHelper.MediaItem>()
    private var currentPage = 0
    private val pageSize = 50
    private var isLoading = false
    private var hasMoreData = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_album)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        mediaStoreHelper = MediaStoreHelper(this)
        initViews()
        checkPermissionAndLoadPhotos()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.rv_album)
        progressBar = findViewById(R.id.progress_bar)
        
        photoAdapter = PhotoAdapter(mediaList)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = photoAdapter
        
        // 添加滚动监听实现分页加载
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                
                // 当滚动到倒数第10个item时，加载下一页
                if (!isLoading && hasMoreData && lastVisibleItem >= totalItemCount - 10) {
                    loadMoreMedia()
                }
            }
        })
    }
    
    private fun checkPermissionAndLoadPhotos() {
        if (PermissionHelper.hasMediaPermissions(this)) {
            loadMedia()
        } else {
            ActivityCompat.requestPermissions(
                this,
                PermissionHelper.getRequiredMediaPermissions(),
                requestPermissionCode
            )
        }
    }
    
    private fun loadMedia() {
        if (isLoading) return
        
        isLoading = true
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val pageParams = MediaStoreHelper.PageParams(
                    pageSize = pageSize,
                    offset = currentPage * pageSize
                )
                
                val newItems = mediaStoreHelper.loadAllMedia(pageParams)
                
                if (newItems.isEmpty()) {
                    hasMoreData = false
                } else {
                    mediaList.addAll(newItems)
                    photoAdapter.notifyDataSetChanged()
                    currentPage++
                }
                
                if (currentPage == 1 && mediaList.isEmpty()) {
                    Toast.makeText(
                        this@AlbumActivity,
                        R.string.no_media_found,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@AlbumActivity,
                    getString(R.string.load_media_failed, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadMoreMedia() {
        loadMedia()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadMedia()
            } else {
                Toast.makeText(this, R.string.permission_denied_album, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}