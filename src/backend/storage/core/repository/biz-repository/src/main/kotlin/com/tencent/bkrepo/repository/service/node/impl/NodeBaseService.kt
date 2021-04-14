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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.listener.event.node.NodeCreatedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeUpdatedEvent
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.service.file.FileReferenceService
import com.tencent.bkrepo.repository.service.node.NodeBaseOperation
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import com.tencent.bkrepo.repository.util.MetadataUtils
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 节点基础服务，实现了CRUD基本操作
 */
abstract class NodeBaseService(
    open val nodeDao: NodeDao,
    open val repositoryDao: RepositoryDao,
    open val fileReferenceService: FileReferenceService,
    open val storageCredentialService: StorageCredentialService,
    open val storageService: StorageService,
    open val repositoryProperties: RepositoryProperties
) : NodeService, NodeBaseOperation {

    override fun getNodeDetail(artifact: ArtifactInfo, repoType: String?): NodeDetail? {
        with(artifact) {
            val node = nodeDao.findNode(projectId, repoName, getArtifactFullPath())
            return convertToDetail(node)
        }
    }

    override fun listNode(artifact: ArtifactInfo, option: NodeListOption): List<NodeInfo> {
        with(artifact) {
            val query = NodeQueryHelper.nodeListQuery(projectId, repoName, getArtifactFullPath(), option)
            if (nodeDao.count(query) > repositoryProperties.listCountLimit) {
                throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
            }
            return nodeDao.find(query).map { convert(it)!! }
        }
    }

    override fun listNodePage(artifact: ArtifactInfo, option: NodeListOption): Page<NodeInfo> {
        with(artifact) {
            val pageNumber = option.pageNumber
            val pageSize = option.pageSize
            Preconditions.checkArgument(pageNumber >= 0, "pageNumber")
            Preconditions.checkArgument(pageSize >= 0 && pageSize <= repositoryProperties.listCountLimit, "pageSize")
            val query = NodeQueryHelper.nodeListQuery(projectId, repoName, getArtifactFullPath(), option)
            val totalRecords = nodeDao.count(query)
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val records = nodeDao.find(query.with(pageRequest)).map { convert(it)!! }

            return Pages.ofResponse(pageRequest, totalRecords, records)
        }
    }

    override fun checkExist(artifact: ArtifactInfo): Boolean {
        return nodeDao.exists(artifact.projectId, artifact.repoName, artifact.getArtifactFullPath())
    }

    override fun listExistFullPath(projectId: String, repoName: String, fullPathList: List<String>): List<String> {
        val queryList = fullPathList.map { PathUtils.normalizeFullPath(it) }.filter { !PathUtils.isRoot(it) }
        val nodeQuery = NodeQueryHelper.nodeQuery(projectId, repoName, queryList)
        return nodeDao.find(nodeQuery).map { it.fullPath }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun createNode(createRequest: NodeCreateRequest): NodeDetail {
        with(createRequest) {
            val fullPath = PathUtils.normalizeFullPath(fullPath)
            Preconditions.checkArgument(!PathUtils.isRoot(fullPath), this::fullPath.name)
            Preconditions.checkArgument(folder || !sha256.isNullOrBlank(), this::sha256.name)
            Preconditions.checkArgument(folder || !md5.isNullOrBlank(), this::md5.name)
            // 路径唯一性校验
            checkConflict(createRequest, fullPath)
            // 判断父目录是否存在，不存在先创建
            mkdirs(projectId, repoName, PathUtils.resolveParent(fullPath), operator)
            // 创建节点
            val node = TNode(
                projectId = projectId,
                repoName = repoName,
                path = PathUtils.resolveParent(fullPath),
                name = PathUtils.resolveName(fullPath),
                fullPath = fullPath,
                folder = folder,
                expireDate = if (folder) null else parseExpireDate(expires),
                size = if (folder) 0 else size ?: 0,
                sha256 = if (folder) null else sha256,
                md5 = if (folder) null else md5,
                metadata = MetadataUtils.fromMap(metadata),
                createdBy = createdBy ?: operator,
                createdDate = createdDate ?: LocalDateTime.now(),
                lastModifiedBy = createdBy ?: operator,
                lastModifiedDate = lastModifiedDate ?: LocalDateTime.now()
            )
            doCreate(node)
            publishEvent(NodeCreatedEvent(this))
            logger.info("Create node[/$projectId/$repoName$fullPath], sha256[$sha256] success.")

            return convertToDetail(node)!!
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun updateNode(updateRequest: NodeUpdateRequest) {
        with(updateRequest) {
            val fullPath = PathUtils.normalizeFullPath(fullPath)
            val node = nodeDao.findNode(projectId, repoName, fullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            val selfQuery = NodeQueryHelper.nodeQuery(projectId, repoName, node.fullPath)
            val selfUpdate = NodeQueryHelper.nodeExpireDateUpdate(parseExpireDate(expires), operator)
            nodeDao.updateFirst(selfQuery, selfUpdate)
            publishEvent(NodeUpdatedEvent(this))
            logger.info("Update node [$this] success.")
        }
    }

    fun doCreate(node: TNode, repository: TRepository? = null): TNode {
        try {
            nodeDao.insert(node)
            if (!node.folder) {
                fileReferenceService.increment(node, repository)
            }
        } catch (exception: DuplicateKeyException) {
            logger.warn("Insert node[$node] error: [${exception.message}]")
        }

        return node
    }

    /**
     * 递归创建目录
     */
    fun mkdirs(projectId: String, repoName: String, path: String, createdBy: String) {
        // 格式化
        val fullPath = PathUtils.toFullPath(path)
        if (!nodeDao.exists(projectId, repoName, fullPath)) {
            val parentPath = PathUtils.resolveParent(fullPath)
            val name = PathUtils.resolveName(fullPath)
            mkdirs(projectId, repoName, parentPath, createdBy)
            val node = TNode(
                folder = true,
                path = parentPath,
                name = name,
                fullPath = PathUtils.combineFullPath(parentPath, name),
                size = 0,
                expireDate = null,
                metadata = mutableListOf(),
                projectId = projectId,
                repoName = repoName,
                createdBy = createdBy,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = createdBy,
                lastModifiedDate = LocalDateTime.now()
            )
            doCreate(node)
        }
    }

    private fun checkConflict(createRequest: NodeCreateRequest, fullPath: String) {
        with(createRequest) {
            nodeDao.findNode(projectId, repoName, fullPath)?.let {
                if (!overwrite) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, fullPath)
                } else if (it.folder || this.folder) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, fullPath)
                } else {
                    deleteByPath(projectId, repoName, fullPath, operator)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeServiceImpl::class.java)

        private fun convert(tNode: TNode?): NodeInfo? {
            return tNode?.let {
                val metadata = MetadataUtils.toMap(it.metadata)
                NodeInfo(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    projectId = it.projectId,
                    repoName = it.repoName,
                    folder = it.folder,
                    path = it.path,
                    name = it.name,
                    fullPath = it.fullPath,
                    size = it.size,
                    sha256 = it.sha256,
                    md5 = it.md5,
                    metadata = metadata
                )
            }
        }

        private fun convertToDetail(tNode: TNode?): NodeDetail? {
            return convert(tNode)?.let { NodeDetail(it) }
        }

        /**
         * 根据有效天数，计算到期时间
         */
        private fun parseExpireDate(expireDays: Long?): LocalDateTime? {
            return expireDays?.takeIf { it > 0 }?.run { LocalDateTime.now().plusDays(this) }
        }
    }
}
