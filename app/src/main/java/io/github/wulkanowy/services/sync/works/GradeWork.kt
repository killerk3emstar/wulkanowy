package io.github.wulkanowy.services.sync.works

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_ALL
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.GradeRepository
import io.github.wulkanowy.data.repositories.PreferencesRepository
import io.github.wulkanowy.services.sync.channels.NewGradesChannel
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.getCompatBitmap
import io.github.wulkanowy.utils.getCompatColor
import io.github.wulkanowy.utils.nickOrName
import io.github.wulkanowy.utils.waitForResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class GradeWork @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val gradeRepository: GradeRepository,
    private val preferencesRepository: PreferencesRepository
) : Work {

    override suspend fun doWork(student: Student, semester: Semester) {
        gradeRepository.getGrades(
            student = student,
            semester = semester,
            forceRefresh = true,
            notify = preferencesRepository.isNotificationsEnable
        ).waitForResult()

        gradeRepository.getNotNotifiedGrades(semester).first().let {
            if (it.isNotEmpty()) it.forEach { item ->
                sendDetailNotification(student, item)
            }
            gradeRepository.updateGrades(it.onEach { grade -> grade.isNotified = true })
        }

        gradeRepository.getNotNotifiedPredictedGrades(semester).first().let {
            if (it.isNotEmpty()) it.forEach { item ->
                sendPredictedNotification(student, item)
            }
            gradeRepository.updateGradesSummary(it.onEach { grade ->
                grade.isPredictedGradeNotified = true
            })
        }

        gradeRepository.getNotNotifiedFinalGrades(semester).first().let {
            if (it.isNotEmpty()) it.forEach { item ->
                setFinalNotification(student, item)
            }
            gradeRepository.updateGradesSummary(it.onEach { grade ->
                grade.isFinalGradeNotified = true
            })
        }
    }

    private fun sendDetailNotification(student: Student, grade: Grade) {
        notificationManager.notify(
            Random.nextInt(Int.MAX_VALUE), getNotificationBuilder()
                .setContentTitle(context.getString(R.string.grade_new_item))
                .setContentText("${grade.subject}: ${grade.entry}")
                .setStyle(NotificationCompat.InboxStyle().run {
                    setSummaryText(student.nickOrName)
                    addLine("${grade.subject}: ${grade.entry}")
                    this
                })
                .build()
        )
    }

    private fun sendPredictedNotification(student: Student, gradesSummary: GradeSummary) {
        notificationManager.notify(
            Random.nextInt(Int.MAX_VALUE), getNotificationBuilder()
                .setContentTitle(context.getString(R.string.grade_new_items_predicted))
                .setContentText("${gradesSummary.subject}: ${gradesSummary.predictedGrade}")
                .setStyle(NotificationCompat.InboxStyle().run {
                    setSummaryText(student.nickOrName)
                    addLine("${gradesSummary.subject}: ${gradesSummary.predictedGrade}")
                    this
                })
                .build()
        )
    }

    private fun setFinalNotification(student: Student, gradesSummary: GradeSummary) {
        notificationManager.notify(
            Random.nextInt(Int.MAX_VALUE), getNotificationBuilder()
                .setContentTitle(context.getString(R.string.grade_new_items_final))
                .setContentText("${gradesSummary.subject}: ${gradesSummary.finalGrade}")
                .setStyle(NotificationCompat.InboxStyle().run {
                    setSummaryText(student.nickOrName)
                    addLine("${gradesSummary.subject}: ${gradesSummary.finalGrade}")
                    this
                })
                .build()
        )
    }

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NewGradesChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_all)
            .setLargeIcon(
                context.getCompatBitmap(R.drawable.ic_stat_grade, R.color.colorPrimary)
            )
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setDefaults(DEFAULT_ALL)
            .setColor(context.getCompatColor(R.color.colorPrimary))
            .setContentIntent(
                PendingIntent.getActivity(
                    context, MainView.Section.GRADE.id,
                    MainActivity.getStartIntent(context, MainView.Section.GRADE, true),
                    FLAG_UPDATE_CURRENT
                )
            )
    }
}
