package com.example.myapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.button.MaterialButton

/**
 * 常用功能数据类
 */
data class QuickAccessItem(
    val title: String,
    val iconResId: Int
)

/**
 * 常用功能区适配器
 */
class QuickAccessAdapter(
    private val items: List<QuickAccessItem>,
    private val onItemClick: (QuickAccessItem) -> Unit
) : RecyclerView.Adapter<QuickAccessAdapter.ViewHolder>() {

    class ViewHolder(val button: MaterialButton) : RecyclerView.ViewHolder(button)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val button = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_access, parent, false) as MaterialButton
        return ViewHolder(button)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.button.apply {
            text = item.title
            setIconResource(item.iconResId)
            setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}