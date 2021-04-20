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

package com.tencent.bkrepo.common.artifact.util.http

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.StringPool.BYTES
import com.tencent.bkrepo.common.api.constant.StringPool.NO_CACHE
import com.tencent.bkrepo.common.artifact.constant.CONTENT_DISPOSITION_TEMPLATE
import com.tencent.bkrepo.common.artifact.constant.X_CHECKSUM_MD5
import com.tencent.bkrepo.common.artifact.exception.ArtifactResponseException
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.STREAM_BUFFER_SIZE
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.monitor.measureThroughput
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import org.springframework.boot.web.server.MimeMappings
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriUtils
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object ArtifactResourceWriter {

    private val mimeMappings = MimeMappings(MimeMappings.DEFAULT).apply {
        add("yaml", MediaTypes.APPLICATION_YAML)
        add("tgz", MediaTypes.APPLICATION_TGZ)
        add("ico", MediaTypes.APPLICATION_ICO)
    }

    @Throws(ArtifactResponseException::class)
    fun write(resource: ArtifactResource): Throughput {
        val request = HttpContextHolder.getRequest()
        val response = HttpContextHolder.getResponse()
        val artifact = resource.artifact
        val node = resource.node
        val range = resource.inputStream.range

        response.bufferSize = STREAM_BUFFER_SIZE
        response.characterEncoding = resource.characterEncoding
        response.contentType = resource.contentType ?: determineMediaType(artifact)
        response.status = resource.status?.value ?: resolveStatus(request)
        response.setHeader(HttpHeaders.ACCEPT_RANGES, BYTES)
        response.setHeader(HttpHeaders.CACHE_CONTROL, NO_CACHE)
        response.setHeader(HttpHeaders.CONTENT_LENGTH, resolveContentLength(range))
        response.setHeader(HttpHeaders.CONTENT_RANGE, resolveContentRange(range))
        if (resource.useDisposition) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition(artifact))
        }
        node?.let {
            response.setHeader(HttpHeaders.ETAG, resolveETag(it))
            response.setHeader(X_CHECKSUM_MD5, it.md5)
            response.setDateHeader(HttpHeaders.LAST_MODIFIED, resolveLastModified(it.lastModifiedDate))
        }
        return writeRangeStream(resource.inputStream, request, response)
    }

    private fun resolveStatus(request: HttpServletRequest): Int {
        val isRangeRequest = request.getHeader(HttpHeaders.RANGE)?.isNotBlank() ?: false
        return if (isRangeRequest) HttpStatus.PARTIAL_CONTENT.value else HttpStatus.OK.value
    }

    private fun resolveContentLength(range: Range): String {
        return range.length.toString()
    }

    private fun resolveContentRange(range: Range): String {
        return "$BYTES $range"
    }

    private fun resolveLastModified(lastModifiedDate: String): Long {
        val localDateTime = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME)
        return localDateTime.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli()
    }

    private fun writeRangeStream(
        inputStream: ArtifactInputStream,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Throughput {
        if (request.method == HttpMethod.HEAD.name) {
            return Throughput.EMPTY
        }
        try {
            return measureThroughput { inputStream.copyTo(response.outputStream, STREAM_BUFFER_SIZE) }
        } catch (exception: IOException) {
            // 不处理IOException会CglibAopProxy会抛java.lang.reflect.UndeclaredThrowableException: null
            // 由于在上面已经设置了Content-Type为application/octet-stream, spring找不到对应的Converter，导致抛
            // org.springframework.http.converter.HttpMessageNotWritableException异常，会重定向到/error页面
            // 又因为/error页面不存在，最终返回404，所以这里要对异常进行处理
            throw ArtifactResponseException(exception.message.orEmpty())
        }
    }

    private fun determineMediaType(name: String): String {
        val extension = PathUtils.resolveExtension(name)
        return mimeMappings.get(extension) ?: MediaTypes.APPLICATION_OCTET_STREAM
    }

    private fun encodeDisposition(filename: String): String {
        val encodeFilename = UriUtils.encode(filename, Charsets.UTF_8)
        return CONTENT_DISPOSITION_TEMPLATE.format(encodeFilename, encodeFilename)
    }

    private fun resolveETag(node: NodeDetail): String {
        return node.sha256!!
    }
}
