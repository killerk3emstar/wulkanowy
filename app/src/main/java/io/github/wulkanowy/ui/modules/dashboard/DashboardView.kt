package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.ui.base.BaseView

interface DashboardView : BaseView {

    fun initView()

    fun updateData(data: List<DashboardTile>)

    fun showProgress(show: Boolean)

    fun showContent(show: Boolean)

    fun showErrorView(show: Boolean)

    fun setErrorDetails(message: String)
}