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

package com.tencent.bkrepo.repository.service.node.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.pojo.node.NodeDeleteResult
import com.tencent.bkrepo.repository.pojo.node.NodeDeletedPoint
import com.tencent.bkrepo.repository.pojo.node.NodeRestoreOption
import com.tencent.bkrepo.repository.pojo.node.NodeRestoreResult
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.service.file.FileReferenceService
import com.tencent.bkrepo.repository.service.repo.QuotaService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 节点服务实现类
 */
@Service
class NodeServiceImpl(
    override val nodeDao: NodeDao,
    override val repositoryDao: RepositoryDao,
    override val fileReferenceService: FileReferenceService,
    override val storageCredentialService: StorageCredentialService,
    override val storageService: StorageService,
    override val quotaService: QuotaService,
    override val repositoryProperties: RepositoryProperties
) : NodeBaseService(
    nodeDao,
    repositoryDao,
    fileReferenceService,
    storageCredentialService,
    storageService,
    quotaService,
    repositoryProperties
) {

    override fun computeSize(artifact: ArtifactInfo): NodeSizeInfo {
        return NodeStatsSupport(this).computeSize(artifact)
    }

    override fun aggregateComputeSize(criteria: Criteria): Long {
        return NodeStatsSupport(this).aggregateComputeSize(criteria)
    }

    override fun countFileNode(artifact: ArtifactInfo): Long {
        return NodeStatsSupport(this).countFileNode(artifact)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteNode(deleteRequest: NodeDeleteRequest): NodeDeleteResult {
        return NodeDeleteSupport(this).deleteNode(deleteRequest)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteByPath(
        projectId: String,
        repoName: String,
        fullPath: String,
        operator: String
    ): NodeDeleteResult {
        return NodeDeleteSupport(this).deleteByPath(projectId, repoName, fullPath, operator)
    }

    override fun deleteBeforeDate(
        projectId: String,
        repoName: String,
        date: LocalDateTime,
        operator: String
    ): NodeDeleteResult {
        return NodeDeleteSupport(this).deleteBeforeDate(projectId, repoName, date, operator)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun moveNode(moveRequest: NodeMoveCopyRequest) {
        NodeMoveCopySupport(this).moveNode(moveRequest)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun copyNode(copyRequest: NodeMoveCopyRequest) {
        NodeMoveCopySupport(this).copyNode(copyRequest)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun renameNode(renameRequest: NodeRenameRequest) {
        NodeRenameSupport(this).renameNode(renameRequest)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun restoreNode(artifact: ArtifactInfo, nodeRestoreOption: NodeRestoreOption): NodeRestoreResult {
        return NodeRestoreSupport(this).restoreNode(artifact, nodeRestoreOption)
    }

    override fun listDeletedPoint(artifact: ArtifactInfo): List<NodeDeletedPoint> {
        return NodeRestoreSupport(this).listDeletedPoint(artifact)
    }
}
