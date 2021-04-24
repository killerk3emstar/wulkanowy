package io.github.wulkanowy.ui.modules.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.data.db.entities.LuckyNumber
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.databinding.ItemDashboardAccountBinding
import io.github.wulkanowy.databinding.ItemDashboardGradesBinding
import io.github.wulkanowy.databinding.ItemDashboardHorizontalGroupBinding
import io.github.wulkanowy.utils.createNameInitialsDrawable
import io.github.wulkanowy.utils.nickOrName
import javax.inject.Inject

class DashboardAdapter @Inject constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = emptyList<DashboardData>()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = items[position].viewType.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            DashboardViewType.ACCOUNT.id -> AccountViewHolder(
                ItemDashboardAccountBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.HORIZONTAL_GROUP.id -> HorizontalGroupViewHolder(
                ItemDashboardHorizontalGroupBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.GRADES.id -> GradesViewHolder(
                ItemDashboardGradesBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AccountViewHolder -> bindAccountViewHolder(holder, position)
            is HorizontalGroupViewHolder -> bindHorizontalGroupViewHolder(holder, position)
            is GradesViewHolder -> bindGradesViewHolder(holder, position)
        }
    }

    private fun bindAccountViewHolder(accountViewHolder: AccountViewHolder, position: Int) {
        val item = items[position].data as Student
        val avatar = accountViewHolder.binding.root.context.createNameInitialsDrawable(
            text = item.nickOrName,
            backgroundColor = item.avatarColor
        )

        with(accountViewHolder.binding) {
            dashboardAccountItemAvatar.setImageDrawable(avatar)
            dashboardAccountItemName.text = item.nickOrName
            dashboardAccountItemSchoolName.text = item.schoolName
        }
    }

    private fun bindHorizontalGroupViewHolder(
        luckyNumberViewHolder: HorizontalGroupViewHolder,
        position: Int
    ) {
        val item = items[position].data as LuckyNumber
    }

    private fun bindGradesViewHolder(gradesViewHolder: GradesViewHolder, position: Int) {
        val item = items[position]
    }

    class AccountViewHolder(val binding: ItemDashboardAccountBinding) :
        RecyclerView.ViewHolder(binding.root)

    class HorizontalGroupViewHolder(val binding: ItemDashboardHorizontalGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    class GradesViewHolder(val binding: ItemDashboardGradesBinding) :
        RecyclerView.ViewHolder(binding.root)
}
