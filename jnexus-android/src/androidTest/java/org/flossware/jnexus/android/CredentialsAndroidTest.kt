package org.flossware.jnexus.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for CredentialsAndroid.
 * Requires Android runtime to test EncryptedSharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
class CredentialsAndroidTest {

    private lateinit var context: Context
    private lateinit var credentials: CredentialsAndroid

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        credentials = CredentialsAndroid(context)
        credentials.clearAll() // Start with clean state
    }

    @After
    fun tearDown() {
        credentials.clearAll() // Clean up after tests
    }

    @Test
    fun testSaveAndGetCredentials() {
        // Save credentials
        credentials.saveCredentials(
            "https://nexus.example.com",
            "testuser",
            "testpass"
        )

        // Verify retrieval
        assertEquals("https://nexus.example.com", credentials.getUrl())
        assertEquals("testuser", credentials.getUser())
        assertEquals("testpass", credentials.getPassword())
    }

    @Test
    fun testSaveAndGetRepositories() {
        // Save as comma-separated string
        credentials.saveRepositories("maven-releases, maven-snapshots, npm-public")

        // Verify parsing
        val repos = credentials.getRepositories()
        assertEquals(3, repos.size)
        assertTrue(repos.contains("maven-releases"))
        assertTrue(repos.contains("maven-snapshots"))
        assertTrue(repos.contains("npm-public"))
    }

    @Test
    fun testSaveAndGetDefaults() {
        credentials.saveDefaults("maven-releases", ".*SNAPSHOT.*", false)

        assertEquals("maven-releases", credentials.getDefaultRepository())
        assertEquals(".*SNAPSHOT.*", credentials.getDefaultRegex())
        assertFalse(credentials.getDefaultDryRun())
    }

    @Test
    fun testSaveAndGetHttpTimeout() {
        credentials.saveHttpTimeout(60)

        assertEquals(60, credentials.getHttpTimeoutSeconds())
    }

    @Test
    fun testDefaultValues() {
        // Without saving anything, should return defaults
        assertNull(credentials.getUrl())
        assertNull(credentials.getUser())
        assertNull(credentials.getPassword())
        assertNull(credentials.getProfile())
        assertTrue(credentials.getRepositories().isEmpty())
        assertEquals("", credentials.getDefaultRepository())
        assertEquals("", credentials.getDefaultRegex())
        assertTrue(credentials.getDefaultDryRun())
        assertEquals(30, credentials.getHttpTimeoutSeconds())
    }

    @Test
    fun testHasCredentials_True() {
        credentials.saveCredentials(
            "https://nexus.example.com",
            "testuser",
            "testpass"
        )

        assertTrue(credentials.hasCredentials())
    }

    @Test
    fun testHasCredentials_False() {
        // No credentials saved
        assertFalse(credentials.hasCredentials())

        // Only URL saved
        credentials.saveCredentials("https://nexus.example.com", "", "")
        assertFalse(credentials.hasCredentials())
    }

    @Test
    fun testClearAll() {
        // Save data
        credentials.saveCredentials(
            "https://nexus.example.com",
            "testuser",
            "testpass"
        )
        credentials.saveRepositories("maven-releases")
        credentials.saveDefaults("maven-releases", ".*SNAPSHOT.*", false)

        // Verify saved
        assertTrue(credentials.hasCredentials())

        // Clear all
        credentials.clearAll()

        // Verify cleared
        assertFalse(credentials.hasCredentials())
        assertNull(credentials.getUrl())
        assertNull(credentials.getUser())
        assertNull(credentials.getPassword())
        assertTrue(credentials.getRepositories().isEmpty())
    }

    @Test
    fun testEmptyRepositoryList() {
        credentials.saveRepositories("")

        assertTrue(credentials.getRepositories().isEmpty())
    }

    @Test
    fun testRepositoryListWithSpaces() {
        credentials.saveRepositories("  maven-releases  ,  maven-snapshots  ")

        val repos = credentials.getRepositories()
        assertEquals(2, repos.size)
        assertTrue(repos.contains("maven-releases"))
        assertTrue(repos.contains("maven-snapshots"))
    }

    @Test
    fun testSingleRepository() {
        credentials.saveRepositories("maven-releases")

        val repos = credentials.getRepositories()
        assertEquals(1, repos.size)
        assertEquals("maven-releases", repos[0])
    }

    @Test
    fun testSaveProfile() {
        credentials.saveProfile("production")

        assertEquals("production", credentials.getProfile())
    }

    @Test
    fun testPersistenceAcrossInstances() {
        // Save in first instance
        credentials.saveCredentials(
            "https://nexus.example.com",
            "testuser",
            "testpass"
        )

        // Create new instance
        val credentials2 = CredentialsAndroid(context)

        // Verify data persisted
        assertEquals("https://nexus.example.com", credentials2.getUrl())
        assertEquals("testuser", credentials2.getUser())
        assertEquals("testpass", credentials2.getPassword())

        // Clean up
        credentials2.clearAll()
    }

    @Test
    fun testEncryption() {
        // Save sensitive data
        val password = "super_secret_password_12345"
        credentials.saveCredentials(
            "https://nexus.example.com",
            "testuser",
            password
        )

        // Verify retrieval (decryption happens automatically)
        assertEquals(password, credentials.getPassword())

        // Note: We can't directly verify encryption without accessing
        // the underlying SharedPreferences file, but EncryptedSharedPreferences
        // guarantees encryption is applied.
    }
}
