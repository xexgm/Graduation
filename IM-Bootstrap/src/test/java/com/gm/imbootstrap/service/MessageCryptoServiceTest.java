package com.gm.imbootstrap.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MessageCryptoServiceTest {

    private final MessageCryptoService messageCryptoService = new MessageCryptoService();

    @AfterEach
    void tearDown() {
        System.clearProperty("message.crypto.enabled");
        System.clearProperty("message.crypto.key");
    }

    @Test
    void encryptAndDecrypt_Enabled_Success() {
        System.setProperty("message.crypto.enabled", "true");
        System.setProperty("message.crypto.key", generateKey());

        String encrypted = messageCryptoService.encrypt("hello", "private:1:2");
        String decrypted = messageCryptoService.decrypt(encrypted, "private:1:2");

        assertNotEquals("hello", encrypted);
        assertEquals("hello", decrypted);
    }

    @Test
    void decrypt_Disabled_ReturnOriginalContent() {
        String content = "plain old message";

        assertEquals(content, messageCryptoService.decrypt(content, "private:1:2"));
    }

    private String generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
