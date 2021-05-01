package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.enums.MessageFolder
import io.github.wulkanowy.data.repositories.AttendanceSummaryRepository
import io.github.wulkanowy.data.repositories.GradeRepository
import io.github.wulkanowy.data.repositories.HomeworkRepository
import io.github.wulkanowy.data.repositories.LuckyNumberRepository
import io.github.wulkanowy.data.repositories.MessageRepository
import io.github.wulkanowy.data.repositories.SemesterRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.data.repositories.TimetableRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.calculatePercentage
import io.github.wulkanowy.utils.flowWithResource
import io.github.wulkanowy.utils.flowWithResourceIn
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val luckyNumberRepository: LuckyNumberRepository,
    private val gradeRepository: GradeRepository,
    private val semesterRepository: SemesterRepository,
    private val messageRepository: MessageRepository,
    private val attendanceSummaryRepository: AttendanceSummaryRepository,
    private val timetableRepository: TimetableRepository,
    private val homeworkRepository: HomeworkRepository
) : BasePresenter<DashboardView>(errorHandler, studentRepository) {

    private val dashboardDataList = mutableListOf<DashboardData>()

    override fun onAttachView(view: DashboardView) {
        super.onAttachView(view)

        view.initView()

        loadCurrentAccount()
        loadHorizontalGroup()
        loadLessons()
        loadGrades()
        loadHomework()
        loadAnnouncements()
        loadExams()
        loadConferences()
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

    private fun loadHorizontalGroup() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            val messageFlow =
                messageRepository.getMessages(student, semester, MessageFolder.RECEIVED, false)
            val luckyNumberFlow = luckyNumberRepository.getLuckyNumber(student, false)
            val attendanceSummaryFlow =
                attendanceSummaryRepository.getAttendanceSummary(student, semester, -1, false)

            combineTransform(
                messageFlow,
                luckyNumberFlow,
                attendanceSummaryFlow
            ) { messages, luckyNumberResource, attendanceSummaryList ->
                val unreadMessagesCount = messages.data?.count { it.unread }
                val attendancePercentage = attendanceSummaryList.data?.calculatePercentage()
                val luckyNumber = luckyNumberResource.data

                val groupTriple = Triple(luckyNumber, unreadMessagesCount, attendancePercentage)
                emit(Resource(Status.SUCCESS, groupTriple, null))
            }

        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard horizontal group data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard horizontal group result: Success")
                    updateData(it.data!!, DashboardViewType.HORIZONTAL_GROUP)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard horizontal group result: An exception occurred")
                }
            }
        }.launch("dashboard_horizontal_group")
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

    private fun loadLessons() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            timetableRepository.getTimetable(
                student = student,
                semester = semester,
                start = LocalDate.now(),
                end = LocalDate.now(),
                forceRefresh = false
            )

        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard lessons data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard lessons result: Success")
                    updateData(it.data!!, DashboardViewType.LESSONS)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard lessons result: An exception occurred")
                }
            }
        }.launch("dashboard_lessons")
    }

    private fun loadHomework() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            homeworkRepository.getHomework(
                student = student,
                semester = semester,
                start = LocalDate.now(),
                end = LocalDate.now(),
                forceRefresh = false
            )

        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard lessons data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard lessons result: Success")
                    updateData(it.data!!, DashboardViewType.HOMEWORK)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard lessons result: An exception occurred")
                }
            }
        }.launch("dashboard_homework")
    }

    private fun loadAnnouncements() {
        updateData(Any(), DashboardViewType.ANNOUNCEMENTS)
    }

    private fun loadExams() {
        updateData(Any(), DashboardViewType.EXAMS)
    }

    private fun loadConferences() {
        updateData(Any(), DashboardViewType.CONFERENCES)
    }

    private fun updateData(data: Any, dashboardViewType: DashboardViewType) {
        dashboardDataList.removeAll { it.viewType == dashboardViewType }
        dashboardDataList.add(DashboardData(dashboardViewType, data))

        dashboardDataList.sortBy { it.viewType.id }

        view?.updateData(dashboardDataList)
    }
}