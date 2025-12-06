package com.example.myapplication.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.ui.AlbumActivity

class HomeFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?{
        val view=inflater.inflate(R.layout.fragment_home,container,false)
        val openAlbumButton: Button =view.findViewById(R.id.btn_open_album)
        openAlbumButton.setOnClickListener{
            val intent = Intent(activity, AlbumActivity::class.java)
            startActivity(intent)
        }
        return view
    }
}