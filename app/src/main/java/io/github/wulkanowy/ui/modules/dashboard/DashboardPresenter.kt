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
import io.github.wulkanowy.data.repositories.SchoolAnnouncementRepository
import io.github.wulkanowy.data.repositories.SemesterRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.data.repositories.TimetableRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.calculatePercentage
import io.github.wulkanowy.utils.flowWithResource
import io.github.wulkanowy.utils.flowWithResourceIn
import io.github.wulkanowy.utils.nextOrSameSchoolDay
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val preferencesRepository: PreferencesRepository,
    private val schoolAnnouncementRepository: SchoolAnnouncementRepository
) : BasePresenter<DashboardView>(errorHandler, studentRepository) {

    private val dashboardTileList = mutableListOf<DashboardTile>()

    private lateinit var lastError: Throwable

    private val dashboardTilesToLoad = setOf(
        1 to DashboardTile.Type.ACCOUNT,
        2 to DashboardTile.Type.HORIZONTAL_GROUP,
        3 to DashboardTile.Type.LESSONS,
        4 to DashboardTile.Type.GRADES,
        5 to DashboardTile.Type.HOMEWORK,
        6 to DashboardTile.Type.ANNOUNCEMENTS,
        7 to DashboardTile.Type.EXAMS,
        8 to DashboardTile.Type.CONFERENCES,
        //9 to DashboardViewType.ADS,
    )

    override fun onAttachView(view: DashboardView) {
        super.onAttachView(view)

        with(view) {
            initView()
            showProgress(true)
            showContent(false)
        }

        dashboardTilesToLoad.forEach { (_, type) ->
            when (type) {
                DashboardTile.Type.ACCOUNT -> loadCurrentAccount()
                DashboardTile.Type.HORIZONTAL_GROUP -> loadHorizontalGroup()
                DashboardTile.Type.LESSONS -> loadLessons()
                DashboardTile.Type.GRADES -> loadGrades()
                DashboardTile.Type.HOMEWORK -> loadHomework()
                DashboardTile.Type.ANNOUNCEMENTS -> loadSchoolAnnouncements()
                DashboardTile.Type.EXAMS -> loadExams()
                DashboardTile.Type.CONFERENCES -> loadConferences()
                DashboardTile.Type.ADS -> TODO()
            }
        }
    }

    fun onRetry() {
        view?.run {
            showErrorView(false)
            showProgress(true)
        }
    }

    fun onDetailsClick() {
        view?.showErrorDetailsDialog(lastError)
    }

    private fun loadCurrentAccount() {
        flowWithResource { studentRepository.getCurrentStudent(false) }
            .onEach {
                when (it.status) {
                    Status.LOADING -> {
                        updateData(DashboardTile.Account(it.data, isLoading = true))
                        Timber.i("Loading dashboard account data started")
                    }
                    Status.SUCCESS -> {
                        Timber.i("Loading dashboard account result: Success")
                        updateData(DashboardTile.Account(it.data))
                    }
                    Status.ERROR -> {
                        Timber.i("Loading dashboard account result: An exception occurred")
                        errorHandler.dispatch(it.error!!)
                        showErrorInTile(DashboardTile.Account(error = it.error))
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

                when {
                    messages.status == Status.ERROR || attendanceSummaryList.status == Status.ERROR -> {
                        val message = messages.error?.stackTraceToString() +
                            luckyNumberResource.error?.stackTraceToString() +
                            attendanceSummaryList.error?.stackTraceToString()

                        emit(Resource(Status.ERROR, groupTriple, Exception(message)))
                    }
                    messages.status == Status.LOADING || attendanceSummaryList.status == Status.LOADING || luckyNumberResource.status == Status.LOADING -> {
                        emit(Resource(Status.LOADING, groupTriple, null))
                    }
                    else -> emit(Resource(Status.SUCCESS, groupTriple, null))
                }
            }

        }
            .distinctUntilChangedBy { it.status }
            .onEach {
                when (it.status) {
                    Status.LOADING -> {
                        Timber.i("Loading dashboard horizontal group data started")

                        val luckyNumber = it.data?.first
                        val unreadMessagesCount = it.data?.second
                        val attendancePercentage = it.data?.third

                        updateData(
                            DashboardTile.HorizontalGroup(
                                unreadMessagesCount,
                                attendancePercentage,
                                luckyNumber,
                                isLoading = true
                            )
                        )
                    }
                    Status.SUCCESS -> {
                        Timber.i("Loading dashboard horizontal group result: Success")
                        val (luckyNumber, unreadMessagesCount, attendancePercentage) = it.data!!

                        updateData(
                            DashboardTile.HorizontalGroup(
                                unreadMessagesCount,
                                attendancePercentage,
                                luckyNumber
                            )
                        )
                    }
                    Status.ERROR -> {
                        Timber.i("Loading dashboard horizontal group result: An exception occurred")
                        errorHandler.dispatch(it.error!!)
                        showErrorInTile(DashboardTile.HorizontalGroup(error = it.error))
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
                Status.LOADING -> {
                    Timber.i("Loading dashboard grades data started")

                    val filteredSubjectWithGrades = it.data?.first.orEmpty()
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
                    updateData(DashboardTile.Grades(filteredSubjectWithGrades, isLoading = true))
                }
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
                    updateData(DashboardTile.Grades(filteredSubjectWithGrades))
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard grades result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(DashboardTile.Grades(error = it.error))
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
                end = LocalDate.now().plusDays(1),
                forceRefresh = false
            )

        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard lessons data started")
                    updateData(DashboardTile.Lessons(it.data, isLoading = true))
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard lessons result: Success")
                    updateData(DashboardTile.Lessons(it.data))
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard lessons result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(DashboardTile.Lessons(error = it.error))
                }
            }
        }.launch("dashboard_lessons")
    }

    private fun loadHomework() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)
            val date = LocalDate.now().nextOrSameSchoolDay

            homeworkRepository.getHomework(
                student = student,
                semester = semester,
                start = date,
                end = date,
                forceRefresh = false
            )
        }.map { homeworkResource ->
            val currentDate = LocalDate.now()

            val filteredHomework = homeworkResource.data?.filter {
                (it.date.isAfter(currentDate) || it.date == currentDate) && !it.isDone
            }

            homeworkResource.copy(data = filteredHomework)
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard homework data started")
                    updateData(DashboardTile.Homework(it.data ?: emptyList(), isLoading = true))
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard homework result: Success")
                    updateData(DashboardTile.Homework(it.data ?: emptyList()))
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard homework result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(DashboardTile.Homework(error = it.error))
                }
            }
        }.launch("dashboard_homework")
    }

    private fun loadSchoolAnnouncements() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)

            schoolAnnouncementRepository.getSchoolAnnouncements(student, false)
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard announcements data started")
                    updateData(
                        DashboardTile.Announcements(
                            it.data ?: emptyList(),
                            isLoading = true
                        )
                    )
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard announcements result: Success")
                    updateData(DashboardTile.Announcements(it.data ?: emptyList()))
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard announcements result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(DashboardTile.Announcements(error = it.error))
                }
            }
        }.launch("dashboard_announcements")
    }

    private fun loadExams() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            examRepository.getExams(
                student = student,
                semester = semester,
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(7),
                forceRefresh = false
            )
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard exams data started")
                    updateData(DashboardTile.Exams(it.data ?: emptyList(), isLoading = true))
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard exams result: Success")
                    updateData(DashboardTile.Exams(it.data ?: emptyList()))
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard exams result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(DashboardTile.Exams(error = it.error))
                }
            }
        }.launch("dashboard_exams")
    }

    private fun loadConferences() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            conferenceRepository.getConferences(student, semester, false)
        }.map { conferencesResource ->
            val currentDateTime = LocalDateTime.now()

            val filteredConferences = conferencesResource.data?.filter {
                it.date.isAfter(currentDateTime)
            }

            conferencesResource.copy(data = filteredConferences)
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard conferences data started")
                    updateData(DashboardTile.Conferences(it.data ?: emptyList(), isLoading = true))
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard conferences result: Success")
                    updateData(DashboardTile.Conferences(it.data ?: emptyList()))
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard conferences result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    showErrorInTile(DashboardTile.Conferences(error = it.error))
                }
            }
        }.launch("dashboard_conferences")
    }

    private fun updateData(dashboardTile: DashboardTile) {
        dashboardTileList.removeAll { it.type == dashboardTile.type }
        dashboardTileList.add(dashboardTile)

        dashboardTileList.sortBy { tile ->
            dashboardTilesToLoad.single { (_, type) -> type == tile.type }.first
        }

        val isTilesLoaded =
            dashboardTilesToLoad.all { (_, type) -> dashboardTileList.any { it.type == type } }

        view?.run {
            showProgress(!isTilesLoaded)
            showContent(isTilesLoaded)
            updateData(dashboardTileList.toList())
        }
    }

    private fun showErrorInTile(dashboardTile: DashboardTile) {
        dashboardTileList.removeAll { it.type == dashboardTile.type }
        dashboardTileList.add(dashboardTile)

        dashboardTileList.sortBy { tile ->
            dashboardTilesToLoad.single { (_, type) -> type == tile.type }.first
        }

        val isTilesLoaded =
            dashboardTilesToLoad.all { (_, type) -> dashboardTileList.any { it.type == type } }

        view?.run {
            showProgress(!isTilesLoaded)
            showContent(isTilesLoaded)
            updateData(dashboardTileList.toList())
        }

        if (isTilesLoaded) {
            val filteredTiles =
                dashboardTileList.filterNot { it.type == DashboardTile.Type.ACCOUNT }
            val isAccountTileError =
                dashboardTileList.single { it.type == DashboardTile.Type.ACCOUNT }.error != null
            val isGeneralError = filteredTiles.all { it.error != null } || isAccountTileError

            val errorMessage = filteredTiles.map { it.error?.stackTraceToString() }.toString()

            lastError = Exception(errorMessage)

            view?.run {
                showContent(!isGeneralError)
                showErrorView(isGeneralError)
            }
        }
    }

    private fun updateGradeTheme() {
        view?.updateGradeTheme(preferencesRepository.gradeColorTheme)
    }
}