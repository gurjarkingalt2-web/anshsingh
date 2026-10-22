package com.example.data

import android.content.Context
import com.example.R
import com.example.model.TaskItem
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

class TaskRepository(private val db: FirebaseFirestore) {

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

    fun observeTasks(): Flow<List<TaskItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(TaskItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createTask(
        title: String,
        priority: String = "urgent",
        category: String = "urgent",
        dueDate: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "title" to title.trim(),
                "priority" to priority.lowercase(),
                "category" to category.lowercase(),
                "dueDate" to dueDate,
                "isCompleted" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection("users").document(uid).collection("tasks").add(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(taskId: String, currentStatus: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/tasks/$taskId"
        return try {
            val updates = mapOf<String, Any>(
                "isCompleted" to !currentStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTaskPriority(taskId: String, newPriority: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/tasks/$taskId"
        return try {
            val updates = mapOf<String, Any>(
                "priority" to newPriority.lowercase(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/tasks/$taskId"
        return try {
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
