package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.model.ContactItem

object ContactsHelper {

    private val sampleContacts = listOf(
        ContactItem(id = "c1", name = "Ansh Singh", phoneNumber = "+91 98765 43210"),
        ContactItem(id = "c2", name = "Mummy", phoneNumber = "+91 98111 22334"),
        ContactItem(id = "c3", name = "Papa", phoneNumber = "+91 98222 33445"),
        ContactItem(id = "c4", name = "Rahul Sharma", phoneNumber = "+91 98333 44556"),
        ContactItem(id = "c5", name = "Boss", phoneNumber = "+91 98444 55667"),
        ContactItem(id = "c6", name = "Dr. Verma", phoneNumber = "+91 98555 66778"),
        ContactItem(id = "c7", name = "Priya", phoneNumber = "+91 98666 77889")
    )

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getContacts(context: Context): List<ContactItem> {
        if (!hasContactsPermission(context)) {
            return sampleContacts
        }

        val contactsList = mutableListOf<ContactItem>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null

                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNumber.isNotEmpty() && seenNumbers.add(cleanNumber)) {
                        contactsList.add(ContactItem(id, name, number, photoUri))
                    }
                }
            }
        } catch (e: Exception) {
            // In case of any provider errors, fallback gracefully
        }

        return if (contactsList.isEmpty()) sampleContacts else contactsList
    }

    fun findBestMatch(context: Context, spokenName: String): ContactItem? {
        val all = getContacts(context)
        val normalized = spokenName.trim().lowercase()
            .replace("ko", "")
            .replace("call", "")
            .replace("phone", "")
            .replace("milao", "")
            .replace("lagao", "")
            .trim()

        if (normalized.isEmpty()) return all.firstOrNull()

        // 1. Exact match
        all.find { it.name.lowercase() == normalized }?.let { return it }

        // 2. Starts with / Contains match
        all.find { it.name.lowercase().contains(normalized) || normalized.contains(it.name.lowercase()) }?.let { return it }

        // 3. First name match
        val firstWord = normalized.split(" ").firstOrNull() ?: ""
        if (firstWord.length >= 3) {
            all.find { it.name.lowercase().contains(firstWord) }?.let { return it }
        }

        return all.firstOrNull()
    }
}
