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

package com.tencent.bkrepo.common.artifact.repository.local

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_OCTET_STREAM_MD5
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_OCTET_STREAM_SHA256
import com.tencent.bkrepo.common.artifact.event.ArtifactUploadedEvent
import com.tencent.bkrepo.common.artifact.exception.ArtifactException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.api.DownloadStatisticsClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.download.service.DownloadStatisticsAddRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpHeaders
import java.util.concurrent.Executor
import java.util.regex.Pattern
import javax.annotation.Resource

abstract class LocalRepository : AbstractArtifactRepository() {

    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var publisher: ApplicationEventPublisher

    @Autowired
    lateinit var downloadStatisticsClient: DownloadStatisticsClient

    @Resource
    private lateinit var taskAsyncExecutor: Executor

    override fun onUpload(context: ArtifactUploadContext) {
        val nodeCreateRequest = getNodeCreateRequest(context)
        storageService.store(nodeCreateRequest.sha256!!, context.getArtifactFile(), context.storageCredentials)
        nodeClient.create(nodeCreateRequest)
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            val artifactUri = determineArtifactUri(this)
            val artifactName = determineArtifactName(this)
            val node = nodeClient.detail(repositoryInfo.projectId, repositoryInfo.name, artifactUri).data ?: return null
            node.takeIf { !it.folder } ?: return null
            val range = resolveRange(context, node.size)
            val inputStream = storageService.load(node.sha256!!, range, storageCredentials) ?: return null
            return ArtifactResource(inputStream, artifactName, node)
        }
    }

    open fun countDownloads(context: ArtifactDownloadContext) {
        taskAsyncExecutor.execute {
            val artifactInfo = context.artifactInfo
            downloadStatisticsClient.add(
                DownloadStatisticsAddRequest(
                    artifactInfo.projectId,
                    artifactInfo.repoName,
                    artifactInfo.artifact,
                    artifactInfo.version
                )
            )
        }
    }

    /**
     * 获取节点fullPath
     */
    open fun determineArtifactUri(context: ArtifactDownloadContext): String {
        return context.artifactInfo.artifactUri
    }

    /**
     * 获取节点创建请求
     */
    open fun getNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val artifactInfo = context.artifactInfo
        val repositoryInfo = context.repositoryInfo
        val artifactFile = context.getArtifactFile()
        val sha256 = context.contextAttributes[ATTRIBUTE_OCTET_STREAM_SHA256] as String
        val md5 = context.contextAttributes[ATTRIBUTE_OCTET_STREAM_MD5] as String

        return NodeCreateRequest(
            projectId = repositoryInfo.projectId,
            repoName = repositoryInfo.name,
            folder = false,
            fullPath = artifactInfo.artifactUri,
            size = artifactFile.getSize(),
            sha256 = sha256,
            md5 = md5,
            operator = context.userId
        )
    }

    override fun onUploadSuccess(context: ArtifactUploadContext) {
        super.onUploadSuccess(context)
        publisher.publishEvent(ArtifactUploadedEvent(context))
    }

    override fun onDownloadSuccess(context: ArtifactDownloadContext) {
        super.onDownloadSuccess(context)
        countDownloads(context)
    }

    open fun resolveRange(context: ArtifactDownloadContext, total: Long): Range {
        val request = context.request
        val rangeHeader = request.getHeader(HttpHeaders.RANGE)?.trim()
        try {
            if (rangeHeader.isNullOrEmpty()) return Range.full(total)
            val matcher = RANGE_HEADER.matcher(rangeHeader)
            require(matcher.matches()) { "Invalid range header: $rangeHeader" }
            require(matcher.groupCount() >= 1) { "Invalid range header: $rangeHeader" }
            return if (matcher.group(1).isNullOrEmpty()) {
                val start = total - matcher.group(2).toLong()
                val end = total - 1
                Range(start, end, total)
            } else {
                val start = matcher.group(1).toLong()
                val end = if (matcher.group(2).isNullOrEmpty()) total - 1 else matcher.group(2).toLong()
                Range(start, end, total)
            }
        } catch (exception: IllegalArgumentException) {
            logger.warn("Failed to parse range header: $rangeHeader, message: ${exception.message}")
            throw ArtifactException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
        }
    }

    companion object {
        private val RANGE_HEADER = Pattern.compile("bytes=(\\d+)?-(\\d+)?")
        private val logger = LoggerFactory.getLogger(LocalRepository::class.java)
    }
}
