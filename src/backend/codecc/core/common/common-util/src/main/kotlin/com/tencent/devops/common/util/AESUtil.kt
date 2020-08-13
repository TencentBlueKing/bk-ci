/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.util

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import java.util.*
import javax.crypto.KeyGenerator

object AESUtil {
    private val UTF8 = "UTF-8"
    private val AES = "AES"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private fun generateKeyParameter(key: String): KeyParameter {
        val keyGenerator: KeyGenerator
        val secureRandom: SecureRandom
        try {
            keyGenerator = KeyGenerator.getInstance(AES)
            secureRandom = SecureRandom.getInstance("SHA1PRNG")
            secureRandom.setSeed(key.toByteArray(charset(UTF8)))
        } catch (e: Exception) {
            throw RuntimeException(e.message, e)
        }

        keyGenerator.init(256, secureRandom)
        val secretKey = keyGenerator.generateKey()
        val encoded = secretKey.encoded
        return KeyParameter(encoded)
    }

    private fun processData(encrypt: Boolean, keyParameter: KeyParameter, bytes: ByteArray): ByteArray {
        val blockCipherPadding = PKCS7Padding()
        val blockCipher = CBCBlockCipher(AESEngine())
        val paddedBufferedBlockCipher = PaddedBufferedBlockCipher(blockCipher, blockCipherPadding)
        paddedBufferedBlockCipher.init(encrypt, keyParameter)

        val output = ByteArray(paddedBufferedBlockCipher.getOutputSize(bytes.size))
        val offset = paddedBufferedBlockCipher.processBytes(bytes, 0, bytes.size, output, 0)
        val outputLength = paddedBufferedBlockCipher.doFinal(output, offset)
        return Arrays.copyOf(output, offset + outputLength)
    }

    fun encrypt(key: String, content: String): String {
        val bytes = content.toByteArray(charset(UTF8))
        val keyParameter = generateKeyParameter(key)
        val output = processData(true, keyParameter, bytes)
        return Base64.getEncoder().encodeToString(output)
    }

    fun decrypt(key: String, content: String): String {
        val bytes = Base64.getDecoder().decode(content)
        val keyParameter = generateKeyParameter(key)
        val output = processData(false, keyParameter, bytes)
        return output.toString(charset(UTF8))
    }

    fun encrypt(key: String, content: ByteArray): ByteArray {
        val keyParameter = generateKeyParameter(key)
        return processData(true, keyParameter, content)
    }

    fun decrypt(key: String, content: ByteArray): ByteArray {
        val keyParameter = generateKeyParameter(key)
        return processData(false, keyParameter, content)
    }
}
