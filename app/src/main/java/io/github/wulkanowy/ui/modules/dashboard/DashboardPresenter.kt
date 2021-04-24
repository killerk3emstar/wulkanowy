package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.flowWithResource
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
) : BasePresenter<DashboardView>(errorHandler, studentRepository) {

    private val dashboardDataList = mutableListOf<DashboardData>()

    override fun onAttachView(view: DashboardView) {
        super.onAttachView(view)

        view.initView()

        loadCurrentAccount()
    }

    fun loadCurrentAccount() {
        flowWithResource { studentRepository.getCurrentStudent(false) }
            .onEach {
                when (it.status) {
                    Status.LOADING -> Timber.i("Loading dashboard account data started")
                    Status.SUCCESS -> {
                        Timber.i("Loading dashboard account result: Success")
                        updateData(it.data!!, DashboardViewType.ACCOUNT)
                    }
                    Status.ERROR -> {
                        Timber.i("Loading dashboard account result: An exception occurred")
                    }
                }
            }
            .launch("dashboard_account")
    }

    private fun updateData(data: Any, dashboardViewType: DashboardViewType) {
        dashboardDataList.removeAll { it.viewType == dashboardViewType }
        dashboardDataList.add(DashboardData(dashboardViewType, data))

        view?.updateData(dashboardDataList)
    }
}