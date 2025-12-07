package com.example.myapplication.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.R
import com.example.myapplication.ui.AlbumActivity
import com.example.myapplication.ui.EditorActivity
import java.io.File

class HomeFragment : Fragment() {

    private var latestTmpUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                // 图片拍摄成功，跳转到编辑页面
                val intent = Intent(requireActivity(), EditorActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        val authority = "${requireActivity().packageName}.provider"
        return FileProvider.getUriForFile(requireActivity().applicationContext, authority, tmpFile)
    }

    // 为方便演示，先在内部创建简单的 Adapter
    class SimpleTextAdapter(private val items: List<String>) : RecyclerView.Adapter<SimpleTextAdapter.ViewHolder>() {
        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.view as android.widget.TextView).text = items[position]
        }
        override fun getItemCount() = items.size
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // --- 1. 设置轮播图 ---
        val viewPager: ViewPager2 = view.findViewById(R.id.carousel_view_pager)
        // TODO: 此处需要一个为 ViewPager2 准备的 Adapter

        // --- 2. 设置核心操作点击事件 ---
        val launchCameraButton: CardView = view.findViewById(R.id.btn_launch_camera)
        val importGalleryButton: CardView = view.findViewById(R.id.btn_import_gallery)

        launchCameraButton.setOnClickListener {
            // TODO: 在实际应用中，调用前应检查相机权限
            try {
                // 这是正确的代码
                getTmpFileUri().let { uri ->
                    latestTmpUri = uri
                    takePictureLauncher.launch(uri)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开相机或创建临时文件", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        importGalleryButton.setOnClickListener {
            val intent = Intent(activity, AlbumActivity::class.java)
            startActivity(intent)
        }

        // --- 3. 设置常用功能网格 ---
        val quickAccessRecyclerView: RecyclerView = view.findViewById(R.id.quick_access_recycler_view)
        quickAccessRecyclerView.layoutManager = GridLayoutManager(context, 4)
        val quickAccessItems = listOf("滤镜", "裁剪", "拼图", "美颜", "文字", "贴纸", "画笔", "全部")
        quickAccessRecyclerView.adapter = SimpleTextAdapter(quickAccessItems)


        // --- 4. 设置作品/草稿列表 ---
        val draftsRecyclerView: RecyclerView = view.findViewById(R.id.drafts_recycler_view)
        draftsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val draftItems = listOf("草稿1", "草稿2", "草稿3", "草稿4", "草稿5")
        draftsRecyclerView.adapter = SimpleTextAdapter(draftItems)


        return view
    }
}
