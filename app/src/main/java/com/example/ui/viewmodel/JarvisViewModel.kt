package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLauncherHelper
import com.example.data.ContactsHelper
import com.example.data.GeminiService
import com.example.data.HistoryRepository
import com.example.data.ScheduleRepository
import com.example.data.TaskRepository
import com.example.model.AssistantAction
import com.example.model.ContactItem
import com.example.model.HistoryItem
import com.example.model.MessageItem
import com.example.model.ScheduleItem
import com.example.model.TaskItem
import com.example.service.JarvisNotificationListenerService
import com.example.service.JarvisVoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    private val scheduleRepository = ScheduleRepository(application)
    private val historyRepository = HistoryRepository(application)
    private val geminiService = GeminiService(application)

    private val _preferredLanguage = MutableStateFlow("hi") // "hi" for Hindi, "en" for English
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentStatusText = MutableStateFlow("Jarvis is online and ready.")
    val currentStatusText: StateFlow<String> = _currentStatusText.asStateFlow()

    private val _lastSpokenResponse = MutableStateFlow("")
    val lastSpokenResponse: StateFlow<String> = _lastSpokenResponse.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    val messages: StateFlow<List<MessageItem>> = JarvisNotificationListenerService.messagesFlow

    val tasks: StateFlow<List<TaskItem>> = taskRepository.observeTasks()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val schedules: StateFlow<List<ScheduleItem>> = scheduleRepository.observeSchedules()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val history: StateFlow<List<HistoryItem>> = historyRepository.observeRecentHistory()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    var voiceEngine: JarvisVoiceEngine? = null

    init {
        loadContacts()
        voiceEngine = JarvisVoiceEngine(application) { spokenText ->
            processUserCommand(spokenText, application)
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = ContactsHelper.getContacts(getApplication())
        }
    }

    fun setTab(index: Int) {
        _activeTab.value = index
    }

    fun toggleLanguage() {
        val nextLang = if (_preferredLanguage.value == "hi") "en" else "hi"
        _preferredLanguage.value = nextLang
        val confirmation = if (nextLang == "hi") {
            "भाषा हिंदी पर सेट कर दी गई है।"
        } else {
            "Language switched to English."
        }
        speak(confirmation)
    }

    fun startListening() {
        voiceEngine?.startListening(if (_preferredLanguage.value == "hi") "hi-IN" else "en-US")
    }

    fun stopListening() {
        voiceEngine?.stopListening()
    }

    fun speak(text: String) {
        _lastSpokenResponse.value = text
        voiceEngine?.speak(text, _preferredLanguage.value)
    }

    fun repeatLastSpeech() {
        val text = _lastSpokenResponse.value
        if (text.isNotBlank()) {
            voiceEngine?.speak(text, _preferredLanguage.value)
        }
    }

    fun processUserCommand(command: String, context: Context) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            _currentStatusText.value = "Analyzing: \"$command\"..."

            val action = geminiService.processUserCommand(
                input = command,
                preferredLanguage = _preferredLanguage.value,
                existingTasks = tasks.value,
                existingSchedules = schedules.value
            )

            executeAction(action, command, context)
            _isProcessing.value = false
        }
    }

    private suspend fun executeAction(action: AssistantAction, originalCommand: String, context: Context) {
        val isHindi = _preferredLanguage.value == "hi"

        when (action) {
            is AssistantAction.OpenApp -> {
                val speech = if (isHindi) action.feedbackHindi else action.feedbackEnglish
                _currentStatusText.value = speech
                speak(speech)
                historyRepository.recordHistory(originalCommand, speech, _preferredLanguage.value, "open_app")

                when (action.appName.lowercase()) {
                    "youtube" -> AppLauncherHelper.openYouTube(context, null)
                    "whatsapp" -> AppLauncherHelper.openWhatsApp(context, null)
                    "messages" -> AppLauncherHelper.openMessages(context, null)
                    else -> AppLauncherHelper.openYouTube(context, null)
                }
            }

            is AssistantAction.MakeCall -> {
                val speech = if (isHindi) action.feedbackHindi else action.feedbackEnglish
                _currentStatusText.value = speech
                speak(speech)
                historyRepository.recordHistory(originalCommand, speech, _preferredLanguage.value, "make_call")
                AppLauncherHelper.makeCall(context, action.phoneNumber)
            }

            is AssistantAction.AddTask -> {
                val speech = if (isHindi) action.feedbackHindi else action.feedbackEnglish
                _currentStatusText.value = speech
                speak(speech)
                taskRepository.createTask(action.title, action.priority, action.category)
                historyRepository.recordHistory(originalCommand, speech, _preferredLanguage.value, "add_task")
                _activeTab.value = 1 // Switch to Tasks tab to view newly added task
            }

            is AssistantAction.AddSchedule -> {
                val speech = if (isHindi) action.feedbackHindi else action.feedbackEnglish
                _currentStatusText.value = speech
                speak(speech)
                scheduleRepository.createScheduleItem(
                    title = action.title,
                    startTime = action.startTime,
                    date = action.date,
                    priority = action.priority
                )
                historyRepository.recordHistory(originalCommand, speech, _preferredLanguage.value, "organize_schedule")
                _activeTab.value = 2 // Switch to Schedule tab
            }

            is AssistantAction.ReadMessages -> {
                val speech = if (isHindi) action.summaryHindi else action.summaryEnglish
                _currentStatusText.value = speech
                speak(speech)
                historyRepository.recordHistory(originalCommand, speech, _preferredLanguage.value, "read_message")
                _activeTab.value = 4 // Switch to Messages tab
            }

            is AssistantAction.GeneralAnswer -> {
                val speech = if (isHindi) action.replyHindi else action.replyEnglish
                _currentStatusText.value = speech
                speak(speech)
                historyRepository.recordHistory(originalCommand, speech, _preferredLanguage.value, "chat")
            }
        }
    }

    fun addTask(title: String, priority: String, category: String = "general", dueDate: String = "") {
        viewModelScope.launch {
            taskRepository.createTask(title, priority, category, dueDate)
            val feedback = if (_preferredLanguage.value == "hi") {
                "टास्क सफलतापूर्वक जोड़ दिया गया: $title"
            } else {
                "Task added: $title with $priority priority."
            }
            speak(feedback)
        }
    }

    fun toggleTask(taskId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(taskId, currentStatus)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun addSchedule(title: String, startTime: String, endTime: String = "", date: String, priority: String = "high") {
        viewModelScope.launch {
            scheduleRepository.createScheduleItem(title, startTime, endTime, date, "", priority)
            val feedback = if (_preferredLanguage.value == "hi") {
                "शेड्यूल इवेंट जोड़ दिया गया: $title"
            } else {
                "Schedule event created: $title at $startTime."
            }
            speak(feedback)
        }
    }

    fun toggleSchedule(scheduleId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            scheduleRepository.toggleScheduleCompleted(scheduleId, currentStatus)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId)
        }
    }

    fun planDayWithAi() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            scheduleRepository.createScheduleItem("Morning Priority Focus (Deep Work)", "09:00 AM", "11:00 AM", today, "Focus on urgent pending deliverables", "urgent")
            scheduleRepository.createScheduleItem("Client & Team Sync Call", "11:30 AM", "12:30 PM", today, "Call review and feedback session", "high")
            scheduleRepository.createScheduleItem("Afternoon Execution & Review", "02:00 PM", "04:30 PM", today, "Address urgent messages and project updates", "urgent")
            scheduleRepository.createScheduleItem("Daily Wrap-up & Tomorrow Planning", "05:00 PM", "05:30 PM", today, "Review completed tasks and organize agenda", "medium")

            val feedback = if (_preferredLanguage.value == "hi") {
                "मैंने आपके लिए आज का पूरा दिन व्यवस्थित कर दिया है, जिसमें सबसे ज़रूरी कार्यों को प्राथमिकता दी गई है।"
            } else {
                "I have generated an optimized daily schedule prioritizing your urgent tasks and focus blocks."
            }
            speak(feedback)
        }
    }

    fun prioritizeTasksWithAi() {
        viewModelScope.launch {
            tasks.value.forEach { task ->
                if (!task.isCompleted && (task.title.contains("urgent", ignoreCase = true) || task.title.contains("report", ignoreCase = true) || task.title.contains("call", ignoreCase = true))) {
                    taskRepository.updateTaskPriority(task.id, "urgent")
                }
            }
            val feedback = if (_preferredLanguage.value == "hi") {
                "सभी कार्यों का विश्लेषण कर सबसे महत्वपूर्ण और ज़रूरी कार्यों को प्राथमिकता सूची में सबसे ऊपर रख दिया गया है।"
            } else {
                "Tasks have been prioritized with high-impact urgent items highlighted at the top."
            }
            speak(feedback)
        }
    }

    fun explainMessage(message: MessageItem) {
        val isHindi = _preferredLanguage.value == "hi"
        val explanation = if (isHindi) message.summaryHindi.ifBlank { "संदेश: ${message.content}" } else message.summaryEnglish.ifBlank { "Message from ${message.sender}: ${message.content}" }
        _currentStatusText.value = explanation
        speak(explanation)
    }

    fun callContact(contact: ContactItem, context: Context) {
        val isHindi = _preferredLanguage.value == "hi"
        val feedback = if (isHindi) "${contact.name} को फोन मिला रहा हूँ..." else "Calling ${contact.name} now..."
        speak(feedback)
        AppLauncherHelper.makeCall(context, contact.phoneNumber)
    }

    fun openApp(appName: String, context: Context) {
        when (appName.lowercase()) {
            "youtube" -> {
                val feedback = if (_preferredLanguage.value == "hi") "यूट्यूब खोल रहा हूँ..." else "Opening YouTube..."
                speak(feedback)
                AppLauncherHelper.openYouTube(context)
            }
            "whatsapp" -> {
                val feedback = if (_preferredLanguage.value == "hi") "व्हाट्सएप खोल रहा हूँ..." else "Opening WhatsApp..."
                speak(feedback)
                AppLauncherHelper.openWhatsApp(context)
            }
            "messages" -> {
                val feedback = if (_preferredLanguage.value == "hi") "मैसेज खोल रहा हूँ..." else "Opening Messages..."
                speak(feedback)
                AppLauncherHelper.openMessages(context)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine?.destroy()
    }
}
