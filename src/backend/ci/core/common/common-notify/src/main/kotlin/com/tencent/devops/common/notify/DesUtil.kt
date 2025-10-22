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

package com.tencent.devops.common.notify

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec

object DesUtil {

    fun encrypt(message: String, key: String): ByteArray {
        // 密钥只能是8位
        val newKey = getKey(key)
        val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")

        val desKeySpec = DESKeySpec(newKey.toByteArray(charset("UTF-8")))

        val keyFactory = SecretKeyFactory.getInstance("DES")
        val secretKey = keyFactory.generateSecret(desKeySpec)
        val iv = IvParameterSpec(newKey.toByteArray(charset("UTF-8")))
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

        return cipher.doFinal(message.toByteArray(charset("UTF-8")))
    }

    fun toHexString(b: ByteArray): String {
        val hexString = StringBuilder()
        for (aB in b) {
            var plainText = Integer.toHexString(0xff and aB.toInt())
            if (plainText.length < 2)
                plainText = "0" + plainText
            hexString.append(plainText)
        }

        return hexString.toString()
    }

    private fun getKey(key: String): String {
        val tmp = "--------"
        return (key + tmp).substring(0, 8)
    }
}
