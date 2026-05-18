package org.flossware.jnexus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
