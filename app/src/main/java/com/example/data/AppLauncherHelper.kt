package com.example.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

object AppLauncherHelper {

    fun openYouTube(context: Context, query: String? = null): Boolean {
        return try {
            val intent = if (!query.isNullOrBlank()) {
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(if (!query.isNullOrBlank()) "https://www.youtube.com/results?search_query=${Uri.encode(query)}" else "https://www.youtube.com")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String? = null): Boolean {
        return try {
            if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openMessages(context: Context, phoneNumber: String? = null): Boolean {
        return try {
            val uri = if (!phoneNumber.isNullOrBlank()) Uri.parse("smsto:$phoneNumber") else Uri.parse("sms:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun makeCall(context: Context, phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) return false

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }
}
