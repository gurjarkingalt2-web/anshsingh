package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ScheduleItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ScheduleRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeSchedules(): Flow<List<ScheduleItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/schedules"
        emitAll(
            db.collection("users").document(uid).collection("schedules")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ScheduleItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createScheduleItem(
        title: String,
        startTime: String,
        endTime: String = "",
        date: String,
        description: String = "",
        priority: String = "high"
    ): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/schedules"
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "title" to title.trim(),
                "startTime" to startTime.trim(),
                "endTime" to endTime.trim(),
                "date" to date.trim(),
                "description" to description.trim(),
                "priority" to priority.lowercase(),
                "isCompleted" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection("users").document(uid).collection("schedules").add(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleScheduleCompleted(scheduleId: String, currentStatus: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/schedules/$scheduleId"
        return try {
            val updates = mapOf<String, Any>(
                "isCompleted" to !currentStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("schedules").document(scheduleId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/schedules/$scheduleId"
        return try {
            db.collection("users").document(uid).collection("schedules").document(scheduleId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
