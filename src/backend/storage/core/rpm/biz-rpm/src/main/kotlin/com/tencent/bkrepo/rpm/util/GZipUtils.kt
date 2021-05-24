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

package com.tencent.bkrepo.rpm.util

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GZipUtils {
    /**
     * 将字节数组写入临时 gzip 压缩文件
     */
    fun ByteArray.gZip(): File {
        val file = File.createTempFile("rpm_", "_xml.gz")
        GZIPOutputStream(FileOutputStream(file)).use { it.write(this) }
        return file
    }

    /**
     * 将输入流写入临时 gzip 压缩文件
     */
    fun InputStream.gZip(): File {
        this.use {
            val file = File.createTempFile("rpm_", "_xml.gz")
            GZIPOutputStream(FileOutputStream(file)).use { gzipOutputStream ->
                var len: Int
                val buffer = ByteArray(1 * 1024 * 1024)
                while (this.read(buffer).also { len = it } > 0) {
                    gzipOutputStream.write(buffer, 0, len)
                }
                gzipOutputStream.flush()
            }
            return file
        }
    }

    /**
     * 将文件压缩为临时 gzip 文件
     */
    fun File.gZip(): File {
        return this.inputStream().gZip()
    }

    /**
     * 将 gzip 输入流解压到临时文件
     */
    fun InputStream.unGzipInputStream(): File {
        GZIPInputStream(this).use { gZIPInputStream ->
            val file = File.createTempFile("rpm_", ".xmlStream")
            BufferedOutputStream(FileOutputStream(file)).use { bufferedOutputStream ->
                var len: Int
                val buffer = ByteArray(1 * 1024 * 1024)
                while (gZIPInputStream.read(buffer).also { len = it } > 0) {
                    bufferedOutputStream.write(buffer, 0, len)
                }
                bufferedOutputStream.flush()
            }
            return file
        }
    }
}
