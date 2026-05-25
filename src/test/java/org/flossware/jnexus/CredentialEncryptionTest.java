package org.flossware.jnexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CredentialEncryption.
 */
class CredentialEncryptionTest {

    private CredentialEncryption encryption;

    @BeforeEach
    void setUp() {
        encryption = new CredentialEncryption();
    }

    // ========== Encryption/Decryption Tests ==========

    @Test
    void testEncryptDecryptRoundTrip() {
        String plaintext = "my-secret-password";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Decrypted value should match original");
    }

    @Test
    void testEncryptedValueIsDifferent() {
        String plaintext = "my-secret-password";

        String encrypted = encryption.encrypt(plaintext);

        assertNotEquals(plaintext, encrypted, "Encrypted value should be different from plaintext");
    }

    @Test
    void testEncryptedValueIsBase64() {
        String plaintext = "test-password";

        String encrypted = encryption.encrypt(plaintext);

        // Base64 should decode without exception
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encrypted));
    }

    @Test
    void testEncryptedValueIsLongerThanPlaintext() {
        String plaintext = "short";

        String encrypted = encryption.encrypt(plaintext);

        // Encrypted value includes IV (12 bytes) + tag (16 bytes) + ciphertext
        // Base64 encoded should be at least 40 characters
        assertTrue(encrypted.length() > 32, "Encrypted value should be longer than plaintext");
    }

    @Test
    void testEncryptGeneratesDifferentIVs() {
        String plaintext = "same-password";

        String encrypted1 = encryption.encrypt(plaintext);
        String encrypted2 = encryption.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2, "Same plaintext should produce different encrypted values (random IV)");

        // Both should decrypt to same value
        assertEquals(plaintext, encryption.decrypt(encrypted1));
        assertEquals(plaintext, encryption.decrypt(encrypted2));
    }

    @Test
    void testEncryptSpecialCharacters() {
        String plaintext = "p@ss:w0rd!#$%^&*(){}[]|\\<>?/~`";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Special characters should be preserved");
    }

    @Test
    void testEncryptUnicodeCharacters() {
        String plaintext = "パスワード-密码-مرور";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Unicode characters should be preserved");
    }

    @Test
    void testEncryptLongPassword() {
        String plaintext = "a".repeat(1000); // 1000 character password

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Long passwords should be handled correctly");
    }

    @Test
    void testEncryptWhitespace() {
        String plaintext = "  password with spaces  ";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Whitespace should be preserved");
    }

    // ========== Error Handling Tests ==========

    @Test
    void testEncryptNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            encryption.encrypt(null),
            "Encrypting null should throw"
        );
    }

    @Test
    void testEncryptEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            encryption.encrypt(""),
            "Encrypting empty string should throw"
        );
    }

    @Test
    void testDecryptNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            encryption.decrypt(null),
            "Decrypting null should throw"
        );
    }

    @Test
    void testDecryptEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            encryption.decrypt(""),
            "Decrypting empty string should throw"
        );
    }

    @Test
    void testDecryptInvalidBase64Throws() {
        String invalidBase64 = "not-valid-base64!!!";

        assertThrows(IllegalStateException.class, () ->
            encryption.decrypt(invalidBase64),
            "Invalid base64 should throw"
        );
    }

    @Test
    void testDecryptCorruptedCiphertextThrows() {
        // Encrypt valid value
        String encrypted = encryption.encrypt("test");

        // Corrupt it by changing a character
        String corrupted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        assertThrows(IllegalStateException.class, () ->
            encryption.decrypt(corrupted),
            "Corrupted ciphertext should throw"
        );
    }

    @Test
    void testDecryptWrongLengthThrows() {
        // Too short to contain IV + tag + ciphertext
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[10]);

        assertThrows(IllegalStateException.class, () ->
            encryption.decrypt(tooShort),
            "Too-short ciphertext should throw"
        );
    }

    // ========== isEncrypted() Tests ==========

    @Test
    void testIsEncryptedWithValidEncryptedValue() {
        String encrypted = encryption.encrypt("test-password");

        assertTrue(CredentialEncryption.isEncrypted(encrypted),
            "Valid encrypted value should be detected as encrypted");
    }

    @Test
    void testIsEncryptedWithPlaintextPassword() {
        String plaintext = "my-plaintext-password";

        assertFalse(CredentialEncryption.isEncrypted(plaintext),
            "Plaintext should not be detected as encrypted");
    }

    @Test
    void testIsEncryptedWithNull() {
        assertFalse(CredentialEncryption.isEncrypted(null),
            "Null should not be detected as encrypted");
    }

    @Test
    void testIsEncryptedWithEmpty() {
        assertFalse(CredentialEncryption.isEncrypted(""),
            "Empty string should not be detected as encrypted");
    }

    @Test
    void testIsEncryptedWithShortString() {
        String shortString = "abc123";

        assertFalse(CredentialEncryption.isEncrypted(shortString),
            "Short strings should not be detected as encrypted");
    }

    @Test
    void testIsEncryptedWithNonBase64() {
        String nonBase64 = "this-contains-invalid-base64-characters!!!";

        assertFalse(CredentialEncryption.isEncrypted(nonBase64),
            "Non-base64 strings should not be detected as encrypted");
    }

    @Test
    void testIsEncryptedWithValidBase64ButNotEncrypted() {
        // Valid base64 but not actually encrypted by us (make it long enough)
        byte[] randomData = new byte[40]; // Enough to exceed 32 chars in base64
        java.util.Arrays.fill(randomData, (byte) 0xFF);
        String validBase64 = java.util.Base64.getEncoder().encodeToString(randomData);

        // Should be detected as encrypted (it's valid base64 and long enough)
        // Trying to decrypt it would fail, but isEncrypted only checks format
        assertTrue(CredentialEncryption.isEncrypted(validBase64),
            "Valid base64 should be detected as potentially encrypted");
    }

    // ========== Machine-Specific Key Tests ==========

    @Test
    void testSameInstanceCanDecrypt() {
        // Same instance should be able to decrypt its own encrypted values
        CredentialEncryption enc = new CredentialEncryption();

        String plaintext = "test-password";
        String encrypted = enc.encrypt(plaintext);
        String decrypted = enc.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void testDifferentInstanceSameMachineCanDecrypt() {
        // Different instances on same machine should be able to decrypt
        // (key is derived from machine-specific data)
        CredentialEncryption enc1 = new CredentialEncryption();
        CredentialEncryption enc2 = new CredentialEncryption();

        String plaintext = "test-password";
        String encrypted = enc1.encrypt(plaintext);
        String decrypted = enc2.decrypt(encrypted);

        assertEquals(plaintext, decrypted,
            "Different instances on same machine should use same key");
    }

    // ========== Edge Cases ==========

    @Test
    void testEncryptSingleCharacter() {
        String plaintext = "a";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptNumbersOnly() {
        String plaintext = "1234567890";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptNewlines() {
        String plaintext = "line1\nline2\nline3";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptControlCharacters() {
        String plaintext = "test ";

        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }
}
