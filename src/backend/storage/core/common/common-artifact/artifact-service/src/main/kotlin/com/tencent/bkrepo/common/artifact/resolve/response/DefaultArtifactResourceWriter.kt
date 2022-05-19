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

package com.tencent.bkrepo.common.artifact.resolve.response

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.constant.CONTENT_DISPOSITION_TEMPLATE
import com.tencent.bkrepo.common.artifact.constant.X_CHECKSUM_MD5
import com.tencent.bkrepo.common.artifact.constant.X_CHECKSUM_SHA256
import com.tencent.bkrepo.common.artifact.exception.ArtifactResponseException
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.STREAM_BUFFER_SIZE
import com.tencent.bkrepo.common.artifact.stream.closeQuietly
import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.common.artifact.util.http.IOExceptionUtils.isClientBroken
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageProperties
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * ArtifactResourceWriter默认实现
 */
open class DefaultArtifactResourceWriter(
    private val storageProperties: StorageProperties
) : ArtifactResourceWriter {

    @Throws(ArtifactResponseException::class)
    override fun write(resource: ArtifactResource): Throughput {
        return if (resource.containsMultiArtifact()) {
            writeMultiArtifact(resource)
        } else {
            writeSingleArtifact(resource)
        }
    }

    /**
     * 响应单个构件数据
     * 单个响应流支持range下载
     */
    private fun writeSingleArtifact(resource: ArtifactResource): Throughput {
        val request = HttpContextHolder.getRequest()
        val response = HttpContextHolder.getResponse()
        val name = resource.getSingleName()
        val range = resource.getSingleStream().range
        val cacheControl = resource.node?.metadata?.get(HttpHeaders.CACHE_CONTROL)?.toString()
            ?: resource.node?.metadata?.get(HttpHeaders.CACHE_CONTROL.toLowerCase())?.toString()
            ?: StringPool.NO_CACHE

        response.bufferSize = getBufferSize(range.length.toInt())
        response.characterEncoding = resource.characterEncoding
        response.contentType = resource.contentType ?: determineMediaType(name)
        response.status = resource.status?.value ?: resolveStatus(request)
        response.setContentLengthLong(range.length)
        response.setHeader(HttpHeaders.ACCEPT_RANGES, StringPool.BYTES)
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl)
        response.setHeader(HttpHeaders.CONTENT_RANGE, resolveContentRange(range))
        if (resource.useDisposition) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition(name))
        }

        resource.node?.let {
            response.setHeader(HttpHeaders.ETAG, resolveETag(it))
            response.setHeader(X_CHECKSUM_MD5, it.md5)
            response.setHeader(X_CHECKSUM_SHA256, it.sha256)
            response.setDateHeader(HttpHeaders.LAST_MODIFIED, resolveLastModified(it.lastModifiedDate))
        }
        return writeRangeStream(resource, request, response)
    }

    /**
     * 响应多个构件数据，将以ZipOutputStream方式输出
     * 不支持Range下载
     * 不支持E-TAG和X_CHECKSUM_MD5头
     * 下载前无法预知content-length，将以Transfer-Encoding: chunked下载，某些下载工具或者浏览器的显示进度可能存在问题
     */
    private fun writeMultiArtifact(resource: ArtifactResource): Throughput {
        val request = HttpContextHolder.getRequest()
        val response = HttpContextHolder.getResponse()
        val name = resolveMultiArtifactName(resource)

        response.bufferSize = getBufferSize(resource.getTotalSize().toInt())
        response.characterEncoding = resource.characterEncoding
        response.contentType = determineMediaType(name)
        response.status = HttpStatus.OK.value
        response.setHeader(HttpHeaders.CACHE_CONTROL, StringPool.NO_CACHE)
        if (resource.useDisposition) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition(name))
        }
        resource.node?.let {
            response.setDateHeader(HttpHeaders.LAST_MODIFIED, resolveLastModified(it.lastModifiedDate))
        }
        return writeZipStream(resource, request, response)
    }

    /**
     * 响应多个构件时解析构件名称
     */
    private fun resolveMultiArtifactName(resource: ArtifactResource): String {
        val baseName = when {
            resource.node == null -> System.currentTimeMillis().toString()
            PathUtils.isRoot(resource.node.name) -> resource.node.projectId + "-" + resource.node.repoName
            else -> resource.node.name
        }
        return "$baseName.zip"
    }

    /**
     * 解析响应状态
     */
    private fun resolveStatus(request: HttpServletRequest): Int {
        val isRangeRequest = request.getHeader(HttpHeaders.RANGE)?.isNotBlank() ?: false
        return if (isRangeRequest) HttpStatus.PARTIAL_CONTENT.value else HttpStatus.OK.value
    }

    /**
     * 解析content range
     */
    private fun resolveContentRange(range: Range): String {
        return "${StringPool.BYTES} $range"
    }

    /**
     * 解析last modified
     */
    private fun resolveLastModified(lastModifiedDate: String): Long {
        val localDateTime = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME)
        return localDateTime.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 将数据流以Range方式写入响应
     */
    private fun writeRangeStream(
        resource: ArtifactResource,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Throughput {
        val inputStream = resource.getSingleStream()
        if (request.method == HttpMethod.HEAD.name) {
            return Throughput.EMPTY
        }
        try {
            return measureThroughput {
                inputStream.rateLimit(storageProperties.response.rateLimit.toBytes()).use {
                    it.copyTo(
                        out = response.outputStream,
                        bufferSize = getBufferSize(inputStream.range.length.toInt())
                    )
                }
            }
        } catch (exception: IOException) {
            // 直接向上抛IOException经过CglibAopProxy会抛java.lang.reflect.UndeclaredThrowableException: null
            // 由于已经设置了Content-Type为application/octet-stream, spring找不到对应的Converter，导致抛
            // org.springframework.http.converter.HttpMessageNotWritableException异常，会重定向到/error页面
            // 又因为/error页面不存在，最终返回404，所以要对IOException进行包装，在上一层捕捉处理
            val message = exception.message.orEmpty()
            val status = if (isClientBroken(exception)) HttpStatus.BAD_REQUEST else HttpStatus.INTERNAL_SERVER_ERROR
            throw ArtifactResponseException(message, status)
        }
    }

    /**
     * 将数据流以ZipOutputStream方式写入响应
     */
    private fun writeZipStream(
        resource: ArtifactResource,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Throughput {
        if (request.method == HttpMethod.HEAD.name) {
            return Throughput.EMPTY
        }
        try {
            return measureThroughput {
                val zipOutput = ZipOutputStream(response.outputStream.buffered())
                zipOutput.setMethod(ZipOutputStream.DEFLATED)
                zipOutput.use {
                    resource.artifactMap.forEach { (name, inputStream) ->
                        zipOutput.putNextEntry(generateZipEntry(name, inputStream))
                        inputStream.rateLimit(storageProperties.response.rateLimit.toBytes()).use {
                            it.copyTo(
                                out = zipOutput,
                                bufferSize = getBufferSize(inputStream.range.length.toInt())
                            )
                        }
                        zipOutput.closeEntry()
                    }
                }
                resource.getTotalSize()
            }
        } catch (exception: IOException) {
            val message = exception.message.orEmpty()
            val status = if (isClientBroken(exception)) HttpStatus.BAD_REQUEST else HttpStatus.INTERNAL_SERVER_ERROR
            throw ArtifactResponseException(message, status)
        } finally {
            resource.artifactMap.values.forEach { it.closeQuietly() }
        }
    }

    /**
     * 判断MediaType
     */
    private fun determineMediaType(name: String): String {
        val extension = PathUtils.resolveExtension(name)
        return mimeMappings.get(extension) ?: MediaTypes.APPLICATION_OCTET_STREAM
    }

    /**
     * 编码Content-Disposition内容
     */
    private fun encodeDisposition(filename: String): String {
        val encodeFilename = UriUtils.encode(filename, Charsets.UTF_8)
        return CONTENT_DISPOSITION_TEMPLATE.format(encodeFilename, encodeFilename)
    }

    /**
     * 解析e-tag
     */
    private fun resolveETag(node: NodeDetail): String {
        return node.sha256!!
    }

    /**
     * 获取动态buffer size
     * @param totalSize 数据总大小
     */
    private fun getBufferSize(totalSize: Int): Int {
        val bufferSize = storageProperties.response.bufferSize.toBytes().toInt()
        if (bufferSize < 0 || totalSize < 0) {
            return STREAM_BUFFER_SIZE
        }
        return if (totalSize < bufferSize) totalSize else bufferSize
    }

    /**
     * 根据[artifactName]生成ZipEntry
     */
    private fun generateZipEntry(artifactName: String, inputStream: ArtifactInputStream): ZipEntry {
        val entry = ZipEntry(artifactName)
        entry.size = inputStream.range.length
        return entry
    }

    companion object {
        private val mimeMappings = MimeMappings(MimeMappings.DEFAULT).apply {
            add("yaml", MediaTypes.APPLICATION_YAML)
            add("tgz", MediaTypes.APPLICATION_TGZ)
            add("ico", MediaTypes.APPLICATION_ICO)
        }
    }
}
