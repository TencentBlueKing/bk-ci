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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.storage.util

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

@Deprecated("Use ArtifactFile.sha256() or HashAlgorithm's extension function")
object FileDigestUtils {

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileSha1(inputStreamList: List<InputStream>): String {
        return digest(inputStreamList, DigestUtils.getSha1Digest())
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileMd5(file: File): String {
        return digest(listOf(file.inputStream()), DigestUtils.getMd5Digest())
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileMd5(inputStream: InputStream): String {
        return digest(listOf(inputStream), DigestUtils.getMd5Digest())
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileMd5(inputStreamList: List<InputStream>): String {
        return digest(inputStreamList, DigestUtils.getMd5Digest())
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileSha256(file: File): String {
        return digest(listOf(file.inputStream()), DigestUtils.getSha256Digest())
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileSha256(inputStream: InputStream): String {
        return digest(listOf(inputStream), DigestUtils.getSha256Digest())
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fileSha256(inputStreamList: List<InputStream>): String {
        return digest(inputStreamList, DigestUtils.getSha256Digest())
    }

    private fun digest(inputStreamList: List<InputStream>, messageDigest: MessageDigest): String {
        inputStreamList.forEach { it.use { inputStream -> DigestUtils.updateDigest(messageDigest, inputStream) } }
        return Hex.encodeHexString(messageDigest.digest())
    }
}
