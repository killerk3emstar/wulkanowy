package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.enums.MessageFolder
import io.github.wulkanowy.data.repositories.AttendanceSummaryRepository
import io.github.wulkanowy.data.repositories.ConferenceRepository
import io.github.wulkanowy.data.repositories.ExamRepository
import io.github.wulkanowy.data.repositories.GradeRepository
import io.github.wulkanowy.data.repositories.HomeworkRepository
import io.github.wulkanowy.data.repositories.LuckyNumberRepository
import io.github.wulkanowy.data.repositories.MessageRepository
import io.github.wulkanowy.data.repositories.PreferencesRepository
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
    private val homeworkRepository: HomeworkRepository,
    private val examRepository: ExamRepository,
    private val conferenceRepository: ConferenceRepository,
    private val preferencesRepository: PreferencesRepository
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

                    val filteredSubjectWithGrades = it.data!!.first
                        .filter { grade ->
                            grade.date.isAfter(LocalDate.now().minusDays(7))
                        }
                        .groupBy { grade -> grade.subject }
                        .mapValues { entry ->
                            entry.value
                                .take(5)
                                .sortedBy { grade -> grade.date }
                        }
                        .toList()
                        .sortedBy { subjectWithGrades -> subjectWithGrades.second[0].date }
                        .toMap()

                    updateGradeTheme()
                    updateData(filteredSubjectWithGrades, DashboardViewType.GRADES)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard grades result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(it.error, DashboardViewType.GRADES)
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
                end = LocalDate.now().plusDays(7),
                forceRefresh = false
            )

        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard homework data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard homework result: Success")
                    updateData(it.data!!, DashboardViewType.HOMEWORK)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard homework result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(it.error, DashboardViewType.HOMEWORK)
                }
            }
        }.launch("dashboard_homework")
    }

    private fun loadAnnouncements() {
        updateData(Any(), DashboardViewType.ANNOUNCEMENTS)
    }

    private fun loadExams() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            examRepository.getExams(student, semester, LocalDate.now(), LocalDate.now(), false)
        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard exams data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard exams result: Success")
                    updateData(it.data!!, DashboardViewType.EXAMS)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard exams result: An exception occurred")
                }
            }
        }.launch("dashboard_exams")
    }

    private fun loadConferences() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            conferenceRepository.getConferences(student, semester, false)
        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading dashboard conferences data started")
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard conferences result: Success")
                    updateData(it.data!!, DashboardViewType.CONFERENCES)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard conferences result: An exception occurred")
                }
            }
        }.launch("dashboard_conferences")
    }

    private fun updateData(data: Any, dashboardViewType: DashboardViewType) {
        dashboardDataList.removeAll { it.viewType == dashboardViewType }
        dashboardDataList.add(DashboardData(viewType = dashboardViewType, data = data))

        dashboardDataList.sortBy { it.viewType.id }

        view?.updateData(dashboardDataList)
    }

    private fun showErrorInTile(exception: Throwable?, dashboardViewType: DashboardViewType) {
        dashboardDataList.removeAll { it.viewType == dashboardViewType }
        dashboardDataList.add(
            DashboardData(
                viewType = dashboardViewType,
                data = null,
                error = exception
            )
        )

        dashboardDataList.sortBy { it.viewType.id }

        view?.updateData(dashboardDataList)
    }

    private fun updateGradeTheme() {
        view?.updateGradeTheme(preferencesRepository.gradeColorTheme)
    }
}