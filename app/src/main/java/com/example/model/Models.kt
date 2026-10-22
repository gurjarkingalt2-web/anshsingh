package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val priority: String = "urgent", // urgent, high, medium, low
    val category: String = "general", // urgent, work, personal, call, reminder
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ScheduleItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val date: String = "",
    val priority: String = "high", // urgent, high, medium, low
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class HistoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val commandText: String = "",
    val responseText: String = "",
    val language: String = "hi", // hi or en
    val actionType: String = "chat", // open_app, make_call, add_task, organize_schedule, read_message, chat
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class MessageItem(
    val id: String = "",
    val sender: String = "",
    val appName: String = "WhatsApp",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val summaryHindi: String = "",
    val summaryEnglish: String = "",
    val isUrgent: Boolean = false
)

data class ContactItem(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val photoUri: String? = null
)

sealed class AssistantAction {
    data class OpenApp(
        val appName: String,
        val packageName: String,
        val targetUrl: String? = null,
        val feedbackHindi: String,
        val feedbackEnglish: String
    ) : AssistantAction()

    data class MakeCall(
        val contactName: String,
        val phoneNumber: String,
        val feedbackHindi: String,
        val feedbackEnglish: String
    ) : AssistantAction()

    data class AddTask(
        val title: String,
        val priority: String = "urgent",
        val category: String = "urgent",
        val feedbackHindi: String,
        val feedbackEnglish: String
    ) : AssistantAction()

    data class AddSchedule(
        val title: String,
        val startTime: String,
        val date: String,
        val priority: String = "high",
        val feedbackHindi: String,
        val feedbackEnglish: String
    ) : AssistantAction()

    data class ReadMessages(
        val count: Int,
        val summaryHindi: String,
        val summaryEnglish: String
    ) : AssistantAction()

    data class GeneralAnswer(
        val replyHindi: String,
        val replyEnglish: String
    ) : AssistantAction()
}
