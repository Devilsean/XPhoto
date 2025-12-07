package com.example.myapplication.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class MyContentFragment: Fragment() {
    private var contentType: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contentType = it.getString(ARG_CONTENT_TYPE)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val recyclerView= RecyclerView(requireContext())
        recyclerView.layoutManager= GridLayoutManager(context,3)
        recyclerView.adapter= object: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val textView = TextView(parent.context).apply{
                    layoutParams=ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        300
                    )
                    gravity=android.view.Gravity.CENTER
                }
                return object: RecyclerView.ViewHolder(textView){}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text="$contentType item ${position+1}"
            }
            override fun getItemCount(): Int = 21
        }
        return recyclerView
        }
    companion object{
        private const val ARG_CONTENT_TYPE="content_type"

        fun newInstance(contentType: String): MyContentFragment {
            return MyContentFragment().apply{
                arguments=Bundle().apply {
                    putString(ARG_CONTENT_TYPE, contentType)
                }
            }
        }
    }
}