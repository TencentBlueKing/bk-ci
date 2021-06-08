/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
