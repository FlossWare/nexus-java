package org.flossware.jnexus;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Provides AES-256-GCM encryption for credential storage.
 * <p>
 * Uses machine-specific salt derived from hostname and user home directory
 * to generate encryption keys via PBKDF2. Encrypted values are stored with
 * a random IV prepended and base64 encoded.
 * </p>
 * <p>
 * This implementation mirrors the Android EncryptedSharedPreferences approach
 * but adapted for desktop Java environments.
 * </p>
 *
 * <h2>Security Properties:</h2>
 * <ul>
 *   <li>AES-256-GCM authenticated encryption</li>
 *   <li>PBKDF2 key derivation with 100,000 iterations</li>
 *   <li>Random IV per encryption (prepended to ciphertext)</li>
 *   <li>Machine-specific key derivation (credentials tied to machine)</li>
 *   <li>Base64 encoding for storage in properties files</li>
 * </ul>
 *
 * <h2>Limitations:</h2>
 * <ul>
 *   <li>Credentials encrypted on one machine cannot be decrypted on another</li>
 *   <li>Changing hostname or user home directory invalidates encrypted credentials</li>
 *   <li>Not suitable for sharing credentials across machines</li>
 * </ul>
 *
 * @author sfloess
 * @since 1.30
 */
public class CredentialEncryption {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // 96 bits recommended for GCM
    private static final int TAG_SIZE = 128; // 128 bits authentication tag
    private static final int PBKDF2_ITERATIONS = 100_000;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Creates a new CredentialEncryption instance with a machine-specific key.
     *
     * @throws IllegalStateException if encryption initialization fails
     */
    public CredentialEncryption() {
        try {
            this.secureRandom = new SecureRandom();
            this.secretKey = generateMachineSpecificKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize credential encryption", e);
        }
    }

    /**
     * Encrypts a plaintext value using AES-256-GCM.
     * <p>
     * Generates a random IV, encrypts the plaintext, and returns the IV + ciphertext
     * as a base64-encoded string.
     * </p>
     *
     * @param plaintext the value to encrypt
     * @return base64-encoded IV + ciphertext
     * @throws IllegalStateException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Cannot encrypt null or empty plaintext");
        }

        try {
            // Generate random IV
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
                .put(iv)
                .put(ciphertext)
                .array();

            // Base64 encode
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a base64-encoded ciphertext using AES-256-GCM.
     * <p>
     * Extracts the IV from the beginning of the ciphertext and decrypts
     * using AES-256-GCM.
     * </p>
     *
     * @param encryptedValue base64-encoded IV + ciphertext
     * @return decrypted plaintext
     * @throws IllegalStateException if decryption fails
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            throw new IllegalArgumentException("Cannot decrypt null or empty value");
        }

        try {
            // Base64 decode
            byte[] combined = Base64.getDecoder().decode(encryptedValue);

            // Extract IV
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);

            // Extract ciphertext
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed - credentials may be corrupted or from a different machine", e);
        }
    }

    /**
     * Generates a machine-specific encryption key using PBKDF2.
     * <p>
     * Derives a key from the hostname and user home directory path,
     * making the key unique to this machine and user.
     * </p>
     *
     * @return machine-specific secret key
     * @throws Exception if key generation fails
     */
    private SecretKey generateMachineSpecificKey() throws Exception {
        // Generate machine-specific salt from hostname and user.home
        String hostname = InetAddress.getLocalHost().getHostName();
        String userHome = System.getProperty("user.home");
        String machineSalt = hostname + ":" + userHome;

        // Derive key using PBKDF2
        String password = "jnexus-credential-encryption"; // Fixed password for key derivation
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            machineSalt.getBytes(StandardCharsets.UTF_8),
            PBKDF2_ITERATIONS,
            KEY_SIZE
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Checks if a value is encrypted (starts with base64 characters).
     * <p>
     * This is a heuristic check - not 100% reliable, but sufficient for
     * distinguishing encrypted values from typical plaintext passwords.
     * </p>
     *
     * @param value the value to check
     * @return true if value appears to be encrypted
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Encrypted values are base64 and should be at least 32 characters
        // (12 byte IV + 16 byte tag + some ciphertext = ~40+ base64 chars)
        if (value.length() < 32) {
            return false;
        }

        // Check if valid base64
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
