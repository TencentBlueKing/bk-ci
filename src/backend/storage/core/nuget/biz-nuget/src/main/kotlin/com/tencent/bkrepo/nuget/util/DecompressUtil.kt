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

package com.tencent.bkrepo.nuget.util

import com.tencent.bkrepo.common.api.util.readXmlString
import com.tencent.bkrepo.nuget.model.nuspec.NuspecPackage
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.InputStream
import org.apache.commons.compress.archivers.ArchiveEntry

object DecompressUtil {

    private const val BUFFER_SIZE = 2048

    private const val NUSPEC = ".nuspec"

    fun InputStream.resolverNuspec(): NuspecPackage {
        val nuspecContent = getNuspec(ZipArchiveInputStream(this))
        return nuspecContent.readXmlString()
    }

    /**
     * 获取nupkg 压缩中的'.nuspec'文件
     * [archiveInputStream] 压缩文件流
     * return 以字符串格式返回.nuspec 文件内容
     */
    private fun getNuspec(archiveInputStream: ArchiveInputStream): String {
        val stringBuilder = StringBuffer("")
        var zipEntry: ArchiveEntry
        loop@while (archiveInputStream.nextEntry.also { zipEntry = it } != null) {
            if ((!zipEntry.isDirectory) && zipEntry.name.endsWith(NUSPEC)) {
                var length: Int
                val bytes = ByteArray(BUFFER_SIZE)
                while ((archiveInputStream.read(bytes).also { length = it }) != -1) {
                    stringBuilder.append(String(bytes, 0, length))
                }
                break@loop
            }
        }
        return stringBuilder.toString()
    }
}
