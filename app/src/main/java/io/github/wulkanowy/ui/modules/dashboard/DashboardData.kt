package io.github.wulkanowy.ui.modules.dashboard

data class DashboardData(

    val viewType: DashboardViewType,

    val data: Any?,

    val error: Exception? = null
)
