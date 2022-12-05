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

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.event.metadata.MetadataDeletedEvent
import com.tencent.bkrepo.common.artifact.event.metadata.MetadataSavedEvent
import com.tencent.bkrepo.common.artifact.event.node.NodeCopiedEvent
import com.tencent.bkrepo.common.artifact.event.node.NodeCreatedEvent
import com.tencent.bkrepo.common.artifact.event.node.NodeDeletedEvent
import com.tencent.bkrepo.common.artifact.event.node.NodeMovedEvent
import com.tencent.bkrepo.common.artifact.event.node.NodeRenamedEvent
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest

/**
 * 节点事件构造类
 */
object NodeEventFactory {

    /**
     * 节点创建事件
     */
    fun buildCreatedEvent(node: TNode): NodeCreatedEvent {
        with(node) {
            return NodeCreatedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = node.createdBy
            )
        }
    }

    /**
     * 节点删除事件
     */
    fun buildDeletedEvent(
        projectId: String,
        repoName: String,
        fullPath: String,
        userId: String
    ): NodeDeletedEvent {
        return NodeDeletedEvent(
            projectId = projectId,
            repoName = repoName,
            resourceKey = fullPath,
            userId = userId
        )
    }

    /**
     * 节点重命名事件
     */
    fun buildRenamedEvent(request: NodeRenameRequest): NodeRenamedEvent {
        with(request) {
            return NodeRenamedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = operator,
                newFullPath = newFullPath
            )
        }
    }

    /**
     * 节点移动事件
     */
    fun buildMovedEvent(request: NodeMoveCopyRequest): NodeMovedEvent {
        with(request) {
            return NodeMovedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = operator,
                dstProjectId = destProjectId ?: projectId,
                dstRepoName = destRepoName ?: repoName,
                dstFullPath = destFullPath
            )
        }
    }

    /**
     * 节点拷贝事件
     */
    fun buildCopiedEvent(request: NodeMoveCopyRequest): NodeCopiedEvent {
        with(request) {
            return NodeCopiedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = operator,
                dstProjectId = destProjectId ?: projectId,
                dstRepoName = destRepoName ?: repoName,
                dstFullPath = destFullPath
            )
        }
    }

    /**
     * 元数据保存事件
     */
    fun buildMetadataSavedEvent(request: MetadataSaveRequest): MetadataSavedEvent {
        with(request) {
            return MetadataSavedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = operator,
                metadata = metadata.orEmpty()
            )
        }
    }

    /**
     * 元数据删除事件
     */
    fun buildMetadataDeletedEvent(request: MetadataDeleteRequest): MetadataDeletedEvent {
        with(request) {
            return MetadataDeletedEvent(
                projectId = projectId,
                repoName = repoName,
                resourceKey = fullPath,
                userId = operator,
                keys = keyList
            )
        }
    }
}
