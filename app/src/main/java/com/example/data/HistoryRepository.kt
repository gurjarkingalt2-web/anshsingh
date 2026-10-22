package com.example.data

import android.content.Context
import com.example.R
import com.example.model.HistoryItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HistoryRepository(private val db: FirebaseFirestore) {

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

    fun observeRecentHistory(limit: Long = 25): Flow<List<HistoryItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/history"
        emitAll(
            db.collection("users").document(uid).collection("history")
                .limit(limit)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(HistoryItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordHistory(
        commandText: String,
        responseText: String,
        language: String,
        actionType: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/history"
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "commandText" to commandText.trim(),
                "responseText" to responseText.trim(),
                "language" to language.lowercase(),
                "actionType" to actionType.lowercase(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection("users").document(uid).collection("history").add(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
