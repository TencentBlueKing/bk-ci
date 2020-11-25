package com.tencent.devops.sign.utils

import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.Arrays
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加解密工具.
 *
 * @author ianqu
 * @date 2019-06-12 11:14
 */
object EncryptUtil {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val ALGORITHM_SORT = "AES"

    /**
     * AES 加密.
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(original: ByteArray?, key: String): ByteArray {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val iv = Arrays.copyOf(keyBytes, 16)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, ALGORITHM_SORT), IvParameterSpec(iv))
        return cipher.doFinal(original)
    }

    /**
     * AES 加密.
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(originalContent: String, key: String): String {
        val original = originalContent.toByteArray(StandardCharsets.UTF_8)
        val encrypted = encrypt(original, key)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * AES 解密.
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(encrypted: ByteArray?, key: String): ByteArray {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val iv = Arrays.copyOf(keyBytes, 16)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, ALGORITHM_SORT), IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

    /**
     * AES 解密.
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(encryptedContent: String?, key: String): String {
        val encrypted = Base64.getDecoder().decode(encryptedContent)
        val decrypted = decrypt(encrypted, key)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    /**
     * AES 解密.
     */
    @Throws(GeneralSecurityException::class)
    fun decryptToBytes(encryptedContent: String?, key: String): ByteArray {
        val encrypted = Base64.getDecoder().decode(encryptedContent)
        return decrypt(encrypted, key)
    }
}