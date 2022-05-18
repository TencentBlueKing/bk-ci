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

package com.tencent.bkrepo.common.artifact.event.listener

import com.tencent.bkrepo.common.artifact.event.ArtifactDownloadedEvent
import com.tencent.bkrepo.common.artifact.event.node.NodeDownloadedEvent
import com.tencent.bkrepo.common.operate.api.OperateLogService
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import org.springframework.context.event.EventListener

class ArtifactDownloadListener(
    private val nodeClient: NodeClient,
    private val operateLogService: OperateLogService
) {

    @EventListener(ArtifactDownloadedEvent::class)
    fun listen(event: ArtifactDownloadedEvent) {
        val projectId = event.context.projectId
        val repoName = event.context.repoName
        val fullPath = event.context.artifactInfo.getArtifactFullPath()
        val userId = event.context.userId
        val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        if (node == null) {
            val downloadedEvent = NodeDownloadedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = userId,
                data = emptyMap()
            )
            operateLogService.saveEventAsync(downloadedEvent, HttpContextHolder.getClientAddress())
        } else if (node.folder) {
            val nodeList =
                nodeClient.listNode(projectId, repoName, node.path, includeFolder = false, deep = true).data!!
            val eventList = nodeList.map { buildDownloadEvent(NodeDetail(it), userId) }
            operateLogService.saveEventsAsync(eventList, HttpContextHolder.getClientAddress())
        } else {
            val downloadedEvent = buildDownloadEvent(node, userId)
            operateLogService.saveEventAsync(downloadedEvent, HttpContextHolder.getClientAddress())
        }
    }

    private fun buildDownloadEvent(
        node: NodeDetail,
        userId: String
    ): NodeDownloadedEvent {
        val data = node.metadata.toMutableMap()
        data[MD5] = node.md5 ?: ""
        data[SHA256] = node.sha256 ?: ""
        return NodeDownloadedEvent(
            projectId = node.projectId,
            repoName = node.repoName,
            resourceKey = node.fullPath,
            userId = userId,
            data = data
        )
    }

    companion object {
        private const val MD5 = "md5"
        private const val SHA256 = "sha256"
    }
}
