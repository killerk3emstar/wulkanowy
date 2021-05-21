package io.github.wulkanowy.ui.modules.dashboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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
import io.github.wulkanowy.utils.getCompatColor
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

        with(horizontalGroupViewHolder.binding) {
            dashboardHorizontalGroupItemAttendanceValue.text = "%.2f%%".format(attendancePercentage)
            dashboardHorizontalGroupItemLuckyValue.text = luckyNumber?.luckyNumber.toString()
            dashboardHorizontalGroupItemMessageValue.text = messageCount.toString()
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
        val timetableFull = item.data as TimetableFull
        val binding = lessonsViewHolder.binding
        val currentDateTime = LocalDateTime.now()
        val currentDate = LocalDate.now()

        val currentTimetable = timetableFull.lessons
            .filter { it.date == currentDate }
            .filter { it.end.isAfter(currentDateTime) }
            .filterNot { it.canceled }
        val currentDayHeader =
            timetableFull.headers.singleOrNull { it.date == currentDate }

        val tomorrowTimetable = timetableFull.lessons
            .filter { it.date == currentDate.plusDays(1) }
            .filterNot { it.canceled }
        val tomorrowDayHeader =
            timetableFull.headers.singleOrNull { it.date == currentDate.plusDays(1) }

        when {
            currentTimetable.isNotEmpty() -> {
                updateLessonView(item, currentTimetable, binding)
                binding.dashboardLessonsItemTitleTomorrow.isVisible = false
            }
            currentDayHeader != null && currentDayHeader.content.isNotBlank() -> {
                updateLessonView(item, emptyList(), binding)
                binding.dashboardLessonsItemTitleTomorrow.isVisible = false
            }
            tomorrowTimetable.isNotEmpty() -> {
                updateLessonView(item, tomorrowTimetable, binding)
                binding.dashboardLessonsItemTitleTomorrow.isVisible = true
            }
            tomorrowDayHeader != null && tomorrowDayHeader.content.isNotBlank() -> {
                updateLessonView(item, emptyList(), binding)
                binding.dashboardLessonsItemTitleTomorrow.isVisible = true
            }
            else -> {
                updateLessonView(item, emptyList(), binding)
                binding.dashboardLessonsItemTitleTomorrow.isVisible = true
            }
        }

        lessonsTimer?.cancel()
        lessonsTimer = timer(period = 1000) {
            val currentDateTime = LocalDateTime.now()
            val currentDate = LocalDate.now()

            Handler(Looper.getMainLooper()).post {
                val currentTimetable = timetableFull.lessons
                    .filter { it.date == currentDate }
                    .filter { it.end.isAfter(currentDateTime) }
                    .filterNot { it.canceled }
                val currentDayHeader =
                    timetableFull.headers.singleOrNull { it.date == currentDate }

                val tomorrowTimetable = timetableFull.lessons
                    .filter { it.date == currentDate.plusDays(1) }
                    .filterNot { it.canceled }
                val tomorrowDayHeader =
                    timetableFull.headers.singleOrNull { it.date == currentDate.plusDays(1) }

                when {
                    currentTimetable.isNotEmpty() -> {
                        updateLessonView(item, currentTimetable, binding)
                        binding.dashboardLessonsItemTitleTomorrow.isVisible = false
                    }
                    currentDayHeader != null && currentDayHeader.content.isNotBlank() -> {
                        updateLessonView(item, emptyList(), binding)
                        binding.dashboardLessonsItemTitleTomorrow.isVisible = false
                    }
                    tomorrowTimetable.isNotEmpty() -> {
                        updateLessonView(item, tomorrowTimetable, binding)
                        binding.dashboardLessonsItemTitleTomorrow.isVisible = true
                    }
                    tomorrowDayHeader != null && tomorrowDayHeader.content.isNotBlank() -> {
                        updateLessonView(item, emptyList(), binding)
                        binding.dashboardLessonsItemTitleTomorrow.isVisible = true
                    }
                    else -> {
                        updateLessonView(item, emptyList(), binding)
                        binding.dashboardLessonsItemTitleTomorrow.isVisible = true
                    }
                }
            }
        }
    }

    private fun updateLessonView(
        item: DashboardData,
        timetableToShow: List<Timetable>,
        binding: ItemDashboardLessonsBinding,
    ) {
        val currentDateTime = LocalDateTime.now()
        val nextLessons = timetableToShow.filter { it.end.isAfter(currentDateTime) }
            .sortedBy { it.start }

        with(binding) {
            dashboardLessonsItemEmpty.isVisible =
                (timetableToShow.isEmpty() || nextLessons.isEmpty()) && item.error == null
            dashboardLessonsItemError.isVisible = item.error != null

            val firstLesson = nextLessons.getOrNull(0)

            dashboardLessonsItemFirstTitle.isVisible = firstLesson != null
            dashboardLessonsItemFirstTime.isVisible = firstLesson != null
            dashboardLessonsItemFirstTimeRange.isVisible = firstLesson != null
            dashboardLessonsItemFirstValue.isVisible = firstLesson != null

            firstLesson?.let {
                dashboardLessonsItemFirstValue.text =
                    "${firstLesson.subject}, Sala ${firstLesson.room}"

                if (currentDateTime.isBefore(firstLesson.start)) {
                    val duration =
                        Duration.between(currentDateTime, firstLesson.start).toMinutes() + 1

                    if (duration > 60) {
                        dashboardLessonsItemFirstTimeRange.text =
                            "${firstLesson.start.toFormattedString("HH:mm")}-${
                                firstLesson.end.toFormattedString("HH:mm")
                            }"
                        dashboardLessonsItemFirstTimeRange.isVisible = true
                        dashboardLessonsItemFirstTime.isVisible = false
                    } else {
                        dashboardLessonsItemFirstTime.text = "za $duration minut"
                        dashboardLessonsItemFirstTime.isVisible = true
                        dashboardLessonsItemFirstTimeRange.isVisible = false
                    }
                } else {
                    dashboardLessonsItemFirstTime.isVisible = true
                    dashboardLessonsItemFirstTimeRange.isVisible = false
                    dashboardLessonsItemFirstTime.text =
                        "jeszcze ${firstLesson.left?.toMinutes()?.plus(1)} minut"
                }

                dashboardLessonsItemFirstTitle.text =
                    if (currentDateTime.isBefore(firstLesson.start)) {
                        val duration =
                            Duration.between(currentDateTime, firstLesson.start).toMinutes() + 1
                        when {
                            duration < 60 -> {
                                dashboardLessonsItemFirstTitle.setTextColor(
                                    binding.root.context.getCompatColor(
                                        R.color.colorPrimary
                                    )
                                )
                                dashboardLessonsItemFirstValue.setTextColor(
                                    binding.root.context.getCompatColor(
                                        R.color.colorPrimary
                                    )
                                )

                                dashboardLessonsItemFirstTitle.typeface =
                                    Typeface.create("sans-serif-medium", Typeface.NORMAL)
                                dashboardLessonsItemFirstValue.typeface =
                                    Typeface.create("sans-serif-medium", Typeface.NORMAL)
                                "Za chwilę:"
                            }
                            duration < 240 -> {
                                dashboardLessonsItemFirstTitle.setTextColor(
                                    Color.BLACK

                                )
                                dashboardLessonsItemFirstValue.setTextColor(
                                    Color.BLACK
                                )

                                dashboardLessonsItemFirstTitle.typeface =
                                    Typeface.create("sans-serif", Typeface.NORMAL)
                                dashboardLessonsItemFirstValue.typeface =
                                    Typeface.create("sans-serif", Typeface.NORMAL)
                                "Niebawem"
                            }
                            else -> {
                                dashboardLessonsItemFirstTitle.setTextColor(
                                    Color.BLACK

                                )
                                dashboardLessonsItemFirstValue.setTextColor(
                                    Color.BLACK

                                )

                                dashboardLessonsItemFirstTitle.typeface =
                                    Typeface.create("sans-serif", Typeface.NORMAL)
                                dashboardLessonsItemFirstValue.typeface =
                                    Typeface.create("sans-serif", Typeface.NORMAL)

                                "Najpierw:"
                            }
                        }
                    } else {
                        "Teraz:"
                    }
            }

            val secondLesson = nextLessons.getOrNull(1)

            dashboardLessonsItemSecondTime.isVisible = secondLesson != null
            dashboardLessonsItemSecondTitle.isVisible =
                !(secondLesson == null && firstLesson == null)
            dashboardLessonsItemSecondValue.isVisible =
                !(secondLesson == null && firstLesson == null)

            dashboardLessonsItemSecondValue.text =
                if (secondLesson != null) {
                    "${secondLesson.subject}, Sala ${secondLesson.room}"
                } else {
                    "Koniec lekcji"
                }
            dashboardLessonsItemSecondTime.text =
                "${secondLesson?.start?.toFormattedString("HH:mm")}-${
                    secondLesson?.end?.toFormattedString("HH:mm")
                }"

            dashboardLessonsItemThirdTime.isVisible = nextLessons.size > 2
            dashboardLessonsItemThirdTitle.isVisible = nextLessons.size > 2
            dashboardLessonsItemThirdValue.isVisible = nextLessons.size > 2
            dashboardLessonsItemDivider.isVisible = nextLessons.size > 2

            dashboardLessonsItemThirdValue.text =
                "jeszcze ${nextLessons.size - 2} kolejnych lekcji"
            dashboardLessonsItemThirdTime.text =
                "do ${nextLessons.lastOrNull()?.end?.toFormattedString("HH:mm")}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindHomeworkViewHolder(homeworkViewHolder: HomeworkViewHolder, position: Int) {
        val item = items[position]
        val homeworkList = item.data as List<Homework>? ?: emptyList()
        val homeworkAdapter = homeworkViewHolder.adapter.apply {
            this.items = homeworkList.take(5)
        }

        with(homeworkViewHolder.binding) {
            dashboardHomeworkItemEmpty.isVisible = homeworkList.isEmpty() && item.error == null
            dashboardHomeworkItemError.isVisible = item.error != null
            dashboardHomeworkItemDivider.isVisible = homeworkList.size > 5
            dashboardHomeworkItemMore.isVisible = homeworkList.size > 5
            dashboardHomeworkItemMore.text = "Jeszcze ${homeworkList.size - 5} zadań więcej"

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
            dashboardAnnouncementsItemMore.text =
                "Jeszcze ${schoolAnnouncementList.size - 5} ogłoszeń więcej"

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
        val examList = item.data as List<Exam>? ?: emptyList()
        val examAdapter = examsViewHolder.adapter.apply {
            this.items = examList.take(5)
        }

        with(examsViewHolder.binding) {
            dashboardExamsItemEmpty.isVisible = examList.isEmpty() && item.error == null
            dashboardExamsItemError.isVisible = item.error != null
            dashboardExamsItemDivider.isVisible = examList.size > 5
            dashboardExamsItemMore.isVisible = examList.size > 5
            dashboardExamsItemMore.text = "Jeszcze ${examList.size - 5} sprawdzianów więcej"

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
        val conferenceList = item.data as List<Conference>? ?: emptyList()
        val conferenceAdapter = conferencesViewHolder.adapter.apply {
            this.items = conferenceList.take(5)
        }

        with(conferencesViewHolder.binding) {
            dashboardConferencesItemEmpty.isVisible = conferenceList.isEmpty() && item.error == null
            dashboardConferencesItemError.isVisible = item.error != null
            dashboardConferencesItemDivider.isVisible = conferenceList.size > 5
            dashboardConferencesItemMore.isVisible = conferenceList.size > 5
            dashboardConferencesItemMore.text = "Jeszcze ${conferenceList.size - 5} zebrań więcej"

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
