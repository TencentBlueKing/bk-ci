/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.stream

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import java.math.BigInteger
import java.security.MessageDigest

/**
 * 摘要计算监听器
 * 用于接收数据流的同时计算文件摘要，避免多次读取数据
 */
class DigestCalculateListener : StreamReceiveListener {
    private val md5Digest = MessageDigest.getInstance("MD5")
    private val sha256Digest = MessageDigest.getInstance("SHA-256")

    private var md5: String? = null
    private var sha256: String? = null

    override fun data(b: Int) {
        val v = b.toByte()
        md5Digest.update(v)
        sha256Digest.update(v)
    }

    override fun data(buffer: ByteArray, offset: Int, length: Int) {
        md5Digest.update(buffer, offset, length)
        sha256Digest.update(buffer, offset, length)
    }

    override fun finished() {
        md5 = hexToString(md5Digest.digest(), MD5_LENGTH)
        sha256 = hexToString(sha256Digest.digest(), SHA256_LENGTH)
    }

    fun getMd5(): String {
        return md5 ?: throw ErrorCodeException(ArtifactMessageCode.ARTIFACT_RECEIVE_FAILED)
    }

    fun getSha256(): String {
        return sha256 ?: throw ErrorCodeException(ArtifactMessageCode.ARTIFACT_RECEIVE_FAILED)
    }

    private fun hexToString(byteArray: ByteArray, length: Int): String {
        val hashInt = BigInteger(1, byteArray)
        val hashText = hashInt.toString(HASH_RADIX)
        return if (hashText.length < length) "0".repeat(length - hashText.length) + hashText else hashText
    }

    companion object {
        private const val HASH_RADIX = 16
        private const val MD5_LENGTH = 32
        private const val SHA256_LENGTH = 64
    }
}
