package com.example.myapplication.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.slider.Slider


class AdjustmentAdapter(
    private val adjustmentTypes: List<AdjustmentType>,
    private val adjustmentParams: AdjustmentParams,
    private val onAdjustmentChanged: (AdjustmentType, Float) -> Unit
) : RecyclerView.Adapter<AdjustmentAdapter.AdjustmentViewHolder>() {

    inner class AdjustmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.adjustment_name)
        val valueTextView: TextView = itemView.findViewById(R.id.adjustment_value)
        val slider: Slider = itemView.findViewById(R.id.adjustment_slider)
        
        fun bind(adjustmentType: AdjustmentType) {
            nameTextView.text = getAdjustmentDisplayName(itemView.context, adjustmentType)
            
            // 设置滑杆范围
            slider.valueFrom = adjustmentType.minValue * 100
            slider.valueTo = adjustmentType.maxValue * 100
            
            // 获取当前值并设置
            val currentValue = adjustmentType.getValue(adjustmentParams)
            slider.value = currentValue * 100
            valueTextView.text = String.format("%.0f", currentValue * 100)
            
            // 监听滑杆变化
            slider.clearOnChangeListeners()
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val normalizedValue = value / 100f
                    valueTextView.text = String.format("%.0f", value)
                    onAdjustmentChanged(adjustmentType, normalizedValue)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdjustmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_adjustment, parent, false)
        return AdjustmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdjustmentViewHolder, position: Int) {
        holder.bind(adjustmentTypes[position])
    }

    override fun getItemCount(): Int = adjustmentTypes.size
    
    /**
     * 更新所有调整项的显示值
     */
    fun updateValues() {
        notifyDataSetChanged()
    }
    
    /**
     * 更新特定调整项的显示值
     */
    fun updateValue(adjustmentType: AdjustmentType) {
        val position = adjustmentTypes.indexOf(adjustmentType)
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }
    
    companion object {
        /**
         * 获取调整参数的本地化显示名称
         */
        fun getAdjustmentDisplayName(context: Context, adjustmentType: AdjustmentType): String {
            return when (adjustmentType) {
                AdjustmentType.BRIGHTNESS -> context.getString(R.string.adjust_brightness)
                AdjustmentType.CONTRAST -> context.getString(R.string.adjust_contrast)
                AdjustmentType.SATURATION -> context.getString(R.string.adjust_saturation)
                AdjustmentType.HIGHLIGHTS -> context.getString(R.string.adjust_highlights)
                AdjustmentType.SHADOWS -> context.getString(R.string.adjust_shadows)
                AdjustmentType.TEMPERATURE -> context.getString(R.string.adjust_temperature)
                AdjustmentType.TINT -> context.getString(R.string.adjust_tint)
                AdjustmentType.CLARITY -> context.getString(R.string.adjust_clarity)
                AdjustmentType.SHARPEN -> context.getString(R.string.adjust_sharpen)
            }
        }
    }
}