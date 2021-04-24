package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.repositories.GradeRepository
import io.github.wulkanowy.data.repositories.LuckyNumberRepository
import io.github.wulkanowy.data.repositories.SemesterRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.flowWithResource
import io.github.wulkanowy.utils.flowWithResourceIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val luckyNumberRepository: LuckyNumberRepository,
    private val gradeRepository: GradeRepository,
    private val semesterRepository: SemesterRepository
) : BasePresenter<DashboardView>(errorHandler, studentRepository) {

    private val dashboardDataList = mutableListOf<DashboardData>()

    override fun onAttachView(view: DashboardView) {
        super.onAttachView(view)

        view.initView()

        loadCurrentAccount()
        loadLuckyNumber()
        loadGrades()
    }

    private fun loadCurrentAccount() {
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

    private fun loadLuckyNumber() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            luckyNumberRepository.getLuckyNumber(student, false)
        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard lucky number data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard lucky number result: Success")
                    updateData(it.data!!, DashboardViewType.HORIZONTAL_GROUP)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard lucky number result: An exception occurred")
                }
            }
        }.launch("dashboard_lucky_number")
    }

    private fun loadGrades() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            gradeRepository.getGrades(student, semester, false)
        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard grades data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard grades result: Success")
                    updateData(it.data!!.first, DashboardViewType.GRADES)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard grades result: An exception occurred")
                }
            }
        }.launch("dashboard_grades")
    }

    private fun updateData(data: Any, dashboardViewType: DashboardViewType) {
        dashboardDataList.removeAll { it.viewType == dashboardViewType }
        dashboardDataList.add(DashboardData(dashboardViewType, data))

        view?.updateData(dashboardDataList)
    }
}