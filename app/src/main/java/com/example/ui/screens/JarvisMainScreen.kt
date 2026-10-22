package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.model.ContactItem
import com.example.model.HistoryItem
import com.example.model.MessageItem
import com.example.model.ScheduleItem
import com.example.model.TaskItem
import com.example.service.JarvisNotificationListenerService
import com.example.ui.theme.JarvisBgDark
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisHighOrange
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisUrgentRed
import com.example.ui.viewmodel.JarvisViewModel
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JarvisMainScreen(
    user: FirebaseUser,
    viewModel: JarvisViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val activeTab by viewModel.activeTab.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val currentStatusText by viewModel.currentStatusText.collectAsState()

    val tasks by viewModel.tasks.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val history by viewModel.history.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    val voiceEngine = viewModel.voiceEngine
    val isListening = voiceEngine?.isListening?.collectAsState()?.value ?: false
    val isSpeaking = voiceEngine?.isSpeaking?.collectAsState()?.value ?: false

    var commandInputText by remember { mutableStateOf("") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    // Request permissions for mic, contacts, calls, and notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CONTACTS] == true) {
            viewModel.loadContacts()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    Scaffold(
        containerColor = JarvisBgDark,
        topBar = {
            JarvisTopHeader(
                user = user,
                preferredLanguage = preferredLanguage,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onSignOut = {
                    signOutGoogle(context, credentialManager, onSignOut, scope)
                }
            )
        },
        bottomBar = {
            JarvisBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        floatingActionButton = {
            if (activeTab == 1) {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    containerColor = JarvisCyan,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                }
            } else if (activeTab == 2) {
                FloatingActionButton(
                    onClick = { showAddScheduleDialog = true },
                    containerColor = JarvisGold,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_schedule_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Status Banner & Speech feedback
            Surface(
                color = JarvisSurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSpeaking) JarvisGold.copy(alpha = 0.5f) else JarvisCyan.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.SmartToy,
                        contentDescription = "Status",
                        tint = if (isSpeaking) JarvisGold else JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentStatusText,
                        fontSize = 12.sp,
                        color = JarvisTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.repeatLastSpeech() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Replay",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Quick command action chips
            QuickActionChipsRow(
                preferredLanguage = preferredLanguage,
                onChipClicked = { command ->
                    commandInputText = command
                    viewModel.processUserCommand(command, context)
                }
            )

            // Dynamic Main Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> AssistantVoiceTab(
                        viewModel = viewModel,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isProcessing = isProcessing,
                        preferredLanguage = preferredLanguage,
                        history = history,
                        commandText = commandInputText,
                        onCommandTextChange = { commandInputText = it },
                        onSubmitCommand = { cmd ->
                            viewModel.processUserCommand(cmd, context)
                            commandInputText = ""
                        }
                    )
                    1 -> UrgentTasksTab(
                        tasks = tasks,
                        viewModel = viewModel
                    )
                    2 -> DailyScheduleTab(
                        schedules = schedules,
                        viewModel = viewModel
                    )
                    3 -> CallsAndContactsTab(
                        contacts = contacts,
                        viewModel = viewModel
                    )
                    4 -> MessageReaderTab(
                        messages = messages,
                        preferredLanguage = preferredLanguage,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, priority, category, dueDate ->
                viewModel.addTask(title, priority, category, dueDate)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { title, startTime, endTime, date, priority ->
                viewModel.addSchedule(title, startTime, endTime, date, priority)
                showAddScheduleDialog = false
            }
        )
    }
}

@Composable
private fun JarvisTopHeader(
    user: FirebaseUser,
    preferredLanguage: String,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = JarvisSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JARVIS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = JarvisCyan,
                        letterSpacing = 2.sp
                    )
                }
                // USER REQUIREMENT: "on the home screen write Jarvis made by Ansh Singh"
                Text(
                    text = "Jarvis made by Ansh Singh",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisGold,
                    modifier = Modifier.testTag("made_by_ansh_singh_label")
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Toggle (Hindi / English)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = JarvisSurfaceElevated,
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onToggleLanguage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = JarvisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (preferredLanguage == "hi") "हिंदी (HI)" else "English (EN)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sign out button
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = JarvisTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChipsRow(
    preferredLanguage: String,
    onChipClicked: (String) -> Unit
) {
    val isHindi = preferredLanguage == "hi"
    val chips = listOf(
        if (isHindi) "▶ Hey Jarvis, YouTube kholo" else "▶ Hey Jarvis, open YouTube",
        if (isHindi) "📞 Hey Jarvis, Rahul ko phone milao" else "📞 Hey Jarvis, call Ansh Singh",
        if (isHindi) "⚡ Hey Jarvis, urgent task add karo" else "⚡ Hey Jarvis, add urgent task",
        if (isHindi) "📅 Hey Jarvis, mera schedule banao" else "📅 Hey Jarvis, organize my schedule",
        if (isHindi) "💬 Hey Jarvis, WhatsApp message padho" else "💬 Hey Jarvis, read WhatsApp messages"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { chipText ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = JarvisSurfaceElevated,
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clickable { onChipClicked(chipText.replace("▶ ", "").replace("📞 ", "").replace("⚡ ", "").replace("📅 ", "").replace("💬 ", "")) }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = chipText,
                    fontSize = 12.sp,
                    color = JarvisTextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantVoiceTab(
    viewModel: JarvisViewModel,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    preferredLanguage: String,
    history: List<HistoryItem>,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onSubmitCommand: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isListening) 1.0f else 0.95f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Futuristic Glowing Arc Reactor Voice Core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp)
                .size(130.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isListening) JarvisCyan.copy(alpha = 0.5f) else JarvisGold.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    BorderStroke(
                        2.5.dp,
                        Brush.sweepGradient(
                            if (isListening) listOf(JarvisCyan, Color.White, JarvisCyan)
                            else listOf(JarvisCyan, JarvisGold, JarvisCyan)
                        )
                    ),
                    CircleShape
                )
                .clickable {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
                .testTag("arc_reactor_mic_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(JarvisSurfaceElevated)
                    .border(1.5.dp, JarvisCyan, CircleShape)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = JarvisCyan,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak to Jarvis",
                        tint = if (isListening) JarvisCyan else JarvisGold,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Text(
            text = if (isListening) "Listening... speak now" else if (isProcessing) "Jarvis is processing..." else "Tap to Speak (Hindi / English)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isListening) JarvisCyan else JarvisTextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Text input bar for typing or emulator testing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = onCommandTextChange,
                placeholder = {
                    Text(
                        text = if (preferredLanguage == "hi") "लिखें: YouTube kholo / phone milao..." else "Type: Open YouTube / call someone...",
                        fontSize = 12.sp,
                        color = JarvisTextSecondary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                    focusedContainerColor = JarvisSurfaceDark,
                    unfocusedContainerColor = JarvisSurfaceDark,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onSubmitCommand(commandText)
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History logs
        Text(
            text = "Activity Log & Jarvis Conversations",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = JarvisTextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = JarvisTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Say \"Hey Jarvis, YouTube kholo\" or \"is insaan ko phone milao\"",
                        fontSize = 13.sp,
                        color = JarvisTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(history) { item ->
                    HistoryItemCard(item = item, onSpeak = { viewModel.speak(it) })
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: HistoryItem, onSpeak: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You: ${item.commandText}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JarvisCyan
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = JarvisGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.actionType.replace("_", " ").uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.responseText,
                    fontSize = 13.sp,
                    color = JarvisTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onSpeak(item.responseText) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen",
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgentTasksTab(
    tasks: List<TaskItem>,
    viewModel: JarvisViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Urgent Tasks & Priorities",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
                Text(
                    text = "${tasks.count { !it.isCompleted }} pending • ${tasks.count { it.priority == "urgent" && !it.isCompleted }} urgent",
                    fontSize = 12.sp,
                    color = JarvisTextSecondary
                )
            }
            Button(
                onClick = { viewModel.prioritizeTasksWithAi() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyan,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("ai_prioritize_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Prioritize", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = JarvisTextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No tasks yet. Say \"Hey Jarvis, add urgent task...\" or tap +",
                        fontSize = 13.sp,
                        color = JarvisTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val sorted = tasks.sortedWith(
                compareBy<TaskItem> { it.isCompleted }
                    .thenByDescending { it.priority == "urgent" }
                    .thenByDescending { it.priority == "high" }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                items(sorted, key = { it.id }) { task ->
                    TaskCardItem(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, task.isCompleted) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCardItem(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority.lowercase()) {
        "urgent" -> JarvisUrgentRed
        "high" -> JarvisHighOrange
        "medium" -> JarvisGold
        else -> JarvisCyan
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) JarvisSurfaceElevated.copy(alpha = 0.6f) else JarvisSurfaceDark
        ),
        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) JarvisSuccessGreen else priorityColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) JarvisTextSecondary else JarvisTextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = task.priority.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (task.category.isNotBlank() && task.category != "general") {
                        Text(
                            text = "• ${task.category}",
                            fontSize = 11.sp,
                            color = JarvisTextSecondary
                        )
                    }
                    if (task.dueDate.isNotBlank()) {
                        Text(
                            text = "• Due: ${task.dueDate}",
                            fontSize = 11.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = JarvisTextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyScheduleTab(
    schedules: List<ScheduleItem>,
    viewModel: JarvisViewModel
) {
    val today = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Schedule Agenda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
                Text(
                    text = today,
                    fontSize = 12.sp,
                    color = JarvisTextSecondary
                )
            }
            Button(
                onClick = { viewModel.planDayWithAi() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisGold,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("plan_my_day_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Plan My Day", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = JarvisTextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No schedule blocks for today.\nTap \"Plan My Day\" to auto-organize or use voice \"schedule banao\"",
                        fontSize = 13.sp,
                        color = JarvisTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                items(schedules, key = { it.id }) { item ->
                    ScheduleCardItem(
                        item = item,
                        onToggle = { viewModel.toggleSchedule(item.id, item.isCompleted) },
                        onDelete = { viewModel.deleteSchedule(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleCardItem(
    item: ScheduleItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) JarvisSurfaceElevated.copy(alpha = 0.6f) else JarvisSurfaceDark
        ),
        border = BorderStroke(1.dp, if (item.priority == "urgent") JarvisUrgentRed.copy(alpha = 0.4f) else JarvisCyan.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = JarvisSurfaceElevated,
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                modifier = Modifier.width(76.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.startTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyan
                    )
                    if (item.endTime.isNotBlank()) {
                        Text(
                            text = item.endTime,
                            fontSize = 10.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) JarvisTextSecondary else JarvisTextPrimary,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = JarvisTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (item.isCompleted) JarvisSuccessGreen else JarvisCyan
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = JarvisTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CallsAndContactsTab(
    contacts: List<ContactItem>,
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = if (searchQuery.isBlank()) contacts else {
        contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Contacts & Voice Calling",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = JarvisTextPrimary
        )
        Text(
            text = "Say \"Hey Jarvis, [name] ko phone milao\" or tap to call directly",
            fontSize = 12.sp,
            color = JarvisTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick App Launcher Buttons (YouTube, WhatsApp, Messages)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.openApp("youtube", context) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("YouTube", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.openApp("whatsapp", context) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.openApp("messages", context) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search contact bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search contact name or number...", fontSize = 12.sp, color = JarvisTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                focusedContainerColor = JarvisSurfaceDark,
                unfocusedContainerColor = JarvisSurfaceDark,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(filteredContacts, key = { it.id }) { contact ->
                ContactCardItem(
                    contact = contact,
                    onCall = { viewModel.callContact(contact, context) }
                )
            }
        }
    }
}

@Composable
private fun ContactCardItem(
    contact: ContactItem,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan.copy(alpha = 0.15f))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisCyan
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = JarvisTextSecondary
                )
            }

            Button(
                onClick = onCall,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisSuccessGreen,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MessageReaderTab(
    messages: List<MessageItem>,
    preferredLanguage: String,
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val hasNotificationAccess = JarvisNotificationListenerService.isNotificationAccessGranted(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Message Assistant & Reader",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
                Text(
                    text = "WhatsApp & Messages Comprehension in Hindi/English",
                    fontSize = 12.sp,
                    color = JarvisTextSecondary
                )
            }

            IconButton(
                onClick = { JarvisNotificationListenerService.openNotificationAccessSettings(context) }
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Notification Settings",
                    tint = if (hasNotificationAccess) JarvisSuccessGreen else JarvisGold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Notification listener banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (hasNotificationAccess) JarvisSuccessGreen.copy(alpha = 0.12f) else JarvisGold.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (hasNotificationAccess) JarvisSuccessGreen.copy(alpha = 0.4f) else JarvisGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (hasNotificationAccess) JarvisSuccessGreen else JarvisGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasNotificationAccess) "Real-time Notification Reader Active" else "Notification Access Recommended",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasNotificationAccess) JarvisSuccessGreen else JarvisGold
                    )
                    Text(
                        text = if (hasNotificationAccess) "Jarvis automatically captures WhatsApp and SMS messages." else "Enable to intercept incoming WhatsApp notifications directly.",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                }
                if (!hasNotificationAccess) {
                    TextButton(
                        onClick = { JarvisNotificationListenerService.openNotificationAccessSettings(context) }
                    ) {
                        Text("ENABLE", fontSize = 11.sp, color = JarvisGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageCardItem(
                    message = message,
                    preferredLanguage = preferredLanguage,
                    onExplain = { viewModel.explainMessage(message) }
                )
            }
        }
    }
}

@Composable
private fun MessageCardItem(
    message: MessageItem,
    preferredLanguage: String,
    onExplain: () -> Unit
) {
    val isWhatsApp = message.appName.contains("whatsapp", ignoreCase = true)
    val appColor = if (isWhatsApp) Color(0xFF25D366) else JarvisCyan

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        border = BorderStroke(1.dp, if (message.isUrgent) JarvisUrgentRed.copy(alpha = 0.5f) else appColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = appColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = message.appName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.sender,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTextPrimary
                    )
                }

                if (message.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = JarvisUrgentRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "URGENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = JarvisUrgentRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${message.content}\"",
                fontSize = 13.sp,
                color = JarvisTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // AI Explanation in Hindi & English
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = JarvisSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Jarvis Explanation (हिंदी / EN):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisGold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (preferredLanguage == "hi") message.summaryHindi else message.summaryEnglish,
                        fontSize = 12.sp,
                        color = JarvisTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onExplain,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyan.copy(alpha = 0.2f),
                    contentColor = JarvisCyan
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (preferredLanguage == "hi") "जार्विस से यह संदेश हिंदी में सुनें" else "Listen to Jarvis Explanation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun JarvisBottomNavigation(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = JarvisSurfaceDark,
        contentColor = JarvisTextPrimary,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("Assistant", Icons.Default.SmartToy, 0),
            Triple("Tasks", Icons.Default.PriorityHigh, 1),
            Triple("Schedule", Icons.Default.CalendarMonth, 2),
            Triple("Calls", Icons.Default.Call, 3),
            Triple("Messages", Icons.Default.Message, 4)
        )

        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = activeTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = JarvisCyan,
                    indicatorColor = JarvisCyan,
                    unselectedIconColor = JarvisTextSecondary,
                    unselectedTextColor = JarvisTextSecondary
                )
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, priority: String, category: String, dueDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("urgent") }
    var category by remember { mutableStateOf("urgent") }
    var dueDate by remember { mutableStateOf("Today 5:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceDark,
        title = {
            Text("Add Prioritized Task", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title", color = JarvisTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                Text("Priority Level:", fontSize = 12.sp, color = JarvisTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("urgent", "high", "medium", "low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (p == "urgent") JarvisUrgentRed else JarvisCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date / Time", color = JarvisTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, priority, category, dueDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
            ) {
                Text("Add Task", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = JarvisTextSecondary)
            }
        }
    )
}

@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startTime: String, endTime: String, date: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("10:00 AM") }
    var endTime by remember { mutableStateOf("11:30 AM") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var priority by remember { mutableStateOf("high") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceDark,
        title = {
            Text("Add Schedule Item", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JarvisGold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event / Agenda Title", color = JarvisTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisGold,
                        unfocusedBorderColor = JarvisGold.copy(alpha = 0.3f),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time", color = JarvisTextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisGold,
                            unfocusedBorderColor = JarvisGold.copy(alpha = 0.3f),
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time", color = JarvisTextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisGold,
                            unfocusedBorderColor = JarvisGold.copy(alpha = 0.3f),
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)", color = JarvisTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisGold,
                        unfocusedBorderColor = JarvisGold.copy(alpha = 0.3f),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, startTime, endTime, date, priority)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisGold, contentColor = Color.Black)
            ) {
                Text("Add to Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = JarvisTextSecondary)
            }
        }
    )
}
