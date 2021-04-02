package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
) : BasePresenter<DashboardView>(errorHandler, studentRepository) {

    override fun onAttachView(view: DashboardView) {
        super.onAttachView(view)

        view.initView()
    }
}