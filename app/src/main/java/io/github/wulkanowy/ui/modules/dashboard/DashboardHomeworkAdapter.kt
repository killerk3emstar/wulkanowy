package io.github.wulkanowy.ui.modules.dashboard

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.data.db.entities.Homework
import io.github.wulkanowy.databinding.SubitemDashboardHomeworkBinding
import io.github.wulkanowy.utils.toFormattedString

class DashboardHomeworkAdapter : RecyclerView.Adapter<DashboardHomeworkAdapter.ViewHolder>() {

    var items = emptyList<Homework>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        SubitemDashboardHomeworkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        with(holder.binding) {
            dashboardHomeworkSubitemTitle.text = "${item.subject} - ${item.content}"
            dashboardHomeworkSubitemTime.text = "do ${item.date.toFormattedString("dd.MM")}"
        }
    }

    class ViewHolder(val binding: SubitemDashboardHomeworkBinding) :
        RecyclerView.ViewHolder(binding.root)
}