/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.stream.binder.file

import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

internal object StreamFileUtils {

    private val fileSync = ConcurrentHashMap<String, Any>()

    fun getDestinationFile(folder: String, destination: String): String {
        val fileName = destination.toLowerCase() + ".stream"
        return Paths.get(folder, fileName).toString()
    }

    fun truncateFile(file: String) {
        val sync = fileSync.getOrPut(file) { Any() }
        try {
            // 防止 producer 和 consumer 在同一个进程
            synchronized(sync) {
                RandomAccessFile(file, "rw").use {
                    val lock = it.channel.lock(0L, Long.MAX_VALUE, true)
                    try {
                        it.setLength(0)
                    } finally {
                        lock.release()
                    }
                }
            }
        } catch (ignored: FileNotFoundException) {
            File(file).createNewFile()
            return truncateFile(file)
        }
    }
}
