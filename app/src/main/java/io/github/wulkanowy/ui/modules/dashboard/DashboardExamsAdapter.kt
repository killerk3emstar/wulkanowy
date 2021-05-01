package io.github.wulkanowy.ui.modules.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.databinding.SubitemDashboardExamsBinding

class DashboardExamsAdapter :
    RecyclerView.Adapter<DashboardExamsAdapter.ViewHolder>() {

    val items = emptyList<Any>()

    override fun getItemCount() = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        SubitemDashboardExamsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    class ViewHolder(val binding: SubitemDashboardExamsBinding) :
        RecyclerView.ViewHolder(binding.root)
}