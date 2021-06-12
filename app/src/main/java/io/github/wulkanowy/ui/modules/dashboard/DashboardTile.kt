package io.github.wulkanowy.ui.modules.dashboard

import io.github.wulkanowy.data.db.entities.Conference
import io.github.wulkanowy.data.db.entities.Exam
import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.SchoolAnnouncement
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.pojos.TimetableFull
import io.github.wulkanowy.data.db.entities.Homework as EntitiesHomework

sealed class DashboardTile(val type: Type) {

    abstract val error: Throwable?

    abstract val isLoading: Boolean

    data class Account(
        val student: Student? = null,
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.ACCOUNT)

    data class HorizontalGroup(
        val unreadMessagesCount: Int? = null,
        val attendancePercentage: Double? = null,
        val luckyNumber: Int? = null,
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.HORIZONTAL_GROUP)

    data class Grades(
        val subjectWithGrades: Map<String, List<Grade>> = mapOf(),
        val gradeTheme: String? = null,
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.GRADES)

    data class Lessons(
        val lessons: TimetableFull? = null,
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.LESSONS)

    data class Homework(
        val homework: List<EntitiesHomework> = emptyList(),
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.HOMEWORK)

    data class Announcements(
        val announcement: List<SchoolAnnouncement> = emptyList(),
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.ANNOUNCEMENTS)

    data class Exams(
        val exams: List<Exam> = emptyList(),
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.EXAMS)

    data class Conferences(
        val conferences: List<Conference> = emptyList(),
        override val error: Throwable? = null,
        override val isLoading: Boolean = false
    ) : DashboardTile(Type.CONFERENCES)

    enum class Type(val id: Int) {
        ACCOUNT(1),
        HORIZONTAL_GROUP(2),
        LESSONS(3),
        GRADES(4),
        HOMEWORK(5),
        ANNOUNCEMENTS(6),
        EXAMS(7),
        CONFERENCES(8),
        ADS(9)
    }

    enum class DataType {
        ACCOUNT,
        LUCKY_NUMBER,
        MESSAGES,
        ATTENDANCE,
        LESSONS,
        GRADES,
        HOMEWORK,
        ANNOUNCEMENTS,
        EXAMS,
        CONFERENCES,
        ADS
    }
}