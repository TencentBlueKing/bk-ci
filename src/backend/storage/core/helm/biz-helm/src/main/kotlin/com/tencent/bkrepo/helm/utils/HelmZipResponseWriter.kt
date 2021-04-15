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

package com.tencent.bkrepo.helm.utils

import com.tencent.bkrepo.common.api.util.executeAndMeasureNanoTime
import com.tencent.bkrepo.common.artifact.constant.CONTENT_DISPOSITION_TEMPLATE
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.springframework.boot.web.server.MimeMappings
import org.springframework.http.HttpHeaders
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HelmZipResponseWriter {

    private const val NO_CACHE = "no-cache"
    private const val BUFFER_SIZE = 8 * 1024
    private const val NAME = "chart.zip"

    fun write(artifactResourceList: List<ArtifactResource>) {
        val response = HttpContextHolder.getResponse()

        response.bufferSize = BUFFER_SIZE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = MimeMappings.DEFAULT.get(PathUtils.resolveExtension(NAME).orEmpty())
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_TEMPLATE.format(NAME, NAME))
        response.setHeader(HttpHeaders.CACHE_CONTROL, NO_CACHE)

        val zos = ZipOutputStream(BufferedOutputStream(response.outputStream))
        try {
            artifactResourceList.forEach {
                zos.putNextEntry(ZipEntry(it.artifact))
                it.inputStream.use {
                    executeAndMeasureNanoTime {
                        it.copyTo(zos, response.bufferSize)
                    }
                    zos.closeEntry()
                    zos.flush()
                }
            }
            response.flushBuffer()
        } catch (exception: IOException) {
            val message = exception.message.orEmpty()
            when {
                message.contains("Connection reset by peer") -> {
                    LoggerHolder.logException(exception, "Stream response failed[Connection reset by peer]", false)
                }
                message.contains("Broken pipe") -> {
                    LoggerHolder.logException(exception, "Stream response failed[Broken pipe]", false)
                }
                else -> throw exception
            }
        } finally {
            zos.close()
        }
    }
}
