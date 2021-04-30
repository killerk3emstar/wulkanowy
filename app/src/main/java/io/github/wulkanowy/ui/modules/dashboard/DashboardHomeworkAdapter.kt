package io.github.wulkanowy.ui.modules.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.databinding.SubitemDashboardHomeworkBinding

class DashboardHomeworkAdapter : RecyclerView.Adapter<DashboardHomeworkAdapter.ViewHolder>() {

    val items = emptyList<Any>()

    override fun getItemCount() = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        SubitemDashboardHomeworkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    class ViewHolder(val binding: SubitemDashboardHomeworkBinding) :
        RecyclerView.ViewHolder(binding.root)
}