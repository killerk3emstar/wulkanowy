package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.ui.base.BaseView

interface DashboardView : BaseView {

    fun initView()

    fun updateData(data: List<DashboardData>)

    fun updateGradeTheme(theme: String)
}