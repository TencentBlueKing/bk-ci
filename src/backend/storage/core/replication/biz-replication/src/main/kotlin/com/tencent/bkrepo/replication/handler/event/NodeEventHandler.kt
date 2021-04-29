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

package com.tencent.bkrepo.replication.handler.event

import com.tencent.bkrepo.replication.exception.WaitPreorderNodeFailedException
import com.tencent.bkrepo.replication.job.ReplicationContext
import com.tencent.bkrepo.replication.message.node.NodeCopiedMessage
import com.tencent.bkrepo.replication.message.node.NodeCreatedMessage
import com.tencent.bkrepo.replication.message.node.NodeDeletedMessage
import com.tencent.bkrepo.replication.message.node.NodeMovedMessage
import com.tencent.bkrepo.replication.message.node.NodeRenamedMessage
import com.tencent.bkrepo.replication.message.node.NodeUpdatedMessage
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * handler node message and replicate
 * include create ,copy ,rename,move
 */
@Component
class NodeEventHandler : BaseEventHandler() {

    @Async
    @EventListener(NodeCreatedMessage::class)
    fun handle(message: NodeCreatedMessage) {
        with(message.request) {
            forEachRelativeTask(projectId, repoName) {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                val context = ReplicationContext(it)
                val repoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: return@forEachRelativeTask
                context.currentRepoDetail = repoDetail
                logger.info("start to handle create event [$projectId,$repoName,$fullPath]")
                this.copy(
                    projectId = remoteProjectId,
                    repoName = remoteRepoName
                ).apply { replicationService.replicaNodeCreateRequest(context, this) }
            }
        }
    }

    @Async
    @EventListener(NodeRenamedMessage::class)
    fun handle(message: NodeRenamedMessage) {
        with(message.request) {
            forEachRelativeTask(projectId, repoName) {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                val context = ReplicationContext(it)
                val repoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: return@forEachRelativeTask
                context.currentRepoDetail = repoDetail

                logger.info("start to handle rename event [$projectId,$repoName,$fullPath]")
                val result = waitForPreorderNode(context, remoteProjectId, remoteRepoName, fullPath)
                if (!result) throw WaitPreorderNodeFailedException("rename time out")
                this.copy(
                    projectId = remoteProjectId,
                    repoName = remoteRepoName
                ).apply { replicationService.replicaNodeRenameRequest(context, this) }
            }
        }
    }

    @Async
    @EventListener(NodeUpdatedMessage::class)
    fun handle(message: NodeUpdatedMessage) {
        with(message.request) {
            forEachRelativeTask(projectId, repoName) {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                val context = ReplicationContext(it)
                val repoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: return@forEachRelativeTask
                context.currentRepoDetail = repoDetail

                logger.info("start to handle update event [$projectId,$repoName,$fullPath]")
                val result = waitForPreorderNode(context, remoteProjectId, remoteRepoName, fullPath)
                if (!result) throw WaitPreorderNodeFailedException("update time out")
                this.copy(
                    projectId = remoteProjectId,
                    repoName = remoteRepoName
                ).apply { replicationService.replicaNodeUpdateRequest(context, this) }
            }
        }
    }

    @Async
    @EventListener(NodeCopiedMessage::class)
    fun handle(message: NodeCopiedMessage) {
        with(message.request) {
            forEachRelativeTask(projectId, repoName) {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                val context = ReplicationContext(it)
                val repoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: return@forEachRelativeTask
                context.currentRepoDetail = repoDetail

                logger.info("start to handle copy event [$projectId,$repoName,$srcFullPath]")
                val result = waitForPreorderNode(context, remoteProjectId, remoteRepoName, srcFullPath)
                if (!result) throw WaitPreorderNodeFailedException("copy time out")
                this.copy(
                    srcProjectId = remoteProjectId,
                    srcRepoName = remoteRepoName
                ).apply { replicationService.replicaNodeCopyRequest(context, this) }
            }
        }
    }

    @Async
    @EventListener(NodeMovedMessage::class)
    fun handle(message: NodeMovedMessage) {
        with(message.request) {
            forEachRelativeTask(projectId, repoName) {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                val context = ReplicationContext(it)
                val repoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: return@forEachRelativeTask
                context.currentRepoDetail = repoDetail

                logger.info("start to handle move event [$projectId,$repoName,$fullPath]")
                val result = waitForPreorderNode(context, remoteProjectId, remoteRepoName, fullPath)
                if (!result) throw WaitPreorderNodeFailedException("move time out")
                this.copy(
                    srcProjectId = remoteProjectId,
                    srcRepoName = remoteRepoName
                ).apply { replicationService.replicaNodeMoveRequest(context, this) }
            }
        }
    }

    @Async
    @EventListener(NodeDeletedMessage::class)
    fun handle(message: NodeDeletedMessage) {
        with(message.request) {
            forEachRelativeTask(projectId, repoName) {
                val remoteProjectId = getRemoteProjectId(it, projectId)
                val remoteRepoName = getRemoteRepoName(it, repoName)
                val context = ReplicationContext(it)
                val repoDetail = getRepoDetail(projectId, repoName, remoteRepoName) ?: return@forEachRelativeTask
                context.currentRepoDetail = repoDetail

                logger.info("start to handle delete event [$projectId,$repoName,$fullPath]")
                val result = waitForPreorderNode(context, remoteProjectId, remoteRepoName, fullPath)
                if (!result) throw WaitPreorderNodeFailedException("delete time out")
                this.copy(
                    projectId = remoteProjectId,
                    repoName = remoteRepoName
                ).apply { replicationService.replicaNodeDeleteRequest(context, this) }
            }
        }
    }

    // wait max 120s for pre order node
    private fun waitForPreorderNode(
        context: ReplicationContext,
        projectId: String,
        repoName: String,
        fullPath: String
    ): Boolean {
        var retryCount = WAIT_RETRY_COUNT
        while (retryCount > 0) {
            val result = replicationService.checkNodeExistRequest(context, projectId, repoName, fullPath)
            if (result) return true
            retryCount -= 1
            TimeUnit.SECONDS.sleep(1)
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeEventHandler::class.java)
        private const val WAIT_RETRY_COUNT = 120
    }
}
