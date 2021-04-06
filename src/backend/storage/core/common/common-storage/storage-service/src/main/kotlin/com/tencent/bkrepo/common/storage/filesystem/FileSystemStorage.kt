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

package com.tencent.bkrepo.common.storage.filesystem

import com.tencent.bkrepo.common.api.constant.StringPool.TEMP
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.bound
import com.tencent.bkrepo.common.storage.core.AbstractFileStorage
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import java.io.File
import java.io.InputStream
import java.nio.file.Paths

/**
 * 文件系统存储
 */
open class FileSystemStorage : AbstractFileStorage<FileSystemCredentials, FileSystemClient>() {

    override fun store(path: String, name: String, file: File, client: FileSystemClient) {
        file.inputStream().use {
            client.store(path, name, it, file.length())
        }
    }

    override fun store(path: String, name: String, inputStream: InputStream, size: Long, client: FileSystemClient) {
        inputStream.use {
            client.store(path, name, it, size)
        }
    }

    override fun load(path: String, name: String, range: Range, client: FileSystemClient): InputStream? {
        return client.load(path, name)?.bound(range)
    }

    override fun delete(path: String, name: String, client: FileSystemClient) {
        client.delete(path, name)
    }

    override fun exist(path: String, name: String, client: FileSystemClient): Boolean {
        return client.exist(path, name)
    }

    override fun onCreateClient(credentials: FileSystemCredentials) = FileSystemClient(credentials.path)

    override fun getTempPath(storageCredentials: StorageCredentials): String {
        require(storageCredentials is FileSystemCredentials)
        return Paths.get(storageCredentials.path, TEMP).toString()
    }
}
