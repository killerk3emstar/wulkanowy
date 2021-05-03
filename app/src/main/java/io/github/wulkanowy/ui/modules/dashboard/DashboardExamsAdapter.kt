package io.github.wulkanowy.ui.modules.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.data.db.entities.Exam
import io.github.wulkanowy.databinding.SubitemDashboardExamsBinding
import io.github.wulkanowy.utils.toFormattedString

class DashboardExamsAdapter :
    RecyclerView.Adapter<DashboardExamsAdapter.ViewHolder>() {

    var items = emptyList<Exam>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        SubitemDashboardExamsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        with(holder.binding) {
            dashboardHomeworkSubitemTime.text = item.date.toFormattedString("dd.MM")
            dashboardHomeworkSubitemTitle.text = "${item.type} - ${item.subject}"
        }
    }

    class ViewHolder(val binding: SubitemDashboardExamsBinding) :
        RecyclerView.ViewHolder(binding.root)
}