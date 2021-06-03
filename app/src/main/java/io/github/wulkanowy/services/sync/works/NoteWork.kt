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
import io.github.wulkanowy.data.db.entities.Note
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.NoteRepository
import io.github.wulkanowy.data.repositories.PreferencesRepository
import io.github.wulkanowy.sdk.scrapper.notes.NoteCategory
import io.github.wulkanowy.sdk.scrapper.notes.NoteCategory.NEUTRAL
import io.github.wulkanowy.sdk.scrapper.notes.NoteCategory.POSITIVE
import io.github.wulkanowy.services.sync.channels.NewNotesChannel
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.getCompatBitmap
import io.github.wulkanowy.utils.getCompatColor
import io.github.wulkanowy.utils.nickOrName
import io.github.wulkanowy.utils.waitForResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class NoteWork @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val noteRepository: NoteRepository,
    private val preferencesRepository: PreferencesRepository
) : Work {

    override suspend fun doWork(student: Student, semester: Semester) {
        noteRepository.getNotes(
            student = student,
            semester = semester,
            forceRefresh = true,
            notify = preferencesRepository.isNotificationsEnable
        ).waitForResult()

        noteRepository.getNotNotifiedNotes(student).first().let {
            if (it.isNotEmpty()) it.forEach { item ->
                sendNotification(student, item)
            }
            noteRepository.updateNotes(it.onEach { note -> note.isNotified = true })
        }
    }

    private fun sendNotification(student: Student, note: Note) {
        notificationManager.notify(
            Random.nextInt(Int.MAX_VALUE),
            NotificationCompat.Builder(context, NewNotesChannel.CHANNEL_ID)
                .setContentTitle(
                    when (NoteCategory.getByValue(note.categoryType)) {
                        POSITIVE -> context.getString(R.string.praise_new_item)
                        NEUTRAL -> context.getString(R.string.neutral_note_new_item)
                        else -> context.getString(R.string.note_new_item)
                    }
                )
                .setContentText("${note.teacher}: ${note.category}")
                .setSmallIcon(R.drawable.ic_stat_all)
                .setLargeIcon(
                    context.getCompatBitmap(R.drawable.ic_stat_note, R.color.colorPrimary)
                )
                .setAutoCancel(true)
                .setDefaults(DEFAULT_ALL)
                .setPriority(PRIORITY_HIGH)
                .setColor(context.getCompatColor(R.color.colorPrimary))
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        MainView.Section.NOTE.id,
                        MainActivity.getStartIntent(context, MainView.Section.NOTE, true),
                        FLAG_UPDATE_CURRENT
                    )
                )
                .setStyle(NotificationCompat.InboxStyle().run {
                    setSummaryText(student.nickOrName)
                    addLine("${note.teacher}: ${note.category}")
                    this
                })
                .build()
        )
    }
}
