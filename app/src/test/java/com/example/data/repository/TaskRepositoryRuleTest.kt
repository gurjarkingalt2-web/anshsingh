package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(TASK_TITLE, "urgent", "call")
    }
    assertTrue("createResult should be successful", createResult.isSuccess)
    val taskId = createResult.getOrThrow()
    assertNotNull("taskId should not be null", taskId)
    assertTrue("taskId should not be empty", taskId.isNotEmpty())
  }

  @Test
  fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(TASK_TITLE, "urgent", "work").getOrThrow()
    }

    val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks().first { list -> list.any { it.id == taskId } }
    }
    assertTrue("emittedTasks should contain task", emittedTasks.any { it.id == taskId })
  }

  @Test
  fun observeTasks_unauthenticatedUser_failsWithException() = runBlocking {
    auth.signOut()
    val repository = TaskRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeTasks().first()
      }
      fail("Should have failed when unauthenticated")
    } catch (e: IllegalStateException) {
      assertTrue("Exception should indicate unauthenticated", e.message?.contains("User must be signed in") == true)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val TASK_TITLE = "Finish project urgent review"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
