package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ContactNotificationMethod
import com.example.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SafetyRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testCreateAndReadProfile_authenticated() = runBlocking {
    val uid = signInTestUser("alice@test.com")
    val repo = SafetyRepository(firestore)

    repo.setAuthenticatedUser(uid, "Alice Vance", "alice@test.com")
    val profile = UserProfile(id = uid, name = "Alice Vance", email = "alice@test.com")
    repo.updateProfile(profile)

    val fetched = repo.fetchUserProfileDirect(uid)
    assertNotNull(fetched)
    assertEquals("Alice Vance", fetched?.name)
  }

  @Test
  fun testAddAndObserveEmergencyContacts() = runBlocking {
    val uid = signInTestUser("bob@test.com")
    val repo = SafetyRepository(firestore)
    repo.setAuthenticatedUser(uid, "Bob Guardian", "bob@test.com")

    val success = repo.addContact("Sarah Vance", "+15551234567", "sarah@test.com", ContactNotificationMethod.SMS_AND_CALL)
    assertTrue(success)

    val contacts = repo.fetchContactsDirect(uid)
    assertEquals(1, contacts.size)
    assertEquals("Sarah Vance", contacts[0].name)
  }

  @Test
  fun testUnauthenticatedAccessFails() = runBlocking {
    val repo = SafetyRepository(firestore)
    auth.signOut()

    try {
      repo.requireUserId()
      fail("Should throw IllegalStateException when unauthenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("User must be signed in") == true)
    }
  }
}
