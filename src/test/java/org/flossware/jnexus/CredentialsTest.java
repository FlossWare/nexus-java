package org.flossware.jnexus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsTest {

    private String originalUrl;
    private String originalUser;
    private String originalPassword;
    private String originalHome;

    @BeforeEach
    void saveEnvironment() {
        originalUrl = System.getenv("NEXUS_URL");
        originalUser = System.getenv("NEXUS_USER");
        originalPassword = System.getenv("NEXUS_PASSWORD");
        originalHome = System.getProperty("user.home");
    }

    @AfterEach
    void restoreEnvironment() {
        if (originalHome != null) {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testMissingUrlThrowsException(@TempDir Path tempDir) {
        System.setProperty("user.home", tempDir.toString());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });

        assertTrue(exception.getMessage().contains("Nexus URL not configured"));
    }

    @Test
    void testMissingUserThrowsException(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile, "nexus.url=https://test.nexus.com\n");

        System.setProperty("user.home", tempDir.toString());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });

        assertTrue(exception.getMessage().contains("Nexus user not configured"));
    }

    @Test
    void testMissingPasswordThrowsException(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n"
        );

        System.setProperty("user.home", tempDir.toString());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });

        assertTrue(exception.getMessage().contains("Nexus password not configured"));
    }

    @Test
    void testLoadFromPropertiesFile(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        assertEquals("https://test.nexus.com", creds.getUrl());
        assertEquals("testuser", creds.getUser());
        assertEquals("testpass", creds.getPassword());
    }

    @Test
    void testPropertiesFileNotFoundIsHandledGracefully(@TempDir Path tempDir) {
        System.setProperty("user.home", tempDir.toString());

        assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });
    }

    @Test
    void testEmptyUrlThrowsException(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });

        assertTrue(exception.getMessage().contains("Nexus URL not configured"));
    }

    @Test
    void testBlankCredentialsThrowException(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=   \n" +
            "nexus.user=   \n" +
            "nexus.password=   \n"
        );

        System.setProperty("user.home", tempDir.toString());

        assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });
    }

    @Test
    void testMalformedPropertiesFileIsHandled(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile, "this is not valid properties format <<<");

        System.setProperty("user.home", tempDir.toString());

        assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        });
    }

    @Test
    void testGettersReturnCorrectValues(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://my-nexus.example.com\n" +
            "nexus.user=admin\n" +
            "nexus.password=secret123\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        assertNotNull(creds.getUrl());
        assertNotNull(creds.getUser());
        assertNotNull(creds.getPassword());
        assertEquals("https://my-nexus.example.com", creds.getUrl());
        assertEquals("admin", creds.getUser());
        assertEquals("secret123", creds.getPassword());
    }

    @Test
    void testHttpTimeoutDefaultValue(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        // Default timeout should be 30 seconds
        assertEquals(30, creds.getHttpTimeoutSeconds());
    }

    @Test
    void testHttpTimeoutFromProperties(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n" +
            "nexus.http.timeout.seconds=60\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        assertEquals(60, creds.getHttpTimeoutSeconds());
    }

    @Test
    void testUIDefaultsFromProperties(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n" +
            "nexus.default.repository=maven-releases\n" +
            "nexus.default.regex=.*SNAPSHOT.*\n" +
            "nexus.default.dryrun=false\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        assertEquals("maven-releases", creds.getDefaultRepository());
        assertEquals(".*SNAPSHOT.*", creds.getDefaultRegex());
        assertFalse(creds.isDefaultDryRun());
    }

    @Test
    void testRepositoriesListEmpty(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        // No repositories configured
        assertTrue(creds.getRepositories().isEmpty());
    }

    @Test
    void testRepositoriesListSingle(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n" +
            "nexus.repositories=maven-releases\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        assertEquals(1, creds.getRepositories().size());
        assertEquals("maven-releases", creds.getRepositories().get(0));
    }

    @Test
    void testRepositoriesListMultiple(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n" +
            "nexus.repositories=maven-releases,maven-snapshots,npm-public\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        assertEquals(3, creds.getRepositories().size());
        assertEquals("maven-releases", creds.getRepositories().get(0));
        assertEquals("maven-snapshots", creds.getRepositories().get(1));
        assertEquals("npm-public", creds.getRepositories().get(2));
    }

    @Test
    void testRepositoriesListWithSpaces(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n" +
            "nexus.repositories= maven-releases , maven-snapshots , npm-public \n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        // Spaces should be trimmed
        assertEquals(3, creds.getRepositories().size());
        assertEquals("maven-releases", creds.getRepositories().get(0));
        assertEquals("maven-snapshots", creds.getRepositories().get(1));
        assertEquals("npm-public", creds.getRepositories().get(2));
    }

    @Test
    void testRepositoriesListUnmodifiable(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://test.nexus.com\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n" +
            "nexus.repositories=maven-releases\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials();

        // List should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            creds.getRepositories().add("should-fail");
        });
    }

    @Test
    void testProfileLoadingDev(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus-dev.properties");
        Files.writeString(configFile,
            "nexus.url=https://dev.nexus.com\n" +
            "nexus.user=devuser\n" +
            "nexus.password=devpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials("dev");

        assertEquals("https://dev.nexus.com", creds.getUrl());
        assertEquals("devuser", creds.getUser());
        assertEquals("devpass", creds.getPassword());
        assertEquals("dev", creds.getProfile());
    }

    @Test
    void testProfileLoadingProd(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus-prod.properties");
        Files.writeString(configFile,
            "nexus.url=https://prod.nexus.com\n" +
            "nexus.user=produser\n" +
            "nexus.password=prodpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials("prod");

        assertEquals("https://prod.nexus.com", creds.getUrl());
        assertEquals("produser", creds.getUser());
        assertEquals("prodpass", creds.getPassword());
        assertEquals("prod", creds.getProfile());
    }

    @Test
    void testProfileNotFoundThrowsException(@TempDir Path tempDir) {
        Path configDir = tempDir.resolve(".flossware/nexus");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            fail("Failed to create config directory");
        }

        System.setProperty("user.home", tempDir.toString());

        // Profile file doesn't exist, should throw exception
        assertThrows(IllegalStateException.class, () -> {
            new Credentials("nonexistent");
        });
    }

    @Test
    void testDefaultProfileWhenNull(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://default.nexus.com\n" +
            "nexus.user=defaultuser\n" +
            "nexus.password=defaultpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials(null);

        assertEquals("https://default.nexus.com", creds.getUrl());
        assertEquals("defaultuser", creds.getUser());
        assertEquals("defaultpass", creds.getPassword());
        assertNull(creds.getProfile());
    }

    @Test
    void testDefaultProfileWhenEmpty(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://default.nexus.com\n" +
            "nexus.user=defaultuser\n" +
            "nexus.password=defaultpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials("");

        assertEquals("https://default.nexus.com", creds.getUrl());
        assertEquals("defaultuser", creds.getUser());
        assertEquals("defaultpass", creds.getPassword());
        assertNull(creds.getProfile());
    }

    @Test
    void testDefaultProfileWhenBlank(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=https://default.nexus.com\n" +
            "nexus.user=defaultuser\n" +
            "nexus.password=defaultpass\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials("   ");

        assertEquals("https://default.nexus.com", creds.getUrl());
        assertEquals("defaultuser", creds.getUser());
        assertEquals("defaultpass", creds.getPassword());
        assertNull(creds.getProfile());
    }

    @Test
    void testProfileWithUIDefaults(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus-staging.properties");
        Files.writeString(configFile,
            "nexus.url=https://staging.nexus.com\n" +
            "nexus.user=staginguser\n" +
            "nexus.password=stagingpass\n" +
            "nexus.default.repository=staging-releases\n" +
            "nexus.default.regex=.*RC.*\n" +
            "nexus.default.dryrun=false\n"
        );

        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials("staging");

        assertEquals("staging", creds.getProfile());
        assertEquals("staging-releases", creds.getDefaultRepository());
        assertEquals(".*RC.*", creds.getDefaultRegex());
        assertFalse(creds.isDefaultDryRun());
    }

    @Test
    void testDiscoverProfilesEmpty(@TempDir Path tempDir) {
        System.setProperty("user.home", tempDir.toString());

        java.util.List<String> profiles = Credentials.discoverProfiles();

        assertTrue(profiles.isEmpty());
    }

    @Test
    void testDiscoverProfilesDefaultOnly(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("nexus.properties"), "test=value\n");

        System.setProperty("user.home", tempDir.toString());

        java.util.List<String> profiles = Credentials.discoverProfiles();

        assertEquals(1, profiles.size());
        assertEquals("default", profiles.get(0));
    }

    @Test
    void testDiscoverProfilesMultiple(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("nexus.properties"), "test=value\n");
        Files.writeString(configDir.resolve("nexus-dev.properties"), "test=value\n");
        Files.writeString(configDir.resolve("nexus-prod.properties"), "test=value\n");

        System.setProperty("user.home", tempDir.toString());

        java.util.List<String> profiles = Credentials.discoverProfiles();

        assertEquals(3, profiles.size());
        // "default" should always be first
        assertEquals("default", profiles.get(0));
        // Others should be alphabetically sorted
        assertTrue(profiles.contains("dev"));
        assertTrue(profiles.contains("prod"));
    }

    @Test
    void testDiscoverProfilesWithoutDefault(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("nexus-staging.properties"), "test=value\n");
        Files.writeString(configDir.resolve("nexus-prod.properties"), "test=value\n");

        System.setProperty("user.home", tempDir.toString());

        java.util.List<String> profiles = Credentials.discoverProfiles();

        assertEquals(2, profiles.size());
        // Should be alphabetically sorted
        assertEquals("prod", profiles.get(0));
        assertEquals("staging", profiles.get(1));
    }

    @Test
    void testDiscoverProfilesIgnoresNonPropertyFiles(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("nexus.properties"), "test=value\n");
        Files.writeString(configDir.resolve("nexus-dev.properties"), "test=value\n");
        Files.writeString(configDir.resolve("readme.txt"), "not a properties file\n");
        Files.writeString(configDir.resolve("backup.properties"), "not a nexus file\n");

        System.setProperty("user.home", tempDir.toString());

        java.util.List<String> profiles = Credentials.discoverProfiles();

        assertEquals(2, profiles.size());
        assertEquals("default", profiles.get(0));
        assertEquals("dev", profiles.get(1));
    }

    @Test
    void testExplicitCredentialsConstructor() {
        Credentials creds = new Credentials(
            "https://nexus.example.com",
            "testuser",
            "testpass",
            "maven-releases,npm-public"
        );

        assertEquals("https://nexus.example.com", creds.getUrl());
        assertEquals("testuser", creds.getUser());
        assertEquals("testpass", creds.getPassword());
        assertEquals(2, creds.getRepositories().size());
        assertEquals("maven-releases", creds.getRepositories().get(0));
        assertEquals("npm-public", creds.getRepositories().get(1));
        assertNull(creds.getProfile());
        assertEquals("maven-releases", creds.getDefaultRepository()); // First repo becomes default
        assertEquals("", creds.getDefaultRegex());
        assertTrue(creds.isDefaultDryRun());
        assertEquals(30, creds.getHttpTimeoutSeconds());
    }

    @Test
    void testExplicitCredentialsConstructorNullUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Credentials(null, "user", "pass", "");
        });
    }

    @Test
    void testExplicitCredentialsConstructorBlankUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Credentials("  ", "user", "pass", "");
        });
    }

    @Test
    void testExplicitCredentialsConstructorNullUser() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Credentials("https://nexus.example.com", null, "pass", "");
        });
    }

    @Test
    void testExplicitCredentialsConstructorBlankUser() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Credentials("https://nexus.example.com", "  ", "pass", "");
        });
    }

    @Test
    void testExplicitCredentialsConstructorNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Credentials("https://nexus.example.com", "user", null, "");
        });
    }

    @Test
    void testExplicitCredentialsConstructorBlankPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Credentials("https://nexus.example.com", "user", "  ", "");
        });
    }

    @Test
    void testExplicitCredentialsConstructorNullRepos() {
        Credentials creds = new Credentials(
            "https://nexus.example.com",
            "testuser",
            "testpass",
            null
        );

        assertTrue(creds.getRepositories().isEmpty());
    }

    @Test
    void testExplicitCredentialsConstructorEmptyRepos() {
        Credentials creds = new Credentials(
            "https://nexus.example.com",
            "testuser",
            "testpass",
            ""
        );

        assertTrue(creds.getRepositories().isEmpty());
    }

    @Test
    void testSaveToPropertiesFileDefault(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials(
            "https://nexus.example.com",
            "testuser",
            "testpass",
            "maven-releases,npm-public"
        );

        creds.saveToPropertiesFile(null);

        Path configFile = tempDir.resolve(".flossware/nexus/nexus.properties");
        assertTrue(Files.exists(configFile));

        // Read and verify contents
        Properties props = new Properties();
        try (java.io.InputStream input = Files.newInputStream(configFile)) {
            props.load(input);
        }

        assertEquals("https://nexus.example.com", props.getProperty("nexus.url"));
        assertEquals("testuser", props.getProperty("nexus.user"));

        // Password should be encrypted
        String encryptedPassword = props.getProperty("nexus.password");
        assertTrue(CredentialEncryption.isEncrypted(encryptedPassword), "Password should be encrypted");

        // Decrypt and verify
        CredentialEncryption encryption = new CredentialEncryption();
        String decryptedPassword = encryption.decrypt(encryptedPassword);
        assertEquals("testpass", decryptedPassword, "Decrypted password should match original");

        assertEquals("maven-releases,npm-public", props.getProperty("nexus.repositories"));
        assertEquals("true", props.getProperty("nexus.default.dryrun"));
    }

    @Test
    void testSaveToPropertiesFileWithProfile(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials(
            "https://dev.nexus.example.com",
            "devuser",
            "devpass",
            "maven-snapshots"
        );

        creds.saveToPropertiesFile("dev");

        Path configFile = tempDir.resolve(".flossware/nexus/nexus-dev.properties");
        assertTrue(Files.exists(configFile));

        // Read and verify contents
        Properties props = new Properties();
        try (java.io.InputStream input = Files.newInputStream(configFile)) {
            props.load(input);
        }

        assertEquals("https://dev.nexus.example.com", props.getProperty("nexus.url"));
        assertEquals("devuser", props.getProperty("nexus.user"));

        // Password should be encrypted
        String encryptedPassword = props.getProperty("nexus.password");
        assertTrue(CredentialEncryption.isEncrypted(encryptedPassword), "Password should be encrypted");

        // Decrypt and verify
        CredentialEncryption encryption = new CredentialEncryption();
        String decryptedPassword = encryption.decrypt(encryptedPassword);
        assertEquals("devpass", decryptedPassword, "Decrypted password should match original");
        assertEquals("maven-snapshots", props.getProperty("nexus.repositories"));
    }

    @Test
    void testSaveToPropertiesFileCreatesDirectory(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials(
            "https://nexus.example.com",
            "testuser",
            "testpass",
            null
        );

        // Directory should not exist yet
        Path configDir = tempDir.resolve(".flossware/nexus");
        assertFalse(Files.exists(configDir));

        creds.saveToPropertiesFile(null);

        // Directory should now exist
        assertTrue(Files.exists(configDir));
        assertTrue(Files.isDirectory(configDir));

        // File should exist
        Path configFile = configDir.resolve("nexus.properties");
        assertTrue(Files.exists(configFile));
    }

    @Test
    void testSaveToPropertiesFileNoRepositories(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());

        Credentials creds = new Credentials(
            "https://nexus.example.com",
            "testuser",
            "testpass",
            null
        );

        creds.saveToPropertiesFile(null);

        Path configFile = tempDir.resolve(".flossware/nexus/nexus.properties");
        Properties props = new Properties();
        try (java.io.InputStream input = Files.newInputStream(configFile)) {
            props.load(input);
        }

        // Repositories should not be in the file
        assertNull(props.getProperty("nexus.repositories"));
    }

    @Test
    void testSaveToPropertiesFileOverwritesExisting(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());

        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");

        // Create an existing file
        Files.writeString(configFile, "nexus.url=https://old.nexus.com\nnexus.user=olduser\nnexus.password=oldpass\n");

        Credentials creds = new Credentials(
            "https://new.nexus.example.com",
            "newuser",
            "newpass",
            "new-repo"
        );

        creds.saveToPropertiesFile(null);

        // Read and verify new contents
        Properties props = new Properties();
        try (java.io.InputStream input = Files.newInputStream(configFile)) {
            props.load(input);
        }

        assertEquals("https://new.nexus.example.com", props.getProperty("nexus.url"));
        assertEquals("newuser", props.getProperty("nexus.user"));

        // Password should be encrypted
        String encryptedPassword = props.getProperty("nexus.password");
        assertTrue(CredentialEncryption.isEncrypted(encryptedPassword), "Password should be encrypted");

        // Decrypt and verify
        CredentialEncryption encryption = new CredentialEncryption();
        String decryptedPassword = encryption.decrypt(encryptedPassword);
        assertEquals("newpass", decryptedPassword, "Decrypted password should match original");

        assertEquals("new-repo", props.getProperty("nexus.repositories"));
    }
}
