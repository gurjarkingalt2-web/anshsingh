package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import com.example.model.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _messagesFlow = MutableStateFlow<List<MessageItem>>(getInitialSampleMessages())
        val messagesFlow: StateFlow<List<MessageItem>> = _messagesFlow.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).toTypedArray()
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(packageName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun injectSampleMessage(message: MessageItem) {
            val current = _messagesFlow.value.toMutableList()
            current.add(0, message)
            _messagesFlow.value = current
        }

        private fun getInitialSampleMessages(): List<MessageItem> {
            return listOf(
                MessageItem(
                    id = "msg1",
                    sender = "Boss (Office)",
                    appName = "WhatsApp",
                    content = "Rahul, submit the Q4 financial report by 3:00 PM today. Very urgent!",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                    summaryHindi = "बॉस का सन्देश: आज दोपहर 3 बजे तक Q4 वित्तीय रिपोर्ट जमा करनी है, यह बहुत ज़रूरी है।",
                    summaryEnglish = "Message from Boss: Submit Q4 financial report by 3:00 PM today. Marked as high priority.",
                    isUrgent = true
                ),
                MessageItem(
                    id = "msg2",
                    sender = "Mummy",
                    appName = "WhatsApp",
                    content = "Ghar aate waqt dahi aur medicines lete aana beta.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                    summaryHindi = "मम्मी का सन्देश: घर आते समय दही और दवाइयाँ लेते आना।",
                    summaryEnglish = "Message from Mummy: Pick up curd and medicines on the way back home.",
                    isUrgent = false
                ),
                MessageItem(
                    id = "msg3",
                    sender = "Ansh Singh",
                    appName = "Messages",
                    content = "Hey! Jarvis project demo is ready for testing. Check the latest features.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 90,
                    summaryHindi = "अंश सिंह का सन्देश: जार्विस प्रोजेक्ट डेमो टेस्टिंग के लिए तैयार है, नए फीचर्स देखें।",
                    summaryEnglish = "Message from Ansh Singh: Jarvis project demo is ready for testing, please check latest features.",
                    isUrgent = false
                ),
                MessageItem(
                    id = "msg4",
                    sender = "Dr. Verma",
                    appName = "WhatsApp",
                    content = "Your dental appointment is scheduled for tomorrow at 11:30 AM.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 180,
                    summaryHindi = "डॉ. वर्मा का सन्देश: आपकी डेंटल अपॉइंटमेंट कल सुबह 11:30 बजे निर्धारित है।",
                    summaryEnglish = "Message from Dr. Verma: Dental checkup appointment scheduled for tomorrow at 11:30 AM.",
                    isUrgent = true
                )
            )
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification.extras ?: return

        val appName = when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("messaging") || packageName.contains("mms") || packageName.contains("sms") -> "Messages"
            packageName.contains("telegram") -> "Telegram"
            else -> return // Only track messaging apps
        }

        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Contact"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return

        if (text.isBlank()) return

        val isUrgent = text.contains("urgent", ignoreCase = true) ||
                text.contains("jaldi", ignoreCase = true) ||
                text.contains("turant", ignoreCase = true) ||
                text.contains("important", ignoreCase = true) ||
                text.contains("emergency", ignoreCase = true)

        val summaryHindi = "संदेश प्रेषक $title ($appName): $text"
        val summaryEnglish = "Message from $title via $appName: $text"

        val item = MessageItem(
            id = UUID.randomUUID().toString(),
            sender = title,
            appName = appName,
            content = text,
            timestamp = System.currentTimeMillis(),
            summaryHindi = summaryHindi,
            summaryEnglish = summaryEnglish,
            isUrgent = isUrgent
        )

        injectSampleMessage(item)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
