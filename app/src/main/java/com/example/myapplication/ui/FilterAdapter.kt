package com.example.myapplication.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.card.MaterialCardView

class FilterAdapter(
    private val filters: List<FilterType>,
    private val thumbnailBitmap: Bitmap?,
    private val onFilterSelected: (FilterType) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var selectedPosition = 0

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val previewCard: MaterialCardView = itemView.findViewById(R.id.filter_preview_card)
        val previewImage: ImageView = itemView.findViewById(R.id.filter_preview_image)
        val filterName: TextView = itemView.findViewById(R.id.filter_name)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                    onFilterSelected(filters[position])
                }
            }
        }

        fun bind(filter: FilterType, isSelected: Boolean) {
            filterName.text = getFilterDisplayName(itemView.context, filter)
            
            // 设置选中状态
            if (isSelected) {
                previewCard.strokeWidth = 4
                previewCard.strokeColor = itemView.context.getColor(R.color.color_primary)
            } else {
                previewCard.strokeWidth = 0
            }

            // 设置预览图（如果有缩略图）
            thumbnailBitmap?.let { bitmap ->
                previewImage.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position], position == selectedPosition)
    }

    override fun getItemCount() = filters.size

    fun setSelectedFilter(filter: FilterType) {
        val position = filters.indexOf(filter)
        if (position != -1 && position != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }
    
    companion object {
        /**
         * 获取滤镜的本地化显示名称
         */
        fun getFilterDisplayName(context: Context, filter: FilterType): String {
            return when (filter) {
                FilterType.NONE -> context.getString(R.string.filter_none)
                FilterType.GRAYSCALE -> context.getString(R.string.filter_grayscale)
                FilterType.SEPIA -> context.getString(R.string.filter_sepia)
                FilterType.COOL -> context.getString(R.string.filter_cool)
                FilterType.WARM -> context.getString(R.string.filter_warm)
                FilterType.VIVID -> context.getString(R.string.filter_vivid)
                FilterType.FADE -> context.getString(R.string.filter_fade)
                FilterType.INVERT -> context.getString(R.string.filter_invert)
                FilterType.BRIGHTNESS -> context.getString(R.string.filter_brightness)
                FilterType.CONTRAST -> context.getString(R.string.filter_contrast)
            }
        }
    }
}