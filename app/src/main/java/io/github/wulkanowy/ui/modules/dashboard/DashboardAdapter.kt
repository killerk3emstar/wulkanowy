package io.github.wulkanowy.ui.modules.dashboard

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.data.db.entities.TimetableHeader
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

    private val items = mutableListOf<DashboardTile>()

    var lessonsTimer: Timer? = null

    fun submitList(newItems: List<DashboardTile>) {
        val dashboardAdapterDiffCallback = DashboardAdapterDiffCallback(newItems, items)
        val diffResult = DiffUtil.calculateDiff(dashboardAdapterDiffCallback)

        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = items[position].type.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            DashboardTile.Type.ACCOUNT.id -> AccountViewHolder(
                ItemDashboardAccountBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.HORIZONTAL_GROUP.id -> HorizontalGroupViewHolder(
                ItemDashboardHorizontalGroupBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.GRADES.id -> GradesViewHolder(
                ItemDashboardGradesBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.LESSONS.id -> LessonsViewHolder(
                ItemDashboardLessonsBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.HOMEWORK.id -> HomeworkViewHolder(
                ItemDashboardHomeworkBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.ANNOUNCEMENTS.id -> AnnouncementsViewHolder(
                ItemDashboardAnnouncementsBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.EXAMS.id -> ExamsViewHolder(
                ItemDashboardExamsBinding.inflate(inflater, parent, false)
            )
            DashboardTile.Type.CONFERENCES.id -> ConferencesViewHolder(
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

    fun clearTimers() {
        lessonsTimer?.let {
            it.cancel()
            it.purge()
        }
        lessonsTimer = null
    }

    private fun bindAccountViewHolder(accountViewHolder: AccountViewHolder, position: Int) {
        val student = (items[position] as DashboardTile.Account).student ?: return
        val avatar = accountViewHolder.binding.root.context.createNameInitialsDrawable(
            text = student.nickOrName,
            backgroundColor = student.avatarColor
        )

        with(accountViewHolder.binding) {
            dashboardAccountItemAvatar.setImageDrawable(avatar)
            dashboardAccountItemName.text = student.nickOrName
            dashboardAccountItemSchoolName.text = student.schoolName
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindHorizontalGroupViewHolder(
        horizontalGroupViewHolder: HorizontalGroupViewHolder,
        position: Int
    ) {
        val (unreadMessagesCount, attendancePercentage, luckyNumber, error, isLoading) = items[position] as DashboardTile.HorizontalGroup
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
            dashboardHorizontalGroupItemLuckyValue.text = luckyNumber?.toString()
            dashboardHorizontalGroupItemMessageValue.text = unreadMessagesCount.toString()

            if (dashboardHorizontalGroupItemInfoContainer.isVisible != (error != null || isLoading)) {
                dashboardHorizontalGroupItemInfoContainer.isVisible = error != null || isLoading
            }

            if (dashboardHorizontalGroupItemInfoProgress.isVisible != isLoading) {
                dashboardHorizontalGroupItemInfoProgress.isVisible = isLoading
            }

            dashboardHorizontalGroupItemInfoErrorText.isVisible = error != null

            dashboardHorizontalGroupItemLuckyContainer.isVisible = error == null && !isLoading
            dashboardHorizontalGroupItemAttendanceContainer.isVisible = error == null && !isLoading
            dashboardHorizontalGroupItemMessageContainer.isVisible = error == null && !isLoading
            /* dashboardHorizontalGroupItemAttendanceContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                 matchConstraintPercentWidth = if (luckyNumber == null) 0.5f else 0.4f
             }*/
        }
    }

    private fun bindGradesViewHolder(gradesViewHolder: GradesViewHolder, position: Int) {
        val item = items[position] as DashboardTile.Grades
        val subjectWithGrades = item.subjectWithGrades.orEmpty()
        val gradeTheme = item.gradeTheme
        val error = item.error
        val isLoading = item.isLoading
        val dashboardGradesAdapter = gradesViewHolder.adapter.apply {
            this.items = subjectWithGrades.toList()
            this.gradeTheme = gradeTheme.orEmpty()
        }

        with(gradesViewHolder.binding) {
            dashboardGradesItemEmpty.isVisible =
                subjectWithGrades.isEmpty() && error == null && !isLoading
            dashboardGradesItemError.isVisible = error != null && !isLoading
            dashboardGradesItemProgress.isVisible =
                isLoading && error == null && subjectWithGrades.isEmpty()

            with(dashboardGradesItemRecycler) {
                adapter = dashboardGradesAdapter
                layoutManager = LinearLayoutManager(context)
                isVisible = subjectWithGrades.isNotEmpty() && error == null
            }
        }
    }

    private fun bindLessonsViewHolder(lessonsViewHolder: LessonsViewHolder, position: Int) {
        val item = items[position] as DashboardTile.Lessons
        val timetableFull = item.lessons
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
        item: DashboardTile.Lessons,
        timetableToShow: List<Timetable>,
        binding: ItemDashboardLessonsBinding,
        header: TimetableHeader? = null,
    ) {
        val currentDateTime = LocalDateTime.now()
        val nextLessons = timetableToShow.filter { it.end.isAfter(currentDateTime) }
            .sortedBy { it.start }

        with(binding) {
            dashboardLessonsItemEmpty.isVisible =
                (timetableToShow.isEmpty() || nextLessons.isEmpty()) && item.error == null && header == null && !item.isLoading
            dashboardLessonsItemError.isVisible = item.error != null && !item.isLoading
            dashboardLessonsItemProgress.isVisible =
                item.isLoading && (timetableToShow.isEmpty() || nextLessons.isEmpty()) && item.error == null && header == null

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

    private fun bindHomeworkViewHolder(homeworkViewHolder: HomeworkViewHolder, position: Int) {
        val item = items[position] as DashboardTile.Homework
        val homeworkList = item.homework.orEmpty()
        val error = item.error
        val isLoading = item.isLoading
        val context = homeworkViewHolder.binding.root.context
        val homeworkAdapter = homeworkViewHolder.adapter.apply {
            this.items = homeworkList.take(5)
        }

        with(homeworkViewHolder.binding) {
            dashboardHomeworkItemEmpty.isVisible =
                homeworkList.isEmpty() && error == null && !isLoading
            dashboardHomeworkItemError.isVisible = error != null && !isLoading
            dashboardHomeworkItemProgress.isVisible =
                isLoading && error == null && homeworkList.isEmpty()
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
                isVisible = homeworkList.isNotEmpty() && error == null
            }
        }
    }

    private fun bindAnnouncementsViewHolder(
        announcementsViewHolder: AnnouncementsViewHolder,
        position: Int
    ) {
        val item = items[position] as DashboardTile.Announcements
        val schoolAnnouncementList = item.announcement.orEmpty()
        val error = item.error
        val isLoading = item.isLoading
        val context = announcementsViewHolder.binding.root.context
        val schoolAnnouncementsAdapter = announcementsViewHolder.adapter.apply {
            this.items = schoolAnnouncementList.take(5)
        }

        with(announcementsViewHolder.binding) {
            dashboardAnnouncementsItemEmpty.isVisible =
                schoolAnnouncementList.isEmpty() && error == null && !isLoading
            dashboardAnnouncementsItemError.isVisible = error != null && !isLoading
            dashboardAnnouncementsItemProgress.isVisible =
                isLoading && error == null && schoolAnnouncementList.isEmpty()
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
                isVisible = schoolAnnouncementList.isNotEmpty() && error == null
            }
        }
    }

    private fun bindExamsViewHolder(examsViewHolder: ExamsViewHolder, position: Int) {
        val item = items[position] as DashboardTile.Exams
        val exams = item.exams.orEmpty()
        val error = item.error
        val isLoading = item.isLoading
        val context = examsViewHolder.binding.root.context
        val examAdapter = examsViewHolder.adapter.apply {
            this.items = exams.take(5)
        }

        with(examsViewHolder.binding) {
            dashboardExamsItemEmpty.isVisible = exams.isEmpty() && error == null && !isLoading
            dashboardExamsItemError.isVisible = error != null && !isLoading
            dashboardExamsItemProgress.isVisible = isLoading && error == null && exams.isEmpty()
            dashboardExamsItemDivider.isVisible = exams.size > 5
            dashboardExamsItemMore.isVisible = exams.size > 5
            dashboardExamsItemMore.text = context.resources.getQuantityString(
                R.plurals.dashboard_exams_more,
                exams.size - 5,
                exams.size - 5
            )

            with(dashboardExamsItemRecycler) {
                layoutManager = LinearLayoutManager(context)
                adapter = examAdapter
                isVisible = exams.isNotEmpty() && error == null
            }
        }
    }

    private fun bindConferencesViewHolder(
        conferencesViewHolder: ConferencesViewHolder,
        position: Int
    ) {
        val item = items[position] as DashboardTile.Conferences
        val conferences = item.conferences.orEmpty()
        val error = item.error
        val isLoading = item.isLoading
        val context = conferencesViewHolder.binding.root.context
        val conferenceAdapter = conferencesViewHolder.adapter.apply {
            this.items = conferences.take(5)
        }

        with(conferencesViewHolder.binding) {
            dashboardConferencesItemEmpty.isVisible =
                conferences.isEmpty() && error == null && !isLoading
            dashboardConferencesItemError.isVisible = error != null && !isLoading
            dashboardConferencesItemProgress.isVisible =
                isLoading && error == null && conferences.isEmpty()
            dashboardConferencesItemDivider.isVisible = conferences.size > 5
            dashboardConferencesItemMore.isVisible = conferences.size > 5
            dashboardConferencesItemMore.text = context.resources.getQuantityString(
                R.plurals.dashboard_conference_more,
                conferences.size - 5,
                conferences.size - 5
            )

            with(dashboardConferencesItemRecycler) {
                layoutManager = LinearLayoutManager(context)
                adapter = conferenceAdapter
                isVisible = conferences.isNotEmpty() && error == null
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

    class DashboardAdapterDiffCallback(
        private val newItems: List<DashboardTile>,
        private val oldItems: List<DashboardTile>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition].type == newItems[newItemPosition].type
    }
}
