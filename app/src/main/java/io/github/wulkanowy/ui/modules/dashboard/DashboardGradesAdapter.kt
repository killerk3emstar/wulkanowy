package io.github.wulkanowy.ui.modules.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.databinding.SubitemDashboardGradesBinding

class DashboardGradesAdapter : RecyclerView.Adapter<DashboardGradesAdapter.ViewHolder>() {

    val items = emptyList<Any>()

    override fun getItemCount() = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        SubitemDashboardGradesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    class ViewHolder(val binding: SubitemDashboardGradesBinding) :
        RecyclerView.ViewHolder(binding.root)
}