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

package com.tencent.devops.common.api.util

import org.bouncycastle.util.encoders.Hex
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object ShaUtils {

    /**
     * 适用于大数据量（文件）的流处理方式
     */
    fun sha1InputStream(inputStream: InputStream): String {
        // 指定sha1算法
        val messageDigest = MessageDigest.getInstance("SHA-1")
        DigestInputStream(inputStream, messageDigest).use { ins ->
            val buffer = ByteArray(1024 * 16) // 16KB
            var size: Int
            do {
                size = ins.read(buffer)
            } while (size > -1)
        }

        val digestBytes = messageDigest.digest()
        // 字节数组转换为 十六进制 数
        return digestBytes.toHexString()
    }

    /**
     * 注意，data过大的byteArray请使用流的方式
     */
    fun sha1(data: ByteArray): String {
        // 指定sha1算法
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(data)
        // 获取字节数组
        val messageDigest = digest.digest()

        // 字节数组转换为 十六进制 数
        return messageDigest.toHexString()
    }

    fun sha256(input: String) = MessageDigest
        .getInstance("SHA-256")
        .digest(input.toByteArray()).toHexString()

    private fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }

    fun hmacSha1(key: ByteArray, data: ByteArray): String {
        val secretKey = SecretKeySpec(key, "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(secretKey)
        val messageDigest = mac.doFinal(data)

        // 字节数组转换为 十六进制 数
        return messageDigest.toHexString()
    }

    fun isEqual(shaA: String, shaB: String): Boolean {
        return isEqual(Hex.decode(shaA), Hex.decode(shaB))
    }

    fun isEqual(shaA: ByteArray, shaB: ByteArray): Boolean {
        return MessageDigest.isEqual(shaA, shaB)
    }
}
