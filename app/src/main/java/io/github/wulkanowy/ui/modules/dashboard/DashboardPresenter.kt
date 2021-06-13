package io.github.wulkanowy.ui.modules.dashboard

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

    private lateinit var dashboardTilesToLoad: Set<DashboardTile.Type>

    private lateinit var dashboardDataToLoad: Set<DashboardTile.DataType>

    private lateinit var lastError: Throwable

    override fun onAttachView(view: DashboardView) {
        super.onAttachView(view)

        with(view) {
            initView()
            showProgress(true)
            showContent(false)
        }

        loadData()
    }

    fun loadData() {
        dashboardDataToLoad = preferencesRepository.dashboardData
        dashboardTilesToLoad = dashboardDataToLoad.map {
            when (it) {
                DashboardTile.DataType.ACCOUNT -> DashboardTile.Type.ACCOUNT
                DashboardTile.DataType.LUCKY_NUMBER -> DashboardTile.Type.HORIZONTAL_GROUP
                DashboardTile.DataType.MESSAGES -> DashboardTile.Type.HORIZONTAL_GROUP
                DashboardTile.DataType.ATTENDANCE -> DashboardTile.Type.HORIZONTAL_GROUP
                DashboardTile.DataType.LESSONS -> DashboardTile.Type.LESSONS
                DashboardTile.DataType.GRADES -> DashboardTile.Type.GRADES
                DashboardTile.DataType.HOMEWORK -> DashboardTile.Type.HOMEWORK
                DashboardTile.DataType.ANNOUNCEMENTS -> DashboardTile.Type.ANNOUNCEMENTS
                DashboardTile.DataType.EXAMS -> DashboardTile.Type.EXAMS
                DashboardTile.DataType.CONFERENCES -> DashboardTile.Type.CONFERENCES
                DashboardTile.DataType.ADS -> DashboardTile.Type.ADS
            }
        }.toSet()

        dashboardDataToLoad.forEach {
            when (it) {
                DashboardTile.DataType.ACCOUNT -> loadCurrentAccount()
                DashboardTile.DataType.LUCKY_NUMBER -> loadLuckyNumber()
                DashboardTile.DataType.MESSAGES -> loadMessages()
                DashboardTile.DataType.ATTENDANCE -> loadAttendance()
                DashboardTile.DataType.LESSONS -> loadLessons()
                DashboardTile.DataType.GRADES -> loadGrades()
                DashboardTile.DataType.HOMEWORK -> loadHomework()
                DashboardTile.DataType.ANNOUNCEMENTS -> loadSchoolAnnouncements()
                DashboardTile.DataType.EXAMS -> loadExams()
                DashboardTile.DataType.CONFERENCES -> loadConferences()
                DashboardTile.DataType.ADS -> TODO()
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

    private fun loadLuckyNumber() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)

            luckyNumberRepository.getLuckyNumber(student, true)
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard lucky number data started")
                    processHorizontalGroupData(luckyNumber = it.data?.luckyNumber, isLoading = true)
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard lucky number result: Success")
                    processHorizontalGroupData(luckyNumber = it.data?.luckyNumber ?: -1)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard lucky number result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    processHorizontalGroupData(error = it.error)
                }
            }
        }.launch("dashboard_lucky_number")
    }

    private fun loadMessages() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            messageRepository.getMessages(student, semester, MessageFolder.RECEIVED, true)
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard messages data started")
                    val unreadMessagesCount = it.data?.count { message -> message.unread }

                    processHorizontalGroupData(
                        unreadMessagesCount = unreadMessagesCount,
                        isLoading = true
                    )
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard messages result: Success")
                    val unreadMessagesCount = it.data?.count { message -> message.unread }

                    processHorizontalGroupData(unreadMessagesCount = unreadMessagesCount)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard messages result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                    processHorizontalGroupData(error = it.error)
                }
            }
        }.launch("dashboard_messages")
    }

    private fun loadAttendance() {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent(true)
            val semester = semesterRepository.getCurrentSemester(student)

            attendanceSummaryRepository.getAttendanceSummary(student, semester, -1, false)
        }.onEach {
            when (it.status) {
                Status.LOADING -> {
                    Timber.i("Loading dashboard attendance data started")
                    val attendancePercentage = it.data?.calculatePercentage()

                    processHorizontalGroupData(
                        attendancePercentage = attendancePercentage,
                        isLoading = true
                    )
                }
                Status.SUCCESS -> {
                    Timber.i("Loading dashboard attendance result: Success")
                    val attendancePercentage = it.data?.calculatePercentage()

                    processHorizontalGroupData(attendancePercentage = attendancePercentage)
                }
                Status.ERROR -> {
                    Timber.i("Loading dashboard attendance result: An exception occurred")
                    errorHandler.dispatch(it.error!!)

                    processHorizontalGroupData(error = it.error)
                }
            }
        }.launch("dashboard_attendance")
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

                    updateData(
                        DashboardTile.Grades(
                            subjectWithGrades = filteredSubjectWithGrades,
                            gradeTheme = preferencesRepository.gradeColorTheme,
                            isLoading = true
                        )
                    )
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

                    updateData(
                        DashboardTile.Grades(
                            subjectWithGrades = filteredSubjectWithGrades,
                            gradeTheme = preferencesRepository.gradeColorTheme
                        )
                    )
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
            val date = LocalDate.now().nextOrSameSchoolDay

            timetableRepository.getTimetable(
                student = student,
                semester = semester,
                start = date,
                end = date.plusDays(1),
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

    private fun processHorizontalGroupData(
        luckyNumber: Int? = null,
        unreadMessagesCount: Int? = null,
        attendancePercentage: Double? = null,
        error: Throwable? = null,
        isLoading: Boolean = false
    ) {
        val isLuckyNumberToLoad =
            dashboardDataToLoad.any { it == DashboardTile.DataType.LUCKY_NUMBER }
        val isMessagesToLoad = dashboardDataToLoad.any { it == DashboardTile.DataType.MESSAGES }
        val isAttendanceToLoad = dashboardDataToLoad.any { it == DashboardTile.DataType.ATTENDANCE }

        val isPushedToList =
            dashboardTileList.any { it.type == DashboardTile.Type.HORIZONTAL_GROUP }

        if (error != null && !isPushedToList) {
            showErrorInTile(DashboardTile.HorizontalGroup(error = error))
            return
        } else if (error != null && isPushedToList) return

        if (!isPushedToList && isLoading) {
            updateData(DashboardTile.HorizontalGroup(isLoading = true))
            return
        }

        val horizontalGroup =
            dashboardTileList.single { it is DashboardTile.HorizontalGroup } as DashboardTile.HorizontalGroup

        when {
            luckyNumber != null -> {
                updateData(horizontalGroup.copy(luckyNumber = luckyNumber))
            }
            unreadMessagesCount != null -> {
                updateData(horizontalGroup.copy(unreadMessagesCount = unreadMessagesCount))
            }
            attendancePercentage != null -> {
                updateData(horizontalGroup.copy(attendancePercentage = attendancePercentage))
            }
        }

        val isHorizontalGroupLoaded = dashboardTileList.any {
            if (it !is DashboardTile.HorizontalGroup) return@any false

            val isLuckyNumberStateCorrect = (it.luckyNumber != null) == isLuckyNumberToLoad
            val isMessagesStateCorrect = (it.unreadMessagesCount != null) == isMessagesToLoad
            val isAttendanceStateCorrect = (it.attendancePercentage != null) == isAttendanceToLoad

            isLuckyNumberStateCorrect && isAttendanceStateCorrect && isMessagesStateCorrect
        }

        if (isHorizontalGroupLoaded) {
            val updatedHorizontalGroup =
                dashboardTileList.single { it is DashboardTile.HorizontalGroup } as DashboardTile.HorizontalGroup

            updateData(updatedHorizontalGroup.copy(isLoading = false))
        }
    }

    private fun updateData(dashboardTile: DashboardTile) {
        dashboardTileList.removeAll { it.type == dashboardTile.type }
        dashboardTileList.add(dashboardTile)

        dashboardTileList.sortBy { tile -> dashboardTilesToLoad.single { it == tile.type }.ordinal }

        val isTilesLoaded =
            dashboardTilesToLoad.all { type -> dashboardTileList.any { it.type == type } }
        val isTilesDataLoaded = isTilesLoaded && dashboardTileList.all {
            it.isDataLoaded || it.error != null
        }

        view?.run {
            showProgress(!isTilesDataLoaded)
            showContent(isTilesDataLoaded)
            updateData(dashboardTileList.toList())
        }
    }

    private fun showErrorInTile(dashboardTile: DashboardTile) {
        dashboardTileList.removeAll { it.type == dashboardTile.type }
        dashboardTileList.add(dashboardTile)

        dashboardTileList.sortBy { tile -> dashboardTilesToLoad.single { it == tile.type }.ordinal }

        val isTilesLoaded =
            dashboardTilesToLoad.all { type -> dashboardTileList.any { it.type == type } }
        val isTilesDataLoaded = isTilesLoaded && dashboardTileList.all {
            it.isDataLoaded || it.error != null
        }

        view?.run {
            showProgress(!isTilesDataLoaded)
            showContent(isTilesDataLoaded)
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
}