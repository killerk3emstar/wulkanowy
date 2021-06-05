package io.github.wulkanowy.ui.modules.dashboard

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Conference
import io.github.wulkanowy.data.db.entities.Exam
import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.Homework
import io.github.wulkanowy.data.db.entities.LuckyNumber
import io.github.wulkanowy.data.db.entities.SchoolAnnouncement
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.data.db.entities.TimetableHeader
import io.github.wulkanowy.data.pojos.TimetableFull
import io.github.wulkanowy.databinding.ItemDashboardAccountBinding
import io.github.wulkanowy.databinding.ItemDashboardAnnouncementsBinding
import io.github.wulkanowy.databinding.ItemDashboardConferencesBinding
import io.github.wulkanowy.databinding.ItemDashboardExamsBinding
import io.github.wulkanowy.databinding.ItemDashboardGradesBinding
import io.github.wulkanowy.databinding.ItemDashboardHomeworkBinding
import io.github.wulkanowy.databinding.ItemDashboardHorizontalGroupBinding
import io.github.wulkanowy.databinding.ItemDashboardLessonsBinding
import io.github.wulkanowy.utils.createNameInitialsDrawable
import io.github.wulkanowy.utils.getThemeAttrColor
import io.github.wulkanowy.utils.left
import io.github.wulkanowy.utils.nickOrName
import io.github.wulkanowy.utils.toFormattedString
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer

