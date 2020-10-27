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

package com.tencent.bkrepo.replication.handler.event

import com.tencent.bkrepo.common.stream.message.node.NodeCopiedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeCreatedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeDeletedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeMovedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeRenamedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeUpdatedMessage
import com.tencent.bkrepo.replication.job.ReplicationContext
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * handler node message and replicate
 * include create ,copy ,rename,move
 */
@Component
class NodeEventHandler : AbstractEventHandler() {

    @EventListener(NodeCreatedMessage::class)
    fun handle(message: NodeCreatedMessage) {
        with(message.request) {
            getRelativeTaskList(projectId, repoName).forEach {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                var context = ReplicationContext(it)

                context.currentRepoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: run {
                    logger.warn("found no repo detail [$projectId, $repoName]")
                    return
                }
                this.copy(
                    projectId = remoteProjectId,
                    repoName = remoteRepoName
                ).apply { replicationService.replicaNodeCreateRequest(context, this) }
            }
        }
    }

    @EventListener(NodeRenamedMessage::class)
    fun handle(message: NodeRenamedMessage) {
        with(message.request) {
            getRelativeTaskList(projectId, repoName).forEach {
                val context = ReplicationContext(it)
                this.copy(
                    projectId = getRemoteProjectId(it, projectId),
                    repoName = getRemoteRepoName(it, repoName)
                ).apply { replicationService.replicaNodeRenameRequest(context, this) }
            }
        }
    }

    @EventListener(NodeUpdatedMessage::class)
    fun handle(message: NodeUpdatedMessage) {
        with(message.request) {
            getRelativeTaskList(projectId, repoName).forEach {
                val context = ReplicationContext(it)
                this.copy(
                    projectId = getRemoteProjectId(it, projectId),
                    repoName = getRemoteRepoName(it, repoName)
                ).apply { replicationService.replicaNodeUpdateRequest(context, this) }
            }
        }
    }

    @EventListener(NodeCopiedMessage::class)
    fun handle(message: NodeCopiedMessage) {
        with(message.request) {
            getRelativeTaskList(projectId, repoName).forEach {
                val context = ReplicationContext(it)
                this.copy(
                    srcProjectId = getRemoteProjectId(it, projectId),
                    srcRepoName = getRemoteRepoName(it, repoName)
                ).apply { replicationService.replicaNodeCopyRequest(context, this) }
            }
        }
    }

    @EventListener(NodeMovedMessage::class)
    fun handle(message: NodeMovedMessage) {
        with(message.request) {
            getRelativeTaskList(projectId, repoName).forEach {
                val context = ReplicationContext(it)
                context.currentProjectDetail
                this.copy(
                    srcProjectId = getRemoteProjectId(it, projectId),
                    srcRepoName = getRemoteRepoName(it, repoName)
                ).apply { replicationService.replicaNodeMoveRequest(context, this) }
            }
        }
    }

    @EventListener(NodeDeletedMessage::class)
    fun handle(message: NodeDeletedMessage) {
        with(message.request) {
            getRelativeTaskList(projectId, repoName).forEach {
                val context = ReplicationContext(it)
                this.copy(
                    projectId = getRemoteProjectId(it, projectId),
                    repoName = getRemoteRepoName(it, repoName)
                ).apply { replicationService.replicaNodeDeleteRequest(context, this) }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeEventHandler::class.java)
    }
}
