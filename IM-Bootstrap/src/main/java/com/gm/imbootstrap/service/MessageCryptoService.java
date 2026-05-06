package com.gm.imbootstrap.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts message content before persistence and decrypts it after loading from DB.
 */
@Service
public class MessageCryptoService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String ENCRYPTED_PREFIX = "ENC:v1:";
    private static final int AES_256_KEY_LENGTH = 32;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText, String aad) {
        if (!enabled() || plainText == null || isEncrypted(plainText)) {
            return plainText;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            addAad(cipher, aad);

            byte[] cipherTextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return ENCRYPTED_PREFIX
                + Base64.getEncoder().encodeToString(iv)
                + ":"
                + Base64.getEncoder().encodeToString(cipherTextWithTag);
        } catch (Exception e) {
            throw new IllegalStateException("消息内容加密失败", e);
        }
    }

    public String decrypt(String encryptedText, String aad) {
        if (!enabled() || encryptedText == null || !isEncrypted(encryptedText)) {
            return encryptedText;
        }

        try {
            String payload = encryptedText.substring(ENCRYPTED_PREFIX.length());
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("密文格式不正确");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherTextWithTag = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            addAad(cipher, aad);

            byte[] plainText = cipher.doFinal(cipherTextWithTag);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("消息内容解密失败", e);
        }
    }

    public boolean isEncrypted(String content) {
        return content != null && content.startsWith(ENCRYPTED_PREFIX);
    }

    private boolean enabled() {
        return Boolean.parseBoolean(getValue("message.crypto.enabled", "false"));
    }

    private SecretKeySpec buildKey() {
        String base64Key = getValue("message.crypto.key", null);
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("已开启消息加密，但未配置 JVM 参数: -Dmessage.crypto.key");
        }

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != AES_256_KEY_LENGTH) {
            throw new IllegalStateException("message.crypto.key 必须是 Base64 编码后的 32 字节 AES-256 密钥");
        }

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    private void addAad(Cipher cipher, String aad) {
        if (aad != null && !aad.isBlank()) {
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String getValue(String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        return defaultValue;
    }
}
