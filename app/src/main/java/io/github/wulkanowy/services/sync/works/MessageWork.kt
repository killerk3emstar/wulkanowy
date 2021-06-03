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
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.enums.MessageFolder.RECEIVED
import io.github.wulkanowy.data.repositories.MessageRepository
import io.github.wulkanowy.data.repositories.PreferencesRepository
import io.github.wulkanowy.services.sync.channels.NewMessagesChannel
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.getCompatBitmap
import io.github.wulkanowy.utils.getCompatColor
import io.github.wulkanowy.utils.nickOrName
import io.github.wulkanowy.utils.waitForResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class MessageWork @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val messageRepository: MessageRepository,
    private val preferencesRepository: PreferencesRepository
) : Work {

    override suspend fun doWork(student: Student, semester: Semester) {
        messageRepository.getMessages(
            student = student,
            semester = semester,
            folder = RECEIVED,
            forceRefresh = true,
            notify = preferencesRepository.isNotificationsEnable
        ).waitForResult()

        messageRepository.getNotNotifiedMessages(student).first().let {
            if (it.isNotEmpty()) it.forEach { item ->
                sendNotification(student, item)
            }
            messageRepository.updateMessages(it.onEach { message -> message.isNotified = true })
        }
    }

    private fun sendNotification(student: Student, message: Message) {
        notificationManager.notify(
            Random.nextInt(Int.MAX_VALUE),
            NotificationCompat.Builder(context, NewMessagesChannel.CHANNEL_ID)
                .setContentTitle(context.getString(R.string.message_new_item))
                .setContentText("${message.sender}: ${message.subject}")
                .setSmallIcon(R.drawable.ic_stat_all)
                .setLargeIcon(
                    context.getCompatBitmap(R.drawable.ic_stat_message, R.color.colorPrimary)
                )
                .setAutoCancel(true)
                .setDefaults(DEFAULT_ALL)
                .setPriority(PRIORITY_HIGH)
                .setColor(context.getCompatColor(R.color.colorPrimary))
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        MainView.Section.MESSAGE.id,
                        MainActivity.getStartIntent(context, MainView.Section.MESSAGE, true),
                        FLAG_UPDATE_CURRENT
                    )
                )
                .setStyle(NotificationCompat.InboxStyle().run {
                    setSummaryText(student.nickOrName)
                    addLine("${message.sender}: ${message.subject}")
                    this
                })
                .build()
        )
    }
}