class DashboardAdapter @Inject constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = emptyList<DashboardData>()

    var gradeTheme = ""

    var lessonsTimer: Timer? = null

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = items[position].viewType.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            DashboardViewType.ACCOUNT.id -> AccountViewHolder(
                ItemDashboardAccountBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.HORIZONTAL_GROUP.id -> HorizontalGroupViewHolder(
                ItemDashboardHorizontalGroupBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.GRADES.id -> GradesViewHolder(
                ItemDashboardGradesBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.LESSONS.id -> LessonsViewHolder(
                ItemDashboardLessonsBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.HOMEWORK.id -> HomeworkViewHolder(
                ItemDashboardHomeworkBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.ANNOUNCEMENTS.id -> AnnouncementsViewHolder(
                ItemDashboardAnnouncementsBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.EXAMS.id -> ExamsViewHolder(
                ItemDashboardExamsBinding.inflate(inflater, parent, false)
            )
            DashboardViewType.CONFERENCES.id -> ConferencesViewHolder(
                ItemDashboardConferencesBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AccountViewHolder -> bindAccountViewHolder(holder, position)
            is HorizontalGroupViewHolder -> bindHorizontalGroupViewHolder(holder, position)
            is GradesViewHolder -> bindGradesViewHolder(holder, position)
            is LessonsViewHolder -> bindLessonsViewHolder(holder, position)
            is HomeworkViewHolder -> bindHomeworkViewHolder(holder, position)
            is AnnouncementsViewHolder -> bindAnnouncementsViewHolder(holder, position)
            is ExamsViewHolder -> bindExamsViewHolder(holder, position)
            is ConferencesViewHolder -> bindConferencesViewHolder(holder, position)
        }
    }

    fun onStopFragment() {
        lessonsTimer?.let {
            it.cancel()
            it.purge()
        }
        lessonsTimer = null
    }

    private fun bindAccountViewHolder(accountViewHolder: AccountViewHolder, position: Int) {
        val item = items[position].data as Student
        val avatar = accountViewHolder.binding.root.context.createNameInitialsDrawable(
            text = item.nickOrName,
            backgroundColor = item.avatarColor
        )

        with(accountViewHolder.binding) {
            dashboardAccountItemAvatar.setImageDrawable(avatar)
            dashboardAccountItemName.text = item.nickOrName
            dashboardAccountItemSchoolName.text = item.schoolName
        }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("UNCHECKED_CAST")
    private fun bindHorizontalGroupViewHolder(
        horizontalGroupViewHolder: HorizontalGroupViewHolder,
        position: Int
    ) {
        val (luckyNumber, messageCount, attendancePercentage) = items[position].data as Triple<LuckyNumber?, Int?, Double?>
        val binding = horizontalGroupViewHolder.binding
        val context = binding.root.context
        val attendanceColor = when {
            attendancePercentage ?: 0.0 <= 50.0 -> context.getThemeAttrColor(R.attr.colorPrimary)
            attendancePercentage ?: 0.0 <= 75.0 -> context.getThemeAttrColor(R.attr.colorTimetableChange)
            else -> context.getThemeAttrColor(R.attr.colorOnSurface)
        }

        with(binding.dashboardHorizontalGroupItemAttendanceValue) {
            text = "%.2f%%".format(attendancePercentage)
            setTextColor(attendanceColor)
        }

        with(binding) {
            dashboardHorizontalGroupItemLuckyValue.text = luckyNumber?.luckyNumber.toString()
            dashboardHorizontalGroupItemMessageValue.text = messageCount.toString()
            dashboardHorizontalGroupItemLuckyContainer.isVisible = luckyNumber != null
            dashboardHorizontalGroupItemAttendanceContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintPercentWidth = if (luckyNumber == null) 0.5f else 0.4f
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindGradesViewHolder(gradesViewHolder: GradesViewHolder, position: Int) {
        val item = items[position]
        val subjectWithGrades = item.data as Map<String, List<Grade>>? ?: emptyMap()
        val dashboardGradesAdapter = gradesViewHolder.adapter.apply {
            this.items = subjectWithGrades.toList()
            this.gradeTheme = this@DashboardAdapter.gradeTheme
        }

        with(gradesViewHolder.binding) {
            dashboardGradesItemEmpty.isVisible = subjectWithGrades.isEmpty() && item.error == null
            dashboardGradesItemError.isVisible = item.error != null

            with(dashboardGradesItemRecycler) {
                adapter = dashboardGradesAdapter
                layoutManager = LinearLayoutManager(context)
                isVisible = subjectWithGrades.isNotEmpty() && item.error == null
            }
        }
    }

    private fun bindLessonsViewHolder(lessonsViewHolder: LessonsViewHolder, position: Int) {
        val item = items[position]
        val timetableFull = item.data as TimetableFull?
        val binding = lessonsViewHolder.binding

        fun updateLessonState() {
            val currentDateTime = LocalDateTime.now()
            val currentDate = LocalDate.now()

            val currentTimetable = timetableFull?.lessons
                .orEmpty()
                .filter { it.date == currentDate }
                .filter { it.end.isAfter(currentDateTime) }
                .filterNot { it.canceled }
            val currentDayHeader =
                timetableFull?.headers.orEmpty().singleOrNull { it.date == currentDate }

            val tomorrowTimetable = timetableFull?.lessons.orEmpty()
                .filter { it.date == currentDate.plusDays(1) }
                .filterNot { it.canceled }
            val tomorrowDayHeader =
                timetableFull?.headers.orEmpty().singleOrNull { it.date == currentDate.plusDays(1) }

            when {
                currentTimetable.isNotEmpty() -> {
                    updateLessonView(item, currentTimetable, binding)
                    binding.dashboardLessonsItemTitleTomorrow.isVisible = false
                }
                currentDayHeader != null && currentDayHeader.content.isNotBlank() -> {
                    updateLessonView(item, emptyList(), binding, currentDayHeader)
                    binding.dashboardLessonsItemTitleTomorrow.isVisible = false
                }
                tomorrowTimetable.isNotEmpty() -> {
                    updateLessonView(item, tomorrowTimetable, binding)
                    binding.dashboardLessonsItemTitleTomorrow.isVisible = true
                }
                tomorrowDayHeader != null && tomorrowDayHeader.content.isNotBlank() -> {
                    updateLessonView(item, emptyList(), binding, tomorrowDayHeader)
                    binding.dashboardLessonsItemTitleTomorrow.isVisible = true
                }
                else -> {
                    updateLessonView(item, emptyList(), binding)
                    binding.dashboardLessonsItemTitleTomorrow.isVisible = true
                }
            }
        }

        updateLessonState()

        lessonsTimer?.cancel()
        lessonsTimer = timer(period = 1000) {
            Handler(Looper.getMainLooper()).post { updateLessonState() }
        }
    }

    private fun updateLessonView(
        item: DashboardData,
        timetableToShow: List<Timetable>,
        binding: ItemDashboardLessonsBinding,
        header: TimetableHeader? = null,
    ) {
        val currentDateTime = LocalDateTime.now()
        val nextLessons = timetableToShow.filter { it.end.isAfter(currentDateTime) }
            .sortedBy { it.start }

        with(binding) {
            dashboardLessonsItemEmpty.isVisible =
                (timetableToShow.isEmpty() || nextLessons.isEmpty()) && item.error == null && header == null
            dashboardLessonsItemError.isVisible = item.error != null

            val secondLesson = nextLessons.getOrNull(1)
            val firstLesson = nextLessons.getOrNull(0)

            updateFirstLessonView(binding, firstLesson, currentDateTime)
            updateSecondLesson(binding, firstLesson, secondLesson)
            updateLessonSummary(binding, nextLessons)
            updateLessonHeader(binding, header)
        }
    }

    private fun updateFirstLessonView(
        binding: ItemDashboardLessonsBinding,
        firstLesson: Timetable?,
        currentDateTime: LocalDateTime
    ) {
        val context = binding.root.context
        val sansSerifFont = Typeface.create("sans-serif", Typeface.NORMAL)
        val sansSerifMediumFont = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        with(binding) {
            dashboardLessonsItemFirstTitle.isVisible = firstLesson != null
            dashboardLessonsItemFirstTime.isVisible = firstLesson != null
            dashboardLessonsItemFirstTimeRange.isVisible = firstLesson != null
            dashboardLessonsItemFirstValue.isVisible = firstLesson != null
        }

        firstLesson ?: return

        val minutesToStartLesson =
            Duration.between(currentDateTime, firstLesson.start).toMinutes()
        val isFirstTimeVisible: Boolean
        val isFirstTimeRangeVisible: Boolean
        val firstTimeText: String
        val firstTimeRangeText: String
        val firstTitleText: String
        val firstTitleAndValueTextColor: Int
        val firstTitleAndValueTextFont: Typeface

        if (currentDateTime.isBefore(firstLesson.start)) {
            if (minutesToStartLesson > 60) {
                val formattedStartTime = firstLesson.start.toFormattedString("HH:mm")
                val formattedEndTime = firstLesson.end.toFormattedString("HH:mm")

                firstTimeRangeText = "${formattedStartTime}-${formattedEndTime}"
                firstTimeText = ""

                isFirstTimeRangeVisible = true
                isFirstTimeVisible = false
            } else {
                firstTimeText = context.resources.getQuantityString(
                    R.plurals.dashboard_timetable_first_lesson_time_in_minutes,
                    minutesToStartLesson.toInt(),
                    minutesToStartLesson
                )
                firstTimeRangeText = ""

                isFirstTimeRangeVisible = false
                isFirstTimeVisible = true
            }

            when {
                minutesToStartLesson < 60 -> {
                    firstTitleAndValueTextColor = context.getThemeAttrColor(R.attr.colorPrimary)
                    firstTitleAndValueTextFont = sansSerifMediumFont
                    firstTitleText =
                        context.getString(R.string.dashboard_timetable_first_lesson_title_moment)
                }
                minutesToStartLesson < 240 -> {
                    firstTitleAndValueTextColor =
                        context.getThemeAttrColor(R.attr.colorOnSurface)
                    firstTitleAndValueTextFont = sansSerifFont
                    firstTitleText =
                        context.getString(R.string.dashboard_timetable_first_lesson_title_soon)
                }
                else -> {
                    firstTitleAndValueTextColor =
                        context.getThemeAttrColor(R.attr.colorOnSurface)
                    firstTitleAndValueTextFont = sansSerifFont
                    firstTitleText =
                        context.getString(R.string.dashboard_timetable_first_lesson_title_first)
                }
            }
        } else {
            val minutesToEndLesson = firstLesson.left!!.toMinutes()

            firstTimeText = context.resources.getQuantityString(
                R.plurals.dashboard_timetable_first_lesson_time_more_minutes,
                minutesToEndLesson.toInt(),
                minutesToEndLesson
            )
            firstTimeRangeText = ""

            isFirstTimeRangeVisible = false
            isFirstTimeVisible = true

            firstTitleAndValueTextColor = context.getThemeAttrColor(R.attr.colorPrimary)
            firstTitleAndValueTextFont = sansSerifMediumFont
            firstTitleText = context.getString(R.string.dashboard_timetable_first_lesson_title_now)
        }

        with(binding.dashboardLessonsItemFirstTime) {
            isVisible = isFirstTimeVisible
            text = firstTimeText
        }
        with(binding.dashboardLessonsItemFirstTimeRange) {
            isVisible = isFirstTimeRangeVisible
            text = firstTimeRangeText
        }
        with(binding.dashboardLessonsItemFirstTitle) {
            setTextColor(firstTitleAndValueTextColor)
            typeface = firstTitleAndValueTextFont
            text = firstTitleText
        }
        with(binding.dashboardLessonsItemFirstValue) {
            setTextColor(firstTitleAndValueTextColor)
            typeface = firstTitleAndValueTextFont
            text = context.getString(
                R.string.dashboard_timetable_lesson_value,
                firstLesson.subject,
                firstLesson.room
            )
        }
    }

    private fun updateSecondLesson(
        binding: ItemDashboardLessonsBinding,
        firstLesson: Timetable?,
        secondLesson: Timetable?
    ) {
        val context = binding.root.context

        val formattedStartTime = secondLesson?.start?.toFormattedString("HH:mm")
        val formattedEndTime = secondLesson?.end?.toFormattedString("HH:mm")

        val secondTimeText = "${formattedStartTime}-${formattedEndTime}"
        val secondValueText = if (secondLesson != null) {
            context.getString(
                R.string.dashboard_timetable_lesson_value,
                secondLesson.subject,
                secondLesson.room
            )
        } else {
            context.getString(R.string.dashboard_timetable_second_lesson_value_end)
        }

        with(binding.dashboardLessonsItemSecondTime) {
            isVisible = secondLesson != null
            text = secondTimeText
        }
        with(binding.dashboardLessonsItemSecondValue) {
            isVisible = !(secondLesson == null && firstLesson == null)
            text = secondValueText
        }
        binding.dashboardLessonsItemSecondTitle.isVisible =
            !(secondLesson == null && firstLesson == null)
    }

    private fun updateLessonSummary(
        binding: ItemDashboardLessonsBinding,
        nextLessons: List<Timetable>
    ) {
        val context = binding.root.context
        val formattedEndTime = nextLessons.lastOrNull()?.end?.toFormattedString("HH:mm")

        with(binding) {
            dashboardLessonsItemThirdTime.isVisible = nextLessons.size > 2
            dashboardLessonsItemThirdTitle.isVisible = nextLessons.size > 2
            dashboardLessonsItemThirdValue.isVisible = nextLessons.size > 2
            dashboardLessonsItemDivider.isVisible = nextLessons.size > 2

            dashboardLessonsItemThirdValue.text = context.resources.getQuantityString(
                R.plurals.dashboard_timetable_third_value,
                nextLessons.size - 2,
                nextLessons.size - 2
            )
            dashboardLessonsItemThirdTime.text =
                context.getString(R.string.dashboard_timetable_third_time, formattedEndTime)
        }
    }

    private fun updateLessonHeader(
        binding: ItemDashboardLessonsBinding,
        header: TimetableHeader?
    ) {
        with(binding.dashboardLessonsItemDayHeader) {
            isVisible = header != null
            text = header?.content
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindHomeworkViewHolder(homeworkViewHolder: HomeworkViewHolder, position: Int) {
        val item = items[position]
        val context = homeworkViewHolder.binding.root.context
        val homeworkList = item.data as List<Homework>? ?: emptyList()
        val homeworkAdapter = homeworkViewHolder.adapter.apply {
            this.items = homeworkList.take(5)
        }

        with(homeworkViewHolder.binding) {
            dashboardHomeworkItemEmpty.isVisible = homeworkList.isEmpty() && item.error == null
            dashboardHomeworkItemError.isVisible = item.error != null
            dashboardHomeworkItemDivider.isVisible = homeworkList.size > 5
            dashboardHomeworkItemMore.isVisible = homeworkList.size > 5
            dashboardHomeworkItemMore.text = context.resources.getQuantityString(
                R.plurals.dashboard_homework_more,
                homeworkList.size - 5,
                homeworkList.size - 5
            )

            with(dashboardHomeworkItemRecycler) {
                adapter = homeworkAdapter
                layoutManager = LinearLayoutManager(context)
                isVisible = homeworkList.isNotEmpty() && item.error == null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindAnnouncementsViewHolder(
        announcementsViewHolder: AnnouncementsViewHolder,
        position: Int
    ) {
        val item = items[position]
        val context = announcementsViewHolder.binding.root.context
        val schoolAnnouncementList = item.data as List<SchoolAnnouncement>? ?: emptyList()
        val schoolAnnouncementsAdapter = announcementsViewHolder.adapter.apply {
            this.items = schoolAnnouncementList.take(5)
        }

        with(announcementsViewHolder.binding) {
            dashboardAnnouncementsItemEmpty.isVisible =
                schoolAnnouncementList.isEmpty() && item.error == null
            dashboardAnnouncementsItemError.isVisible = item.error != null
            dashboardAnnouncementsItemDivider.isVisible = schoolAnnouncementList.size > 5
            dashboardAnnouncementsItemMore.isVisible = schoolAnnouncementList.size > 5
            dashboardAnnouncementsItemMore.text = context.resources.getQuantityString(
                R.plurals.dashboard_announcements_more,
                schoolAnnouncementList.size - 5,
                schoolAnnouncementList.size - 5
            )
            with(dashboardAnnouncementsItemRecycler) {
                layoutManager = LinearLayoutManager(context)
                adapter = schoolAnnouncementsAdapter
                isVisible = schoolAnnouncementList.isNotEmpty() && item.error == null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindExamsViewHolder(examsViewHolder: ExamsViewHolder, position: Int) {
        val item = items[position]
        val context = examsViewHolder.binding.root.context
        val examList = item.data as List<Exam>? ?: emptyList()
        val examAdapter = examsViewHolder.adapter.apply {
            this.items = examList.take(5)
        }

        with(examsViewHolder.binding) {
            dashboardExamsItemEmpty.isVisible = examList.isEmpty() && item.error == null
            dashboardExamsItemError.isVisible = item.error != null
            dashboardExamsItemDivider.isVisible = examList.size > 5
            dashboardExamsItemMore.isVisible = examList.size > 5
            dashboardExamsItemMore.text = context.resources.getQuantityString(
                R.plurals.dashboard_exams_more,
                examList.size - 5,
                examList.size - 5
            )

            with(dashboardExamsItemRecycler) {
                layoutManager = LinearLayoutManager(context)
                adapter = examAdapter
                isVisible = examList.isNotEmpty() && item.error == null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindConferencesViewHolder(
        conferencesViewHolder: ConferencesViewHolder,
        position: Int
    ) {
        val item = items[position]
        val context = conferencesViewHolder.binding.root.context
        val conferenceList = item.data as List<Conference>? ?: emptyList()
        val conferenceAdapter = conferencesViewHolder.adapter.apply {
            this.items = conferenceList.take(5)
        }

        with(conferencesViewHolder.binding) {
            dashboardConferencesItemEmpty.isVisible = conferenceList.isEmpty() && item.error == null
            dashboardConferencesItemError.isVisible = item.error != null
            dashboardConferencesItemDivider.isVisible = conferenceList.size > 5
            dashboardConferencesItemMore.isVisible = conferenceList.size > 5
            dashboardConferencesItemMore.text = context.resources.getQuantityString(
                R.plurals.dashboard_conference_more,
                conferenceList.size - 5,
                conferenceList.size - 5
            )

            with(dashboardConferencesItemRecycler) {
                layoutManager = LinearLayoutManager(context)
                adapter = conferenceAdapter
                isVisible = conferenceList.isNotEmpty() && item.error == null
            }
        }
    }

    class AccountViewHolder(val binding: ItemDashboardAccountBinding) :
        RecyclerView.ViewHolder(binding.root)

    class HorizontalGroupViewHolder(val binding: ItemDashboardHorizontalGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    class GradesViewHolder(val binding: ItemDashboardGradesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val adapter by lazy { DashboardGradesAdapter() }
    }

    class LessonsViewHolder(val binding: ItemDashboardLessonsBinding) :
        RecyclerView.ViewHolder(binding.root)

    class HomeworkViewHolder(val binding: ItemDashboardHomeworkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val adapter by lazy { DashboardHomeworkAdapter() }
    }

    class AnnouncementsViewHolder(val binding: ItemDashboardAnnouncementsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val adapter by lazy { DashboardAnnouncementsAdapter() }
    }

    class ExamsViewHolder(val binding: ItemDashboardExamsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val adapter by lazy { DashboardExamsAdapter() }
    }

    class ConferencesViewHolder(val binding: ItemDashboardConferencesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val adapter by lazy { DashboardConferencesAdapter() }
    }
}
