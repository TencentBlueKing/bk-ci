/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.api.digest.enc

import com.tencent.devops.common.api.exception.EncryptException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

/**
 * 默认实现
 */
@DigestPriority("SecurityUtil", priority = 1)
open class AESSecurityDigest(private val aesKey: String = "b*nK#3%t") : SecurityDigest {

    private val secretKeySpec: SecretKeySpec

    companion object {
        private const val UTF8 = "UTF-8"
        private const val AES = "AES"
        private const val ALGORITHM_PATTERN_COMPLEMENT = "AES/ECB/PKCS5Padding" // 算法/模式/补码方式
        private const val SEED = 128
    }

    init {
        secretKeySpec = generateSecretKeySpec(aesKey)
    }

    private fun generateSecretKeySpec(key: String): SecretKeySpec {
        val keyGenerator: KeyGenerator
        val secureRandom: SecureRandom
        try {
            keyGenerator = KeyGenerator.getInstance(AES)
            secureRandom = SecureRandom.getInstance("SHA1PRNG")
            secureRandom.setSeed(key.toByteArray(charset(UTF8)))
        } catch (ignored: Exception) {
            throw EncryptException(ignored.message, ignored)
        }

        keyGenerator.init(SEED, secureRandom)
        val secretKey = keyGenerator.generateKey()
        val encoded = secretKey.encoded
        return SecretKeySpec(encoded, AES)
    }

    override fun encrypt(content: String): String = encrypt(key = aesKey, content = content)

    override fun decrypt(encryptString: String): String = decrypt(key = aesKey, encryptString = encryptString)

    override fun encrypt(key: String, content: String): String =
        Base64.getEncoder().encodeToString(encrypt(key = key, bytes = content.toByteArray(charset(UTF8))))

    override fun decrypt(key: String, encryptString: String): String =
        decrypt(key = key, encryptBytes = Base64.getDecoder().decode(encryptString)).toString(charset(UTF8))

    override fun encrypt(key: String, bytes: ByteArray): ByteArray {
        val secretKeySpec = generateSecretKeySpec(key)
        val cipher = Cipher.getInstance(ALGORITHM_PATTERN_COMPLEMENT)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        return cipher.doFinal(bytes)
    }

    override fun decrypt(key: String, encryptBytes: ByteArray): ByteArray {
        val secretKeySpec = generateSecretKeySpec(key)
        val cipher = Cipher.getInstance(ALGORITHM_PATTERN_COMPLEMENT)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        return cipher.doFinal(encryptBytes)
    }
}
