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

package com.tencent.bkrepo.common.artifact.api

import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.hash.sha256
import java.io.File
import java.nio.file.Files

/**
 * 用系统文件接口实现的ArtifactFile
 */
class FileSystemArtifactFile(private val file: File) : ArtifactFile {

    private var md5: String? = null
    private var sha1: String? = null
    private var sha256: String? = null

    override fun getInputStream() = file.inputStream()

    override fun getSize() = file.length()

    override fun isInMemory() = false

    override fun getFile() = file

    override fun flushToFile() = file

    override fun isFallback() = false

    override fun getFileMd5(): String {
        return md5 ?: run { file.md5().apply { md5 = this } }
    }

    override fun getFileSha1(): String {
        return sha1 ?: run { file.sha1().apply { sha1 = this } }
    }

    override fun getFileSha256(): String {
        return sha256 ?: run { file.sha256().apply { sha256 = this } }
    }

    override fun delete() {
        Files.deleteIfExists(file.toPath())
    }

    override fun hasInitialized(): Boolean {
        return true
    }
}

fun File.toArtifactFile(): ArtifactFile {
    return FileSystemArtifactFile(this)
}
