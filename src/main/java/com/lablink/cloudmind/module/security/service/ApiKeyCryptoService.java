package com.lablink.cloudmind.module.security.service;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.security.config.CredentialSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加密服务。
 *
 * <p>使用 AES-GCM 加密用户 LLM API Key。</p>
 */
@Service
@RequiredArgsConstructor
public class ApiKeyCryptoService {

    private static final String AES = "AES";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BIT = 128;

    private final CredentialSecurityProperties properties;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "API Key不能为空");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(buildAesKey(), AES),
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv)
            );

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "API Key加密失败");
        }
    }

    public String decrypt(String cipherTextBase64) {
        if (!StringUtils.hasText(cipherTextBase64)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "加密API Key不能为空");
        }

        try {
            byte[] allBytes = Base64.getDecoder().decode(cipherTextBase64);

            ByteBuffer buffer = ByteBuffer.wrap(allBytes);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(buildAesKey(), AES),
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv)
            );

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "API Key解密失败");
        }
    }

    private byte[] buildAesKey() throws Exception {
        String secret = properties.getCredentialSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "凭证加密密钥未配置");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
    }
}